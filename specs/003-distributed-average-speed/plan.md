# Plan: Version 3 Distributed Average Speed

## Implementation Strategy

Build Version 3 as an incremental extension of Version 2. Keep the calculation core stable and introduce distribution at the orchestration boundary. The first target is a working distributed experiment that can be tested locally with multiple JVM processes and later deployed on the university server.

## Architecture Decision

Use:

- Master-Worker as the main distributed pattern.
- Producer-Consumer for work item creation and consumption.
- Separable Dependencies to keep domain logic independent from distribution mechanics.

The Version 2 `ThreadPoolSpeedCalculator` already behaves as a local Master-Worker. Version 3 should preserve that mental model while replacing local `Callable` tasks with external worker process tasks.

## Proposed Runtime Shape

### Master CLI

Responsibilities:

- Parse Version 3 CLI options.
- Load active routes.
- Partition datagrams into work items.
- Create a task manifest.
- Launch or coordinate workers.
- Wait for worker outputs.
- Merge partial outputs.
- Write final CSV.
- Print distributed run summary.

### Worker CLI

Responsibilities:

- Read a work item descriptor.
- Read only the assigned partition.
- Reuse existing cleaning, segment, and aggregation services.
- Write a partial result file.
- Exit with a clear status code.

### Merger

Responsibilities:

- Read all partial result files.
- Merge totals by route/month.
- Recompute weighted average speed and average segment speed from mergeable totals.
- Preserve deterministic final ordering.

## Package Plan

Keep existing packages for domain behavior:

```text
edu.icesi.sitmmio.csv
edu.icesi.sitmmio.geo
edu.icesi.sitmmio.model
edu.icesi.sitmmio.output
edu.icesi.sitmmio.service
```

Add a distribution package:

```text
edu.icesi.sitmmio.distributed
```

Likely classes:

```text
DistributedCommandLineApp
DistributedSpeedMaster
DistributedRunSummary
PartitionManifest
PartitionWorkItem
PartitionWriter
WorkerCommandLineApp
WorkerProcessor
PartialResultReader
PartialResultMerger
WorkerProcessLauncher
```

## Work Directory Layout

Use a generated work directory, ignored by Git:

```text
results/distributed/<run-id>/
  manifest.csv
  partitions/
    partition-00000.csv
    partition-00001.csv
  partial-results/
    worker-result-00000.csv
    worker-result-00001.csv
  logs/
    worker-00000.log
```

If `results/` remains reserved for final artifacts, use:

```text
build/distributed-runs/<run-id>/
```

## Partitioning Plan

Initial recommended implementation:

1. Read datagrams sequentially.
2. Clean and filter rows to active routes.
3. Compute a partition id from `routeId + busId`.
4. Append each cleaned point to its partition file.
5. Emit a manifest row for each partition.

This avoids keeping all datagrams in memory and prepares the data for distributed workers.

## Merge Plan

Worker partial results must include mergeable values:

```text
route_id
month
total_distance_km
total_time_hours
total_segment_speed_kmh
valid_segments
buses_observed
```

The final merger computes:

```text
avg_speed_kmh = total_distance_km / total_time_hours
avg_segment_speed_kmh = total_segment_speed_kmh / valid_segments
```

Important: `buses_observed` is not safely mergeable by summing if the same bus can appear in multiple partitions. Partitioning by `routeId + busId` keeps each bus in one partition, so summing observed buses is valid.

## Implementation Phases

### Phase 1: Distributed Spec Baseline

- Create Version 3 spec files.
- Document selected patterns.
- Document partition and merge assumptions.
- Keep implementation unchanged.

### Phase 2: Mergeable Aggregation

- Add an internal partial aggregate model.
- Teach aggregation to expose mergeable totals.
- Add tests for merging partial aggregates.
- Verify results match current `RouteMonthSpeed` outputs.

### Phase 3: Filesystem Producer-Consumer

- Implement partition writer.
- Implement manifest writer/reader.
- Add tests with tiny CSV input.
- Verify partitioning does not load all rows in memory.

### Phase 4: Worker CLI

- Add worker mode or worker app entry point.
- Process a single partition.
- Write one partial result file.
- Add worker fixture tests.

### Phase 5: Master CLI

- Add distributed master mode.
- Launch multiple worker JVMs or document manual worker startup.
- Collect partial outputs.
- Merge final CSV.
- Print distributed metrics.

### Phase 6: Experiment

- Run Version 2 MiniPilot baseline.
- Run Version 3 MiniPilot with 1 worker.
- Run Version 3 MiniPilot with multiple workers.
- Compare outputs.
- Run Version 3 on datagrams4Pilot if environment allows.
- Record the point where distribution is justified.

## Validation Commands

Existing checks:

```bash
./gradlew clean build
./gradlew check
./gradlew run --args="--help"
```

Planned distributed help:

```bash
./gradlew run --args="--distributed-master --help"
./gradlew run --args="--distributed-worker --help"
```

Planned MiniPilot distributed run:

```bash
./gradlew run --args="--distributed-master --lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot_v3.csv --workers 4 --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

## Risks

- Naive partitioning by file chunk can break segment correctness at chunk boundaries.
- Route-only partitioning may create load imbalance.
- Summing observed bus counts is only correct if buses do not appear in multiple partitions.
- Launching worker JVMs may be harder to validate on the university server than local process execution.
- Filesystem queues are simple and explainable, but not as robust as a real broker.

## Decision Checkpoint

Before implementing sockets, brokers, or advanced infrastructure, first build the filesystem-based distributed Master-Worker. It is easier to test, easier to explain in class, and satisfies the distribution pattern without introducing unnecessary runtime dependencies.
