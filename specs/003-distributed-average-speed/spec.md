# Specification: Version 3 Distributed Average Speed

## Status

Draft specification for Version 3 planning. Implementation has not started.

## Purpose

Calculate average SITM-MIO bus speeds by route and month using a distributed architecture that extends the Version 2 Thread Pool solution. Version 3 must keep the same domain behavior and output semantics while distributing the workload across multiple worker processes.

## Version Scope

Version 3 is a distributed Java CLI/runtime experiment:

- Multiple JVM processes.
- Local or remote worker processes.
- File-based work-item coordination.
- Distributed Master-Worker as the only distribution pattern.
- Domain calculation remains independent from distribution infrastructure as code organization, not as an additional architecture pattern.

## Non-Goals

- No Spring Boot requirement.
- No database requirement.
- No message broker requirement unless a later decision explicitly accepts one.
- No REST API requirement unless needed by the selected deployment plan.
- No change to the mathematical definition of average speed.
- No change to the output CSV schema unless required by a documented compatibility decision.

## Inputs

University server paths:

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
/opt/sitm-mio/datagrams-MiniPilot.csv
/opt/sitm-mio/datagrams4Pilot.csv
/opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

Existing real-data mapping:

```text
active route column = LINEID
route-index         = 7
bus-index           = 11
timestamp-index     = 10
latitude-index      = 4
longitude-index     = 5
coordinate-scale    = 10000000
```

## Output

Version 3 must produce the same deterministic CSV result columns as Version 2:

```text
route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,avg_segment_speed_kmh,valid_segments,buses_observed
```

Ordering must remain stable:

1. `route_id` ascending.
2. `month` ascending.

Numeric formatting must remain locale-independent.

## Functional Requirements

1. Load active routes from `lines-241-ActiveGT.csv`.
2. Partition datagrams into independent work units.
3. Process work units with distributed workers.
4. Keep all speed calculation rules compatible with Version 2.
5. Aggregate partial worker outputs into final route/month results.
6. Preserve deterministic output ordering.
7. Produce a run summary with distributed execution metrics.
8. Support MiniPilot for correctness validation.
9. Support datagrams4Pilot as the target scalability experiment.
10. Retry failed worker work items a configurable number of times before failing the run.
11. Check Ice worker availability before assigning remote partitions.
12. Reassign Ice partition work to another healthy worker when an invocation fails and retry budget remains.

## Distribution Pattern

### Distributed Master-Worker

The master coordinates the distributed execution:

- Reads CLI options and active route metadata.
- Creates work partitions.
- Publishes work items.
- Starts or contacts worker processes depending on the selected deployment.
- Collects worker result files or messages.
- Merges partial results.
- Writes the final deterministic CSV.

Workers execute isolated computation:

- Consume one work item at a time.
- Read the assigned partition.
- Sort points by route, bus, and timestamp as needed.
- Calculate valid speed segments.
- Aggregate partial route/month results.
- Write or send partial results back to the master.

Partition files, manifest files, and partial result files are implementation details of the Master-Worker pattern. They are not presented as separate distribution patterns.

## Partitioning Strategy

Preferred partition key:

```text
routeId + busId
```

Rationale:

- Segment calculation depends on consecutive points from the same route and bus.
- Keeping all points for a route/bus pair in the same partition avoids cross-partition boundary correction.
- Final aggregation by route/month remains simple because partial route/month accumulators can be merged deterministically.

Fallback partition key:

```text
routeId
```

This is simpler to implement and easier to explain, but it may create uneven partitions when a route has many more datagrams than others.

## Quality Requirements

- Correctness must match Version 2 on small synthetic fixtures and MiniPilot.
- Distributed execution must be reproducible.
- Worker failure should be detectable.
- Worker failures should be recoverable when another attempt or healthy Ice worker is available.
- Ice workers should expose a lightweight health check operation.
- Worker outputs must be mergeable without depending on processing order.
- The implementation must record enough metrics to decide when distribution is worth it.
- The design should avoid loading the full 67 GB pilot file into memory.

## Required Metrics

For every experiment run, record:

- Input dataset name.
- Input file size when available.
- Active route count.
- Raw datagram count.
- Cleaned datagram count.
- Skipped invalid datagram count.
- Work partition count.
- Worker count.
- Per-worker task count.
- Per-worker elapsed time.
- Total valid segment count.
- Output row count.
- Total runtime.
- Merge runtime.
- Whether the run completed, failed by memory, or failed by worker error.

## Compatibility Requirements

- Version 3 output must be compared against Version 2 on MiniPilot.
- If outputs differ, the difference must be explained and documented before Version 3 is accepted.
- Existing tests for parsing, cleaning, speed calculation, aggregation, and CSV output should continue to pass.

## Open Questions

- Should the first implementation use filesystem work-item files or sockets?
- Should workers be launched by the master or manually started?
- Should partitioning be by `routeId + busId` from the start, or should Version 3 start with `routeId` and evolve?
- Should the master stream partitions directly, or first materialize partition CSV files under `results/` or `build/`?
- How many worker processes are available on the university server environment?
