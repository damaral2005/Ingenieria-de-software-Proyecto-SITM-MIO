package edu.icesi.sitmmio.geo;

public final class HaversineDistanceCalculator {
    public static final double EARTH_RADIUS_KM = 6_371.0088;

    public double calculateKm(double lat1, double lon1, double lat2, double lon2) {
        validateLatitude(lat1);
        validateLatitude(lat2);
        validateLongitude(lon1);
        validateLongitude(lon2);

        double latRadians1 = Math.toRadians(lat1);
        double latRadians2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double sinHalfLat = Math.sin(deltaLat / 2.0);
        double sinHalfLon = Math.sin(deltaLon / 2.0);
        double a = sinHalfLat * sinHalfLat
                + Math.cos(latRadians1) * Math.cos(latRadians2) * sinHalfLon * sinHalfLon;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static void validateLatitude(double latitude) {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
    }

    public static void validateLongitude(double longitude) {
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
    }
}
