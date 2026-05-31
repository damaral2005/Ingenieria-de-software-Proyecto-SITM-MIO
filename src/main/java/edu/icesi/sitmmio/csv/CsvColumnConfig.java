package edu.icesi.sitmmio.csv;

public final class CsvColumnConfig {
    private final String activeRouteColumn;
    private final String routeColumn;
    private final String busColumn;
    private final String timestampColumn;
    private final String latitudeColumn;
    private final String longitudeColumn;
    private final boolean datagramsHasHeader;
    private final Integer routeIndex;
    private final Integer busIndex;
    private final Integer timestampIndex;
    private final Integer latitudeIndex;
    private final Integer longitudeIndex;
    private final double coordinateScale;

    public CsvColumnConfig(
            String activeRouteColumn,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn
    ) {
        this(
                activeRouteColumn,
                routeColumn,
                busColumn,
                timestampColumn,
                latitudeColumn,
                longitudeColumn,
                true,
                null,
                null,
                null,
                null,
                null,
                1.0);
    }

    public CsvColumnConfig(
            String activeRouteColumn,
            String routeColumn,
            String busColumn,
            String timestampColumn,
            String latitudeColumn,
            String longitudeColumn,
            boolean datagramsHasHeader,
            Integer routeIndex,
            Integer busIndex,
            Integer timestampIndex,
            Integer latitudeIndex,
            Integer longitudeIndex,
            double coordinateScale
    ) {
        if (!Double.isFinite(coordinateScale) || coordinateScale <= 0.0) {
            throw new IllegalArgumentException("Coordinate scale must be greater than zero.");
        }
        this.activeRouteColumn = activeRouteColumn;
        this.routeColumn = routeColumn;
        this.busColumn = busColumn;
        this.timestampColumn = timestampColumn;
        this.latitudeColumn = latitudeColumn;
        this.longitudeColumn = longitudeColumn;
        this.datagramsHasHeader = datagramsHasHeader;
        this.routeIndex = routeIndex;
        this.busIndex = busIndex;
        this.timestampIndex = timestampIndex;
        this.latitudeIndex = latitudeIndex;
        this.longitudeIndex = longitudeIndex;
        this.coordinateScale = coordinateScale;
    }

    public String activeRouteColumn() {
        return activeRouteColumn;
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
}
