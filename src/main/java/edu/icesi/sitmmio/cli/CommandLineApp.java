package edu.icesi.sitmmio.cli;

import edu.icesi.sitmmio.distributed.DistributedRunSummary;
import edu.icesi.sitmmio.distributed.DistributedMergeSummary;
import edu.icesi.sitmmio.distributed.DistributedPartialMerger;
import edu.icesi.sitmmio.distributed.DistributedPartitionSummary;
import edu.icesi.sitmmio.distributed.DistributedPartitioner;
import edu.icesi.sitmmio.distributed.DistributedSpeedMaster;
import edu.icesi.sitmmio.distributed.PartitionWorkItem;
import edu.icesi.sitmmio.distributed.ScanWorkerProcessor;
import edu.icesi.sitmmio.distributed.ScanWorkerRunSummary;
import edu.icesi.sitmmio.distributed.WorkerProcessor;
import edu.icesi.sitmmio.distributed.WorkerRunSummary;
import edu.icesi.sitmmio.csv.CsvColumnConfig;
import edu.icesi.sitmmio.csv.ActiveRoutesCsvReader;
import edu.icesi.sitmmio.service.ThreadPoolRunSummary;
import edu.icesi.sitmmio.service.ThreadPoolSpeedCalculator;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Locale;

public final class CommandLineApp {
    private final PrintStream out;
    private final PrintStream err;
    private final CliParser parser;
    private final ThreadPoolSpeedCalculator calculator;
    private final DistributedPartitioner distributedPartitioner;
    private final DistributedSpeedMaster distributedMaster;
    private final WorkerProcessor workerProcessor;
    private final ScanWorkerProcessor scanWorkerProcessor;
    private final DistributedPartialMerger distributedPartialMerger;
    private final ActiveRoutesCsvReader activeRoutesReader;

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.parser = new CliParser();
        this.calculator = new ThreadPoolSpeedCalculator();
        this.distributedPartitioner = new DistributedPartitioner();
        this.distributedMaster = new DistributedSpeedMaster();
        this.workerProcessor = new WorkerProcessor();
        this.scanWorkerProcessor = new ScanWorkerProcessor();
        this.distributedPartialMerger = new DistributedPartialMerger();
        this.activeRoutesReader = new ActiveRoutesCsvReader();
    }

    public int run(String[] args) {
        ParseResult result = parser.parse(args);
        if (result.helpRequested()) {
            out.print(parser.usage());
            return 0;
        }
        if (!result.valid()) {
            err.println(result.errorMessage());
            err.println();
            err.print(parser.usage());
            return 2;
        }

        try {
            CliOptions options = result.options();
            if (options.executionMode() == ExecutionMode.DISTRIBUTED_PARTITION) {
                DistributedPartitionSummary summary = distributedPartitioner.partition(options);
                out.print(summary.formatForConsole());
                return 0;
            }
            if (options.executionMode() == ExecutionMode.DISTRIBUTED_MASTER) {
                DistributedRunSummary summary = distributedMaster.run(options);
                out.print(summary.formatForConsole());
                return 0;
            }
            if (options.executionMode() == ExecutionMode.DISTRIBUTED_WORKER) {
                WorkerRunSummary summary = workerProcessor.process(
                        new PartitionWorkItem(0, options.partitionPath(), options.partialResultPath()),
                        Duration.ofMinutes(options.maxGapMinutes()),
                        options.maxSpeedKmh());
                out.print(formatWorkerSummary(summary));
                return 0;
            }
            if (options.executionMode() == ExecutionMode.DISTRIBUTED_SCAN_WORKER) {
                ScanWorkerRunSummary summary = scanWorkerProcessor.process(
                        options.datagramsPath(),
                        activeRoutesReader.read(options.linesPath(), options.activeRouteColumn()),
                        columnConfig(options),
                        options.partialResultPath(),
                        options.partitionId(),
                        options.partitionCount(),
                        Duration.ofMinutes(options.maxGapMinutes()),
                        options.maxSpeedKmh());
                out.print(formatScanWorkerSummary(summary));
                return 0;
            }
            if (options.executionMode() == ExecutionMode.DISTRIBUTED_MERGE) {
                DistributedMergeSummary summary = distributedPartialMerger.merge(
                        options.linesPath(),
                        options.activeRouteColumn(),
                        options.partialResultsDirectory(),
                        options.outputPath());
                out.print(summary.formatForConsole());
                return 0;
            }
            ThreadPoolRunSummary summary = calculator.run(options);
            out.print(summary.formatForConsole());
            return 0;
        } catch (IOException | IllegalArgumentException exception) {
            err.println("Failed to run speed calculation: " + exception.getMessage());
            return 1;
        }
    }

    private static String formatWorkerSummary(WorkerRunSummary summary) {
        String template = "SITM-MIO distributed worker completed%n"
                + "Partition CSV: %s%n"
                + "Partial result CSV: %s%n"
                + "Input points: %d%n"
                + "Valid segments: %d%n"
                + "Partial rows: %d%n"
                + "Runtime ms: %d%n";
        return String.format(Locale.ROOT, template,
                summary.partitionPath(),
                summary.partialResultPath(),
                summary.inputPoints(),
                summary.validSegments(),
                summary.partialRows(),
                summary.runtimeMillis());
    }

    private static String formatScanWorkerSummary(ScanWorkerRunSummary summary) {
        String template = "SITM-MIO distributed scan worker completed%n"
                + "Datagrams CSV: %s%n"
                + "Partial result CSV: %s%n"
                + "Partition id: %d%n"
                + "Partition count: %d%n"
                + "Raw datagrams: %d%n"
                + "Cleaned datagrams: %d%n"
                + "Selected datagrams: %d%n"
                + "Skipped invalid datagrams: %d%n"
                + "Valid segments: %d%n"
                + "Partial rows: %d%n"
                + "Runtime ms: %d%n";
        return String.format(Locale.ROOT, template,
                summary.datagramsPath(),
                summary.partialResultPath(),
                summary.partitionId(),
                summary.partitionCount(),
                summary.rawDatagrams(),
                summary.cleanedDatagrams(),
                summary.selectedDatagrams(),
                summary.skippedInvalidDatagrams(),
                summary.validSegments(),
                summary.partialRows(),
                summary.runtimeMillis());
    }

    private static CsvColumnConfig columnConfig(CliOptions options) {
        return new CsvColumnConfig(
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
    }
}
