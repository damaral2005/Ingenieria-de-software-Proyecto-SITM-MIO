# Version 3 Summary: Distributed Average Speed

## Context

Version 3 extends the SITM-MIO average speed project from the Version 2 Thread Pool implementation into a distributed processing experiment. The goal is to keep the same calculation semantics while moving from local worker threads to independent worker JVM processes coordinated by a master process.

The current branch is:

```text
codex/version-3-distributed-master-worker
```

## Selected Distributed Patterns

### Master-Worker

This is the main Version 3 pattern.

The master process is responsible for:

- Loading active route metadata.
- Partitioning datagrams into independent work items.
- Writing a work manifest.
- Launching worker JVM processes.
- Waiting for partial outputs.
- Merging partial results.
- Writing the final deterministic CSV.

The worker process is responsible for:

- Reading one cleaned datagram partition.
- Sorting points by route, bus, and timestamp.
- Calculating valid speed segments.
- Producing route/month partial aggregates.
- Writing one partial result CSV.

### Producer-Consumer

This is used as the task flow pattern.

- The master/partitioner acts as the producer of work items.
- Partition files and `manifest.csv` act as the work queue.
- Worker JVMs consume partition work items and produce partial results.

### Separable Dependencies

This is used as an internal design rule.

The distributed package coordinates processes and files, but core domain services remain independent:

- Speed calculation remains in `SpeedSegmentCalculator`.
- Geographic distance remains in `HaversineDistanceCalculator`.
- Final output writing remains in `ResultCsvWriter`.
- Distributed infrastructure does not change the mathematical behavior.

## What Was Added

### Spec Kit Baseline

A new Spec Kit folder was added:

```text
specs/003-distributed-average-speed/
```

It contains:

- `spec.md`: Version 3 requirements and architecture constraints.
- `plan.md`: phased implementation strategy.
- `tasks.md`: implementation and verification checklist.
- `research.md`: evaluation of class patterns and selected design.

### CLI Modes

The CLI now supports three execution modes:

```text
default                 Version 2 Thread Pool
--distributed-master    Version 3 master process
--distributed-worker    Version 3 worker process
```

New distributed options:

```text
--workers <number>
--partitions <number>
--work-dir <path>
--partition <path>
--partial-output <path>
```

The existing Version 2 command still works without distributed flags.

### Distributed Package

A new package was added:

```text
edu.icesi.sitmmio.distributed
```

Main classes:

- `DistributedSpeedMaster`: coordinates the distributed run.
- `WorkerProcessLauncher`: launches worker JVMs.
- `WorkerProcessor`: processes one partition.
- `DatagramPartitioner`: streams datagrams into partition files.
- `PartitionWorkItem`: represents one unit of work.
- `PartitionManifestCsv`: writes and reads the work manifest.
- `PartialRouteMonthAggregate`: mergeable route/month totals.
- `PartialRouteMonthAggregator`: creates partial aggregates from segments.
- `PartialResultCsv`: reads and writes partial result CSV files.
- `PartialResultMerger`: merges all partial results into final output rows.

## Partitioning Strategy

Version 3 partitions cleaned datagrams by:

```text
routeId + busId
```

This was chosen because speed segments depend on consecutive points from the same route and bus. Keeping each route/bus pair in the same partition avoids cross-partition boundary correction.

This also helps make `buses_observed` mergeable, because each route/bus pair belongs to exactly one partition.

## Runtime Flow

The distributed master flow is:

1. Parse CLI options.
2. Read active routes.
3. Stream the datagram CSV.
4. Clean and filter rows.
5. Write cleaned points into partition CSV files.
6. Write `manifest.csv`.
7. Launch worker JVMs.
8. Each worker writes a partial result CSV.
9. Master reads all partial result CSVs.
10. Master merges by route/month.
11. Master writes the final deterministic output CSV.
12. Master prints distributed runtime metrics.

## Remote Scripts

Two helper scripts were added.

Run Version 3 on the university server:

```bash
WORKERS=4 PARTITIONS=8 scripts/run-distributed-remote.sh
```

Compare V2 and V3 MiniPilot outputs:

```bash
scripts/compare-v2-v3-minipilot.sh
```

The distributed script uses the known real-data mapping:

```text
active route column = LINEID
route-index         = 7
bus-index           = 11
timestamp-index     = 10
latitude-index      = 4
longitude-index     = 5
coordinate-scale    = 10000000
```

## Verification Completed

Local verification completed:

```bash
.\gradlew.bat check
.\gradlew.bat run --args="--help"
.\gradlew.bat clean build
```

A local smoke test was also run with synthetic data:

- The distributed master created partitions.
- The master launched two worker JVMs.
- Each worker generated a partial CSV.
- The master merged the partials.
- The final CSV had the expected route/month rows.

## Current Limitations

- MiniPilot has not yet been validated on the university server for this branch.
- V2 and V3 output equivalence still needs to be recorded with real MiniPilot data.
- datagrams4Pilot still needs to be tested in the server environment.
- Deployment diagrams and QAW scenarios still need to be documented.
- Worker failure handling is basic: missing or failed worker outputs stop the run.

## Next Steps

1. Upload this branch/package to `swarch@10.147.17.103`.
2. Build on the server with `./gradlew clean build`.
3. Run V2 MiniPilot baseline.
4. Run V3 MiniPilot with several worker/partition counts.
5. Compare V2 and V3 outputs.
6. Record runtime metrics in `docs/experiment-results.md`.
7. Try datagrams4Pilot once MiniPilot equivalence is confirmed.
