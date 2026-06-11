package edu.icesi.sitmmio.distributed;

import java.io.IOException;
import java.util.Properties;

public final class IcePartitionWorkResponse {
    private final int partitionId;
    private final long inputPoints;
    private final long validSegments;
    private final int partialRows;
    private final long runtimeMillis;
    private final String partialCsv;

    public IcePartitionWorkResponse(
            int partitionId,
            long inputPoints,
            long validSegments,
            int partialRows,
            long runtimeMillis,
            String partialCsv
    ) {
        this.partitionId = partitionId;
        this.inputPoints = inputPoints;
        this.validSegments = validSegments;
        this.partialRows = partialRows;
        this.runtimeMillis = runtimeMillis;
        this.partialCsv = partialCsv;
    }

    static IcePartitionWorkResponse fromSummary(int partitionId, WorkerRunSummary summary, String partialCsv) {
        return new IcePartitionWorkResponse(
                partitionId,
                summary.inputPoints(),
                summary.validSegments(),
                summary.partialRows(),
                summary.runtimeMillis(),
                partialCsv);
    }

    static IcePartitionWorkResponse decode(String encoded) throws IOException {
        Properties properties = IcePropertyMessage.decode(encoded);
        return new IcePartitionWorkResponse(
                IcePropertyMessage.intValue(properties, "partitionId"),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "inputPoints")),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "validSegments")),
                IcePropertyMessage.intValue(properties, "partialRows"),
                Long.parseLong(IcePropertyMessage.stringValue(properties, "runtimeMillis")),
                IcePropertyMessage.stringValue(properties, "partialCsv"));
    }

    String encode() {
        Properties properties = new Properties();
        properties.setProperty("partitionId", Integer.toString(partitionId));
        properties.setProperty("inputPoints", Long.toString(inputPoints));
        properties.setProperty("validSegments", Long.toString(validSegments));
        properties.setProperty("partialRows", Integer.toString(partialRows));
        properties.setProperty("runtimeMillis", Long.toString(runtimeMillis));
        properties.setProperty("partialCsv", partialCsv);
        return IcePropertyMessage.encode(properties);
    }

    int partitionId() {
        return partitionId;
    }

    long inputPoints() {
        return inputPoints;
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
