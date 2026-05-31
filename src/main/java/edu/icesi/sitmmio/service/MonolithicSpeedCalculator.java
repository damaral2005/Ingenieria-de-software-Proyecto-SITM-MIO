package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.GpsDatagramCsvReader;
import edu.icesi.sitmmio.csv.GpsDatagramReadResult;
import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class MonolithicSpeedCalculator {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final GpsDatagramCsvReader datagramReader;
    private final RouteMonthAggregator aggregator;
    private final ResultCsvWriter resultWriter;
    private final HaversineDistanceCalculator distanceCalculator;

    public MonolithicSpeedCalculator() {
        this(
                new ActiveRoutesCsvReader(),
                new GpsDatagramCsvReader(),
                new RouteMonthAggregator(),
                new ResultCsvWriter(),
                new HaversineDistanceCalculator());
    }

    MonolithicSpeedCalculator(
            ActiveRoutesCsvReader activeRoutesReader,
            GpsDatagramCsvReader datagramReader,
            RouteMonthAggregator aggregator,
            ResultCsvWriter resultWriter,
            HaversineDistanceCalculator distanceCalculator
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.datagramReader = datagramReader;
        this.aggregator = aggregator;
        this.resultWriter = resultWriter;
        this.distanceCalculator = distanceCalculator;
    }

    public MonolithicRunSummary run(CliOptions options) throws IOException {
        long startNanos = System.nanoTime();

        Set<String> activeRoutes = activeRoutesReader.read(options.linesPath(), options.activeRouteColumn());
        CsvColumnConfig columnConfig = new CsvColumnConfig(
                options.activeRouteColumn(),
                options.routeColumn(),
                options.busColumn(),
                options.timestampColumn(),
                options.latitudeColumn(),
                options.longitudeColumn(),
                options.datagramsHasHeader(),
                options.routeIndex(),
                options.busIndex(),
                options.timestampIndex(),
                options.latitudeIndex(),
                options.longitudeIndex(),
                options.coordinateScale());
        GpsDatagramReadResult readResult = datagramReader.readWithStats(
                options.datagramsPath(),
                activeRoutes,
                columnConfig);

        List<GpsPoint> sortedPoints = readResult.cleanedDatagrams().stream()
                .sorted(Comparator.comparing(GpsPoint::routeId)
                        .thenComparing(GpsPoint::busId)
                        .thenComparing(GpsPoint::timestamp))
                .collect(Collectors.toList());

        SpeedSegmentCalculator segmentCalculator = new SpeedSegmentCalculator(
                distanceCalculator,
                Duration.ofMinutes(options.maxGapMinutes()),
                options.maxSpeedKmh());
        List<SpeedSegment> segments = segmentCalculator.calculateSegments(sortedPoints);
        List<RouteMonthSpeed> completedResults = completeActiveRouteMonths(
                activeRoutes,
                detectedMonths(sortedPoints),
                aggregator.aggregate(segments));

        resultWriter.write(options.outputPath(), completedResults);

        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        return new MonolithicRunSummary(
                options.outputPath(),
                activeRoutes.size(),
                readResult.rawDatagrams(),
                readResult.cleanedDatagrams().size(),
                readResult.skippedInvalidRows(),
                segments.size(),
                completedResults.size(),
                runtimeMillis);
    }

    private static Set<YearMonth> detectedMonths(List<GpsPoint> sortedPoints) {
        Set<YearMonth> months = new TreeSet<>();
        for (GpsPoint point : sortedPoints) {
            months.add(YearMonth.from(point.timestamp().atZone(ZoneOffset.UTC)));
        }
        return months;
    }

    private static List<RouteMonthSpeed> completeActiveRouteMonths(
            Set<String> activeRoutes,
            Set<YearMonth> months,
            List<RouteMonthSpeed> aggregatedResults
    ) {
        Map<RouteMonthKey, RouteMonthSpeed> byKey = new HashMap<>();
        for (RouteMonthSpeed result : aggregatedResults) {
            byKey.put(new RouteMonthKey(result.routeId(), result.month()), result);
        }

        TreeSet<String> sortedRoutes = new TreeSet<>(activeRoutes);
        return sortedRoutes.stream()
                .flatMap(route -> months.stream()
                        .map(month -> byKey.getOrDefault(
                                new RouteMonthKey(route, month),
                                emptyResult(route, month))))
                .collect(Collectors.toList());
    }

    private static RouteMonthSpeed emptyResult(String routeId, YearMonth month) {
        return new RouteMonthSpeed(routeId, month, 0.0, 0.0, 0.0, 0.0, 0, 0);
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
            return Objects.hash(routeId, month);
        }
    }
}
