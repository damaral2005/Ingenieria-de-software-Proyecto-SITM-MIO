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

## Ice Multi-PC Full Pilot Run

After the local/file-based distributed run, the full pilot was also executed with the ZeroC Ice deployment. This run is the strongest deployment evidence because the master communicated with worker PCs over the network instead of launching worker JVMs locally.

### Ice Deployment Shape

```text
Master session: swarch@104m10
Worker endpoint 1: sitm-worker:tcp -h 10.147.17.112 -p 10000
Worker endpoint 2: sitm-worker:tcp -h 10.147.17.104 -p 10001
Project path: ~/sitm-mio-v3
Input routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Input datagrams: /home/swarch/sitm-data/datagrams4Pilot.csv
Output CSV: results/pilot-ice.csv
Work directory: results/ice-pilot
Workers: 2
Partitions: 1024
Java heap: JAVA_TOOL_OPTIONS=-Xmx8g
```

### Ice Full Pilot Command

```bash
export ICE_WORKERS="sitm-worker:tcp -h 10.147.17.112 -p 10000;sitm-worker:tcp -h 10.147.17.104 -p 10001"

JAVA_TOOL_OPTIONS="-Xmx8g" bash ./gradlew run --args="--ice-master --lines /home/swarch/sitm-data/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams4Pilot.csv --output results/pilot-ice.csv --work-dir results/ice-pilot --partitions 1024 --ice-workers \"$ICE_WORKERS\" --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

### Why 1024 Partitions?

The first Ice full-pilot attempts with fewer partitions exposed a deployment-specific memory constraint:

- With 64 partitions, each partition payload was too large to send and decode as one Ice request.
- With 512 partitions, one worker still lost the connection during processing, which was consistent with worker memory pressure.
- With 1024 partitions and `-Xmx8g`, each partition was small enough for the master to send over Ice and for the workers to decode, sort, calculate, aggregate, and return partial results.

The selected partition count is therefore a memory-safety choice for the Ice deployment. It is higher than the 64 partitions used in the file-based run because the Ice run transfers partition CSV content through RPC messages, while the file-based run processes partition files directly.

Each partition contains cleaned GPS points assigned by the stable hash of:

```text
routeId + busId
```

This preserves speed-segment correctness because all consecutive points for the same bus on the same route stay in one work unit.

### Ice Communication Flow

The Ice deployment still uses only the Distributed Master-Worker pattern:

1. The master reads the active routes file.
2. The master streams the full datagram CSV.
3. The master cleans and filters GPS rows.
4. The master writes 1024 cleaned partition CSV files under `results/ice-pilot/partitions/`.
5. For each partition, the master invokes the remote Ice operation `processPartitionCsv`.
6. The Ice request includes the partition id, speed thresholds, and the partition CSV content.
7. The worker writes the received partition content to a local temporary partition file.
8. The worker sorts points by route, bus, and timestamp.
9. The worker calculates valid speed segments and route/month partial aggregates.
10. The worker returns a compact partial-result CSV and metrics to the master.
11. The master writes each returned partial under `results/ice-pilot/partial-results/`.
12. The master merges all partial results and writes `results/pilot-ice.csv`.

Workers do not require a local copy of `datagrams4Pilot.csv` in this Ice flow. They only need the project code and an open Ice worker server.

### Ice Full Pilot Metrics

```text
Active routes: 111
Raw datagrams: 806,400,773
Cleaned datagrams: 782,565,720
Skipped invalid datagrams: 23,835,053
Valid segments: 736,951,733
Output rows: 1,443
Worker count: 2
Partition count: 1024
Partition runtime ms: 3,237,629
Worker runtime ms: 1,861,148
Merge runtime ms: 493
Total runtime ms: 5,099,359
Total runtime: about 84.99 minutes
Work directory: results/ice-pilot
Output CSV: results/pilot-ice.csv
Build result: BUILD SUCCESSFUL in 1h 25m
```

### Ice Full Pilot Interpretation

The Ice full-pilot run completed the same 806,400,773 raw datagrams and produced the same 1,443 output rows as the previous full pilot evidence. The valid segment count also matches the file-based distributed run: 736,951,733.

The Ice run took about 85 minutes. This is slower than the file-based estimate of about 73 minutes because it adds RPC serialization, network transfer, and worker-side decoding for 1024 partition payloads. That overhead is acceptable for this experiment because the purpose of the Ice run is to demonstrate the required networked Master-Worker deployment, not only local multi-process execution.

The successful full-pilot Ice run confirms:

- The master can coordinate remote worker PCs with ZeroC Ice.
- Workers can process assigned partitions without storing the full input dataset.
- The route/bus partition strategy remains correct at full scale.
- Increasing partition count bounds worker memory enough for the large dataset.
- The final merge remains cheap because partial route/month results are compact.
