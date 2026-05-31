package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.SpeedSegment;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class SpeedSegmentCalculator {
    private static final double NANOS_PER_HOUR = 3_600_000_000_000.0;

    private final HaversineDistanceCalculator distanceCalculator;
    private final Duration maxGap;
    private final double maxSpeedKmh;

    public SpeedSegmentCalculator(HaversineDistanceCalculator distanceCalculator, Duration maxGap, double maxSpeedKmh) {
        if (distanceCalculator == null) {
            throw new IllegalArgumentException("Distance calculator is required.");
        }
        if (maxGap == null || maxGap.isZero() || maxGap.isNegative()) {
            throw new IllegalArgumentException("Max gap must be positive.");
        }
        if (!Double.isFinite(maxSpeedKmh) || maxSpeedKmh <= 0.0) {
            throw new IllegalArgumentException("Max speed must be positive.");
        }
        this.distanceCalculator = distanceCalculator;
        this.maxGap = maxGap;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public List<SpeedSegment> calculateSegments(List<GpsPoint> sortedPoints) {
        List<SpeedSegment> segments = new ArrayList<>();
        GpsPoint previous = null;

        for (GpsPoint current : sortedPoints) {
            if (previous != null && belongsToSameRouteAndBus(previous, current)) {
                SpeedSegment segment = calculateValidSegment(previous, current);
                if (segment != null) {
                    segments.add(segment);
                }
            }
            previous = current;
        }

        return List.copyOf(segments);
    }

    private SpeedSegment calculateValidSegment(GpsPoint previous, GpsPoint current) {
        Duration delta = Duration.between(previous.timestamp(), current.timestamp());
        if (delta.isZero() || delta.isNegative() || delta.compareTo(maxGap) > 0) {
            return null;
        }

        double durationHours = delta.toNanos() / NANOS_PER_HOUR;
        double distanceKm = distanceCalculator.calculateKm(
                previous.latitude(),
                previous.longitude(),
                current.latitude(),
                current.longitude());
        double speedKmh = distanceKm / durationHours;

        if (!Double.isFinite(speedKmh) || speedKmh > maxSpeedKmh) {
            return null;
        }

        return new SpeedSegment(
                current.routeId(),
                current.busId(),
                current.timestamp(),
                YearMonth.from(current.timestamp().atZone(ZoneOffset.UTC)),
                distanceKm,
                durationHours,
                speedKmh);
    }

    private static boolean belongsToSameRouteAndBus(GpsPoint previous, GpsPoint current) {
        return previous.routeId().equals(current.routeId()) && previous.busId().equals(current.busId());
    }
}
