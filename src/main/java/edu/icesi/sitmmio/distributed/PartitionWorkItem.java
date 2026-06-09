package edu.icesi.sitmmio.distributed;

import java.nio.file.Path;

public final class PartitionWorkItem {
    private final int partitionId;
    private final Path partitionPath;
    private final Path partialResultPath;

    public PartitionWorkItem(int partitionId, Path partitionPath, Path partialResultPath) {
        if (partitionId < 0) {
            throw new IllegalArgumentException("Partition id must not be negative.");
        }
        if (partitionPath == null) {
            throw new IllegalArgumentException("Partition path is required.");
        }
        if (partialResultPath == null) {
            throw new IllegalArgumentException("Partial result path is required.");
        }
        this.partitionId = partitionId;
        this.partitionPath = partitionPath;
        this.partialResultPath = partialResultPath;
    }

    public int partitionId() {
        return partitionId;
    }

    public Path partitionPath() {
        return partitionPath;
    }

    public Path partialResultPath() {
        return partialResultPath;
    }
}
