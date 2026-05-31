package edu.icesi.sitmmio.csv;

import java.util.List;

final class ColumnAliases {
    static final List<String> ROUTE_ALIASES = List.of(
            "route",
            "route_id",
            "line",
            "line_id",
            "lineid",
            "ruta",
            "id_ruta",
            "idlinea");

    static final List<String> BUS_ALIASES = List.of(
            "bus",
            "bus_id",
            "vehicle",
            "vehicle_id",
            "id_bus",
            "vehiculo",
            "placa");

    static final List<String> TIMESTAMP_ALIASES = List.of(
            "timestamp",
            "time",
            "date",
            "datetime",
            "fecha",
            "fecha_hora",
            "gps_time");

    static final List<String> LATITUDE_ALIASES = List.of(
            "latitude",
            "lat",
            "latitud",
            "gps_lat");

    static final List<String> LONGITUDE_ALIASES = List.of(
            "longitude",
            "lon",
            "lng",
            "longitud",
            "gps_lon",
            "gps_lng");

    private ColumnAliases() {
    }
}
