package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

final class RouteProcessingTask implements Callable<List<RouteMonthSpeed>> {
    private final String routeId;
    private final List<GpsPoint> routePoints;
    private final HaversineDistanceCalculator distanceCalculator;
    private final Duration maxGap;
    private final double maxSpeedKmh;

    RouteProcessingTask(
            String routeId,
            List<GpsPoint> routePoints,
            HaversineDistanceCalculator distanceCalculator,
            Duration maxGap,
            double maxSpeedKmh
    ) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalArgumentException("Route id is required.");
        }
        if (routePoints == null) {
            throw new IllegalArgumentException("Route points are required.");
        }
        if (distanceCalculator == null) {
            throw new IllegalArgumentException("Distance calculator is required.");
        }
        this.routeId = routeId;
        this.routePoints = List.copyOf(routePoints);
        this.distanceCalculator = distanceCalculator;
        this.maxGap = maxGap;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    @Override
    public List<RouteMonthSpeed> call() {
        if (routePoints.isEmpty()) {
            return List.of();
        }

        List<GpsPoint> sortedRoutePoints = routePoints.stream()
                .sorted(Comparator.comparing(GpsPoint::busId)
                        .thenComparing(GpsPoint::timestamp))
                .collect(Collectors.toList());

        SpeedSegmentCalculator segmentCalculator = new SpeedSegmentCalculator(
                distanceCalculator,
                maxGap,
                maxSpeedKmh);
        List<SpeedSegment> segments = segmentCalculator.calculateSegments(sortedRoutePoints);

        return new RouteMonthAggregator().aggregate(segments);
    }

    String routeId() {
        return routeId;
    }
}
