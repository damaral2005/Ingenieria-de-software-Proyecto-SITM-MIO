package edu.icesi.sitmmio.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ActiveRoutesCsvReaderTest {
    private final ActiveRoutesCsvReader reader = new ActiveRoutesCsvReader();

    @Test
    void readsActiveRoutesWithManualColumn() throws IOException {
        String csv = "nombre,codigo\n"
                + "Ruta 1,A01\n"
                + "Ruta 2,B02\n"
                + "Ruta duplicated,A01\n"
                + "Ruta blank,\n";

        Set<String> routes = reader.read(new StringReader(csv), "codigo");

        assertEquals(Set.of("A01", "B02"), routes);
    }

    @Test
    void readsActiveRoutesFromTemporaryCsvFile(@TempDir Path tempDir) throws IOException {
        Path csvPath = tempDir.resolve("lines.csv");
        Files.writeString(csvPath, "ruta,nombre\n"
                + "P10,Primera\n"
                + "P20,Segunda\n");

        Set<String> routes = reader.read(csvPath, null);

        assertEquals(Set.of("P10", "P20"), routes);
    }

    @Test
    void autoDetectsLineIdAliasForActiveRoutes() throws IOException {
        String csv = "LINEID,name\n"
                + "E21,Route E21\n";

        Set<String> routes = reader.read(new StringReader(csv), null);

        assertEquals(Set.of("E21"), routes);
    }
}
