package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;
import java.util.Locale;

public final class DistributedPartitionSummary {
    private final Path workDirectory;
    private final Path manifestPath;
    private final int activeRoutes;
    private final long rawDatagrams;
    private final long cleanedDatagrams;
    private final long skippedInvalidDatagrams;
    private final int partitionCount;
    private final long runtimeMillis;

    public DistributedPartitionSummary(
            Path workDirectory,
            Path manifestPath,
            int activeRoutes,
            long rawDatagrams,
            long cleanedDatagrams,
            long skippedInvalidDatagrams,
            int partitionCount,
            long runtimeMillis
    ) {
        this.workDirectory = workDirectory;
        this.manifestPath = manifestPath;
        this.activeRoutes = activeRoutes;
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.partitionCount = partitionCount;
        this.runtimeMillis = runtimeMillis;
    }

    public String formatForConsole() {
        String template = "SITM-MIO distributed partitioning completed%n"
                + "Active routes: %d%n"
                + "Raw datagrams: %d%n"
                + "Cleaned datagrams: %d%n"
                + "Skipped invalid datagrams: %d%n"
                + "Partition count: %d%n"
                + "Runtime ms: %d%n"
                + "Work directory: %s%n"
                + "Manifest CSV: %s%n";
        return String.format(Locale.ROOT, template,
                activeRoutes,
                rawDatagrams,
                cleanedDatagrams,
                skippedInvalidDatagrams,
                partitionCount,
                runtimeMillis,
                workDirectory,
                manifestPath);
    }
}
