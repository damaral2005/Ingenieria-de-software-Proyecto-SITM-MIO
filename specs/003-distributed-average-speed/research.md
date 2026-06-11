# Research: Version 3 Distributed Average Speed

## Research Goals

Decide how to evolve Version 2 into a distributed solution using one distributed pattern discussed in class.

## Pattern Decision

### Selected Pattern: Master-Worker

Decision:

Use Distributed Master-Worker as the only Version 3 distribution pattern.

Reason:

- Version 2 already uses a local Master-Worker shape through `ThreadPoolSpeedCalculator` and `RouteProcessingTask`.
- Version 3 distributes the same idea across multiple JVM worker processes.
- The master divides work, coordinates execution, receives partial results, and merges the final CSV.
- Workers execute isolated computation over assigned partitions.
- The pattern is easy to explain and matches the assignment requirement for a distributed design pattern.

## Implementation Note

The implementation uses files such as partition CSVs, `manifest.csv`, and partial result CSVs, but these are treated as Master-Worker implementation details rather than as separate patterns.

## Current Version 2 Findings

Version 2 changes from `main` are focused:

- CLI now uses `ThreadPoolSpeedCalculator`.
- `--threads` was added.
- `RouteProcessingTask` processes one active route.
- `ThreadPoolRunSummary` reports thread count and submitted tasks.

The important limitation:

- Version 2 still reads all cleaned datagrams into memory before creating route tasks.
- This means large pilot processing may still fail before concurrent calculation begins.

## Distribution Trigger

Distribution becomes worth considering when:

- The input no longer fits in one JVM heap.
- Increasing thread count does not improve runtime enough.
- The full pilot file cannot complete reliably in Version 2.
- The cost of partitioning and merging is lower than the cost or impossibility of single-process processing.

Existing evidence:

- MiniPilot completed in Version 1.
- datagrams4Pilot is documented as a 67 GB extracted CSV.
- Version 1 failed on datagrams4Pilot with `OutOfMemoryError`.
- Version 2 keeps the same all-in-memory datagram loading pattern, so memory remains a major risk.

## Partitioning Research

### Route-only partitioning

Advantages:

- Simple to explain.
- Matches Version 2 task shape.
- One route can be processed independently.

Risks:

- Some routes may be much larger than others.
- Large route partitions can still exceed memory.

### Route and bus partitioning

Advantages:

- More balanced partitions.
- Segment calculation remains correct because segments only depend on consecutive points from the same route and bus.
- Observed bus counts can be merged by summing if each route/bus appears in only one partition.

Risks:

- Requires partition hashing.
- Requires documenting why this key preserves correctness.

Decision:

Prefer `routeId + busId` partitioning for Version 3. Keep route-only as fallback if the team wants a smaller first implementation.

## Coordination Research

### Filesystem manifest and partition files

Advantages:

- Simple.
- No external services.
- Easy to inspect and debug.
- Good fit for university server constraints.

Risks:

- Less robust than a broker-based implementation.
- Requires careful run directory cleanup or unique run ids.

Decision:

Use filesystem manifests, partition files, and result files as implementation details of the distributed Master-Worker pattern.

### ZeroC Ice

Advantages:

- More visibly distributed.
- Supports long-running remote worker objects.
- Matches the lab deployment expectation: one master PC invokes worker PCs over the network.
- Avoids replacing the assignment with REST, brokers, or a client-server web stack.

Risks:

- More code.
- More failure modes.
- Harder to test.
- Requires the Ice runtime dependency on every PC.

Decision:

Use Ice for the real lab deployment. Keep the filesystem/process modes as local validation and fallback tooling, but present the deployed Version 3 as a Master-Worker system where the master assigns partitions to remote Ice workers.

## Experiment Questions

- How does Version 2 runtime change with `--threads 1`, `2`, `4`, and available processors?
- Does Version 2 complete on datagrams4Pilot or fail by memory?
- How much overhead does Version 3 add on MiniPilot with one worker?
- At what input size does Version 3 beat or survive where Version 2 fails?
- How many partitions give good worker balance without excessive files?
