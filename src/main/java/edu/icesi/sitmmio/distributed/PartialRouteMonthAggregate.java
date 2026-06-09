package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.model.RouteMonthSpeed;

import java.time.YearMonth;

public final class PartialRouteMonthAggregate {
    private final String routeId;
    private final YearMonth month;
    private final double totalDistanceKm;
    private final double totalTimeHours;
    private final double totalSegmentSpeedKmh;
    private final long validSegments;
    private final long observedBuses;

    public PartialRouteMonthAggregate(
            String routeId,
            YearMonth month,
            double totalDistanceKm,
            double totalTimeHours,
            double totalSegmentSpeedKmh,
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
        validateNonNegativeFinite(totalSegmentSpeedKmh, "Total segment speed");
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
        this.totalSegmentSpeedKmh = totalSegmentSpeedKmh;
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

    public double totalSegmentSpeedKmh() {
        return totalSegmentSpeedKmh;
    }

    public long validSegments() {
        return validSegments;
    }

    public long observedBuses() {
        return observedBuses;
    }

    public PartialRouteMonthAggregate plus(PartialRouteMonthAggregate other) {
        if (!routeId.equals(other.routeId) || !month.equals(other.month)) {
            throw new IllegalArgumentException("Only aggregates for the same route and month can be merged.");
        }
        return new PartialRouteMonthAggregate(
                routeId,
                month,
                totalDistanceKm + other.totalDistanceKm,
                totalTimeHours + other.totalTimeHours,
                totalSegmentSpeedKmh + other.totalSegmentSpeedKmh,
                validSegments + other.validSegments,
                observedBuses + other.observedBuses);
    }

    public RouteMonthSpeed toRouteMonthSpeed() {
        if (validSegments == 0 || totalTimeHours == 0.0) {
            return emptyResult(routeId, month);
        }
        return new RouteMonthSpeed(
                routeId,
                month,
                totalDistanceKm,
                totalTimeHours,
                totalDistanceKm / totalTimeHours,
                totalSegmentSpeedKmh / validSegments,
                validSegments,
                observedBuses);
    }

    public static RouteMonthSpeed emptyResult(String routeId, YearMonth month) {
        return new RouteMonthSpeed(routeId, month, 0.0, 0.0, 0.0, 0.0, 0, 0);
    }

    private static void validateNonNegativeFinite(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be a finite non-negative value.");
        }
    }
}
