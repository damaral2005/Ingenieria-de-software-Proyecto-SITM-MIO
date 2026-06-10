package edu.icesi.sitmmio.distributed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DistributedPartialMergerTest {
    private final PartialResultCsv partialResultCsv = new PartialResultCsv();
    private final DistributedPartialMerger merger = new DistributedPartialMerger();

    @Test
    void mergesPartialCsvFilesFromDirectory(@TempDir Path tempDir) throws IOException {
        Path lines = tempDir.resolve("lines.csv");
        Path partialsDir = tempDir.resolve("partials");
        Path output = tempDir.resolve("out.csv");
        Files.writeString(lines, "route\nA01\nB02\n");
        Files.createDirectories(partialsDir);

        YearMonth month = YearMonth.of(2026, 5);
        partialResultCsv.write(partialsDir.resolve("partial-00000.csv"), List.of(
                new PartialRouteMonthAggregate("A01", month, 10.0, 0.5, 20.0, 1, 1),
                new PartialRouteMonthAggregate("B02", month, 0.0, 0.0, 0.0, 0, 0)
        ));
        partialResultCsv.write(partialsDir.resolve("partial-00001.csv"), List.of(
                new PartialRouteMonthAggregate("A01", month, 5.0, 0.25, 30.0, 1, 1),
                new PartialRouteMonthAggregate("B02", month, 0.0, 0.0, 0.0, 0, 0)
        ));

        DistributedMergeSummary summary = merger.merge(lines, null, partialsDir, output);

        assertTrue(summary.formatForConsole().contains("Partial files: 2"));
        List<String> linesOut = Files.readAllLines(output);
        assertEquals(3, linesOut.size());
        assertTrue(linesOut.get(1).startsWith("A01,2026-05,15.000000,0.750000,20.000000,25.000000,2,2"));
        assertEquals("B02,2026-05,0.000000,0.000000,0.000000,0.000000,0,0", linesOut.get(2));
    }
}
