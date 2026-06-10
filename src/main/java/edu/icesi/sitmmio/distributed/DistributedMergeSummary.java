package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;
import java.util.Locale;

public final class DistributedMergeSummary {
    private final Path outputPath;
    private final Path partialResultsDirectory;
    private final int partialFiles;
    private final int partialRows;
    private final long validSegments;
    private final int outputRows;
    private final long runtimeMillis;

    public DistributedMergeSummary(
            Path outputPath,
            Path partialResultsDirectory,
            int partialFiles,
            int partialRows,
            long validSegments,
            int outputRows,
            long runtimeMillis
    ) {
        this.outputPath = outputPath;
        this.partialResultsDirectory = partialResultsDirectory;
        this.partialFiles = partialFiles;
        this.partialRows = partialRows;
        this.validSegments = validSegments;
        this.outputRows = outputRows;
        this.runtimeMillis = runtimeMillis;
    }

    public String formatForConsole() {
        String template = "SITM-MIO distributed partial merge completed%n"
                + "Partial results directory: %s%n"
                + "Partial files: %d%n"
                + "Partial rows: %d%n"
                + "Valid segments: %d%n"
                + "Output rows: %d%n"
                + "Runtime ms: %d%n"
                + "Output CSV: %s%n";
        return String.format(Locale.ROOT, template,
                partialResultsDirectory,
                partialFiles,
                partialRows,
                validSegments,
                outputRows,
                runtimeMillis,
                outputPath);
    }
}
