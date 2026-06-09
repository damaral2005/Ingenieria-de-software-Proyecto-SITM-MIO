package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.model.RouteMonthSpeed;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PartialResultMergerTest {
    private final PartialResultMerger merger = new PartialResultMerger();

    @Test
    void mergesPartialTotalsAndCompletesMissingRouteMonths() {
        YearMonth month = YearMonth.of(2026, 5);
        List<PartialRouteMonthAggregate> partials = List.of(
                new PartialRouteMonthAggregate("A01", month, 10.0, 0.5, 40.0, 1, 1),
                new PartialRouteMonthAggregate("A01", month, 20.0, 0.5, 50.0, 1, 1)
        );

        List<RouteMonthSpeed> results = merger.merge(Set.of("A01", "B02"), Set.of(month), partials);

        assertEquals(2, results.size());
        RouteMonthSpeed merged = results.get(0);
        assertEquals("A01", merged.routeId());
        assertEquals(month, merged.month());
        assertEquals(30.0, merged.totalDistanceKm(), 0.000_001);
        assertEquals(1.0, merged.totalTimeHours(), 0.000_001);
        assertEquals(30.0, merged.averageSpeedKmh(), 0.000_001);
        assertEquals(45.0, merged.averageSegmentSpeedKmh(), 0.000_001);
        assertEquals(2, merged.validSegments());
        assertEquals(2, merged.observedBuses());

        RouteMonthSpeed empty = results.get(1);
        assertEquals("B02", empty.routeId());
        assertEquals(0, empty.validSegments());
        assertEquals(0.0, empty.averageSpeedKmh(), 0.000_001);
    }
}
