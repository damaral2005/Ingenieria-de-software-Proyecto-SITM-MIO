package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class DataCleaner {
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

        try {
            Instant parsedTimestamp = TimestampParser.parse(cleanedTimestamp.get());
            double parsedLatitude = Double.parseDouble(cleanedLatitude.get()) / coordinateScale;
            double parsedLongitude = Double.parseDouble(cleanedLongitude.get()) / coordinateScale;
            return Optional.of(new GpsPoint(route, cleanedBus.get(), parsedTimestamp, parsedLatitude, parsedLongitude));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
