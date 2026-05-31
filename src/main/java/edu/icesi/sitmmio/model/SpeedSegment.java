package edu.icesi.sitmmio.model;

import java.time.Instant;
import java.time.YearMonth;

public final class SpeedSegment {
    private final String routeId;
    private final String busId;
    private final Instant timestamp;
    private final YearMonth month;
    private final double distanceKm;
    private final double durationHours;
    private final double speedKmh;

    public SpeedSegment(
            String routeId,
            String busId,
            Instant timestamp,
            YearMonth month,
            double distanceKm,
            double durationHours,
            double speedKmh
    ) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalArgumentException("Route id is required.");
        }
        if (busId == null || busId.isBlank()) {
            throw new IllegalArgumentException("Bus id is required.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required.");
        }
        if (month == null) {
            throw new IllegalArgumentException("Month is required.");
        }
        validateNonNegativeFinite(distanceKm, "Distance");
        if (!Double.isFinite(durationHours) || durationHours <= 0.0) {
            throw new IllegalArgumentException("Duration must be a finite positive value.");
        }
        validateNonNegativeFinite(speedKmh, "Speed");
        this.routeId = routeId;
        this.busId = busId;
        this.timestamp = timestamp;
        this.month = month;
        this.distanceKm = distanceKm;
        this.durationHours = durationHours;
        this.speedKmh = speedKmh;
    }

    public String routeId() {
        return routeId;
    }

    public String busId() {
        return busId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public YearMonth month() {
        return month;
    }

    public double distanceKm() {
        return distanceKm;
    }

    public double durationHours() {
        return durationHours;
    }

    public double speedKmh() {
        return speedKmh;
    }

    private static void validateNonNegativeFinite(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be a finite non-negative value.");
        }
    }
}
