# Version 3 Experiment Analysis

## Purpose

This document records the Version 3 distributed Master-Worker experiment for the SITM-MIO average speed calculation. The goal was to process the full `datagrams4Pilot.csv` dataset, which the original monolithic implementation could not complete because it loaded all cleaned datagrams into memory.

Version 3 keeps the same speed calculation semantics as previous versions and changes the execution model:

- A master process partitions the cleaned datagrams.
- Worker processes calculate partial route/month aggregates.
- A merge step combines all partial results into the final deterministic CSV.

The only distributed architecture pattern used for Version 3 is **Distributed Master-Worker**.

## Execution Environment

```text
Server: swarch@10.147.17.105
Host: 104m05
Project path: ~/sitm-mio-v3
Data directory: ~/sitm-data
Input datagrams: ~/sitm-data/datagrams4Pilot.csv
Input routes: ~/sitm-data/lines-241-ActiveGT.csv
Available disk before experiment: about 537 GB
Java/Gradle: project Gradle wrapper
Version: 3 distributed Master-Worker
```

The run was moved to `104m05` because `104m03` had about 40 GB free, which was not enough to safely create partition files for the 67 GB CSV. `104m05` had enough space to hold the input file, generated partitions, partial outputs, and final result.

## Methodology

### Step 1: Partition Once

The first step was to create 64 partition files from the full datagram CSV:

```bash
PARTITIONS=64 scripts/create-distributed-partitions.sh
```

This command runs the Version 3 partition-only mode:

```text
--distributed-partition
```

The master reads the raw datagram CSV once, cleans and filters rows, computes a stable partition id from `routeId + busId`, and writes cleaned points to partition files under:

```text
results/distributed-pilot-v3/partitions/
```

It also writes a manifest:

```text
results/distributed-pilot-v3/manifest.csv
```

### Step 2: Process Partitions

Each partition is a worker input. A worker reads one partition, sorts its points by route, bus, and timestamp, calculates valid speed segments, aggregates route/month partial totals, and writes a partial result CSV:

```bash
JAVA_TOOL_OPTIONS="-Xmx6g" PARTITION_ID=<id> WORK_DIR=results/distributed-pilot-v3 scripts/run-partition-worker.sh
```

The experiment processed all 64 partitions and produced 64 partial result files:

```text
results/distributed-pilot-v3/partial-results/partial-00000.csv
...
results/distributed-pilot-v3/partial-results/partial-00063.csv
```

### Step 3: Merge Partial Results

The master merge step read all partial CSVs and produced the final result:

```bash
PARTIALS_DIR=results/distributed-pilot-v3/partial-results \
OUTPUT=results/route_month_speeds_pilot_v3.csv \
scripts/merge-remote-scan-results.sh
```

Final output:

```text
results/route_month_speeds_pilot_v3.csv
```

## Why 64 Partitions?

The first heavy attempt with scan workers and `PARTITIONS=2` failed with `OutOfMemoryError`, because each worker selected too much of the 67 GB dataset. Version 3 therefore used a higher partition count to bound per-worker memory.

`PARTITIONS=64` was selected because it gives each worker a much smaller subset while keeping the number of generated files manageable:

- More partitions reduce heap pressure per worker.
- Fewer partitions reduce file management overhead.
- 64 partitions is a practical compromise for a 67 GB CSV on the available lab machine.

The partition key is:

```text
routeId + busId
```

This is important because speed segments depend on consecutive points from the same route and bus. Keeping a route/bus pair in the same partition prevents broken segment boundaries across workers.

## What The Master Does

The master is responsible for:

- Reading active route metadata.
- Streaming the datagram CSV.
- Cleaning and filtering valid GPS points.
- Assigning each point to a partition by `routeId + busId`.
- Writing partition files and a manifest.
- Coordinating worker execution.
- Merging partial route/month results.
- Writing deterministic final output.

## What Each Worker Does

Each worker is responsible for one partition at a time:

- Read one partition CSV.
- Sort points by route, bus, and timestamp.
- Compute distances with Haversine.
- Compute time deltas.
- Reject invalid segments, including zero/negative time deltas, gaps above the configured threshold, and implausible speeds.
- Aggregate valid segments by route and month.
- Write a partial result CSV.

