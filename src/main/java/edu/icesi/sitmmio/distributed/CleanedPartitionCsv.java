package edu.icesi.sitmmio.distributed;

import edu.icesi.sitmmio.model.GpsPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class CleanedPartitionCsv {
    static final String ROUTE_ID = "route_id";
    static final String BUS_ID = "bus_id";
    static final String TIMESTAMP = "timestamp";
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";

    static final CSVFormat OUTPUT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(ROUTE_ID, BUS_ID, TIMESTAMP, LATITUDE, LONGITUDE)
            .get();

    private CleanedPartitionCsv() {
    }

    static CSVPrinter openWriter(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        BufferedWriter writer = Files.newBufferedWriter(path);
        return new CSVPrinter(writer, OUTPUT_FORMAT);
    }

    static void print(CSVPrinter printer, GpsPoint point) throws IOException {
        printer.printRecord(
                point.routeId(),
                point.busId(),
                point.timestamp(),
                point.latitude(),
                point.longitude());
    }

    static List<GpsPoint> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            List<GpsPoint> points = new ArrayList<>();
            for (CSVRecord record : parser) {
                points.add(new GpsPoint(
                        record.get(ROUTE_ID),
                        record.get(BUS_ID),
                        Instant.parse(record.get(TIMESTAMP)),
                        Double.parseDouble(record.get(LATITUDE)),
                        Double.parseDouble(record.get(LONGITUDE))));
            }
            return List.copyOf(points);
        }
    }
}
