package edu.icesi.sitmmio.distributed;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

final class IcePropertyMessage {
    private IcePropertyMessage() {
    }

    static String encode(Properties properties) {
        try {
            StringWriter writer = new StringWriter();
            properties.store(writer, "sitm-mio-ice-message");
            return writer.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode Ice message.", exception);
        }
    }

    static Properties decode(String message) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(message));
        return properties;
    }

    static String stringValue(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Missing Ice message property: " + key);
        }
        return value;
    }

    static int intValue(Properties properties, String key) throws IOException {
        try {
            return Integer.parseInt(stringValue(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer Ice message property: " + key, exception);
        }
    }

    static double doubleValue(Properties properties, String key) throws IOException {
        try {
            return Double.parseDouble(stringValue(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid decimal Ice message property: " + key, exception);
        }
    }

    static boolean booleanValue(Properties properties, String key) throws IOException {
        String value = stringValue(properties, key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IOException("Invalid boolean Ice message property: " + key);
    }

    static Integer optionalIntValue(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer Ice message property: " + key, exception);
        }
    }

    static String optionalStringValue(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
