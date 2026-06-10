package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;

public final class ScanWorkerRunSummary {
    private final Path datagramsPath;
    private final Path partialResultPath;
    private final int partitionId;
    private final int partitionCount;
    private final long rawDatagrams;
    private final long cleanedDatagrams;
    private final long selectedDatagrams;
    private final long skippedInvalidDatagrams;
    private final long validSegments;
    private final int partialRows;
    private final long runtimeMillis;

    public ScanWorkerRunSummary(
            Path datagramsPath,
            Path partialResultPath,
            int partitionId,
            int partitionCount,
            long rawDatagrams,
            long cleanedDatagrams,
            long selectedDatagrams,
            long skippedInvalidDatagrams,
            long validSegments,
            int partialRows,
            long runtimeMillis
    ) {
        this.datagramsPath = datagramsPath;
        this.partialResultPath = partialResultPath;
        this.partitionId = partitionId;
        this.partitionCount = partitionCount;
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.selectedDatagrams = selectedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.validSegments = validSegments;
        this.partialRows = partialRows;
        this.runtimeMillis = runtimeMillis;
    }

    public Path datagramsPath() {
        return datagramsPath;
    }

    public Path partialResultPath() {
        return partialResultPath;
    }

    public int partitionId() {
        return partitionId;
    }

    public int partitionCount() {
        return partitionCount;
    }

    public long rawDatagrams() {
        return rawDatagrams;
    }

    public long cleanedDatagrams() {
        return cleanedDatagrams;
    }

    public long selectedDatagrams() {
        return selectedDatagrams;
    }

    public long skippedInvalidDatagrams() {
        return skippedInvalidDatagrams;
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
