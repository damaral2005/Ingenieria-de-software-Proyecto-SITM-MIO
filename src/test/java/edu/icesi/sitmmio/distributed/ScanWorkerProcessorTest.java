package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.csv.CsvColumnConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScanWorkerProcessorTest {
    private final ScanWorkerProcessor processor = new ScanWorkerProcessor();
    private final PartialResultCsv partialResultCsv = new PartialResultCsv();

    @Test
    void scansRawDatagramsAndSelectsOnlyAssignedPartition(@TempDir Path tempDir) throws IOException {
        Path datagrams = tempDir.resolve("datagrams.csv");
        Path partial = tempDir.resolve("partial.csv");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "A01,BUS-1,2026-05-01T10:10:00Z,3.4516,-76.5170\n"
                + "A01,BUS-2,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "C03,BUS-3,2026-05-01T10:00:00Z,3.4516,-76.5320\n");

        int partitionId = PartitionKey.partitionId("A01", "BUS-1", 4);
        ScanWorkerRunSummary summary = processor.process(
                datagrams,
                Set.of("A01"),
                new CsvColumnConfig(null, null, null, null, null, null),
                partial,
                partitionId,
                4,
                Duration.ofMinutes(10),
                120.0);

        assertEquals(4, summary.rawDatagrams());
        assertEquals(3, summary.cleanedDatagrams());
        assertEquals(2, summary.selectedDatagrams());
        assertEquals(1, summary.skippedInvalidDatagrams());
        assertEquals(1, summary.validSegments());
        assertTrue(summary.partialRows() >= 1);

        List<PartialRouteMonthAggregate> partials = partialResultCsv.read(partial);
        long validSegments = partials.stream()
                .mapToLong(PartialRouteMonthAggregate::validSegments)
                .sum();
        assertEquals(1, validSegments);
    }
}
