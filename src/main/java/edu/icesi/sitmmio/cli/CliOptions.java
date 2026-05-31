package edu.icesi.sitmmio.cli;

import java.nio.file.Path;

public final class CliOptions {
    public static final int DEFAULT_MAX_GAP_MINUTES = 10;
    public static final double DEFAULT_MAX_SPEED_KMH = 120.0;
    public static final boolean DEFAULT_DATAGRAMS_HAS_HEADER = true;
    public static final double DEFAULT_COORDINATE_SCALE = 1.0;

    private final Path linesPath;
    private final Path datagramsPath;
    private final Path outputPath;
    private final String routeColumn;
    private final String busColumn;
    private final String timestampColumn;
    private final String latitudeColumn;
    private final String longitudeColumn;
    private final String activeRouteColumn;
    private final boolean datagramsHasHeader;
    private final Integer routeIndex;
    private final Integer busIndex;
    private final Integer timestampIndex;
    private final Integer latitudeIndex;
    private final Integer longitudeIndex;
    private final double coordinateScale;
    private final int maxGapMinutes;
    private final double maxSpeedKmh;

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        this(
                linesPath,
                datagramsPath,
                outputPath,
                routeColumn,
                busColumn,
                timestampColumn,
                latitudeColumn,
                longitudeColumn,
                activeRouteColumn,
                DEFAULT_DATAGRAMS_HAS_HEADER,
                null,
                null,
                null,
                null,
                null,
                DEFAULT_COORDINATE_SCALE,
                maxGapMinutes,
                maxSpeedKmh);
    }

    public CliOptions(
            Path linesPath,
            Path datagramsPath,
            Path outputPath,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            String activeRouteColumn,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        if (linesPath == null) {
            throw new IllegalArgumentException("Lines path is required.");
        }
        if (datagramsPath == null) {
            throw new IllegalArgumentException("Datagrams path is required.");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path is required.");
        }
        if (maxGapMinutes <= 0) {
            throw new IllegalArgumentException("Max gap minutes must be greater than zero.");
        }
        if (!Double.isFinite(maxSpeedKmh) || maxSpeedKmh <= 0.0) {
            throw new IllegalArgumentException("Max speed must be greater than zero.");
        }
        if (!Double.isFinite(coordinateScale) || coordinateScale <= 0.0) {
            throw new IllegalArgumentException("Coordinate scale must be greater than zero.");
        }
        this.linesPath = linesPath;
        this.datagramsPath = datagramsPath;
        this.outputPath = outputPath;
        this.routeColumn = routeColumn;
        this.busColumn = busColumn;
        this.timestampColumn = timestampColumn;
        this.latitudeColumn = latitudeColumn;
        this.longitudeColumn = longitudeColumn;
        this.activeRouteColumn = activeRouteColumn;
        this.datagramsHasHeader = datagramsHasHeader;
        this.routeIndex = routeIndex;
        this.busIndex = busIndex;
        this.timestampIndex = timestampIndex;
        this.latitudeIndex = latitudeIndex;
        this.longitudeIndex = longitudeIndex;
        this.coordinateScale = coordinateScale;
        this.maxGapMinutes = maxGapMinutes;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public Path linesPath() {
        return linesPath;
    }

    public Path datagramsPath() {
        return datagramsPath;
    }

    public Path outputPath() {
        return outputPath;
    }

    public String routeColumn() {
        return routeColumn;
    }

    public String busColumn() {
        return busColumn;
    }

    public String timestampColumn() {
        return timestampColumn;
    }

    public String latitudeColumn() {
        return latitudeColumn;
    }

    public String longitudeColumn() {
        return longitudeColumn;
    }

    public String activeRouteColumn() {
        return activeRouteColumn;
    }

    public boolean datagramsHasHeader() {
        return datagramsHasHeader;
    }

    public Integer routeIndex() {
        return routeIndex;
    }

    public Integer busIndex() {
        return busIndex;
    }

    public Integer timestampIndex() {
        return timestampIndex;
    }

    public Integer latitudeIndex() {
        return latitudeIndex;
    }

    public Integer longitudeIndex() {
        return longitudeIndex;
    }

    public double coordinateScale() {
        return coordinateScale;
    }

    public int maxGapMinutes() {
        return maxGapMinutes;
    }

    public double maxSpeedKmh() {
        return maxSpeedKmh;
    }
}
