package edu.icesi.sitmmio.csv;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ActiveRoutesCsvReader {
    private final ColumnResolver columnResolver;
    private final DataCleaner dataCleaner;

    public ActiveRoutesCsvReader() {
        this(new ColumnResolver(), new DataCleaner());
    }

    ActiveRoutesCsvReader(ColumnResolver columnResolver, DataCleaner dataCleaner) {
        this.columnResolver = columnResolver;
        this.dataCleaner = dataCleaner;
    }

    public Set<String> read(Path path, String manualActiveRouteColumn) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return read(reader, manualActiveRouteColumn);
        }
    }

    public Set<String> read(Reader reader, String manualActiveRouteColumn) throws IOException {
        try (CSVParser parser = CsvFormats.inputWithHeader().parse(reader)) {
            String routeColumn = columnResolver.resolve(
                    parser.getHeaderMap().keySet(),
                    manualActiveRouteColumn,
                    ColumnAliases.ROUTE_ALIASES,
                    "active route");
            Set<String> activeRoutes = new LinkedHashSet<>();
            for (CSVRecord record : parser) {
                dataCleaner.cleanRequiredText(record.get(routeColumn)).ifPresent(activeRoutes::add);
            }
            return Set.copyOf(activeRoutes);
        }
    }
}
