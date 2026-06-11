package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.model.RouteMonthSpeed;
import edu.icesi.sitmmio.output.ResultCsvWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class DistributedSpeedMaster {
    private final ActiveRoutesCsvReader activeRoutesReader;
    private final DatagramPartitioner partitioner;
    private final PartitionManifestCsv manifestCsv;
    private final WorkerProcessLauncher workerProcessLauncher;
    private final PartialResultCsv partialResultCsv;
    private final PartialResultMerger merger;
    private final ResultCsvWriter resultWriter;

    public DistributedSpeedMaster() {
        this(
                new ActiveRoutesCsvReader(),
                new DatagramPartitioner(),
                new PartitionManifestCsv(),
                new WorkerProcessLauncher(),
                new PartialResultCsv(),
                new PartialResultMerger(),
                new ResultCsvWriter());
    }

    DistributedSpeedMaster(
            ActiveRoutesCsvReader activeRoutesReader,
            DatagramPartitioner partitioner,
            PartitionManifestCsv manifestCsv,
            WorkerProcessLauncher workerProcessLauncher,
            PartialResultCsv partialResultCsv,
            PartialResultMerger merger,
            ResultCsvWriter resultWriter
    ) {
        this.activeRoutesReader = activeRoutesReader;
        this.partitioner = partitioner;
        this.manifestCsv = manifestCsv;
        this.workerProcessLauncher = workerProcessLauncher;
        this.partialResultCsv = partialResultCsv;
        this.merger = merger;
        this.resultWriter = resultWriter;
    }

    public DistributedRunSummary run(CliOptions options) throws IOException {
        long startNanos = System.nanoTime();
        Set<String> activeRoutes = activeRoutesReader.read(options.linesPath(), options.activeRouteColumn());
        Path workDirectory = resolveWorkDirectory(options);
        Files.createDirectories(workDirectory);

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
        manifestCsv.write(workDirectory.resolve("manifest.csv"), partitioningSummary.workItems());
        long partitionRuntimeMillis = Duration.ofNanos(System.nanoTime() - partitionStartNanos).toMillis();

        long workerStartNanos = System.nanoTime();
        runWorkers(partitioningSummary.workItems(), options.workerCount(), options.workerRetryCount(),
                options.maxGapMinutes(), options.maxSpeedKmh());
        long workerRuntimeMillis = Duration.ofNanos(System.nanoTime() - workerStartNanos).toMillis();

        long mergeStartNanos = System.nanoTime();
        List<PartialRouteMonthAggregate> partialAggregates = readPartials(partitioningSummary.workItems());
        List<RouteMonthSpeed> completedResults = merger.merge(
                activeRoutes,
                partitioningSummary.detectedMonths(),
                partialAggregates);
        resultWriter.write(options.outputPath(), completedResults);
        long mergeRuntimeMillis = Duration.ofNanos(System.nanoTime() - mergeStartNanos).toMillis();

        long validSegments = partialAggregates.stream()
                .mapToLong(PartialRouteMonthAggregate::validSegments)
                .sum();
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
                options.workerCount(),
                options.partitionCount());
    }

    private void runWorkers(
            List<PartitionWorkItem> workItems,
            int workerCount,
            int workerRetryCount,
            int maxGapMinutes,
            double maxSpeedKmh
    ) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (PartitionWorkItem workItem : workItems) {
                futures.add(executor.submit(() -> {
                    runWorkerWithRetries(workItem, workerRetryCount, maxGapMinutes, maxSpeedKmh);
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                waitForWorker(future);
            }
        } finally {
            executor.shutdown();
        }
    }

    private void runWorkerWithRetries(
            PartitionWorkItem workItem,
            int workerRetryCount,
            int maxGapMinutes,
            double maxSpeedKmh
    ) throws IOException {
        IOException lastFailure = null;
        int maxAttempts = workerRetryCount + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Files.deleteIfExists(workItem.partialResultPath());
            try {
                workerProcessLauncher.launchAndWait(workItem, maxGapMinutes, maxSpeedKmh);
                if (!Files.exists(workItem.partialResultPath())) {
                    throw new IOException("Worker completed without writing partial result: "
                            + workItem.partialResultPath());
                }
                return;
            } catch (IOException exception) {
                lastFailure = exception;
                if (attempt == maxAttempts) {
                    throw new IOException("Distributed worker failed for partition "
                            + workItem.partitionId() + " after " + maxAttempts + " attempt(s).", lastFailure);
                }
            }
        }
    }

    private static void waitForWorker(Future<?> future) throws IOException {
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Distributed worker execution was interrupted.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Distributed worker execution failed.", cause);
        }
    }

    private List<PartialRouteMonthAggregate> readPartials(List<PartitionWorkItem> workItems) throws IOException {
        List<PartialRouteMonthAggregate> partialAggregates = new ArrayList<>();
        for (PartitionWorkItem workItem : workItems) {
            if (!Files.exists(workItem.partialResultPath())) {
                throw new IOException("Missing partial result: " + workItem.partialResultPath());
            }
            partialAggregates.addAll(partialResultCsv.read(workItem.partialResultPath()));
        }
        return List.copyOf(partialAggregates);
    }

    private static Path resolveWorkDirectory(CliOptions options) {
        if (options.workDirectory() != null) {
            return options.workDirectory();
        }
        Path outputParent = options.outputPath().getParent();
        Path baseDirectory = outputParent == null ? Path.of("results") : outputParent;
        String outputName = options.outputPath().getFileName().toString();
        return baseDirectory.resolve("distributed-" + outputName.replace('.', '-'));
    }
}
