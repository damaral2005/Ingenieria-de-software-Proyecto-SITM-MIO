package edu.icesi.sitmmio.service;

import java.nio.file.Path;
import java.util.Locale;

public final class MonolithicRunSummary {
    private final Path outputPath;
    private final int activeRoutes;
    private final long rawDatagrams;
    private final int cleanedDatagrams;
    private final long skippedInvalidDatagrams;
    private final int validSegments;
    private final int outputRows;
    private final long runtimeMillis;

    public MonolithicRunSummary(
            Path outputPath,
            int activeRoutes,
            long rawDatagrams,
            int cleanedDatagrams,
            long skippedInvalidDatagrams,
            int validSegments,
            int outputRows,
            long runtimeMillis
    ) {
        this.outputPath = outputPath;
        this.activeRoutes = activeRoutes;
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.validSegments = validSegments;
        this.outputRows = outputRows;
        this.runtimeMillis = runtimeMillis;
    }

    public Path outputPath() {
        return outputPath;
    }

    public int activeRoutes() {
        return activeRoutes;
    }

    public long rawDatagrams() {
        return rawDatagrams;
    }

    public int cleanedDatagrams() {
        return cleanedDatagrams;
    }

    public long skippedInvalidDatagrams() {
        return skippedInvalidDatagrams;
    }

    public int validSegments() {
        return validSegments;
    }

    public int outputRows() {
        return outputRows;
    }

    public long runtimeMillis() {
        return runtimeMillis;
    }

    public String formatForConsole() {
        String template = "SITM-MIO monolithic speed calculator completed%n"
                + "Active routes: %d%n"
                + "Raw datagrams: %d%n"
                + "Cleaned datagrams: %d%n"
                + "Skipped invalid datagrams: %d%n"
                + "Valid segments: %d%n"
                + "Output rows: %d%n"
                + "Total runtime ms: %d%n"
                + "Output CSV: %s%n";
        return String.format(Locale.ROOT, template,
                activeRoutes,
                rawDatagrams,
                cleanedDatagrams,
                skippedInvalidDatagrams,
                validSegments,
                outputRows,
                runtimeMillis,
                outputPath);
    }
}
