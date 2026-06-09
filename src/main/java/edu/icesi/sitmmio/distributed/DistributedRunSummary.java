package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;
import java.util.Locale;

public final class DistributedRunSummary {
    private final Path outputPath;
    private final Path workDirectory;
    private final int activeRoutes;
    private final long rawDatagrams;
    private final long cleanedDatagrams;
    private final long skippedInvalidDatagrams;
    private final long validSegments;
    private final int outputRows;
    private final long runtimeMillis;
    private final long partitionRuntimeMillis;
    private final long workerRuntimeMillis;
    private final long mergeRuntimeMillis;
    private final int workerCount;
    private final int partitionCount;

    public DistributedRunSummary(
            Path outputPath,
            Path workDirectory,
            int activeRoutes,
            long rawDatagrams,
            long cleanedDatagrams,
            long skippedInvalidDatagrams,
            long validSegments,
            int outputRows,
            long runtimeMillis,
            long partitionRuntimeMillis,
            long workerRuntimeMillis,
            long mergeRuntimeMillis,
            int workerCount,
            int partitionCount
    ) {
        this.outputPath = outputPath;
        this.workDirectory = workDirectory;
        this.activeRoutes = activeRoutes;
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.validSegments = validSegments;
        this.outputRows = outputRows;
        this.runtimeMillis = runtimeMillis;
        this.partitionRuntimeMillis = partitionRuntimeMillis;
        this.workerRuntimeMillis = workerRuntimeMillis;
        this.mergeRuntimeMillis = mergeRuntimeMillis;
        this.workerCount = workerCount;
        this.partitionCount = partitionCount;
    }

    public long validSegments() {
        return validSegments;
    }

    public int outputRows() {
        return outputRows;
    }

    public String formatForConsole() {
        String template = "SITM-MIO distributed speed calculator completed%n"
                + "Active routes: %d%n"
                + "Raw datagrams: %d%n"
                + "Cleaned datagrams: %d%n"
                + "Skipped invalid datagrams: %d%n"
                + "Valid segments: %d%n"
                + "Output rows: %d%n"
                + "Worker count: %d%n"
                + "Partition count: %d%n"
                + "Partition runtime ms: %d%n"
                + "Worker runtime ms: %d%n"
                + "Merge runtime ms: %d%n"
                + "Total runtime ms: %d%n"
                + "Work directory: %s%n"
                + "Output CSV: %s%n";
        return String.format(Locale.ROOT, template,
                activeRoutes,
                rawDatagrams,
                cleanedDatagrams,
                skippedInvalidDatagrams,
                validSegments,
                outputRows,
                workerCount,
                partitionCount,
                partitionRuntimeMillis,
                workerRuntimeMillis,
                mergeRuntimeMillis,
                runtimeMillis,
                workDirectory,
                outputPath);
    }
}
