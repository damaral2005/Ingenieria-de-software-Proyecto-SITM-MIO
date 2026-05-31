# Tasks: Version 1 Monolithic Speed Calculator

Keep each task small enough for a focused commit.

## Spec Baseline

- [x] Create `spec.md`.
- [x] Create `plan.md`.
- [x] Create `tasks.md`.
- [x] Create `research.md`.
- [ ] Reconcile this spec with any older Version 1 spec folder.

## Calculation Foundations

- [x] Add Haversine distance helper.
- [x] Test Haversine with known coordinate pairs.
- [x] Add duration helper that converts timestamps to positive hour deltas.
- [x] Test zero and negative time delta rejection.
- [x] Add segment speed calculation.
- [x] Test max speed threshold rejection.
- [x] Test max time gap threshold rejection.

## Domain Model

- [x] Add immutable GPS point record.
- [x] Add immutable speed segment record.
- [x] Add route/month aggregate record.
- [ ] Add diagnostics model for rejected rows and segments.

## In-Memory Aggregation

- [x] Implement active route filtering for in-memory datagrams.
- [x] Create valid segments from consecutive sorted points in the same route and bus.
- [x] Aggregate total distance by route and year-month.
- [x] Aggregate total time by route and year-month.
- [x] Compute weighted average speed.
- [x] Compute arithmetic average segment speed.
- [x] Count valid segments.
- [x] Count observed buses.
- [x] Add small in-memory fixture test with known expected result.
- [x] Verify deterministic result ordering.

## CSV Input

- [x] Implement active route CSV reader with configurable route column.
- [x] Implement datagram CSV reader with configurable route, bus, timestamp, latitude, and longitude columns.
- [x] Support configurable headers without hardcoding assignment-specific names.
- [x] Add tests for missing columns.
- [x] Add tests for invalid coordinates.
- [x] Add tests for missing required fields.
- [ ] Add row rejection counts by reason.

## CSV Output

- [x] Create `results/` output directory when needed.
- [x] Write route/month aggregates as CSV.
- [x] Use deterministic column order.
- [x] Use locale-independent decimal formatting.
- [x] Add output writer test.

## CLI

- [x] Implement `--lines`.
- [x] Implement `--datagrams`.
- [x] Implement `--output`.
- [x] Implement configurable column-name options.
- [x] Implement `--max-gap-minutes`.
- [x] Implement `--max-speed-kmh`.
- [x] Keep `--help` current.
- [x] Print run diagnostics.

## Documentation

- [x] Update README with Version 1 command examples.
- [x] Document default thresholds.
- [x] Document required CSV columns and configurable aliases.
- [x] Document result CSV schema.

## Verification

- [x] Run `./gradlew build`.
- [x] Run `./gradlew test`.
- [x] Run `./gradlew run --args="--help"`.
- [ ] Run MiniPilot command against `/opt/sitm-mio/datagrams-MiniPilot.csv`.
- [ ] Save MiniPilot output under `results/`.
- [ ] Record MiniPilot runtime and counts.
- [ ] Run full pilot command against `/opt/sitm-mio/datagrams4Pilot.csv`.
- [ ] Save full pilot output under `results/`.
- [ ] Record full pilot runtime and counts.

## Guardrails

- [x] Confirm no concurrency was introduced.
- [x] Confirm no distributed pattern was introduced.
- [x] Confirm no network runtime dependency was introduced.
- [x] Confirm no database dependency was introduced.
- [ ] Confirm raw university input files were not committed.
