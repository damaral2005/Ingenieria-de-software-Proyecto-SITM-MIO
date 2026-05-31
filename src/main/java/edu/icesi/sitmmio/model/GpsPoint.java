package edu.icesi.sitmmio.model;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;

import java.time.Instant;

public final class GpsPoint {
    private final String routeId;
    private final String busId;
    private final Instant timestamp;
    private final double latitude;
    private final double longitude;

    public GpsPoint(String routeId, String busId, Instant timestamp, double latitude, double longitude) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalArgumentException("Route id is required.");
        }
        if (busId == null || busId.isBlank()) {
            throw new IllegalArgumentException("Bus id is required.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp is required.");
        }
        HaversineDistanceCalculator.validateLatitude(latitude);
        HaversineDistanceCalculator.validateLongitude(longitude);
        this.routeId = routeId;
        this.busId = busId;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }
}
