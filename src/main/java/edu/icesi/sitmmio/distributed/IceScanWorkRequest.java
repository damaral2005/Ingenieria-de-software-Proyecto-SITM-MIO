package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.cli.CliOptions;
import edu.icesi.sitmmio.csv.CsvColumnConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public final class IceScanWorkRequest {
    private final Path linesPath;
    private final Path datagramsPath;
    private final CsvColumnConfig columnConfig;
    private final int partitionId;
    private final int partitionCount;
    private final int maxGapMinutes;
    private final double maxSpeedKmh;

    public IceScanWorkRequest(
            Path linesPath,
            Path datagramsPath,
            CsvColumnConfig columnConfig,
            int partitionId,
            int partitionCount,
            int maxGapMinutes,
            double maxSpeedKmh
    ) {
        this.linesPath = linesPath;
        this.datagramsPath = datagramsPath;
        this.columnConfig = columnConfig;
        this.partitionId = partitionId;
        this.partitionCount = partitionCount;
        this.maxGapMinutes = maxGapMinutes;
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public static IceScanWorkRequest fromOptions(CliOptions options, int partitionId) {
        return new IceScanWorkRequest(
                options.linesPath(),
                options.datagramsPath(),
                new CsvColumnConfig(
                        options.activeRouteColumn(),
                        options.routeColumn(),
                        options.busColumn(),
                        options.timestampColumn(),
                        options.latitudeColumn(),
                        options.longitudeColumn(),
                        options.datagramsHasHeader(),
                        options.routeIndex(),
                        options.busIndex(),
                        options.timestampIndex(),
                        options.latitudeIndex(),
                        options.longitudeIndex(),
                        options.coordinateScale()),
                partitionId,
                options.partitionCount(),
                options.maxGapMinutes(),
                options.maxSpeedKmh());
    }

    static IceScanWorkRequest decode(String encoded) throws IOException {
        Properties properties = IcePropertyMessage.decode(encoded);
        CsvColumnConfig columnConfig = new CsvColumnConfig(
                IcePropertyMessage.optionalStringValue(properties, "activeRouteColumn"),
                IcePropertyMessage.optionalStringValue(properties, "routeColumn"),
                IcePropertyMessage.optionalStringValue(properties, "busColumn"),
                IcePropertyMessage.optionalStringValue(properties, "timestampColumn"),
                IcePropertyMessage.optionalStringValue(properties, "latitudeColumn"),
                IcePropertyMessage.optionalStringValue(properties, "longitudeColumn"),
                IcePropertyMessage.booleanValue(properties, "datagramsHasHeader"),
                IcePropertyMessage.optionalIntValue(properties, "routeIndex"),
                IcePropertyMessage.optionalIntValue(properties, "busIndex"),
                IcePropertyMessage.optionalIntValue(properties, "timestampIndex"),
                IcePropertyMessage.optionalIntValue(properties, "latitudeIndex"),
                IcePropertyMessage.optionalIntValue(properties, "longitudeIndex"),
                IcePropertyMessage.doubleValue(properties, "coordinateScale"));
        return new IceScanWorkRequest(
                Path.of(IcePropertyMessage.stringValue(properties, "linesPath")),
                Path.of(IcePropertyMessage.stringValue(properties, "datagramsPath")),
                columnConfig,
                IcePropertyMessage.intValue(properties, "partitionId"),
                IcePropertyMessage.intValue(properties, "partitionCount"),
                IcePropertyMessage.intValue(properties, "maxGapMinutes"),
                IcePropertyMessage.doubleValue(properties, "maxSpeedKmh"));
    }

    String encode() {
        Properties properties = new Properties();
        properties.setProperty("linesPath", linesPath.toString());
        properties.setProperty("datagramsPath", datagramsPath.toString());
        setOptional(properties, "activeRouteColumn", columnConfig.activeRouteColumn());
        setOptional(properties, "routeColumn", columnConfig.routeColumn());
        setOptional(properties, "busColumn", columnConfig.busColumn());
        setOptional(properties, "timestampColumn", columnConfig.timestampColumn());
        setOptional(properties, "latitudeColumn", columnConfig.latitudeColumn());
        setOptional(properties, "longitudeColumn", columnConfig.longitudeColumn());
        properties.setProperty("datagramsHasHeader", Boolean.toString(columnConfig.datagramsHasHeader()));
        setOptional(properties, "routeIndex", columnConfig.routeIndex());
        setOptional(properties, "busIndex", columnConfig.busIndex());
        setOptional(properties, "timestampIndex", columnConfig.timestampIndex());
        setOptional(properties, "latitudeIndex", columnConfig.latitudeIndex());
        setOptional(properties, "longitudeIndex", columnConfig.longitudeIndex());
        properties.setProperty("coordinateScale", Double.toString(columnConfig.coordinateScale()));
        properties.setProperty("partitionId", Integer.toString(partitionId));
        properties.setProperty("partitionCount", Integer.toString(partitionCount));
        properties.setProperty("maxGapMinutes", Integer.toString(maxGapMinutes));
        properties.setProperty("maxSpeedKmh", Double.toString(maxSpeedKmh));
        return IcePropertyMessage.encode(properties);
    }

    Path linesPath() {
        return linesPath;
    }

    Path datagramsPath() {
        return datagramsPath;
    }

    CsvColumnConfig columnConfig() {
        return columnConfig;
    }

    int partitionId() {
        return partitionId;
    }

    int partitionCount() {
        return partitionCount;
    }

    int maxGapMinutes() {
        return maxGapMinutes;
    }

    double maxSpeedKmh() {
        return maxSpeedKmh;
    }

    private static void setOptional(Properties properties, String key, Object value) {
        if (value != null) {
            properties.setProperty(key, value.toString());
        }
    }
}
