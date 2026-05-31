# Specification: Version 1 Monolithic Speed Calculator

## Status

Draft specification for implementation. This document defines Version 1 behavior before algorithm work begins.

## Purpose

Calculate average SITM-MIO bus speed by route and year-month for all active routes listed in `lines-241-ActiveGT.csv`, using GPS datagrams from `datagrams-MiniPilot.csv` first. The same design must later run against `datagrams4Pilot.csv` without architecture changes.

## Version Scope

Version 1 is a monolithic Java CLI application:

- Single process.
- Single JVM.
- Local CSV inputs.
- Local CSV output under `results/`.
- No concurrency.
- No distributed architecture.
- No external database or web service.

## Inputs

Default assignment paths:

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
/opt/sitm-mio/datagrams-MiniPilot.csv
```

Later validation input:

```text
/opt/sitm-mio/datagrams4Pilot.csv
```

The application must support configurable column names because real headers may differ from assumptions. At minimum, configuration must identify:

- Active route file route ID column.
- Datagram route ID column.
- Datagram bus ID column.
- Datagram timestamp column.
- Datagram latitude column.
- Datagram longitude column.

## Output

Generate a deterministic CSV file in `results/`.

Required output columns:

```text
route_id,year_month,total_distance_km,total_time_hours,average_speed_kmh,average_segment_speed_kmh,valid_segment_count,observed_bus_count
```

Ordering must be stable:

1. `route_id` ascending.
2. `year_month` ascending.

Numeric output must use a stable decimal format independent of machine locale.

## Domain Model

A GPS datagram represents one bus position at one timestamp.

To compute speed segments:

1. Filter datagrams to active routes.
2. Sort accepted datagrams by route, bus, and timestamp.
3. Compare each point with the previous point from the same route and same bus.
4. Compute distance between the two coordinates with Haversine.
5. Compute elapsed time.
6. Convert the pair into a speed segment if valid.
7. Aggregate valid speed segments by route and year-month.

The segment month must be derived deterministically from the later point timestamp unless later data-dictionary review requires another rule.

## Functional Requirements

### Active Routes

- Read active route IDs from `lines-241-ActiveGT.csv`.
- Trim route IDs.
- Ignore blank route IDs.
- Deduplicate route IDs.
- Fail fast with a clear error if the route ID column is missing.

### Datagrams

- Read datagrams from `datagrams-MiniPilot.csv`.
- Parse route ID, bus ID, timestamp, latitude, and longitude using configured column names.
- Trim route and bus IDs.
- Filter out datagrams whose route ID is not in the active route set.
- Keep processing after rejecting invalid rows, while tracking rejection counts.

### Segment Calculation

For each route and bus:

- Sort points by timestamp.
- Compare each point with its immediate previous point.
- Compute distance in kilometers using the Haversine formula.
- Compute duration in hours.
- Compute speed as `distance_km / duration_hours`.

### Rejection Rules

Discard datagram rows with:

- Missing route ID.
- Missing bus ID.
- Missing timestamp.
- Missing latitude or longitude.
- Unparseable timestamp.
- Unparseable latitude or longitude.
- Invalid coordinates:
  - latitude outside `[-90, 90]`
  - longitude outside `[-180, 180]`

Discard computed segments with:

- Zero or negative time delta.
- Time gap above a configurable threshold.
- Speed above a configurable threshold.
- Non-finite distance, duration, or speed.

Default thresholds:

- Maximum segment time gap: `30 minutes`.
- Maximum plausible speed: `120 km/h`.

These defaults may be changed by CLI options or config once implementation starts.

### Aggregation

Aggregate valid segments by route and year-month.

For each group, compute:

- `total_distance_km`: sum of segment distances.
- `total_time_hours`: sum of segment durations.
- `average_speed_kmh`: `total_distance_km / total_time_hours`.
- `average_segment_speed_kmh`: arithmetic mean of valid segment speeds.
- `valid_segment_count`: number of valid segments.
- `observed_bus_count`: number of unique bus IDs contributing at least one valid segment.

Groups with zero valid segments must not be emitted.

### Diagnostics

The CLI must print a concise run summary:

- Input route file.
- Input datagram file.
- Output file.
- Active route count.
- Datagram rows read.
- Datagram rows accepted.
- Datagram rows rejected by reason.
- Valid segment count.
- Segment rejection counts by reason.
- Output row count.
- Elapsed runtime.

## Non-Functional Requirements

- Prioritize correctness over premature optimization.
- Runtime must be measured for each full run.
- Memory use must be reasonable for `datagrams-MiniPilot.csv` and `datagrams4Pilot.csv`.
- Output must be reproducible across runs on the same input.
- Error messages must be actionable.
- Code must remain readable and testable.

## Architecture Constraints

Version 1 must not include:

- Concurrency.
- Distributed architecture.
- Architecture distribution patterns.
- External database.
- Web service.
- REST API.
- RMI.
- Sockets.
- Message broker.
- Client-server separation.
- Spring Boot runtime.

## CLI Requirements

Minimum planned usage:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv"
```

Expected configurable options:

- `--lines <path>`
- `--datagrams <path>`
- `--output <path>`
- `--active-route-col <name>`
- `--route-col <name>`
- `--bus-col <name>`
- `--timestamp-col <name>`
- `--latitude-col <name>`
- `--longitude-col <name>`
- `--max-gap-minutes <number>`
- `--max-speed-kmh <number>`

## Testing Requirements

Required tests:

- Unit test Haversine distance with known coordinates.
- Unit test time delta validation.
- Unit test speed filtering.
- Unit test route/month aggregation.
- Small in-memory fixture test with known expected output.

Tests must avoid depending on university data files.

## Open Questions

- Exact CSV headers and delimiter for both files.
- Timestamp format and timezone.
- Whether route IDs require normalization beyond trimming.
- Whether `datagrams4Pilot.csv` has the same schema as `datagrams-MiniPilot.csv`.
- Whether the instructor expects weighted average speed, arithmetic mean of segment speeds, or both. Version 1 will output both.
