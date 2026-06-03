package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DataCleanerTest {
    private final DataCleaner cleaner = new DataCleaner();

    @Test
    void acceptsValidScaledGpsPointInOperationArea() {
        Optional<GpsPoint> result = cleaner.cleanGpsPoint(
                "2241",
                "1069",
                "2019-05-27 20:14:43",
                "34761183",
                "-764873683",
                Set.of("2241"),
                10_000_000.0
        );

        assertTrue(result.isPresent());
        assertEquals("2241", result.get().routeId());
        assertEquals("1069", result.get().busId());
        assertEquals(3.4761183, result.get().latitude(), 0.000_000_1);
        assertEquals(-76.4873683, result.get().longitude(), 0.000_000_1);
    }

    @Test
    void rejectsMinusOneLatitudeAndLongitude() {
        Optional<GpsPoint> result = cleaner.cleanGpsPoint(
                "332",
                "1213",
                "2019-05-27 20:14:43",
                "-1",
                "-1",
                Set.of("332"),
                10_000_000.0
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsCoordinatesOutsideOperationArea() {
        Optional<GpsPoint> result = cleaner.cleanGpsPoint(
                "217",
                "1040",
                "2019-05-29 17:00:22",
                "161097017",
                "-862757683",
                Set.of("217"),
                10_000_000.0
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsInactiveRoute() {
        Optional<GpsPoint> result = cleaner.cleanGpsPoint(
                "9999",
                "1069",
                "2019-05-27 20:14:43",
                "34761183",
                "-764873683",
                Set.of("2241"),
                10_000_000.0
        );

        assertTrue(result.isEmpty());
    }
}
