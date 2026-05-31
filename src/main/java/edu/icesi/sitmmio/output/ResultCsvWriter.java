package edu.icesi.sitmmio.output;

import edu.icesi.sitmmio.model.RouteMonthSpeed;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public final class ResultCsvWriter {
    private static final CSVFormat OUTPUT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(
                    "route_id",
                    "month",
                    "total_distance_km",
                    "total_time_hours",
                    "avg_speed_kmh",
                    "avg_segment_speed_kmh",
                    "valid_segments",
                    "buses_observed")
            .get();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
            "0.000000",
            DecimalFormatSymbols.getInstance(Locale.ROOT));

    public void write(Path outputPath, List<RouteMonthSpeed> results) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, OUTPUT_FORMAT)) {
            for (RouteMonthSpeed result : results) {
                printer.printRecord(
                        result.routeId(),
                        result.month(),
                        format(result.totalDistanceKm()),
                        format(result.totalTimeHours()),
                        format(result.averageSpeedKmh()),
                        format(result.averageSegmentSpeedKmh()),
                        result.validSegments(),
                        result.observedBuses());
            }
        }
    }

    private static String format(double value) {
        return DECIMAL_FORMAT.format(value);
    }
}
