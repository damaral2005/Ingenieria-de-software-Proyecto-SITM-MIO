# AGENTS.md

Durable instructions for Codex agents working on the SITM-MIO average speed experiment.

## Project Purpose

This Java project calculates average SITM-MIO bus speeds by route and month from GPS datagram CSV files. The assignment will eventually have three versions:

1. Monolithic implementation.
2. Concurrent implementation.
3. Distributed implementation using an architecture distribution pattern.

The current scope is only Version 1: a monolithic CLI application. Correctness comes first, then reproducible performance measurement.

## Repository Layout

Expected structure:

```text
src/main/java/edu/icesi/sitmmio/      Application code
src/test/java/edu/icesi/sitmmio/      Tests and executable contract checks
specs/                                Spec-driven requirements, plans, and tasks
results/                              Generated experiment outputs and measurements
docs/                                 Supporting documentation
```

Current package root is `edu.icesi.sitmmio`; keep new code under that package unless a spec explicitly changes it.

## Java And Gradle Conventions

- Use Java 11 or newer for Version 1 compatibility with the university Linux servers.
- Use Gradle as the build tool.
- Keep the Gradle wrapper on a Java 11-compatible version unless the server JVM is upgraded.
- Keep the application lightweight; prefer a CLI over frameworks.
- Use immutable domain objects where practical.
- Keep parsing, validation, calculation, and reporting in separate packages.
- Keep names explicit: route, datagram, timestamp, distance, speed, month, sample count.
- Avoid hidden global state. Reproducible output is required.
- Do not commit raw assignment CSV/PDF inputs.

## Build

```bash
./gradlew clean build
```

## Tests

Run all configured verification:

```bash
./gradlew check
```

Run tests directly:

```bash
./gradlew test
```

As implementation grows, add tests for:

- Distance conversion.
- Time delta calculation.
- Invalid or missing data filtering.
- Active route filtering.
- Route/month aggregation.
- Deterministic output ordering.

## Run The Monolithic CLI

Help:

```bash
./gradlew run --args="--help"
```

Planned Version 1 execution:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams-MiniPilot.csv --output results/mini-average-speed.csv"
```

For the larger pilot file:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams4Pilot.csv --output results/pilot-average-speed.csv"
```

## Engineering Rules

- Read `specs/001-monolithic-average-speed/spec.md` before changing behavior.
- Update `specs/001-monolithic-average-speed/plan.md` when the approach changes.
- Update `specs/001-monolithic-average-speed/tasks.md` as tasks are completed or discovered.
- Keep commits small and reviewable.
- Prefer simple, direct Java over premature abstractions.
- Make output deterministic: stable sorting, stable formatting, stable decimal handling.
- Record performance measurements with input name, row counts, rejected counts, output rows, and elapsed time.
- Treat the data dictionary as the authority for CSV columns, units, timestamp formats, and route identifiers.

## Architecture Constraints For Version 1

- Single JVM.
- Single local process.
- Local filesystem inputs and outputs.
- No concurrency.
- No distributed architecture patterns.
- No runtime network dependency.
- No client-server split.
- No database requirement.

## Verification Checklist

Before considering a change done:

- `./gradlew clean build` succeeds.
- `./gradlew check` succeeds.
- CLI help still works with `./gradlew run --args="--help"`.
- New logic has focused tests.
- Output ordering is reproducible.
- Version 1 remains monolithic.
- README/spec/task docs are updated when behavior or usage changes.

## Do-Not Rules

- Do not introduce brokers, message queues, services, sockets, RMI, REST APIs, RPC, microservices, or client-server separation in Version 1.
- Do not implement concurrency yet: no worker pools, threads, parallel streams, async pipelines, actors, or distributed jobs.
- Do not add Spring Boot unless a later spec explicitly requires it.
- Do not optimize before correctness tests exist.
- Do not commit raw university data files.
- Do not make generated results non-reproducible.
- Do not replace the spec-driven workflow with ad hoc implementation.
