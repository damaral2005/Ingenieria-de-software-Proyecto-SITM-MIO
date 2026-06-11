package edu.icesi.sitmmio.distributed;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Blobject;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Object.Ice_invokeResult;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.OperationMode;
import com.zeroc.Ice.UnknownException;
import com.zeroc.Ice.Util;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.cli.ExecutionMode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IceDistributedSpeedMasterTest {
    @Test
    void iceWorkerRespondsToHealthCheck(@TempDir Path tempDir) throws Exception {
        try (Communicator communicator = Util.initialize()) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "IceHealthCheckWorker",
                    "tcp -h 127.0.0.1 -p 0");
            ObjectPrx worker = adapter.add(
                    new IceScanWorkerServant(tempDir.resolve("worker")),
                    Util.stringToIdentity("worker"));
            adapter.activate();

            Ice_invokeResult result = worker.ice_invoke(
                    IceScanWorkerServant.HEALTH_CHECK,
                    OperationMode.Normal,
                    IceInvocationCodec.writeString(communicator, "ping"));

            assertTrue(result.returnValue);
            assertEquals("OK", IceInvocationCodec.readString(communicator, result.outParams));
        }
    }

    @Test
    void masterRetriesIcePartitionOnAnotherHealthyWorker(@TempDir Path tempDir) throws Exception {
        Path lines = tempDir.resolve("lines.csv");
        Path datagrams = tempDir.resolve("datagrams.csv");
        Path output = tempDir.resolve("out.csv");
        Path workDirectory = tempDir.resolve("ice-run");
        Files.writeString(lines, "LINEID,name\nA01,Route A\n");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2024-01-01T00:00:00Z,3.000000,-76.000000\n"
                + "A01,BUS-1,2024-01-01T00:05:00Z,3.010000,-76.000000\n");

        try (Communicator communicator = Util.initialize()) {
            ObjectAdapter failingAdapter = communicator.createObjectAdapterWithEndpoints(
                    "IceFailingWorker",
                    "tcp -h 127.0.0.1 -p 0");
            ObjectAdapter healthyAdapter = communicator.createObjectAdapterWithEndpoints(
                    "IceHealthyWorker",
                    "tcp -h 127.0.0.1 -p 0");
            ObjectPrx failingWorker = failingAdapter.add(
                    new FailingAfterHealthCheckServant(),
                    Util.stringToIdentity("failing-worker"));
            ObjectPrx healthyWorker = healthyAdapter.add(
                    new IceScanWorkerServant(tempDir.resolve("healthy-worker")),
                    Util.stringToIdentity("healthy-worker"));
            failingAdapter.activate();
            healthyAdapter.activate();

            CliOptions options = new CliOptions(
                    lines,
                    datagrams,
                    output,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "LINEID",
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1.0,
                    10,
                    120.0,
                    1,
                    ExecutionMode.ICE_MASTER,
                    2,
                    1,
                    2,
                    workDirectory,
                    null,
                    null,
                    null,
                    null,
                    communicator.proxyToString(failingWorker) + ";" + communicator.proxyToString(healthyWorker),
                    null,
                    10000,
                    "sitm-worker");

            DistributedRunSummary summary = new IceDistributedSpeedMaster().run(options);

            assertEquals(1, summary.validSegments());
            assertEquals(1, summary.outputRows());
            assertTrue(Files.readString(output).contains("A01,2024-01"));
        }
    }

    @Test
    void masterInvokesRemoteIceWorkersAndMergesPartials(@TempDir Path tempDir) throws Exception {
        Path lines = tempDir.resolve("lines.csv");
        Path datagrams = tempDir.resolve("datagrams.csv");
        Path output = tempDir.resolve("out.csv");
        Path workDirectory = tempDir.resolve("ice-run");
        Files.writeString(lines, "LINEID,name\nA01,Route A\n");
        Files.writeString(datagrams, "route,bus,timestamp,latitude,longitude\n"
                + "A01,BUS-1,2024-01-01T00:00:00Z,3.000000,-76.000000\n"
                + "A01,BUS-1,2024-01-01T00:05:00Z,3.010000,-76.000000\n"
                + "A01,BUS-2,2024-01-01T00:00:00Z,3.000000,-76.010000\n"
                + "A01,BUS-2,2024-01-01T00:05:00Z,3.010000,-76.010000\n");

        try (Communicator communicator = Util.initialize()) {
            ObjectAdapter firstAdapter = communicator.createObjectAdapterWithEndpoints(
                    "IceTestWorkerOne",
                    "tcp -h 127.0.0.1 -p 0");
            ObjectAdapter secondAdapter = communicator.createObjectAdapterWithEndpoints(
                    "IceTestWorkerTwo",
                    "tcp -h 127.0.0.1 -p 0");
            ObjectPrx firstWorker = firstAdapter.add(
                    new IceScanWorkerServant(tempDir.resolve("worker-one")),
                    Util.stringToIdentity("worker-one"));
            ObjectPrx secondWorker = secondAdapter.add(
                    new IceScanWorkerServant(tempDir.resolve("worker-two")),
                    Util.stringToIdentity("worker-two"));
            firstAdapter.activate();
            secondAdapter.activate();

            CliOptions options = new CliOptions(
                    lines,
                    datagrams,
                    output,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "LINEID",
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1.0,
                    10,
                    120.0,
                    1,
                    ExecutionMode.ICE_MASTER,
                    2,
                    2,
                    2,
                    workDirectory,
                    null,
                    null,
                    null,
                    null,
                    communicator.proxyToString(firstWorker) + ";" + communicator.proxyToString(secondWorker),
                    null,
                    10000,
                    "sitm-worker");

            DistributedRunSummary summary = new IceDistributedSpeedMaster().run(options);

            assertEquals(2, summary.validSegments());
            assertEquals(1, summary.outputRows());
            assertTrue(Files.exists(output));
            assertTrue(Files.readString(output).contains("A01,2024-01"));
            assertTrue(Files.exists(workDirectory.resolve("ice-worker-metrics.csv")));
        }
    }

    private static final class FailingAfterHealthCheckServant implements Blobject {
        @Override
        public Ice_invokeResult ice_invoke(byte[] inParams, Current current) {
            if (IceScanWorkerServant.HEALTH_CHECK.equals(current.operation)) {
                byte[] outParams = IceInvocationCodec.writeString(
                        current.adapter.getCommunicator(),
                        "OK");
                return new Ice_invokeResult(true, outParams);
            }
            throw new UnknownException("Simulated worker failure after health check.");
        }
    }
}
