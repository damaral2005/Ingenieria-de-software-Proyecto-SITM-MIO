package edu.icesi.sitmmio.distributed;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PartialResultCsv {
    private static final String ROUTE_ID = "route_id";
    private static final String MONTH = "month";
    private static final String TOTAL_DISTANCE_KM = "total_distance_km";
    private static final String TOTAL_TIME_HOURS = "total_time_hours";
    private static final String TOTAL_SEGMENT_SPEED_KMH = "total_segment_speed_kmh";
    private static final String VALID_SEGMENTS = "valid_segments";
    private static final String BUSES_OBSERVED = "buses_observed";

    private static final CSVFormat OUTPUT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(
                    ROUTE_ID,
                    MONTH,
                    TOTAL_DISTANCE_KM,
                    TOTAL_TIME_HOURS,
                    TOTAL_SEGMENT_SPEED_KMH,
                    VALID_SEGMENTS,
                    BUSES_OBSERVED)
            .get();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
            "0.000000",
            DecimalFormatSymbols.getInstance(Locale.ROOT));

    public void write(Path outputPath, List<PartialRouteMonthAggregate> aggregates) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, OUTPUT_FORMAT)) {
            for (PartialRouteMonthAggregate aggregate : aggregates) {
                printer.printRecord(
                        aggregate.routeId(),
                        aggregate.month(),
                        format(aggregate.totalDistanceKm()),
                        format(aggregate.totalTimeHours()),
                        format(aggregate.totalSegmentSpeedKmh()),
                        aggregate.validSegments(),
                        aggregate.observedBuses());
            }
        }
    }

    public List<PartialRouteMonthAggregate> read(Path inputPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(inputPath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            List<PartialRouteMonthAggregate> aggregates = new ArrayList<>();
            for (CSVRecord record : parser) {
                aggregates.add(new PartialRouteMonthAggregate(
                        record.get(ROUTE_ID),
                        YearMonth.parse(record.get(MONTH)),
                        Double.parseDouble(record.get(TOTAL_DISTANCE_KM)),
                        Double.parseDouble(record.get(TOTAL_TIME_HOURS)),
                        Double.parseDouble(record.get(TOTAL_SEGMENT_SPEED_KMH)),
                        Long.parseLong(record.get(VALID_SEGMENTS)),
                        Long.parseLong(record.get(BUSES_OBSERVED))));
            }
            return List.copyOf(aggregates);
        }
    }

    private static String format(double value) {
        return DECIMAL_FORMAT.format(value);
    }
}
