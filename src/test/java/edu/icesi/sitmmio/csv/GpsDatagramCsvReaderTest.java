package edu.icesi.sitmmio.csv;

import edu.icesi.sitmmio.model.GpsPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GpsDatagramCsvReaderTest {
    private final GpsDatagramCsvReader reader = new GpsDatagramCsvReader();

    @Test
    void readsGpsPointsWithManualColumns() throws IOException {
        String csv = "ruta,buseta,cuando,latitud,longitud\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n";
        CsvColumnConfig config = new CsvColumnConfig(
                null,
                "ruta",
                "buseta",
                "cuando",
                "latitud",
                "longitud");

        List<GpsPoint> points = reader.read(new StringReader(csv), Set.of("A01"), config);

        assertEquals(1, points.size());
        GpsPoint point = points.get(0);
        assertEquals("A01", point.routeId());
        assertEquals("BUS-1", point.busId());
        assertEquals(Instant.parse("2026-05-01T10:00:00Z"), point.timestamp());
        assertEquals(3.4516, point.latitude());
        assertEquals(-76.5320, point.longitude());
    }

    @Test
    void autoDetectsAliasesAndFiltersInactiveRoutesAndInvalidCoordinates() throws IOException {
        String csv = "line,vehicle_id,gps_time,lat,lng\n"
                + "A01,BUS-1,2026-05-01T10:00:00Z,3.4516,-76.5320\n"
                + "INACTIVE,BUS-2,2026-05-01T10:01:00Z,3.4516,-76.5320\n"
                + "A01,BUS-3,2026-05-01T10:02:00Z,91.0,-76.5320\n"
                + "A01,BUS-4,,3.4516,-76.5320\n";

        List<GpsPoint> points = reader.read(new StringReader(csv), Set.of("A01"), null);

        assertEquals(1, points.size());
        assertEquals("BUS-1", points.get(0).busId());
    }

    @Test
    void readsGpsPointsFromTemporaryCsvFile(@TempDir Path tempDir) throws IOException {
        Path csvPath = tempDir.resolve("datagrams.csv");
        Files.writeString(csvPath, "route,bus,timestamp,latitude,longitude\n"
                + "P10,B1,2026-05-01 10:00:00,3.4516,-76.5320\n"
                + "P10,B1,2026-05-01 10:01:00,3.4517,-76.5321\n");

        List<GpsPoint> points = reader.read(csvPath, Set.of("P10"), null);

        assertEquals(2, points.size());
        assertEquals(Instant.parse("2026-05-01T10:00:00Z"), points.get(0).timestamp());
    }

    @Test
    void readsHeaderlessDatagramsByIndexAndProcessesFirstRow() throws IOException {
        String csv = "EV,2026-05-01T09:00:00Z,STOP,9999,34516000,-765320000,TASK,A01,TRIP,X,"
                + "2026-05-01T10:00:00Z,BUS-1\n";
        CsvColumnConfig config = headerlessMiniPilotConfig();

        GpsDatagramReadResult result = reader.readWithStats(new StringReader(csv), Set.of("A01"), config);

        assertEquals(1, result.rawDatagrams());
        assertEquals(1, result.cleanedDatagrams().size());
        assertEquals(0, result.skippedInvalidRows());
        GpsPoint point = result.cleanedDatagrams().get(0);
        assertEquals("A01", point.routeId());
        assertEquals("BUS-1", point.busId());
        assertEquals(Instant.parse("2026-05-01T10:00:00Z"), point.timestamp());
        assertEquals(3.4516, point.latitude(), 0.000_001);
        assertEquals(-76.532, point.longitude(), 0.000_001);
    }

    @Test
    void skipsHeaderlessRowsWithTooFewColumnsOrInvalidCoordinates() throws IOException {
        String csv = "EV,2026-05-01T09:00:00Z,STOP,9999,34516000\n"
                + "EV,2026-05-01T09:00:00Z,STOP,9999,910000000,-765320000,TASK,A01,TRIP,X,"
                + "2026-05-01T10:00:00Z,BUS-1\n";

        GpsDatagramReadResult result = reader.readWithStats(
                new StringReader(csv),
                Set.of("A01"),
                headerlessMiniPilotConfig());

        assertEquals(2, result.rawDatagrams());
        assertEquals(0, result.cleanedDatagrams().size());
        assertEquals(2, result.skippedInvalidRows());
    }

    @Test
    void routeIndexSevenReadsLineIdNotOdometerIndexThree() throws IOException {
        String csv = "EV,2026-05-01T09:00:00Z,STOP,ODOMETER_VALUE,34516000,-765320000,TASK,A01,TRIP,X,"
                + "2026-05-01T10:00:00Z,BUS-1\n";

        GpsDatagramReadResult result = reader.readWithStats(
                new StringReader(csv),
                Set.of("A01"),
                headerlessMiniPilotConfig());

        assertEquals(1, result.cleanedDatagrams().size());
        assertEquals("A01", result.cleanedDatagrams().get(0).routeId());
    }

    private static CsvColumnConfig headerlessMiniPilotConfig() {
        return new CsvColumnConfig(
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                7,
                11,
                10,
                4,
                5,
                10_000_000.0);
    }
}
