package edu.icesi.sitmmio.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandLineAppTest {
    @Test
    void runsEndToEndFromCliArguments(@TempDir Path tempDir) throws IOException {
        Path lines = tempDir.resolve("lines.csv");
        Path datagrams = tempDir.resolve("datagrams.csv");
        Path output = tempDir.resolve("results").resolve("speeds.csv");
        Files.writeString(lines, "route\n"
                + "A01\n");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "A01,BUS-1,2026-05-01T10:10:00Z,3.4516,-76.5170\n");

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CommandLineApp app = new CommandLineApp(
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));

        int exitCode = app.run(new String[]{
                "--lines", lines.toString(),
                "--datagrams", datagrams.toString(),
                "--output", output.toString()
        });

        assertEquals(0, exitCode);
        assertEquals("", stderr.toString(StandardCharsets.UTF_8));
        String console = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(console.contains("Active routes: 1"));
        assertTrue(console.contains("Raw datagrams: 2"));
        assertTrue(console.contains("Cleaned datagrams: 2"));
        assertTrue(console.contains("Valid segments: 1"));
        assertTrue(Files.exists(output));
    }
}
