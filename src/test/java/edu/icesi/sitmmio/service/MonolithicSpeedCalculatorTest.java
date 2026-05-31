package edu.icesi.sitmmio.service;

import edu.icesi.sitmmio.cli.CliOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MonolithicSpeedCalculatorTest {
    private final MonolithicSpeedCalculator calculator = new MonolithicSpeedCalculator();

    @Test
    void runsFullMonolithicFlowAndCompletesActiveRoutesForDetectedMonths(@TempDir Path tempDir)
            throws IOException {
        Path lines = tempDir.resolve("lines.csv");
        Path datagrams = tempDir.resolve("datagrams.csv");
        Path output = tempDir.resolve("results").resolve("route_month_speeds.csv");
        Files.writeString(lines, "route\n"
                + "A01\n"
                + "B02\n");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "A01,BUS-1,2026-05-01T10:10:00Z,3.4516,-76.5170\n"
                + "B02,BUS-2,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "C03,BUS-3,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "A01,BUS-4,2026-05-01T10:00:00Z,91.0,-76.5320\n");

        MonolithicRunSummary summary = calculator.run(new CliOptions(
                lines,
                datagrams,
                output,
                null,
                null,
                null,
                null,
                null,
                null,
                10,
                120.0));

        assertEquals(2, summary.activeRoutes());
        assertEquals(5, summary.rawDatagrams());
        assertEquals(3, summary.cleanedDatagrams());
        assertEquals(1, summary.validSegments());
        assertEquals(2, summary.outputRows());

        List<String> outputLines = Files.readAllLines(output);
        assertEquals(3, outputLines.size());
        assertTrue(outputLines.get(1).startsWith("A01,2026-05,"));
        assertEquals("B02,2026-05,0.000000,0.000000,0.000000,0.000000,0,0", outputLines.get(2));
    }
}
