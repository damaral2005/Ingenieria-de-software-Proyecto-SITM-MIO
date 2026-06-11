package edu.icesi.sitmmio.distributed;

import java.io.IOException;
import java.util.Properties;

public final class IcePartitionWorkRequest {
    private final int partitionId;
    private final int maxGapMinutes;
    private final double maxSpeedKmh;
    private final String partitionCsv;

    public IcePartitionWorkRequest(
            int partitionId,
            int maxGapMinutes,
            double maxSpeedKmh,
            String partitionCsv
    ) {
        this.partitionId = partitionId;
        this.maxGapMinutes = maxGapMinutes;
        this.maxSpeedKmh = maxSpeedKmh;
        this.partitionCsv = partitionCsv;
    }

    static IcePartitionWorkRequest decode(String encoded) throws IOException {
        Properties properties = IcePropertyMessage.decode(encoded);
        return new IcePartitionWorkRequest(
                IcePropertyMessage.intValue(properties, "partitionId"),
                IcePropertyMessage.intValue(properties, "maxGapMinutes"),
                IcePropertyMessage.doubleValue(properties, "maxSpeedKmh"),
                IcePropertyMessage.stringValue(properties, "partitionCsv"));
    }

    String encode() {
        Properties properties = new Properties();
        properties.setProperty("partitionId", Integer.toString(partitionId));
        properties.setProperty("maxGapMinutes", Integer.toString(maxGapMinutes));
        properties.setProperty("maxSpeedKmh", Double.toString(maxSpeedKmh));
        properties.setProperty("partitionCsv", partitionCsv);
        return IcePropertyMessage.encode(properties);
    }

    int partitionId() {
        return partitionId;
    }

    int maxGapMinutes() {
        return maxGapMinutes;
    }

    double maxSpeedKmh() {
        return maxSpeedKmh;
    }

    String partitionCsv() {
        return partitionCsv;
    }
}
