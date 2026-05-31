package edu.icesi.sitmmio.model;

import java.time.YearMonth;

public final class RouteMonthSpeed {
    private final String routeId;
    private final YearMonth month;
    private final double totalDistanceKm;
    private final double totalTimeHours;
    private final double averageSpeedKmh;
    private final double averageSegmentSpeedKmh;
    private final long validSegments;
    private final long observedBuses;

    public RouteMonthSpeed(
            String routeId,
            YearMonth month,
            double totalDistanceKm,
            double totalTimeHours,
            double averageSpeedKmh,
            double averageSegmentSpeedKmh,
            long validSegments,
            long observedBuses
    ) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalArgumentException("Route id is required.");
        }
        if (month == null) {
            throw new IllegalArgumentException("Month is required.");
        }
        validateNonNegativeFinite(totalDistanceKm, "Total distance");
        validateNonNegativeFinite(totalTimeHours, "Total time");
        validateNonNegativeFinite(averageSpeedKmh, "Average speed");
        validateNonNegativeFinite(averageSegmentSpeedKmh, "Average segment speed");
        if (validSegments < 0) {
            throw new IllegalArgumentException("Valid segments must not be negative.");
        }
        if (observedBuses < 0) {
            throw new IllegalArgumentException("Observed buses must not be negative.");
        }
        this.routeId = routeId;
        this.month = month;
        this.totalDistanceKm = totalDistanceKm;
        this.totalTimeHours = totalTimeHours;
        this.averageSpeedKmh = averageSpeedKmh;
        this.averageSegmentSpeedKmh = averageSegmentSpeedKmh;
        this.validSegments = validSegments;
        this.observedBuses = observedBuses;
    }

    public String routeId() {
        return routeId;
    }

    public YearMonth month() {
        return month;
    }

    public double totalDistanceKm() {
        return totalDistanceKm;
    }

    public double totalTimeHours() {
        return totalTimeHours;
    }

    public double averageSpeedKmh() {
        return averageSpeedKmh;
    }

    public double averageSegmentSpeedKmh() {
        return averageSegmentSpeedKmh;
    }

    public long validSegments() {
        return validSegments;
    }

    public long observedBuses() {
        return observedBuses;
    }

    private static void validateNonNegativeFinite(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be a finite non-negative value.");
        }
    }
}
