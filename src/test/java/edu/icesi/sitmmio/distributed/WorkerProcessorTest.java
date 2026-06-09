package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.model.GpsPoint;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorkerProcessorTest {
    private final WorkerProcessor workerProcessor = new WorkerProcessor();
    private final PartialResultCsv partialResultCsv = new PartialResultCsv();

    @Test
    void processesOnePartitionIntoPartialResults(@TempDir Path tempDir) throws IOException {
        Path partition = tempDir.resolve("partition.csv");
        Path partial = tempDir.resolve("partial.csv");
        try (CSVPrinter printer = CleanedPartitionCsv.openWriter(partition)) {
            CleanedPartitionCsv.print(printer, point("A01", "BUS-1", "2026-05-01T10:00:00Z", 3.4516, -76.5320));
            CleanedPartitionCsv.print(printer, point("A01", "BUS-1", "2026-05-01T10:10:00Z", 3.4516, -76.5170));
        }

        WorkerRunSummary summary = workerProcessor.process(
                new PartitionWorkItem(0, partition, partial),
                Duration.ofMinutes(10),
                120.0);

        assertEquals(2, summary.inputPoints());
        assertEquals(1, summary.validSegments());
        assertEquals(1, summary.partialRows());

        List<PartialRouteMonthAggregate> partials = partialResultCsv.read(partial);
        assertEquals(1, partials.size());
        assertEquals("A01", partials.get(0).routeId());
        assertEquals(YearMonth.of(2026, 5), partials.get(0).month());
        assertEquals(1, partials.get(0).validSegments());
    }

    private static GpsPoint point(String routeId, String busId, String timestamp, double latitude, double longitude) {
        return new GpsPoint(routeId, busId, Instant.parse(timestamp), latitude, longitude);
    }
}
