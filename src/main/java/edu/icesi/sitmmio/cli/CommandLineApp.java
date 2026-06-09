package edu.icesi.sitmmio.cli;

import edu.icesi.sitmmio.distributed.DistributedRunSummary;
import edu.icesi.sitmmio.distributed.DistributedSpeedMaster;
import edu.icesi.sitmmio.distributed.PartitionWorkItem;
import edu.icesi.sitmmio.distributed.WorkerProcessor;
import edu.icesi.sitmmio.distributed.WorkerRunSummary;
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
    private final DistributedSpeedMaster distributedMaster;
    private final WorkerProcessor workerProcessor;

    public CommandLineApp(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        this.parser = new CliParser();
        this.calculator = new ThreadPoolSpeedCalculator();
        this.distributedMaster = new DistributedSpeedMaster();
        this.workerProcessor = new WorkerProcessor();
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
}
