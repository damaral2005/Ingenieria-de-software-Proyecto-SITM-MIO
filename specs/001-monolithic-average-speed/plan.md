# Implementation Plan: Version 1 Monolithic

## Phase 0: Repository Baseline

- Create Java 17 Gradle CLI project.
- Add Codex project instructions in `AGENTS.md`.
- Add Spec Kit style files: `spec.md`, `plan.md`, and `tasks.md`.
- Add a minimal CLI entry point.
- Add immutable domain records and scalar speed math helpers.
- Add lightweight contract tests that run without external test dependencies.

## Phase 1: Data Discovery

- Inspect `Diccionario_De_Datos-OkGTM.pdf`.
- Confirm CSV delimiters, encodings, headers, column names, and units.
- Capture validated assumptions in `spec.md`.
- Create tiny synthetic CSV fixtures under test resources.

## Phase 2: Parsing

- Implement route CSV reader.
- Implement datagram CSV reader.
- Add validation diagnostics for malformed records.
- Test header mapping and invalid rows.

## Phase 3: Calculation

- Convert valid datagram rows into speed samples.
- Filter to active pilot routes.
- Group samples by route and month.
- Compute average speed in kilometers per hour.
- Track sample counts and rejected-record counts.

## Phase 4: Reporting

- Write deterministic CSV output.
- Include record counts and timing summaries.
- Document CLI arguments and expected paths.

## Phase 5: Performance Baseline

- Run on `datagrams-MiniPilot.csv`.
- Run on `datagrams4Pilot.csv`.
- Record elapsed time, processed rows, rejected rows, and output row count.

## Verification

Baseline:

```bash
gradle clean build
gradle run --args="--help"
```

Future algorithm verification:

```bash
gradle clean check
gradle run --args="--routes /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams-MiniPilot.csv --output build/reports/mini-average-speed.csv"
```
