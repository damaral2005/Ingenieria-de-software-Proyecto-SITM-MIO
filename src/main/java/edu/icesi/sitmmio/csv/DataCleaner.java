package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class DataCleaner {
    private static final double INVALID_RAW_COORDINATE = -1.0;

    /*
     * Rango amplio de operación para Cali y zona cercana.
     * No significa que el MIO salga de Cali, sino que se descartan puntos GPS
     * absurdos como -1, -1 o coordenadas que no corresponden al área esperada.
     */
    private static final double MIN_OPERATION_LATITUDE = 3.0;
    private static final double MAX_OPERATION_LATITUDE = 3.8;
    private static final double MIN_OPERATION_LONGITUDE = -77.2;
    private static final double MAX_OPERATION_LONGITUDE = -76.0;

    public Optional<String> cleanRequiredText(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    public Optional<GpsPoint> cleanGpsPoint(
            String routeId,
            String busId,
            String timestamp,
            String latitude,
            String longitude,
            Set<String> activeRoutes
    ) {
        return cleanGpsPoint(routeId, busId, timestamp, latitude, longitude, activeRoutes, 1.0);
    }

    public Optional<GpsPoint> cleanGpsPoint(
            String routeId,
            String busId,
            String timestamp,
            String latitude,
            String longitude,
            Set<String> activeRoutes,
            double coordinateScale
    ) {
        Optional<String> cleanedRoute = cleanRequiredText(routeId);
        Optional<String> cleanedBus = cleanRequiredText(busId);
        Optional<String> cleanedTimestamp = cleanRequiredText(timestamp);
        Optional<String> cleanedLatitude = cleanRequiredText(latitude);
        Optional<String> cleanedLongitude = cleanRequiredText(longitude);

        if (cleanedRoute.isEmpty()
                || cleanedBus.isEmpty()
                || cleanedTimestamp.isEmpty()
                || cleanedLatitude.isEmpty()
                || cleanedLongitude.isEmpty()) {
            return Optional.empty();
        }

        Set<String> routes = activeRoutes == null ? Set.of() : activeRoutes;
        String route = cleanedRoute.get();

        if (!routes.contains(route)) {
            return Optional.empty();
        }

        if (!Double.isFinite(coordinateScale) || coordinateScale <= 0.0) {
            return Optional.empty();
        }

        try {
            Instant parsedTimestamp = TimestampParser.parse(cleanedTimestamp.get());

            double rawLatitude = Double.parseDouble(cleanedLatitude.get());
            double rawLongitude = Double.parseDouble(cleanedLongitude.get());

            if (!Double.isFinite(rawLatitude) || !Double.isFinite(rawLongitude)) {
                return Optional.empty();
            }

            /*
             * En los datagramas reales aparecen coordenadas crudas en -1.
             * Como las coordenadas vienen escaladas, -1 se convertiría en -0.0000001,
             * que técnicamente pasa como latitud/longitud mundial, pero no representa
             * una ubicación válida del SITM-MIO.
             */
            if (rawLatitude == INVALID_RAW_COORDINATE || rawLongitude == INVALID_RAW_COORDINATE) {
                return Optional.empty();
            }

            double parsedLatitude = rawLatitude / coordinateScale;
            double parsedLongitude = rawLongitude / coordinateScale;

            if (!isInsideOperationArea(parsedLatitude, parsedLongitude)) {
                return Optional.empty();
            }

            return Optional.of(new GpsPoint(
                    route,
                    cleanedBus.get(),
                    parsedTimestamp,
                    parsedLatitude,
                    parsedLongitude));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static boolean isInsideOperationArea(double latitude, double longitude) {
        return latitude >= MIN_OPERATION_LATITUDE
                && latitude <= MAX_OPERATION_LATITUDE
                && longitude >= MIN_OPERATION_LONGITUDE
                && longitude <= MAX_OPERATION_LONGITUDE;
    }
}