package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.model.SpeedSegment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RouteMonthAggregatorTest {
    private final RouteMonthAggregator aggregator = new RouteMonthAggregator();

    @Test
    void aggregatesDistanceTimeSpeedsSegmentsAndBusesByRouteMonth() {
        YearMonth may = YearMonth.of(2026, 5);
        List<SpeedSegment> segments = List.of(
                segment("A01", "BUS-1", may, 10.0, 0.5, 20.0),
                segment("A01", "BUS-1", may, 5.0, 0.25, 20.0),
                segment("A01", "BUS-2", may, 15.0, 0.25, 60.0)
        );

        List<RouteMonthSpeed> results = aggregator.aggregate(segments);

        assertEquals(1, results.size());
        RouteMonthSpeed result = results.get(0);
        assertEquals("A01", result.routeId());
        assertEquals(may, result.month());
        assertEquals(30.0, result.totalDistanceKm());
        assertEquals(1.0, result.totalTimeHours());
        assertEquals(30.0, result.averageSpeedKmh());
        assertEquals(100.0 / 3.0, result.averageSegmentSpeedKmh());
        assertEquals(3, result.validSegments());
        assertEquals(2, result.observedBuses());
    }

    @Test
    void returnsResultsInDeterministicRouteThenMonthOrder() {
        List<SpeedSegment> segments = List.of(
                segment("B02", "BUS-1", YearMonth.of(2026, 5), 1.0, 1.0, 1.0),
                segment("A01", "BUS-1", YearMonth.of(2026, 6), 1.0, 1.0, 1.0),
                segment("A01", "BUS-1", YearMonth.of(2026, 5), 1.0, 1.0, 1.0)
        );

        List<RouteMonthSpeed> results = aggregator.aggregate(segments);

        assertEquals("A01", results.get(0).routeId());
        assertEquals(YearMonth.of(2026, 5), results.get(0).month());
        assertEquals("A01", results.get(1).routeId());
        assertEquals(YearMonth.of(2026, 6), results.get(1).month());
        assertEquals("B02", results.get(2).routeId());
    }

    private static SpeedSegment segment(
            String routeId,
            String busId,
            YearMonth month,
            double distanceKm,
            double durationHours,
            double speedKmh
    ) {
        return new SpeedSegment(
                routeId,
                busId,
                Instant.parse(month + "-01T00:00:00Z"),
                month,
                distanceKm,
                durationHours,
                speedKmh);
    }
}
