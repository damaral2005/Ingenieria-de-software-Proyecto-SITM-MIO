package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.geo.HaversineDistanceCalculator;
import edu.icesi.sitmmio.model.GpsPoint;
import edu.icesi.sitmmio.model.SpeedSegment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpeedSegmentCalculatorTest {
    private final SpeedSegmentCalculator calculator = new SpeedSegmentCalculator(
            new HaversineDistanceCalculator(),
            Duration.ofMinutes(10),
            120.0);

    @Test
    void createsSegmentForConsecutivePointsFromSameRouteAndBus() {
        List<GpsPoint> points = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-1", "2026-05-01T10:10:00Z", 3.4516, -76.5170)
        );

        List<SpeedSegment> segments = calculator.calculateSegments(points);

        assertEquals(1, segments.size());
        SpeedSegment segment = segments.get(0);
        assertEquals("A01", segment.routeId());
        assertEquals("BUS-1", segment.busId());
        assertEquals(YearMonth.of(2026, 5), segment.month());
        assertEquals(1.66, segment.distanceKm(), 0.1);
        assertEquals(1.0 / 6.0, segment.durationHours(), 0.000_001);
        assertEquals(segment.distanceKm() / segment.durationHours(), segment.speedKmh(), 0.000_001);
    }

    @Test
    void ignoresConsecutivePointsFromDifferentRouteOrBus() {
        List<GpsPoint> points = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-2", "2026-05-01T10:05:00Z", 3.4516, -76.5270),
                point("B02", "BUS-2", "2026-05-01T10:10:00Z", 3.4516, -76.5220)
        );

        assertTrue(calculator.calculateSegments(points).isEmpty());
    }

    @Test
    void rejectsZeroAndNegativeTimeDeltas() {
        List<GpsPoint> zeroDelta = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5270)
        );
        List<GpsPoint> negativeDelta = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-1", "2026-05-01T09:59:00Z", 3.4516, -76.5270)
        );

        assertTrue(calculator.calculateSegments(zeroDelta).isEmpty());
        assertTrue(calculator.calculateSegments(negativeDelta).isEmpty());
    }

    @Test
    void rejectsSegmentsAboveMaxGap() {
        List<GpsPoint> points = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-1", "2026-05-01T10:11:00Z", 3.4516, -76.5270)
        );

        assertTrue(calculator.calculateSegments(points).isEmpty());
    }

    @Test
    void rejectsImpossibleSpeedsAboveThreshold() {
        List<GpsPoint> points = List.of(
                point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320),
                point("A01", "BUS-1", "2026-05-01T10:01:00Z", 3.4516, -76.3036)
        );

        assertTrue(calculator.calculateSegments(points).isEmpty());
    }

    private static GpsPoint point(String routeId, String busId, String timestamp, double latitude, double longitude) {
        return new GpsPoint(routeId, busId, Instant.parse(timestamp), latitude, longitude);
    }
}
