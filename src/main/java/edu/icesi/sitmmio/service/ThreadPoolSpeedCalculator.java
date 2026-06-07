package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.GpsDatagramCsvReader;
import edu.icesi.sitmmio.csv.GpsDatagramReadResult;
import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class ThreadPoolSpeedCalculator {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final GpsDatagramCsvReader datagramReader;
    private final ResultCsvWriter resultWriter;
    private final HaversineDistanceCalculator distanceCalculator;

    public ThreadPoolSpeedCalculator() {
        this(
                new ActiveRoutesCsvReader(),
                new GpsDatagramCsvReader(),
                new ResultCsvWriter(),
                new HaversineDistanceCalculator());
    }

    ThreadPoolSpeedCalculator(
            ActiveRoutesCsvReader activeRoutesReader,
            GpsDatagramCsvReader datagramReader,
            ResultCsvWriter resultWriter,
            HaversineDistanceCalculator distanceCalculator
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.datagramReader = datagramReader;
        this.resultWriter = resultWriter;
        this.distanceCalculator = distanceCalculator;
    }

    public ThreadPoolRunSummary run(CliOptions options) throws IOException {
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

        Set<YearMonth> months = detectedMonths(readResult.cleanedDatagrams());
        Map<String, List<GpsPoint>> pointsByRoute = readResult.cleanedDatagrams().stream()
                .collect(Collectors.groupingBy(GpsPoint::routeId));

        List<RouteMonthSpeed> partialResults = runRouteTasks(
                activeRoutes,
                pointsByRoute,
                options.threadCount(),
                Duration.ofMinutes(options.maxGapMinutes()),
                options.maxSpeedKmh());

        List<RouteMonthSpeed> completedResults = completeActiveRouteMonths(
                activeRoutes,
                months,
                partialResults);

        resultWriter.write(options.outputPath(), completedResults);

        long validSegments = partialResults.stream()
                .mapToLong(RouteMonthSpeed::validSegments)
                .sum();
        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        return new ThreadPoolRunSummary(
                options.outputPath(),
                activeRoutes.size(),
                readResult.rawDatagrams(),
                readResult.cleanedDatagrams().size(),
                readResult.skippedInvalidRows(),
                validSegments,
                completedResults.size(),
                runtimeMillis,
                options.threadCount(),
                activeRoutes.size());
    }

    private List<RouteMonthSpeed> runRouteTasks(
            Set<String> activeRoutes,
            Map<String, List<GpsPoint>> pointsByRoute,
            int threadCount,
            Duration maxGap,
            double maxSpeedKmh
    ) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<List<RouteMonthSpeed>>> futures = new ArrayList<>();
            for (String routeId : new TreeSet<>(activeRoutes)) {
                List<GpsPoint> routePoints = pointsByRoute.getOrDefault(routeId, List.of());
                futures.add(executor.submit(new RouteProcessingTask(
                        routeId,
                        routePoints,
                        distanceCalculator,
                        maxGap,
                        maxSpeedKmh)));
            }

            List<RouteMonthSpeed> results = new ArrayList<>();
            for (Future<List<RouteMonthSpeed>> future : futures) {
                results.addAll(waitFor(future));
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }

    private static List<RouteMonthSpeed> waitFor(Future<List<RouteMonthSpeed>> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ThreadPool execution was interrupted.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("ThreadPool worker failed.", exception);
        }
    }

    private static Set<YearMonth> detectedMonths(List<GpsPoint> points) {
        Set<YearMonth> months = new TreeSet<>();
        for (GpsPoint point : points) {
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
