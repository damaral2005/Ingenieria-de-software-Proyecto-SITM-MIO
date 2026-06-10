package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.csv.ColumnResolver;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.CsvFormats;
import edu.icesi.sitmmio.csv.DataCleaner;
import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.SpeedSegment;
import edu.icesi.sitmmio.service.SpeedSegmentCalculator;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ScanWorkerProcessor {
    private final ColumnResolver columnResolver;
    private final DataCleaner dataCleaner;
    private final HaversineDistanceCalculator distanceCalculator;
    private final PartialRouteMonthAggregator aggregator;
    private final PartialResultCsv partialResultCsv;

    public ScanWorkerProcessor() {
        this(
                new ColumnResolver(),
                new DataCleaner(),
                new HaversineDistanceCalculator(),
                new PartialRouteMonthAggregator(),
                new PartialResultCsv());
    }

    ScanWorkerProcessor(
            ColumnResolver columnResolver,
            DataCleaner dataCleaner,
            HaversineDistanceCalculator distanceCalculator,
            PartialRouteMonthAggregator aggregator,
            PartialResultCsv partialResultCsv
    ) {
        this.columnResolver = columnResolver;
        this.dataCleaner = dataCleaner;
        this.distanceCalculator = distanceCalculator;
        this.aggregator = aggregator;
        this.partialResultCsv = partialResultCsv;
    }

    public ScanWorkerRunSummary process(
            Path datagramsPath,
            Set<String> activeRoutes,
            CsvColumnConfig columnConfig,
            Path partialResultPath,
            int partitionId,
            int partitionCount,
            Duration maxGap,
            double maxSpeedKmh
    ) throws IOException {
        if (partitionId < 0 || partitionId >= partitionCount) {
            throw new IllegalArgumentException("Partition id must be between 0 and partition count - 1.");
        }
        long startNanos = System.nanoTime();
        ScanResult scanResult = scan(datagramsPath, activeRoutes, columnConfig, partitionId, partitionCount);

        List<GpsPoint> sortedPoints = scanResult.selectedPoints.stream()
                .sorted(Comparator.comparing(GpsPoint::routeId)
                        .thenComparing(GpsPoint::busId)
                        .thenComparing(GpsPoint::timestamp))
                .collect(Collectors.toList());
        SpeedSegmentCalculator segmentCalculator = new SpeedSegmentCalculator(
                distanceCalculator,
                maxGap,
                maxSpeedKmh);
        List<SpeedSegment> segments = segmentCalculator.calculateSegments(sortedPoints);
        List<PartialRouteMonthAggregate> partials = completeActiveRouteMonths(
                activeRoutes,
                scanResult.detectedMonths,
                aggregator.aggregate(segments));
        partialResultCsv.write(partialResultPath, partials);

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new ScanWorkerRunSummary(
                datagramsPath,
                partialResultPath,
                partitionId,
                partitionCount,
                scanResult.rawRows,
                scanResult.cleanedRows,
                scanResult.selectedRows,
                scanResult.skippedRows,
                segments.size(),
                partials.size(),
                runtimeMillis);
    }

    private ScanResult scan(
            Path datagramsPath,
            Set<String> activeRoutes,
            CsvColumnConfig columnConfig,
            int partitionId,
            int partitionCount
    ) throws IOException {
        CsvColumnConfig config = columnConfig == null
                ? new CsvColumnConfig(null, null, null, null, null, null)
                : columnConfig;
        try (Reader reader = Files.newBufferedReader(datagramsPath)) {
            if (config.datagramsHasHeader()) {
                return scanHeaderDatagrams(reader, activeRoutes, config, partitionId, partitionCount);
            }
            return scanHeaderlessDatagrams(reader, activeRoutes, config, partitionId, partitionCount);
        }
    }

    private ScanResult scanHeaderDatagrams(
            Reader reader,
            Set<String> activeRoutes,
            CsvColumnConfig config,
            int partitionId,
            int partitionCount
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
            ScanResult result = new ScanResult();
            for (CSVRecord record : parser) {
                result.rawRows++;
                cleanAndMaybeSelect(
                        record.get(columns.routeColumn),
                        record.get(columns.busColumn),
                        record.get(columns.timestampColumn),
                        record.get(columns.latitudeColumn),
                        record.get(columns.longitudeColumn),
                        activeRoutes,
                        config.coordinateScale(),
                        partitionId,
                        partitionCount,
                        result);
            }
            return result;
        }
    }

    private ScanResult scanHeaderlessDatagrams(
            Reader reader,
            Set<String> activeRoutes,
            CsvColumnConfig config,
            int partitionId,
            int partitionCount
    ) throws IOException {
        RequiredIndexes indexes = RequiredIndexes.from(config);
        try (CSVParser parser = CsvFormats.inputWithoutHeader().parse(reader)) {
            ScanResult result = new ScanResult();
            for (CSVRecord record : parser) {
                result.rawRows++;
                if (record.size() <= indexes.maxIndex) {
                    result.skippedRows++;
                    continue;
                }
                cleanAndMaybeSelect(
                        record.get(indexes.routeIndex),
                        record.get(indexes.busIndex),
                        record.get(indexes.timestampIndex),
                        record.get(indexes.latitudeIndex),
                        record.get(indexes.longitudeIndex),
                        activeRoutes,
                        config.coordinateScale(),
                        partitionId,
                        partitionCount,
                        result);
            }
            return result;
        }
    }

    private void cleanAndMaybeSelect(
            String routeId,
            String busId,
            String timestamp,
            String latitude,
            String longitude,
            Set<String> activeRoutes,
            double coordinateScale,
            int partitionId,
            int partitionCount,
            ScanResult result
    ) {
        var cleanedPoint = dataCleaner.cleanGpsPoint(
                routeId,
                busId,
                timestamp,
                latitude,
                longitude,
                activeRoutes,
                coordinateScale);
        if (cleanedPoint.isPresent()) {
            selectIfAssigned(cleanedPoint.get(), partitionId, partitionCount, result);
        } else {
            result.skippedRows++;
        }
    }

    private static void selectIfAssigned(
            GpsPoint point,
            int partitionId,
            int partitionCount,
            ScanResult result
    ) {
        result.cleanedRows++;
        result.detectedMonths.add(YearMonth.from(point.timestamp().atZone(ZoneOffset.UTC)));
        if (PartitionKey.partitionId(point.routeId(), point.busId(), partitionCount) == partitionId) {
            result.selectedPoints.add(point);
            result.selectedRows++;
        }
    }

    private static List<PartialRouteMonthAggregate> completeActiveRouteMonths(
            Set<String> activeRoutes,
            Set<YearMonth> months,
            List<PartialRouteMonthAggregate> partials
    ) {
        Map<RouteMonthKey, PartialRouteMonthAggregate> byKey = new HashMap<>();
        for (PartialRouteMonthAggregate partial : partials) {
            byKey.put(new RouteMonthKey(partial.routeId(), partial.month()), partial);
        }

        TreeSet<String> sortedRoutes = new TreeSet<>(activeRoutes);
        TreeSet<YearMonth> sortedMonths = new TreeSet<>(months);
        return sortedRoutes.stream()
                .flatMap(route -> sortedMonths.stream()
                        .map(month -> byKey.getOrDefault(
                                new RouteMonthKey(route, month),
                                new PartialRouteMonthAggregate(route, month, 0.0, 0.0, 0.0, 0, 0))))
                .collect(Collectors.toList());
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

    private static final class RouteMonthKey {
        private final String routeId;
        private final YearMonth month;

        private RouteMonthKey(String routeId, YearMonth month) {
            this.routeId = routeId;
            this.month = month;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RouteMonthKey)) {
                return false;
            }
            RouteMonthKey that = (RouteMonthKey) other;
            return routeId.equals(that.routeId) && month.equals(that.month);
        }

        @Override
        public int hashCode() {
            return routeId.hashCode() * 31 + month.hashCode();
        }
    }

    private static final class ScanResult {
        private long rawRows;
        private long cleanedRows;
        private long selectedRows;
        private long skippedRows;
        private final Set<YearMonth> detectedMonths = new TreeSet<>();
        private final List<GpsPoint> selectedPoints = new ArrayList<>();
    }
}
