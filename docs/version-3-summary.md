# Version 3 Summary: Distributed Average Speed

## Context

Version 3 extends the SITM-MIO average speed project from the Version 2 Thread Pool implementation into a distributed processing experiment. The goal is to keep the same calculation semantics while moving from local worker threads to independent worker JVM processes coordinated by a master process.

The current branch is:

```text
codex/version-3-distributed-master-worker
```

## Selected Distributed Pattern

### Master-Worker

This is the only Version 3 distribution pattern.

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

Partition files, `manifest.csv`, and partial result CSVs are implementation details used by the Master-Worker implementation. They are not presented as additional architecture patterns.

The distributed package coordinates processes and files, while core domain services remain independent:

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
--partition-id <number>
--partial-results-dir <path>
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
- `ScanWorkerProcessor`: scans raw datagrams and processes one hash partition without master-created partition files.
- `DistributedPartialMerger`: merges scan-worker partial CSV files copied back from multiple PCs.
- `DistributedPartitioner`: creates partition files and a manifest without immediately launching workers.
- `DistributedPartialMerger`: merges partial CSV files from local or remote workers.

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

Four helper scripts were added.

Run Version 3 on the university server:

```bash
WORKERS=4 PARTITIONS=8 scripts/run-distributed-remote.sh
```

Compare V2 and V3 MiniPilot outputs:

```bash
scripts/compare-v2-v3-minipilot.sh
```

Run one scan worker partition for the multi-PC flow:

```bash
PARTITION_ID=0 PARTITIONS=4 scripts/run-scan-worker-remote.sh
```

Merge scan-worker partial results:

```bash
scripts/merge-remote-scan-results.sh
```

Create partition files without launching workers:

```bash
PARTITIONS=64 scripts/create-distributed-partitions.sh
```

Run a worker for one generated partition file:

```bash
PARTITION_ID=0 WORK_DIR=results/distributed-pilot-v3 scripts/run-partition-worker.sh
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

## MiniPilot Result

Version 3 successfully processed `datagrams-MiniPilot.csv` on `104m03`.

Summary:

```text
Raw datagrams: 8,145,462
Cleaned datagrams: 7,896,735
Skipped invalid datagrams: 248,727
Valid segments: 7,494,051
Workers: 4
Partitions: 8
Output rows: 111
Output CSV: results/route_month_speeds_minipilot_v3.csv
Total runtime: 30,814 ms
```

The MiniPilot run validates the complete Master-Worker flow on real data. Detailed analysis is documented in:

```text
docs/version-3-minipilot-experiment-analysis.md
```

## Full Pilot Result

Version 3 successfully processed `datagrams4Pilot.csv` on `104m05`.

Summary:

```text
Raw datagrams: 806,400,773
Cleaned datagrams: 782,565,720
Skipped invalid datagrams: 23,835,053
Valid segments: 736,951,733
Partitions: 64
Output rows: 1,443
Output CSV: results/route_month_speeds_pilot_v3.csv
Estimated total runtime: about 73.43 minutes
```

Detailed analysis is documented in:

```text
docs/version-3-experiment-analysis.md
```

## Current Limitations

- V2 and V3 output equivalence still needs a formal file-by-file comparison recorded in the repository.
- Deployment diagrams and QAW scenarios still need to be documented.
- Worker failure handling is basic: missing or failed worker outputs stop the run.
- Multi-PC scan worker mode requires manually copying data and partial result CSV files between PCs.
