package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;

import java.util.List;

public final class GpsDatagramReadResult {
    private final long rawDatagrams;
    private final List<GpsPoint> cleanedDatagrams;
    private final long skippedInvalidRows;

    public GpsDatagramReadResult(long rawDatagrams, List<GpsPoint> cleanedDatagrams) {
        this(rawDatagrams, cleanedDatagrams, Math.max(0, rawDatagrams - cleanedDatagrams.size()));
    }

    public GpsDatagramReadResult(long rawDatagrams, List<GpsPoint> cleanedDatagrams, long skippedInvalidRows) {
        if (rawDatagrams < 0) {
            throw new IllegalArgumentException("Raw datagram count must not be negative.");
        }
        if (skippedInvalidRows < 0) {
            throw new IllegalArgumentException("Skipped invalid row count must not be negative.");
        }
        this.rawDatagrams = rawDatagrams;
        this.cleanedDatagrams = List.copyOf(cleanedDatagrams);
        this.skippedInvalidRows = skippedInvalidRows;
    }

    public long rawDatagrams() {
        return rawDatagrams;
    }

    public List<GpsPoint> cleanedDatagrams() {
        return cleanedDatagrams;
    }

    public long skippedInvalidRows() {
        return skippedInvalidRows;
    }
}
