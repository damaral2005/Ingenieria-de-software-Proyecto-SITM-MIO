# Plan: Version 1 Monolithic Speed Calculator

## Implementation Strategy

Build the monolithic calculator in small, reviewable steps. Keep all computation in-process and deterministic. Start with pure calculation code and in-memory tests before adding file I/O.

## Implementation Order

### 1. Calculation Foundations

- Add Haversine distance helper.
- Add time delta helper.
- Add speed calculation and validation helper.
- Add focused unit tests for distance, duration, and speed thresholds.

### 2. Domain Objects

- Define immutable Java 11-compatible classes for:
  - active route ID
  - raw datagram point
  - valid segment
  - route/month aggregate
  - run diagnostics
- Keep domain records independent from CSV parsing.

### 3. Aggregation

- Implement in-memory route/bus/timestamp sorting.
- Build valid segments from consecutive points for the same route and bus.
- Aggregate by route and year-month.
- Track observed buses per aggregate.
- Add fixture test with known expected result.

### 4. CSV Reading

- Implement configurable-header CSV reader for active routes.
- Implement configurable-header CSV reader for datagrams.
- Add tests with synthetic CSV strings or test resources.
- Track row-level rejection reasons.

### 5. CSV Writing

- Implement deterministic result writer.
- Use stable ordering and locale-independent decimal formatting.
- Write outputs under `results/`.

### 6. CLI Wiring

- Parse CLI options.
- Apply default thresholds.
- Run route loading, datagram loading, segment calculation, aggregation, and output writing.
- Print concise run diagnostics.

### 7. Validation On Assignment Files

- Run first on `datagrams-MiniPilot.csv`.
- Record runtime, row counts, rejection counts, output rows, and any schema assumptions.
- Run later on `datagrams4Pilot.csv` with the same monolithic implementation.

## Validation Commands

Build:

```bash
./gradlew clean build
```

Run all checks:

```bash
./gradlew check
```

Run CLI help:

```bash
./gradlew run --args="--help"
```

Run MiniPilot calculation:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv"
```

Run full pilot calculation:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams4Pilot.csv --output results/route_month_speeds_pilot.csv"
```

## Design Decisions

- Use Haversine for geographic distance because datagrams provide latitude/longitude positions.
- Use weighted average speed as `total_distance_km / total_time_hours`.
- Also output arithmetic average of segment speeds for comparison.
- Assign a segment to the year-month of the later GPS point timestamp.
- Keep Version 1 single-threaded, even if sorting large inputs is slower than later concurrent versions.

## Risks

- Unknown CSV headers may require quick config changes.
- Unknown timestamp timezone may affect month grouping near month boundaries.
- Large datagram files may require memory-conscious sorting or streaming-by-key in later versions.
- GPS noise can create unrealistic short-duration high-speed segments, so speed thresholding is required.

## Performance Measurement

Each real-data run must record:

- Input file names.
- Active route count.
- Datagram rows read.
- Datagram rows accepted after route filtering.
- Datagram row rejection counts.
- Valid segments.
- Segment rejection counts.
- Output aggregate rows.
- Wall-clock elapsed time.

Do not introduce concurrency for performance in Version 1.
