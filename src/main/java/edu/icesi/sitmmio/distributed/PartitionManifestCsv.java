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
import java.util.ArrayList;
import java.util.List;

public final class PartitionManifestCsv {
    private static final String PARTITION_ID = "partition_id";
    private static final String PARTITION_PATH = "partition_path";
    private static final String PARTIAL_RESULT_PATH = "partial_result_path";

    private static final CSVFormat OUTPUT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(PARTITION_ID, PARTITION_PATH, PARTIAL_RESULT_PATH)
            .get();

    public void write(Path outputPath, List<PartitionWorkItem> workItems) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, OUTPUT_FORMAT)) {
            for (PartitionWorkItem workItem : workItems) {
                printer.printRecord(
                        workItem.partitionId(),
                        workItem.partitionPath(),
                        workItem.partialResultPath());
            }
        }
    }

    public List<PartitionWorkItem> read(Path inputPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(inputPath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .get()
                     .parse(reader)) {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (CSVRecord record : parser) {
                workItems.add(new PartitionWorkItem(
                        Integer.parseInt(record.get(PARTITION_ID)),
                        Path.of(record.get(PARTITION_PATH)),
                        Path.of(record.get(PARTIAL_RESULT_PATH))));
            }
            return List.copyOf(workItems);
        }
    }
}
