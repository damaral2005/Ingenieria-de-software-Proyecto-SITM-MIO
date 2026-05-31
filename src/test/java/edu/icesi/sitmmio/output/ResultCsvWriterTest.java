package edu.icesi.sitmmio.output;

import edu.icesi.sitmmio.model.RouteMonthSpeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ResultCsvWriterTest {
    private final ResultCsvWriter writer = new ResultCsvWriter();

    @Test
    void writesExpectedHeaderAndRows(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("nested").resolve("speeds.csv");

        writer.write(output, List.of(new RouteMonthSpeed(
                "A01",
                YearMonth.of(2026, 5),
                1.5,
                0.25,
                6.0,
                7.0,
                2,
                1)));

        List<String> lines = Files.readAllLines(output);
        assertEquals("route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,"
                + "avg_segment_speed_kmh,valid_segments,buses_observed", lines.get(0));
        assertEquals("A01,2026-05,1.500000,0.250000,6.000000,7.000000,2,1", lines.get(1));
    }
}
