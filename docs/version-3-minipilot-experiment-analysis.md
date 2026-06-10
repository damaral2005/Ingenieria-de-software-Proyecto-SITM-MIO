# Version 3 MiniPilot Experiment Analysis

## Purpose

This document records the Version 3 distributed Master-Worker experiment using the smaller `datagrams-MiniPilot.csv` dataset. The goal was to validate the distributed implementation on a manageable dataset before relying on it for the full `datagrams4Pilot.csv` run.

Version 3 uses only the **Distributed Master-Worker** architecture pattern:

- The master partitions the cleaned datagrams into work units.
- Worker JVMs process independent partitions and write partial route/month aggregates.
- The master merges the partial results into the final deterministic CSV.

## Execution Environment

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-v3
Data directory: ~/sitm-data
Input datagrams: /home/swarch/sitm-data/datagrams-MiniPilot.csv
Input routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Output CSV: results/route_month_speeds_minipilot_v3.csv
Version: 3 distributed Master-Worker
Workers: 4
Partitions: 8
```

## Command

```bash
mkdir -p results

./gradlew run --args="--distributed-master --lines /home/swarch/sitm-data/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot_v3.csv --workers 4 --partitions 8 --work-dir results/distributed-minipilot-v3 --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" 2>&1 | tee results/route_month_speeds_minipilot_v3.log
```

## Methodology

The MiniPilot run used the complete distributed master flow in one command:

1. The master read the 111 active routes from `lines-241-ActiveGT.csv`.
2. The master streamed the raw MiniPilot datagrams.
3. Invalid datagrams were rejected during cleaning.
4. Cleaned points were partitioned by `routeId + busId` into 8 CSV partition files.
5. Four worker JVMs processed the 8 partitions.
6. Each worker sorted points by route, bus, and timestamp, then calculated valid speed segments.
7. Each worker wrote one partial route/month aggregate CSV per partition.
8. The master merged the 8 partial result files.
9. The master wrote the final output CSV.

The partition key is `routeId + busId` because valid speed segments require consecutive points from the same bus on the same route. This avoids splitting segment calculation across workers and keeps `buses_observed` mergeable.

## Why 8 Partitions?

MiniPilot is much smaller than the full pilot, so it does not need the 64 partitions used for `datagrams4Pilot.csv`.

The experiment used 8 partitions with 4 workers because:

- It exercises the real Master-Worker flow with more work items than workers.
- It keeps each worker task small enough for quick validation.
- It creates enough partial files to test the merge step.
- It avoids unnecessary file overhead for a 687 MB input CSV.

This configuration is useful for correctness and workflow validation, not as the final scalability setting for the 67 GB dataset.

## Raw Metrics

### Master Summary

```text
Active routes: 111
Raw datagrams: 8,145,462
Cleaned datagrams: 7,896,735
Skipped invalid datagrams: 248,727
Valid segments: 7,494,051
Output rows: 111
Worker count: 4
Partition count: 8
Partition runtime ms: 26,921
Worker runtime ms: 3,859
Merge runtime ms: 15
Total runtime ms: 30,814
Work directory: results/distributed-minipilot-v3
Output CSV: results/route_month_speeds_minipilot_v3.csv
```

### Worker Partition Metrics

```text
partition_id,input_points,valid_segments,partial_rows,runtime_ms
00000,946362,896654,88,1713
00001,924522,879042,88,1710
00002,957321,907928,90,1826
00003,990118,940925,88,1792
00004,1013066,958839,89,1749
00005,1138993,1079645,91,1969
00006,923963,878947,85,1705
00007,1002390,952071,90,1741
```

### Output Validation

```text
Output CSV lines including header: 112
Data rows: 111
Invalid value check: OK, no NaN, Infinity, -Infinity, or null values found
Build result: BUILD SUCCESSFUL in 31s
```

## Output Preview

```text
route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,avg_segment_speed_kmh,valid_segments,buses_observed
-1,2019-05,15640.400722,4849.935000,3.224868,3.390381,541805,797
131,2019-05,19157.851961,1105.233333,17.333762,20.078940,208057,92
140,2019-05,8367.588079,542.746944,15.417108,18.579087,106870,46
142,2019-05,12785.622312,742.731667,17.214322,21.244304,157442,72
1472,2019-05,13264.558737,843.800834,15.720011,18.861434,168874,75
150,2019-05,6283.888098,428.855000,14.652710,18.924430,91594,35
1571,2019-05,12619.306582,828.862778,15.224844,18.739091,163009,67
2101,2019-05,6686.889830,515.583890,12.969548,16.660343,100748,60
2102,2019-05,4814.928672,381.815277,12.610623,17.235125,76205,37
```

## Analysis

The MiniPilot experiment confirms that Version 3 can execute the complete distributed Master-Worker flow end to end on real data. The run created partitions, processed them with worker JVMs, merged partial route/month aggregates, and produced a deterministic final CSV.

The total runtime was 30.814 seconds. Most of that time was spent in partitioning: 26.921 seconds, or about 87.4% of the total. Worker processing took 3.859 seconds, and merge took only 15 ms. This matches the expected behavior for a smaller dataset: the cost of reading, cleaning, and writing partition files dominates, while the aggregate merge is almost free.

Worker runtimes were well balanced. The fastest partition took 1.705 seconds and the slowest took 1.969 seconds. The largest partition had 1,138,993 input points and produced 1,079,645 valid segments. This shows that the `routeId + busId` hash partitioning distributed the MiniPilot workload reasonably evenly.

The output contains 111 data rows, matching one month of data for the active-route subset. The invalid-value check found no `NaN`, `Infinity`, `-Infinity`, or `null` values, which supports basic output correctness.

MiniPilot is small enough that distribution is not expected to beat a simpler local implementation by a large margin. Its main value here is validation: it proves the Version 3 pipeline works before running the heavier full pilot. The full pilot is where distribution becomes necessary, because the monolithic full-file strategy previously failed with `OutOfMemoryError`.

## Conclusion

Version 3 successfully processed `datagrams-MiniPilot.csv` using the Distributed Master-Worker implementation.

Key conclusions:

- The full distributed workflow works on real MiniPilot data.
- Eight partitions and four workers were enough to validate the design.
- Partitioning is the dominant cost for MiniPilot.
- Worker processing is balanced and memory-bounded.
- Merge cost is negligible because workers reduce raw GPS data into compact partial aggregates.
- The output passed basic integrity checks and contains no invalid numeric values.

This MiniPilot run is useful as the correctness and workflow baseline for Version 3. The full pilot run remains the stronger evidence for when distribution is worthwhile.
