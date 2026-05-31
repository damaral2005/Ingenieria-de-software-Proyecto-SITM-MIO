package edu.icesi.sitmmio.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CliParserTest {
    private final CliParser parser = new CliParser();

    @Test
    void parsesRequiredArgumentsAndDefaults() {
        ParseResult result = parser.parse(new String[]{
                "--lines", "lines.csv",
                "--datagrams", "datagrams.csv",
                "--output", "results/out.csv"
        });

        assertTrue(result.valid());
        assertEquals(Path.of("lines.csv"), result.options().linesPath());
        assertEquals(Path.of("datagrams.csv"), result.options().datagramsPath());
        assertEquals(Path.of("results", "out.csv"), result.options().outputPath());
        assertEquals(10, result.options().maxGapMinutes());
        assertEquals(120.0, result.options().maxSpeedKmh());
    }

    @Test
    void parsesOptionalColumnsAndThresholds() {
        ParseResult result = parser.parse(new String[]{
                "--lines", "lines.csv",
                "--datagrams", "datagrams.csv",
                "--output", "results/out.csv",
                "--route-col", "route",
                "--bus-col", "bus",
                "--timestamp-col", "timestamp",
                "--latitude-col", "lat",
                "--longitude-col", "lon",
                "--active-route-col", "route_id",
                "--datagrams-has-header", "false",
                "--route-index", "7",
                "--bus-index", "11",
                "--timestamp-index", "10",
                "--latitude-index", "4",
                "--longitude-index", "5",
                "--coordinate-scale", "10000000",
                "--max-gap-minutes", "15",
                "--max-speed-kmh", "95.5"
        });

        assertTrue(result.valid());
        assertEquals("route", result.options().routeColumn());
        assertEquals("bus", result.options().busColumn());
        assertEquals("timestamp", result.options().timestampColumn());
        assertEquals("lat", result.options().latitudeColumn());
        assertEquals("lon", result.options().longitudeColumn());
        assertEquals("route_id", result.options().activeRouteColumn());
        assertEquals(false, result.options().datagramsHasHeader());
        assertEquals(7, result.options().routeIndex());
        assertEquals(11, result.options().busIndex());
        assertEquals(10, result.options().timestampIndex());
        assertEquals(4, result.options().latitudeIndex());
        assertEquals(5, result.options().longitudeIndex());
        assertEquals(10_000_000.0, result.options().coordinateScale());
        assertEquals(15, result.options().maxGapMinutes());
        assertEquals(95.5, result.options().maxSpeedKmh());
    }

    @Test
    void rejectsMissingRequiredArguments() {
        ParseResult result = parser.parse(new String[]{"--lines", "lines.csv"});

        assertFalse(result.valid());
        assertEquals("Missing required option: --datagrams", result.errorMessage());
    }

    @Test
    void recognizesHelp() {
        ParseResult result = parser.parse(new String[]{"--help"});

        assertTrue(result.helpRequested());
    }
}
