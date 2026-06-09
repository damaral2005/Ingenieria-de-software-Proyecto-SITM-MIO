# Research: Version 3 Distributed Average Speed

## Research Goals

Decide how to evolve Version 2 into a distributed solution using only patterns discussed in class:

- Sender Released
- Producer-Consumer
- Fork / Join
- Separable Dependencies
- Master-Worker

## Pattern Evaluation

### Master-Worker

Fit: High.

Reason:

- Version 2 already uses a local Master-Worker shape through `ThreadPoolSpeedCalculator` and `RouteProcessingTask`.
- Version 3 can distribute the same idea across multiple JVM worker processes.
- The pattern is easy to explain for the assignment deliverable.

Decision:

Use Master-Worker as the primary Version 3 pattern.

### Producer-Consumer

Fit: High as a supporting pattern.

Reason:

- The datagram reader/partitioner can produce work items.
- Workers can consume work items.
- This decouples input partitioning from worker execution.
- It provides a clean explanation for task queues or filesystem manifests.

Decision:

Use Producer-Consumer for the work item flow.

### Separable Dependencies

Fit: High as an internal design rule.

Reason:

- Domain calculators already exist and should not depend on distributed runtime details.
- Keeping calculation independent reduces risk and keeps Version 3 comparable with Version 2.

Decision:

Use Separable Dependencies to define boundaries between domain logic and distributed infrastructure.

### Fork / Join

Fit: Medium.

Reason:

- The calculation splits work and joins partial results.
- However, the current problem is not naturally recursive.
- Master-Worker communicates the distributed deployment more clearly.

Decision:

Mention as related, but do not use it as the primary Version 3 pattern.

### Sender Released

Fit: Low to medium.

Reason:

- The master may dispatch tasks and continue coordinating without blocking per task.
- However, this is less central than Master-Worker and Producer-Consumer.

Decision:

Do not use it as a main Version 3 pattern unless later worker messaging requires it.

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

### Filesystem manifest

Advantages:

- Simple.
- No external services.
- Easy to inspect and debug.
- Good fit for university server constraints.

Risks:

- Less robust than a real queue.
- Requires careful run directory cleanup or unique run ids.

Decision:

Use filesystem manifests and result files for the first Version 3 implementation.

### Sockets or REST

Advantages:

- More visibly distributed.
- Can support long-running workers.

Risks:

- More code.
- More failure modes.
- Harder to test.
- May distract from the assignment goal.

Decision:

Avoid for the initial Version 3 unless the instructor specifically expects networked workers.

## Experiment Questions

- How does Version 2 runtime change with `--threads 1`, `2`, `4`, and available processors?
- Does Version 2 complete on datagrams4Pilot or fail by memory?
- How much overhead does Version 3 add on MiniPilot with one worker?
- At what input size does Version 3 beat or survive where Version 2 fails?
- How many partitions give good worker balance without excessive files?
