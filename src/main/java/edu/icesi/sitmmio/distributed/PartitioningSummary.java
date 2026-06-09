package edu.icesi.sitmmio.distributed;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public final class PartitioningSummary {
    private final long rawDatagrams;
    private final long cleanedDatagrams;
    private final long skippedInvalidDatagrams;
    private final Set<YearMonth> detectedMonths;
    private final List<PartitionWorkItem> workItems;

    public PartitioningSummary(
            long rawDatagrams,
            long cleanedDatagrams,
            long skippedInvalidDatagrams,
            Set<YearMonth> detectedMonths,
            List<PartitionWorkItem> workItems
    ) {
        if (rawDatagrams < 0 || cleanedDatagrams < 0 || skippedInvalidDatagrams < 0) {
            throw new IllegalArgumentException("Datagram counts must not be negative.");
        }
        if (detectedMonths == null) {
            throw new IllegalArgumentException("Detected months are required.");
        }
        if (workItems == null || workItems.isEmpty()) {
            throw new IllegalArgumentException("At least one work item is required.");
        }
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.detectedMonths = Set.copyOf(detectedMonths);
        this.workItems = List.copyOf(workItems);
    }

    public long rawDatagrams() {
        return rawDatagrams;
    }

    public long cleanedDatagrams() {
        return cleanedDatagrams;
    }

    public long skippedInvalidDatagrams() {
        return skippedInvalidDatagrams;
    }

    public Set<YearMonth> detectedMonths() {
        return detectedMonths;
    }

    public List<PartitionWorkItem> workItems() {
        return workItems;
    }
}
