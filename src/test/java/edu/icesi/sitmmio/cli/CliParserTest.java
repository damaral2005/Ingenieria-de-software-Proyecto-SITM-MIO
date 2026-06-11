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
        assertEquals(ExecutionMode.THREAD_POOL, result.options().executionMode());
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
    void parsesDistributedMasterMode() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-master",
                "--lines", "lines.csv",
                "--datagrams", "datagrams.csv",
                "--output", "results/out.csv",
                "--workers", "3",
                "--partitions", "6",
                "--work-dir", "build/distributed-test"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.DISTRIBUTED_MASTER, result.options().executionMode());
        assertEquals(3, result.options().workerCount());
        assertEquals(6, result.options().partitionCount());
        assertEquals(Path.of("build", "distributed-test"), result.options().workDirectory());
    }

    @Test
    void parsesDistributedPartitionMode() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-partition",
                "--lines", "lines.csv",
                "--datagrams", "datagrams.csv",
                "--work-dir", "work",
                "--partitions", "16"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.DISTRIBUTED_PARTITION, result.options().executionMode());
        assertEquals(Path.of("work"), result.options().workDirectory());
        assertEquals(16, result.options().partitionCount());
    }

    @Test
    void parsesDistributedWorkerModeWithoutDatasetInputs() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-worker",
                "--partition", "partition.csv",
                "--partial-output", "partial.csv"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.DISTRIBUTED_WORKER, result.options().executionMode());
        assertEquals(Path.of("partition.csv"), result.options().partitionPath());
        assertEquals(Path.of("partial.csv"), result.options().partialResultPath());
    }

    @Test
    void rejectsSelectingBothDistributedModes() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-master",
                "--distributed-worker",
                "--partition", "partition.csv",
                "--partial-output", "partial.csv"
        });

        assertFalse(result.valid());
        assertEquals("Only one execution mode can be selected.", result.errorMessage());
    }

    @Test
    void parsesDistributedScanWorkerMode() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-scan-worker",
                "--lines", "lines.csv",
                "--datagrams", "datagrams.csv",
                "--partial-output", "partial.csv",
                "--partition-id", "2",
                "--partitions", "8"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.DISTRIBUTED_SCAN_WORKER, result.options().executionMode());
        assertEquals(Path.of("lines.csv"), result.options().linesPath());
        assertEquals(Path.of("datagrams.csv"), result.options().datagramsPath());
        assertEquals(Path.of("partial.csv"), result.options().partialResultPath());
        assertEquals(2, result.options().partitionId());
        assertEquals(8, result.options().partitionCount());
    }

    @Test
    void parsesDistributedMergeMode() {
        ParseResult result = parser.parse(new String[]{
                "--distributed-merge",
                "--lines", "lines.csv",
                "--partial-results-dir", "partials",
                "--output", "results/out.csv"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.DISTRIBUTED_MERGE, result.options().executionMode());
        assertEquals(Path.of("partials"), result.options().partialResultsDirectory());
        assertEquals(Path.of("results", "out.csv"), result.options().outputPath());
    }

    @Test
    void parsesIceMasterMode() {
        ParseResult result = parser.parse(new String[]{
                "--ice-master",
                "--lines", "/home/swarch/sitm-data/lines-241-ActiveGT.csv",
                "--datagrams", "/home/swarch/sitm-data/datagrams4Pilot.csv",
                "--output", "results/out.csv",
                "--ice-workers", "sitm-worker:tcp -h 10.147.17.103 -p 10000;"
                        + "sitm-worker:tcp -h 10.147.17.104 -p 10000",
                "--partitions", "2",
                "--work-dir", "results/ice-run"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.ICE_MASTER, result.options().executionMode());
        assertEquals("sitm-worker:tcp -h 10.147.17.103 -p 10000;"
                + "sitm-worker:tcp -h 10.147.17.104 -p 10000", result.options().iceWorkers());
        assertEquals(2, result.options().partitionCount());
        assertEquals(Path.of("results", "ice-run"), result.options().workDirectory());
    }

    @Test
    void parsesIceWorkerServerMode() {
        ParseResult result = parser.parse(new String[]{
                "--ice-worker-server",
                "--work-dir", "results/ice-worker",
                "--ice-host", "0.0.0.0",
                "--ice-port", "11000",
                "--ice-identity", "worker-103"
        });

        assertTrue(result.valid());
        assertEquals(ExecutionMode.ICE_WORKER_SERVER, result.options().executionMode());
        assertEquals("0.0.0.0", result.options().iceHost());
        assertEquals(11000, result.options().icePort());
        assertEquals("worker-103", result.options().iceIdentity());
        assertEquals(Path.of("results", "ice-worker"), result.options().workDirectory());
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
