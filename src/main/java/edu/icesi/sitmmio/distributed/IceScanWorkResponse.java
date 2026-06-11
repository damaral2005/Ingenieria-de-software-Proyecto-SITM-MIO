package edu.icesi.sitmmio.distributed;

import java.io.IOException;
import java.util.Properties;

public final class IceScanWorkResponse {
    private final int partitionId;
    private final long rawDatagrams;
    private final long cleanedDatagrams;
    private final long selectedDatagrams;
    private final long skippedInvalidDatagrams;
    private final long validSegments;
    private final int partialRows;
    private final long runtimeMillis;
    private final String partialCsv;

    public IceScanWorkResponse(
            int partitionId,
            long rawDatagrams,
            long cleanedDatagrams,
            long selectedDatagrams,
            long skippedInvalidDatagrams,
            long validSegments,
            int partialRows,
            long runtimeMillis,
            String partialCsv
    ) {
        this.partitionId = partitionId;
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = cleanedDatagrams;
        this.selectedDatagrams = selectedDatagrams;
        this.skippedInvalidDatagrams = skippedInvalidDatagrams;
        this.validSegments = validSegments;
        this.partialRows = partialRows;
        this.runtimeMillis = runtimeMillis;
        this.partialCsv = partialCsv;
    }

    static IceScanWorkResponse fromSummary(ScanWorkerRunSummary summary, String partialCsv) {
        return new IceScanWorkResponse(
                summary.partitionId(),
                summary.rawDatagrams(),
                summary.cleanedDatagrams(),
                summary.selectedDatagrams(),
                summary.skippedInvalidDatagrams(),
                summary.validSegments(),
                summary.partialRows(),
                summary.runtimeMillis(),
                partialCsv);
    }

    static IceScanWorkResponse decode(String encoded) throws IOException {
        Properties properties = IcePropertyMessage.decode(encoded);
        return new IceScanWorkResponse(
                IcePropertyMessage.intValue(properties, "partitionId"),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "rawDatagrams")),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "cleanedDatagrams")),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "selectedDatagrams")),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "skippedInvalidDatagrams")),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "validSegments")),
                IcePropertyMessage.intValue(properties, "partialRows"),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "runtimeMillis")),
                IcePropertyMessage.stringValue(properties, "partialCsv"));
    }

    String encode() {
        Properties properties = new Properties();
        properties.setProperty("partitionId", Integer.toString(partitionId));
        properties.setProperty("rawDatagrams", Long.toString(rawDatagrams));
        properties.setProperty("cleanedDatagrams", Long.toString(cleanedDatagrams));
        properties.setProperty("selectedDatagrams", Long.toString(selectedDatagrams));
        properties.setProperty("skippedInvalidDatagrams", Long.toString(skippedInvalidDatagrams));
        properties.setProperty("validSegments", Long.toString(validSegments));
        properties.setProperty("partialRows", Integer.toString(partialRows));
        properties.setProperty("runtimeMillis", Long.toString(runtimeMillis));
        properties.setProperty("partialCsv", partialCsv);
        return IcePropertyMessage.encode(properties);
    }

    int partitionId() {
        return partitionId;
    }

    long rawDatagrams() {
        return rawDatagrams;
    }

    long cleanedDatagrams() {
        return cleanedDatagrams;
    }

    long selectedDatagrams() {
        return selectedDatagrams;
    }

    long skippedInvalidDatagrams() {
        return skippedInvalidDatagrams;
    }

    long validSegments() {
        return validSegments;
    }

    int partialRows() {
        return partialRows;
    }

    long runtimeMillis() {
        return runtimeMillis;
    }

    String partialCsv() {
        return partialCsv;
    }
}
