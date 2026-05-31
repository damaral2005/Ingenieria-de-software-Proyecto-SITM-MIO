# Tasks: Version 1 Monolithic

## Repository Baseline

- [x] Create Gradle Java application structure.
- [x] Add `AGENTS.md` with Codex instructions.
- [x] Add Version 1 specification.
- [x] Add implementation plan.
- [x] Add task checklist.
- [x] Add README with project goal and Version 1 scope.
- [x] Add CLI placeholder.
- [x] Add basic domain records.
- [x] Add lightweight calculation contract test harness.
- [x] Verify `gradle clean build`.

## Data Discovery

- [ ] Read the data dictionary PDF.
- [ ] Document route CSV columns.
- [ ] Document datagram CSV columns.
- [ ] Decide whether average speed is read directly or derived.
- [ ] Decide calendar month timezone.

## Parsing

- [ ] Implement active route reader.
- [ ] Implement datagram reader.
- [ ] Add parser tests with synthetic fixtures.
- [ ] Add malformed-row diagnostics.

## Calculation

- [ ] Implement speed sample derivation.
- [ ] Implement active route filtering.
- [ ] Implement route/month aggregation.
- [ ] Add tests for invalid data.
- [ ] Add tests for deterministic ordering.

## Reporting

- [ ] Define output CSV schema.
- [ ] Implement CSV writer.
- [ ] Add record-count and timing summary.
- [ ] Document full CLI usage.

## Performance

- [ ] Measure mini pilot file runtime.
- [ ] Measure full pilot file runtime.
- [ ] Record baseline metrics.
