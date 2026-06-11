package edu.icesi.sitmmio.distributed;

import com.zeroc.Ice.Blobject;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Object.Ice_invokeResult;
import com.zeroc.Ice.UnknownException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class IceScanWorkerServant implements Blobject {
    static final String PROCESS_SCAN_PARTITION = "processScanPartition";
    static final String PROCESS_PARTITION_CSV = "processPartitionCsv";

    private final ScanWorkerProcessor scanWorkerProcessor;
    private final WorkerProcessor workerProcessor;
    private final Path workDirectory;

    public IceScanWorkerServant(Path workDirectory) {
        this(new ScanWorkerProcessor(), new WorkerProcessor(), workDirectory);
    }

    IceScanWorkerServant(
            ScanWorkerProcessor scanWorkerProcessor,
            WorkerProcessor workerProcessor,
            Path workDirectory
    ) {
        this.scanWorkerProcessor = scanWorkerProcessor;
        this.workerProcessor = workerProcessor;
        this.workDirectory = workDirectory;
    }

    @Override
    public Ice_invokeResult ice_invoke(byte[] inParams, Current current) {
        if (!PROCESS_SCAN_PARTITION.equals(current.operation)
                && !PROCESS_PARTITION_CSV.equals(current.operation)) {
            throw new UnknownException("Unsupported Ice worker operation: " + current.operation);
        }

        try {
            String encodedRequest = IceInvocationCodec.readString(
                    current.adapter.getCommunicator(),
                    inParams);
            String encodedResponse = PROCESS_PARTITION_CSV.equals(current.operation)
                    ? processPartitionCsv(encodedRequest).encode()
                    : processScanPartition(encodedRequest).encode();
            byte[] outParams = IceInvocationCodec.writeString(
                    current.adapter.getCommunicator(),
                    encodedResponse);
            return new Ice_invokeResult(true, outParams);
        } catch (IOException | RuntimeException exception) {
            throw new UnknownException("Ice scan worker failed: " + exception.getMessage(), exception);
        }
    }

    private IceScanWorkResponse processScanPartition(String encodedRequest) throws IOException {
        IceScanWorkRequest request = IceScanWorkRequest.decode(encodedRequest);
        Files.createDirectories(workDirectory);
        Path partialResultPath = workDirectory.resolve(String.format(
                "ice-partial-%05d.csv",
                request.partitionId()));
        ScanWorkerRunSummary summary = scanWorkerProcessor.process(
                request.datagramsPath(),
                new edu.icesi.sitmmio.csv.ActiveRoutesCsvReader()
                        .read(request.linesPath(), request.columnConfig().activeRouteColumn()),
                request.columnConfig(),
                partialResultPath,
                request.partitionId(),
                request.partitionCount(),
                Duration.ofMinutes(request.maxGapMinutes()),
                request.maxSpeedKmh());
        String partialCsv = Files.readString(partialResultPath, StandardCharsets.UTF_8);
        return IceScanWorkResponse.fromSummary(summary, partialCsv);
    }

    private IcePartitionWorkResponse processPartitionCsv(String encodedRequest) throws IOException {
        IcePartitionWorkRequest request = IcePartitionWorkRequest.decode(encodedRequest);
        Files.createDirectories(workDirectory);
        String suffix = String.format("%05d", request.partitionId());
        Path partitionPath = workDirectory.resolve("ice-partition-" + suffix + ".csv");
        Path partialResultPath = workDirectory.resolve("ice-partial-" + suffix + ".csv");
        Files.writeString(partitionPath, request.partitionCsv(), StandardCharsets.UTF_8);

        WorkerRunSummary summary = workerProcessor.process(
                new PartitionWorkItem(request.partitionId(), partitionPath, partialResultPath),
                Duration.ofMinutes(request.maxGapMinutes()),
                request.maxSpeedKmh());
        String partialCsv = Files.readString(partialResultPath, StandardCharsets.UTF_8);
        return IcePartitionWorkResponse.fromSummary(request.partitionId(), summary, partialCsv);
    }
}
