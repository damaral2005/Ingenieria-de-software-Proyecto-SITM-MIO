package edu.icesi.sitmmio.distributed;

public final class PartitionKey {
    private PartitionKey() {
    }

    public static int partitionId(String routeId, String busId, int partitionCount) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalArgumentException("Route id is required.");
        }
        if (busId == null || busId.isBlank()) {
            throw new IllegalArgumentException("Bus id is required.");
        }
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("Partition count must be greater than zero.");
        }
        String key = routeId + "\u0000" + busId;
        return Math.floorMod(key.hashCode(), partitionCount);
    }
}
