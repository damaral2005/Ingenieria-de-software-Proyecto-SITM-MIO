package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.csv.CsvColumnConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DatagramPartitionerTest {
    private final DatagramPartitioner partitioner = new DatagramPartitioner();

    @Test
    void partitionsCleanedDatagramsByRouteAndBus(@TempDir Path tempDir) throws IOException {
        Path datagrams = tempDir.resolve("datagrams.csv");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "A01,BUS-1,2026-05-01T10:10:00Z,3.4516,-76.5170\n"
                + "A01,BUS-2,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "C03,BUS-3,2026-05-01T10:00:00Z,3.4516,-76.5320\n");

        PartitioningSummary summary = partitioner.partition(
                datagrams,
                Set.of("A01"),
                new CsvColumnConfig(null, null, null, null, null, null),
                tempDir.resolve("run"),
                3);

        assertEquals(4, summary.rawDatagrams());
        assertEquals(3, summary.cleanedDatagrams());
        assertEquals(1, summary.skippedInvalidDatagrams());
        assertEquals(Set.of(YearMonth.of(2026, 5)), summary.detectedMonths());
        assertEquals(3, summary.workItems().size());

        int partitionsWithBusOne = 0;
        for (PartitionWorkItem workItem : summary.workItems()) {
            List<String> lines = Files.readAllLines(workItem.partitionPath());
            long busOneRows = lines.stream().filter(line -> line.contains("BUS-1")).count();
            if (busOneRows > 0) {
                partitionsWithBusOne++;
                assertEquals(2, busOneRows);
            }
        }
        assertEquals(1, partitionsWithBusOne);
        assertTrue(Files.exists(summary.workItems().get(0).partialResultPath().getParent()));
    }
}
