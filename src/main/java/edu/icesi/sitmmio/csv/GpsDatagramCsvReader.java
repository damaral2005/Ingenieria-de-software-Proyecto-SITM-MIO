package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GpsDatagramCsvReader {
    private final ColumnResolver columnResolver;
    private final DataCleaner dataCleaner;

    public GpsDatagramCsvReader() {
        this(new ColumnResolver(), new DataCleaner());
    }

    GpsDatagramCsvReader(ColumnResolver columnResolver, DataCleaner dataCleaner) {
        this.columnResolver = columnResolver;
        this.dataCleaner = dataCleaner;
    }

    public List<GpsPoint> read(Path path, Set<String> activeRoutes, CsvColumnConfig columnConfig) throws IOException {
        return readWithStats(path, activeRoutes, columnConfig).cleanedDatagrams();
    }

    public GpsDatagramReadResult readWithStats(Path path, Set<String> activeRoutes, CsvColumnConfig columnConfig)
            throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return readWithStats(reader, activeRoutes, columnConfig);
        }
    }

    public List<GpsPoint> read(Reader reader, Set<String> activeRoutes, CsvColumnConfig columnConfig)
            throws IOException {
        return readWithStats(reader, activeRoutes, columnConfig).cleanedDatagrams();
    }

    public GpsDatagramReadResult readWithStats(Reader reader, Set<String> activeRoutes, CsvColumnConfig columnConfig)
            throws IOException {
        CsvColumnConfig config = columnConfig == null ? new CsvColumnConfig(null, null, null, null, null, null)
                : columnConfig;
        if (!config.datagramsHasHeader()) {
            return readHeaderlessWithStats(reader, activeRoutes, config);
        }

        try (CSVParser parser = CsvFormats.inputWithHeader().parse(reader)) {
            Set<String> headers = parser.getHeaderMap().keySet();
            ResolvedColumns columns = new ResolvedColumns(
                    columnResolver.resolve(headers, config.routeColumn(), ColumnAliases.ROUTE_ALIASES, "route"),
                    columnResolver.resolve(headers, config.busColumn(), ColumnAliases.BUS_ALIASES, "bus"),
                    columnResolver.resolve(headers, config.timestampColumn(), ColumnAliases.TIMESTAMP_ALIASES,
                            "timestamp"),
                    columnResolver.resolve(headers, config.latitudeColumn(), ColumnAliases.LATITUDE_ALIASES,
                            "latitude"),
                    columnResolver.resolve(headers, config.longitudeColumn(), ColumnAliases.LONGITUDE_ALIASES,
                            "longitude"));

            List<GpsPoint> points = new ArrayList<>();
            long rawRows = 0;
            for (CSVRecord record : parser) {
                rawRows++;
                dataCleaner.cleanGpsPoint(
                        record.get(columns.routeColumn()),
                        record.get(columns.busColumn()),
                        record.get(columns.timestampColumn()),
                        record.get(columns.latitudeColumn()),
                        record.get(columns.longitudeColumn()),
                        activeRoutes,
                        config.coordinateScale()).ifPresent(points::add);
            }
            return new GpsDatagramReadResult(rawRows, points);
        }
    }

    private GpsDatagramReadResult readHeaderlessWithStats(
            Reader reader,
            Set<String> activeRoutes,
            CsvColumnConfig config
    ) throws IOException {
        RequiredIndexes indexes = RequiredIndexes.from(config);
        try (CSVParser parser = CsvFormats.inputWithoutHeader().parse(reader)) {
            List<GpsPoint> points = new ArrayList<>();
            long rawRows = 0;
            long skippedRows = 0;
            for (CSVRecord record : parser) {
                rawRows++;
                if (record.size() <= indexes.maxIndex) {
                    skippedRows++;
                    continue;
                }
                int before = points.size();
                dataCleaner.cleanGpsPoint(
                        record.get(indexes.routeIndex),
                        record.get(indexes.busIndex),
                        record.get(indexes.timestampIndex),
                        record.get(indexes.latitudeIndex),
                        record.get(indexes.longitudeIndex),
                        activeRoutes,
                        config.coordinateScale()).ifPresent(points::add);
                if (points.size() == before) {
                    skippedRows++;
                }
            }
            return new GpsDatagramReadResult(rawRows, points, skippedRows);
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

        private String routeColumn() {
            return routeColumn;
        }

        private String busColumn() {
            return busColumn;
        }

        private String timestampColumn() {
            return timestampColumn;
        }

        private String latitudeColumn() {
            return latitudeColumn;
        }

        private String longitudeColumn() {
            return longitudeColumn;
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
            if (value < 0) {
                throw new IllegalArgumentException(optionName + " must be zero or greater.");
            }
            return value;
        }
    }
}
