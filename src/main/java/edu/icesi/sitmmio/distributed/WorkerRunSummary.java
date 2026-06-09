package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;

public final class WorkerRunSummary {
    private final Path partitionPath;
    private final Path partialResultPath;
    private final int inputPoints;
    private final long validSegments;
    private final int partialRows;
    private final long runtimeMillis;

    public WorkerRunSummary(
            Path partitionPath,
            Path partialResultPath,
            int inputPoints,
            long validSegments,
            int partialRows,
            long runtimeMillis
    ) {
        this.partitionPath = partitionPath;
        this.partialResultPath = partialResultPath;
        this.inputPoints = inputPoints;
        this.validSegments = validSegments;
        this.partialRows = partialRows;
        this.runtimeMillis = runtimeMillis;
    }

    public Path partitionPath() {
        return partitionPath;
    }

    public Path partialResultPath() {
        return partialResultPath;
    }

    public int inputPoints() {
        return inputPoints;
    }

    public long validSegments() {
        return validSegments;
    }

    public int partialRows() {
        return partialRows;
    }

    public long runtimeMillis() {
        return runtimeMillis;
    }
}
