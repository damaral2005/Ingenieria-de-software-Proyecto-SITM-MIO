package edu.icesi.sitmmio.csv;

import org.apache.commons.csv.CSVFormat;

public final class CsvFormats {
    private CsvFormats() {
    }

    public static CSVFormat inputWithHeader() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get();
    }

    public static CSVFormat inputWithoutHeader() {
        return CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .get();
    }
}
