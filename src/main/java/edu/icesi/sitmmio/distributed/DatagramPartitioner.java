package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ColumnResolver;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.CsvFormats;
import edu.icesi.sitmmio.csv.DataCleaner;
import edu.icesi.sitmmio.model.GpsPoint;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class DatagramPartitioner {
    private final ColumnResolver columnResolver;
    private final DataCleaner dataCleaner;

    public DatagramPartitioner() {
        this(new ColumnResolver(), new DataCleaner());
    }

    DatagramPartitioner(ColumnResolver columnResolver, DataCleaner dataCleaner) {
        this.columnResolver = columnResolver;
        this.dataCleaner = dataCleaner;
    }

    public PartitioningSummary partition(
            Path datagramsPath,
            Set<String> activeRoutes,
            CsvColumnConfig columnConfig,
            Path runDirectory,
            int partitionCount
    ) throws IOException {
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("Partition count must be greater than zero.");
        }
        CsvColumnConfig config = columnConfig == null
                ? new CsvColumnConfig(null, null, null, null, null, null)
                : columnConfig;

        Path partitionsDirectory = runDirectory.resolve("partitions");
        Path partialResultsDirectory = runDirectory.resolve("partial-results");
        Files.createDirectories(partitionsDirectory);
        Files.createDirectories(partialResultsDirectory);

        List<PartitionWorkItem> workItems = createWorkItems(
                partitionsDirectory,
                partialResultsDirectory,
                partitionCount);
        List<CSVPrinter> printers = openPrinters(workItems);
        try {
            try (Reader reader = Files.newBufferedReader(datagramsPath)) {
                if (config.datagramsHasHeader()) {
                    return partitionHeaderDatagrams(reader, activeRoutes, config, printers, workItems);
                }
                return partitionHeaderlessDatagrams(reader, activeRoutes, config, printers, workItems);
            }
        } finally {
            closePrinters(printers);
        }
    }

    private PartitioningSummary partitionHeaderDatagrams(
            Reader reader,
            Set<String> activeRoutes,
            CsvColumnConfig config,
            List<CSVPrinter> printers,
            List<PartitionWorkItem> workItems
    ) throws IOException {
        try (CSVParser parser = CsvFormats.inputWithHeader().parse(reader)) {
            Set<String> headers = parser.getHeaderMap().keySet();
            ResolvedColumns columns = new ResolvedColumns(
                    columnResolver.resolve(headers, config.routeColumn(), List.of("route", "route_id", "line_id",
                            "lineid"), "route"),
                    columnResolver.resolve(headers, config.busColumn(), List.of("bus", "bus_id", "vehicle_id"),
                            "bus"),
                    columnResolver.resolve(headers, config.timestampColumn(), List.of("timestamp", "time",
                            "datetime", "gps_time"), "timestamp"),
                    columnResolver.resolve(headers, config.latitudeColumn(), List.of("latitude", "lat", "latitud"),
                            "latitude"),
                    columnResolver.resolve(headers, config.longitudeColumn(), List.of("longitude", "lon", "lng",
                            "longitud"), "longitude"));

            Counts counts = new Counts();
            for (CSVRecord record : parser) {
                counts.rawRows++;
                cleanAndWrite(
                        record.get(columns.routeColumn),
                        record.get(columns.busColumn),
                        record.get(columns.timestampColumn),
                        record.get(columns.latitudeColumn),
                        record.get(columns.longitudeColumn),
                        activeRoutes,
                        config.coordinateScale(),
                        printers,
                        counts);
            }
            return counts.toSummary(workItems);
        }
    }

    private PartitioningSummary partitionHeaderlessDatagrams(
            Reader reader,
            Set<String> activeRoutes,
            CsvColumnConfig config,
            List<CSVPrinter> printers,
            List<PartitionWorkItem> workItems
    ) throws IOException {
        RequiredIndexes indexes = RequiredIndexes.from(config);
        try (CSVParser parser = CsvFormats.inputWithoutHeader().parse(reader)) {
            Counts counts = new Counts();
            for (CSVRecord record : parser) {
                counts.rawRows++;
                if (record.size() <= indexes.maxIndex) {
                    counts.skippedRows++;
                    continue;
                }
                cleanAndWrite(
                        record.get(indexes.routeIndex),
                        record.get(indexes.busIndex),
                        record.get(indexes.timestampIndex),
                        record.get(indexes.latitudeIndex),
                        record.get(indexes.longitudeIndex),
                        activeRoutes,
                        config.coordinateScale(),
                        printers,
                        counts);
            }
            return counts.toSummary(workItems);
        }
    }

    private void cleanAndWrite(
            String routeId,
            String busId,
            String timestamp,
            String latitude,
            String longitude,
            Set<String> activeRoutes,
            double coordinateScale,
            List<CSVPrinter> printers,
            Counts counts
    ) throws IOException {
        int before = counts.cleanedRows;
        dataCleaner.cleanGpsPoint(
                routeId,
                busId,
                timestamp,
                latitude,
                longitude,
                activeRoutes,
                coordinateScale).ifPresent(point -> writePoint(printers, counts, point));
        if (counts.cleanedRows == before) {
            counts.skippedRows++;
        }
    }

    private static void writePoint(List<CSVPrinter> printers, Counts counts, GpsPoint point) {
        try {
            int partitionId = partitionId(point, printers.size());
            CleanedPartitionCsv.print(printers.get(partitionId), point);
            counts.cleanedRows++;
            counts.months.add(YearMonth.from(point.timestamp().atZone(ZoneOffset.UTC)));
        } catch (IOException exception) {
            throw new PartitionWriteException(exception);
        }
    }

    private static int partitionId(GpsPoint point, int partitionCount) {
        String key = point.routeId() + "\u0000" + point.busId();
        return Math.floorMod(key.hashCode(), partitionCount);
    }

    private static List<PartitionWorkItem> createWorkItems(
            Path partitionsDirectory,
            Path partialResultsDirectory,
            int partitionCount
    ) {
        List<PartitionWorkItem> workItems = new ArrayList<>();
        for (int index = 0; index < partitionCount; index++) {
            String suffix = String.format("%05d", index);
            workItems.add(new PartitionWorkItem(
                    index,
                    partitionsDirectory.resolve("partition-" + suffix + ".csv"),
                    partialResultsDirectory.resolve("partial-" + suffix + ".csv")));
        }
        return List.copyOf(workItems);
    }

    private static List<CSVPrinter> openPrinters(List<PartitionWorkItem> workItems) throws IOException {
        List<CSVPrinter> printers = new ArrayList<>();
        try {
            for (PartitionWorkItem workItem : workItems) {
                printers.add(CleanedPartitionCsv.openWriter(workItem.partitionPath()));
            }
            return printers;
        } catch (IOException exception) {
            closePrinters(printers);
            throw exception;
        }
    }

    private static void closePrinters(List<CSVPrinter> printers) throws IOException {
        IOException failure = null;
        for (CSVPrinter printer : printers) {
            try {
                printer.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static final class ResolvedColumns {
        private final String routeColumn;
        private final String busColumn;
        private final String timestampColumn;
        private final String latitudeColumn;
        private final String longitudeColumn;

        private ResolvedColumns(
                String routeColumn,
                String busColumn,
                String timestampColumn,
                String latitudeColumn,
                String longitudeColumn
        ) {
            this.routeColumn = routeColumn;
            this.busColumn = busColumn;
            this.timestampColumn = timestampColumn;
            this.latitudeColumn = latitudeColumn;
            this.longitudeColumn = longitudeColumn;
        }
    }

    private static final class RequiredIndexes {
        private final int routeIndex;
        private final int busIndex;
        private final int timestampIndex;
        private final int latitudeIndex;
        private final int longitudeIndex;
        private final int maxIndex;

        private RequiredIndexes(
                int routeIndex,
                int busIndex,
                int timestampIndex,
                int latitudeIndex,
                int longitudeIndex
        ) {
            this.routeIndex = routeIndex;
            this.busIndex = busIndex;
            this.timestampIndex = timestampIndex;
            this.latitudeIndex = latitudeIndex;
            this.longitudeIndex = longitudeIndex;
            this.maxIndex = Math.max(routeIndex,
                    Math.max(busIndex, Math.max(timestampIndex, Math.max(latitudeIndex, longitudeIndex))));
        }

        private static RequiredIndexes from(CsvColumnConfig config) {
            return new RequiredIndexes(
                    requiredIndex(config.routeIndex(), "--route-index"),
                    requiredIndex(config.busIndex(), "--bus-index"),
                    requiredIndex(config.timestampIndex(), "--timestamp-index"),
                    requiredIndex(config.latitudeIndex(), "--latitude-index"),
                    requiredIndex(config.longitudeIndex(), "--longitude-index"));
        }

        private static int requiredIndex(Integer value, String optionName) {
            if (value == null) {
                throw new IllegalArgumentException(optionName
                        + " is required when --datagrams-has-header is false.");
            }
            return value;
        }
    }

    private static final class Counts {
        private long rawRows;
        private int cleanedRows;
        private long skippedRows;
        private final Set<YearMonth> months = new TreeSet<>();

        private PartitioningSummary toSummary(List<PartitionWorkItem> workItems) {
            return new PartitioningSummary(rawRows, cleanedRows, skippedRows, months, workItems);
        }
    }

    private static final class PartitionWriteException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private PartitionWriteException(IOException cause) {
            super(cause);
        }
    }
}
