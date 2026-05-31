package edu.icesi.sitmmio.csv;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

final class TimestampParser {
    private static final List<DateTimeFormatter> LOCAL_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

    private TimestampParser() {
    }

    static Instant parse(String rawTimestamp) {
        String value = rawTimestamp.trim();
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            // Try offset and local formats below.
        }

        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try local formats below.
        }

        for (DateTimeFormatter formatter : LOCAL_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                // Try the next supported deterministic format.
            }
        }

        throw new IllegalArgumentException("Unparseable timestamp: " + rawTimestamp);
    }
}