Workers do not change calculation semantics. They only run the same domain logic on smaller independent work units.

## Raw Metrics

### Partitioning

```text
Active routes: 111
Raw datagrams: 806,400,773
Cleaned datagrams: 782,565,720
Skipped invalid datagrams: 23,835,053
Partition count: 64
Partition runtime ms: 3,180,938
Partition runtime: about 53 min 1 s
Manifest CSV: results/distributed-pilot-v3/manifest.csv
```

### Worker Processing

```text
Worker partition files: 64
Worker min runtime ms: 14,545
Worker max runtime ms: 23,190
Worker average runtime ms: 19,133.7
Total worker runtime ms: 1,224,559
Total worker runtime: about 20 min 25 s
```

### Merge

```text
Partial files: 64
Partial rows: 56,180
Valid segments: 736,951,733
Output rows: 1,443
Merge runtime ms: 170
Output CSV: results/route_month_speeds_pilot_v3.csv
```

### Final Output Validation

```text
Output CSV lines including header: 1,444
Data rows: 1,443
Invalid value check: OK, no NaN, Infinity, -Infinity, or null values found
```

## Total Runtime

Estimated full experiment runtime:

```text
partition runtime + total worker runtime + merge runtime
= 3,180,938 ms + 1,224,559 ms + 170 ms
= 4,405,667 ms
= about 73.43 minutes
= about 1 h 13 min
```

The partitioning phase dominates the total runtime because it streams and writes the full 67 GB input into partition files. Worker execution is comparatively smaller because each worker handles a bounded partition. Merge time is negligible.

## Output Preview

```text
route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,avg_segment_speed_kmh,valid_segments,buses_observed
-1,2018-05,4681.134884,1445.631391,3.238125,3.355045,162772,765
-1,2018-06,144246.849852,42124.017778,3.424337,3.583139,4756765,829
-1,2018-07,145632.874836,41950.349447,3.471553,3.636509,4730906,821
-1,2018-08,148919.785038,42435.275277,3.509339,3.680573,4790741,815
-1,2018-09,146418.503800,41764.067223,3.505849,3.670077,4711474,808
-1,2018-10,144204.464887,41118.696387,3.507029,3.675003,4638295,802
-1,2018-11,134489.093389,39023.801943,3.446335,3.608642,4389468,794
-1,2018-12,155687.070728,41291.805280,3.770411,3.949850,4645964,792
-1,2019-01,148405.587433,40820.664168,3.635551,3.824838,4593422,790
```

## Analysis

Version 3 successfully processed the full pilot dataset that previously exceeded the memory capacity of the monolithic design. The core improvement is not only parallel execution; it is the memory-bounded decomposition of the dataset into independent route/bus partitions.

The data volume is large enough that reading and partitioning dominates the runtime. The 53-minute partitioning phase shows that disk I/O and CSV parsing are major costs. Once partitioned, each worker completed in roughly 14.5 to 23.2 seconds, with an average of 19.1 seconds per partition. This indicates that the per-partition calculation step is controlled and does not suffer from the same memory pressure as the monolithic full-file approach.

The merge step took only 170 ms, which confirms that partial route/month aggregates are small relative to the raw GPS dataset. This is a good property of the Master-Worker design: workers reduce large raw data into compact partial summaries, and the master merges only those summaries.

The final output contains 1,443 route/month rows, and the invalid-value check found no `NaN`, `Infinity`, or `null` values. This supports basic output correctness. The row count is larger than MiniPilot because the full pilot spans more months.

## Conclusion

Version 3 is a successful distributed Master-Worker implementation for the full SITM-MIO pilot dataset.

Key conclusions:

- The monolithic approach is not enough for `datagrams4Pilot.csv` because it loads too much data into one JVM heap.
- Version 3 completes the full pilot by partitioning the input and processing bounded work units.
- The selected partition key, `routeId + busId`, preserves segment correctness.
- 64 partitions avoided the memory issue while keeping file count manageable.
- The final result was generated without invalid numeric values.
- The biggest performance cost is partitioning the full CSV, not merging.

This gives a defensible point for distribution: distribution becomes worthwhile when the full dataset cannot be processed reliably by the single-JVM in-memory strategy and when partitioned workers can keep memory bounded.
