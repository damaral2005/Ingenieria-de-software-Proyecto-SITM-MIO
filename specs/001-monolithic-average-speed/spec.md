# Specification: Version 1 Monolithic Average Speed

## Status

Draft for repository setup. Algorithm implementation is not part of this first step.

## Problem

Given SITM-MIO route metadata and datagram records, calculate average speeds by route and month for every active pilot route in the assignment dataset.

## Goals

- Produce correct monthly average speed results for all active pilot routes.
- Keep the Version 1 architecture monolithic and easy to verify.
- Make performance measurable on the provided CSV files.
- Keep file parsing, validation, calculation, and reporting responsibilities clear.

## Non-Goals

- No distributed processing.
- No microservices.
- No message queues.
- No network calls.
- No database.
- No Spring Boot runtime.
- No concurrency in Version 1 unless a future spec explicitly introduces it.

## Inputs

University server paths:

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
/opt/sitm-mio/datagrams-MiniPilot.csv
/opt/sitm-mio/datagrams4Pilot.csv
/opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

The data dictionary must be reviewed before implementing parsers. Column names, units, timestamp formats, route identifiers, and validity rules must be confirmed from that document.

## Output

Version 1 should produce deterministic tabular output with at least:

- Route identifier.
- Month.
- Number of valid samples contributing to the average.
- Average speed in kilometers per hour.

The final output format is still open. Prefer CSV unless the assignment requires another format.

## Functional Requirements

1. Load the active pilot routes from `lines-241-ActiveGT.csv`.
2. Load datagram records from the pilot datagram CSV files.
3. Match datagrams to active pilot routes.
4. Derive or read movement speed according to the data dictionary.
5. Reject records that cannot produce a valid speed sample.
6. Group valid speed samples by route and calendar month.
7. Calculate average speed per route/month group.
8. Produce deterministic output ordering.
9. Report rejected-record counts or validation diagnostics in a simple form.

## Quality Requirements

- Correctness is more important than premature optimization.
- The implementation must be measurable with timing and record-count summaries.
- Parsing and aggregation should avoid unnecessary object churn once the data shape is known.
- Tests must cover scalar speed math, grouping, filtering, invalid data handling, and output ordering.

## Architecture Constraints

- Java 17 or newer.
- Gradle.
- Lightweight CLI application.
- Single JVM process.
- Local filesystem only.
- No architecture distribution patterns in Version 1.

## Open Questions

- Which datagram columns represent route identity, timestamp, distance, and speed?
- Are speeds precomputed in the datagrams, or must they be derived from distance and time deltas?
- Which timestamp timezone should month grouping use?
- Are both datagram CSV files processed together, or is one a sample and one the full pilot input?
- What exact output format does the instructor expect?
