package edu.icesi.sitmmio.csv;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ColumnResolverTest {
    private final ColumnResolver resolver = new ColumnResolver();

    @Test
    void manualColumnWinsBeforeAliases() {
        String resolved = resolver.resolve(
                List.of("ruta_real", "route"),
                "ruta_real",
                ColumnAliases.ROUTE_ALIASES,
                "route");

        assertEquals("ruta_real", resolved);
    }

    @Test
    void resolvesAliasCaseInsensitively() {
        String resolved = resolver.resolve(
                List.of("Vehicle_ID", "Fecha_Hora"),
                null,
                ColumnAliases.BUS_ALIASES,
                "bus");

        assertEquals("Vehicle_ID", resolved);
    }

    @Test
    void missingManualColumnListsAvailableColumns() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(List.of("route", "bus"), "missing", ColumnAliases.ROUTE_ALIASES, "route"));

        assertTrue(exception.getMessage().contains("Manual route column 'missing' was not found"));
        assertTrue(exception.getMessage().contains("Available columns: route, bus"));
    }

    @Test
    void missingAutoDetectedColumnListsAliasesAndAvailableColumns() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(List.of("a", "b"), null, ColumnAliases.TIMESTAMP_ALIASES, "timestamp"));

        assertTrue(exception.getMessage().contains("Could not auto-detect timestamp column"));
        assertTrue(exception.getMessage().contains("Available columns: a, b"));
    }
}
