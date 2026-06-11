package edu.icesi.sitmmio.distributed;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.LocalException;
import com.zeroc.Ice.Object.Ice_invokeResult;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.OperationMode;
import com.zeroc.Ice.Util;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class IceDistributedSpeedMaster {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final DatagramPartitioner partitioner;
    private final PartialResultCsv partialResultCsv;
    private final PartialResultMerger merger;
    private final ResultCsvWriter resultWriter;

    public IceDistributedSpeedMaster() {
        this(
                new ActiveRoutesCsvReader(),
                new DatagramPartitioner(),
                new PartialResultCsv(),
                new PartialResultMerger(),
                new ResultCsvWriter());
    }

    IceDistributedSpeedMaster(
            ActiveRoutesCsvReader activeRoutesReader,
            DatagramPartitioner partitioner,
            PartialResultCsv partialResultCsv,
            PartialResultMerger merger,
            ResultCsvWriter resultWriter
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.partitioner = partitioner;
        this.partialResultCsv = partialResultCsv;
        this.merger = merger;
        this.resultWriter = resultWriter;
    }

    public DistributedRunSummary run(CliOptions options) throws IOException {
        long startNanos = System.nanoTime();
        Set<String> activeRoutes = activeRoutesReader.read(options.linesPath(), options.activeRouteColumn());
        Path workDirectory = resolveWorkDirectory(options);
        Path partialResultsDirectory = workDirectory.resolve("partial-results");
        Files.createDirectories(partialResultsDirectory);

        CsvColumnConfig columnConfig = new CsvColumnConfig(
                options.activeRouteColumn(),
                options.routeColumn(),
                options.busColumn(),
                options.timestampColumn(),
                options.latitudeColumn(),
                options.longitudeColumn(),
                options.datagramsHasHeader(),
                options.routeIndex(),
                options.busIndex(),
                options.timestampIndex(),
                options.latitudeIndex(),
                options.longitudeIndex(),
                options.coordinateScale());

        long partitionStartNanos = System.nanoTime();
        PartitioningSummary partitioningSummary = partitioner.partition(
                options.datagramsPath(),
                activeRoutes,
                columnConfig,
                workDirectory,
                options.partitionCount());
        long partitionRuntimeMillis = Duration.ofNanos(System.nanoTime() - partitionStartNanos).toMillis();

        List<String> workerEndpoints = parseWorkerEndpoints(options.iceWorkers());
        long workerStartNanos = System.nanoTime();
        IceWorkerInvocationResult invocationResult = invokeWorkers(
                options,
                workerEndpoints,
                partitioningSummary.workItems(),
                partialResultsDirectory);
        List<IcePartitionWorkResponse> responses = invocationResult.responses();
        long workerRuntimeMillis = Duration.ofNanos(System.nanoTime() - workerStartNanos).toMillis();

        writeWorkerMetrics(workDirectory.resolve("ice-worker-metrics.csv"), responses, invocationResult.workerCount());

        long mergeStartNanos = System.nanoTime();
        List<PartialRouteMonthAggregate> partialAggregates = new ArrayList<>();
        for (int partitionId = 0; partitionId < options.partitionCount(); partitionId++) {
            Path partialPath = partialResultsDirectory.resolve(String.format("partial-%05d.csv", partitionId));
            partialAggregates.addAll(partialResultCsv.read(partialPath));
        }
        List<RouteMonthSpeed> completedResults = merger.merge(
                activeRoutes,
                partitioningSummary.detectedMonths(),
                partialAggregates);
        resultWriter.write(options.outputPath(), completedResults);
        long mergeRuntimeMillis = Duration.ofNanos(System.nanoTime() - mergeStartNanos).toMillis();

        long validSegments = responses.stream().mapToLong(IcePartitionWorkResponse::validSegments).sum();
        long runtimeMillis = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();

        return new DistributedRunSummary(
                options.outputPath(),
                workDirectory,
                activeRoutes.size(),
                partitioningSummary.rawDatagrams(),
                partitioningSummary.cleanedDatagrams(),
                partitioningSummary.skippedInvalidDatagrams(),
                validSegments,
                completedResults.size(),
                runtimeMillis,
                partitionRuntimeMillis,
                workerRuntimeMillis,
                mergeRuntimeMillis,
                invocationResult.workerCount(),
                options.partitionCount());
    }

    private IceWorkerInvocationResult invokeWorkers(
            CliOptions options,
            List<String> workerEndpoints,
            List<PartitionWorkItem> workItems,
            Path partialResultsDirectory
    ) throws IOException {
        try (Communicator communicator = Util.initialize(new String[]{"--Ice.MessageSizeMax=0"})) {
            List<String> availableEndpoints = availableWorkerEndpoints(communicator, workerEndpoints);
            List<IcePartitionWorkResponse> responses = new ArrayList<>();
            for (PartitionWorkItem workItem : workItems) {
                String partitionCsv = Files.readString(workItem.partitionPath(), StandardCharsets.UTF_8);
                IcePartitionWorkRequest request = new IcePartitionWorkRequest(
                        workItem.partitionId(),
                        options.maxGapMinutes(),
                        options.maxSpeedKmh(),
                        partitionCsv);
                IcePartitionWorkResponse response = invokeWorkerWithRetries(
                        communicator,
                        availableEndpoints,
                        request,
                        options.workerRetryCount());
                Path partialPath = partialResultsDirectory.resolve(String.format(
                        "partial-%05d.csv",
                        workItem.partitionId()));
                Files.writeString(partialPath, response.partialCsv(), StandardCharsets.UTF_8);
                responses.add(response);
            }
            return new IceWorkerInvocationResult(List.copyOf(responses), availableEndpoints.size());
        }
    }

    private static List<String> availableWorkerEndpoints(
            Communicator communicator,
            List<String> workerEndpoints
    ) throws IOException {
        List<String> available = new ArrayList<>();
        IOException lastFailure = null;
        for (String endpoint : workerEndpoints) {
            try {
                healthCheckWorker(communicator, endpoint);
                available.add(endpoint);
            } catch (IOException exception) {
                lastFailure = exception;
            }
        }
        if (available.isEmpty()) {
            throw new IOException("No Ice workers passed health check.", lastFailure);
        }
        return List.copyOf(available);
    }

    private static void healthCheckWorker(Communicator communicator, String endpoint) throws IOException {
        ObjectPrx worker = communicator.stringToProxy(endpoint);
        if (worker == null) {
            throw new IOException("Invalid Ice worker endpoint: " + endpoint);
        }
        byte[] inParams = IceInvocationCodec.writeString(communicator, "ping");
        try {
            Ice_invokeResult result = worker.ice_invoke(
                    IceScanWorkerServant.HEALTH_CHECK,
                    OperationMode.Normal,
                    inParams);
            if (!result.returnValue) {
                throw new IOException("Ice worker health check returned false for endpoint: " + endpoint);
            }
            String response = IceInvocationCodec.readString(communicator, result.outParams);
            if (!"OK".equals(response)) {
                throw new IOException("Unexpected Ice worker health check response from "
                        + endpoint + ": " + response);
            }
        } catch (LocalException exception) {
            throw new IOException("Ice worker health check failed for " + endpoint + ": "
                    + exception.getClass().getSimpleName() + " " + String.valueOf(exception.getMessage()),
                    exception);
        }
    }

    private static IcePartitionWorkResponse invokeWorkerWithRetries(
            Communicator communicator,
            List<String> workerEndpoints,
            IcePartitionWorkRequest request,
            int workerRetryCount
    ) throws IOException {
        IOException lastFailure = null;
        int maxAttempts = Math.max(workerRetryCount + 1, workerEndpoints.size());
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String endpoint = workerEndpoints.get((request.partitionId() + attempt) % workerEndpoints.size());
            try {
                IcePartitionWorkResponse response = invokeWorker(communicator, endpoint, request);
                if (response.partitionId() != request.partitionId()) {
                    throw new IOException("Ice worker returned partition "
                            + response.partitionId() + " for requested partition " + request.partitionId() + ".");
                }
                return response;
            } catch (IOException exception) {
                lastFailure = exception;
            }
        }
        throw new IOException("Ice partition " + request.partitionId() + " failed after "
                + maxAttempts + " attempt(s).", lastFailure);
    }

    private static IcePartitionWorkResponse invokeWorker(
            Communicator communicator,
            String endpoint,
            IcePartitionWorkRequest request
    ) throws IOException {
        ObjectPrx worker = communicator.stringToProxy(endpoint);
        if (worker == null) {
            throw new IOException("Invalid Ice worker endpoint: " + endpoint);
        }
        byte[] inParams = IceInvocationCodec.writeString(communicator, request.encode());
        try {
            Ice_invokeResult result = worker.ice_invoke(
                    IceScanWorkerServant.PROCESS_PARTITION_CSV,
                    OperationMode.Normal,
                    inParams);
            if (!result.returnValue) {
                throw new IOException("Ice worker returned false for endpoint: " + endpoint);
            }
            return IcePartitionWorkResponse.decode(IceInvocationCodec.readString(communicator, result.outParams));
        } catch (LocalException exception) {
            throw new IOException("Failed to invoke Ice worker " + endpoint + ": "
                    + exception.getClass().getSimpleName() + " " + String.valueOf(exception.getMessage()),
                    exception);
        }
    }

    private static List<String> parseWorkerEndpoints(String endpoints) throws IOException {
        List<String> parsed = Arrays.stream(endpoints.split(";"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
        if (parsed.isEmpty()) {
            throw new IOException("At least one Ice worker endpoint is required.");
        }
        return List.copyOf(parsed);
    }

    private static void writeWorkerMetrics(
            Path metricsPath,
            List<IcePartitionWorkResponse> responses,
            int workerCount
    ) throws IOException {
        Path parent = metricsPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(metricsPath, StandardCharsets.UTF_8)) {
            writer.write("partition_id,worker_count,raw_datagrams,cleaned_datagrams,selected_datagrams,"
                    + "skipped_invalid_datagrams,valid_segments,partial_rows,runtime_ms");
            writer.newLine();
            for (IcePartitionWorkResponse response : responses) {
                writer.write(String.format(Locale.ROOT,
                        "%d,%d,%d,%d,%d,%d,%d,%d,%d",
                        response.partitionId(),
                        workerCount,
                        0L,
                        0L,
                        response.inputPoints(),
                        0L,
                        response.validSegments(),
                        response.partialRows(),
                        response.runtimeMillis()));
                writer.newLine();
            }
        }
    }

    private static Path resolveWorkDirectory(CliOptions options) {
        if (options.workDirectory() != null) {
            return options.workDirectory();
        }
        Path outputParent = options.outputPath().getParent();
        Path baseDirectory = outputParent == null ? Path.of("results") : outputParent;
        String outputName = options.outputPath().getFileName().toString();
        return baseDirectory.resolve("ice-distributed-" + outputName.replace('.', '-'));
    }

    private static final class IceWorkerInvocationResult {
        private final List<IcePartitionWorkResponse> responses;
        private final int workerCount;

        private IceWorkerInvocationResult(List<IcePartitionWorkResponse> responses, int workerCount) {
            this.responses = responses;
            this.workerCount = workerCount;
        }

        private List<IcePartitionWorkResponse> responses() {
            return responses;
        }

        private int workerCount() {
            return workerCount;
        }
    }
}
