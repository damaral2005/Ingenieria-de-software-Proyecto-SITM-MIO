package edu.icesi.sitmmio.csv;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ColumnResolver {
    public String resolve(Collection<String> availableColumns, String manualColumn, List<String> aliases, String purpose) {
        if (availableColumns == null || availableColumns.isEmpty()) {
            throw new IllegalArgumentException("CSV header is empty; cannot resolve " + purpose + " column.");
        }

        Map<String, String> normalizedToActual = availableColumns.stream()
                .collect(Collectors.toMap(
                        ColumnResolver::normalize,
                        Function.identity(),
                        (first, ignored) -> first));

        if (manualColumn != null && !manualColumn.isBlank()) {
            String actual = normalizedToActual.get(normalize(manualColumn));
            if (actual == null) {
                throw missingColumn("Manual " + purpose + " column '" + manualColumn + "' was not found",
                        availableColumns);
            }
            return actual;
        }

        for (String alias : aliases) {
            String actual = normalizedToActual.get(normalize(alias));
            if (actual != null) {
                return actual;
            }
        }

        throw missingColumn("Could not auto-detect " + purpose + " column using aliases " + aliases,
                availableColumns);
    }

    private static IllegalArgumentException missingColumn(String message, Collection<String> availableColumns) {
        return new IllegalArgumentException(message + ". Available columns: " + String.join(", ", availableColumns));
    }

    private static String normalize(String column) {
        return column.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }
}
