package edu.icesi.sitmmio.distributed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PartialResultCsvTest {
    private final PartialResultCsv csv = new PartialResultCsv();

    @Test
    void writesAndReadsPartialAggregates(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("partial.csv");
        YearMonth month = YearMonth.of(2026, 5);

        csv.write(output, List.of(
                new PartialRouteMonthAggregate("A01", month, 1.5, 0.25, 12.5, 2, 1)
        ));

        List<PartialRouteMonthAggregate> read = csv.read(output);

        assertEquals(1, read.size());
        PartialRouteMonthAggregate aggregate = read.get(0);
        assertEquals("A01", aggregate.routeId());
        assertEquals(month, aggregate.month());
        assertEquals(1.5, aggregate.totalDistanceKm(), 0.000_001);
        assertEquals(0.25, aggregate.totalTimeHours(), 0.000_001);
        assertEquals(12.5, aggregate.totalSegmentSpeedKmh(), 0.000_001);
        assertEquals(2, aggregate.validSegments());
        assertEquals(1, aggregate.observedBuses());
    }
}
