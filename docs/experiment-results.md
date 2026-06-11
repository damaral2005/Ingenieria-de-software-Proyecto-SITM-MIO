# Experiment Results

## Environment

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-speed
Java: OpenJDK 11.0.26
Gradle wrapper: 8.12
Version: 1 monolithic
```

Version 1 constraints:

- Single JVM.
- Local filesystem inputs and outputs.
- No concurrency.
- No distributed architecture.
- No database or services.

## MiniPilot Result

Input:

```text
Active routes: /opt/sitm-mio/lines-241-ActiveGT.csv
Datagrams: /home/swarch/sitm-data/datagrams-MiniPilot.csv
Output: results/route_month_speeds_minipilot.csv
Log: results/route_month_speeds_minipilot.log
```

Command:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Observed metrics:

```text
Active routes: 111
Raw datagrams: 8,145,462
Cleaned datagrams: 8,145,462
Skipped invalid datagrams: 0
Valid segments: 7,658,225
Output rows: 111
Runtime: 29,716 ms
```

Validation:

- Output had no `NaN`, `Infinity`, or `null` values.
- All output route IDs were validated against active `LINEID` routes.
- `route_id=-1` is expected because `lines-241-ActiveGT.csv` includes `LINEID=-1` with `SHORTNAME=TESTGT1`.

Conclusion: Version 1 works correctly for MiniPilot under the current monolithic implementation.

## datagrams4Pilot Result

Input:

```text
Zip: /opt/sitm-mio/datagrams4Pilot.zip
Extracted CSV: /home/swarch/sitm-data/datagrams4Pilot.csv
Extracted CSV size: 67 GB
Output target: results/route_month_speeds_pilot.csv
Log: results/route_month_speeds_pilot.log
```

Command:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams4Pilot.csv --output results/route_month_speeds_pilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" 2>&1 | tee results/route_month_speeds_pilot.log
```

Observed result:

```text
Failure time: about 10m39s
Failure: java.lang.OutOfMemoryError: Java heap space
```

Reason:

The current monolithic implementation loads all cleaned datagrams into memory before sorting and segment calculation. That approach works for MiniPilot but exceeds the available Java heap for the 67 GB datagrams4Pilot CSV.

## Architectural Implication

The MiniPilot run validates the Version 1 algorithm and CLI flow. The datagrams4Pilot failure shows that the current monolithic, all-in-memory processing strategy is not enough for the larger dataset.

Version 2 should address this with a more scalable processing strategy, such as memory-bounded streaming, chunking, external sorting, or another approach appropriate for the assignment. Concurrency and distributed architecture should still be introduced only when their corresponding versions require them.

## Version 3 MiniPilot Distributed Master-Worker Result

Environment:

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-v3
Version: 3 distributed Master-Worker
```

Input:

```text
Active routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Datagrams: /home/swarch/sitm-data/datagrams-MiniPilot.csv
Output: results/route_month_speeds_minipilot_v3.csv
```

Method:

- Partition the MiniPilot CSV into 8 route/bus hash partitions.
- Process the partitions with 4 worker JVMs.
- Merge 8 partial result CSVs into the final deterministic output.

Observed metrics:

```text
Active routes: 111
Raw datagrams: 8,145,462
Cleaned datagrams: 7,896,735
Skipped invalid datagrams: 248,727
Valid segments: 7,494,051
Workers: 4
Partitions: 8
Partition runtime: 26,921 ms
Worker runtime: 3,859 ms
Merge runtime: 15 ms
Total runtime: 30,814 ms
Output rows: 111
Output lines including header: 112
```

Worker partition metrics:

```text
Fastest partition runtime: 1,705 ms
Slowest partition runtime: 1,969 ms
Smallest partition input: 923,963 points
Largest partition input: 1,138,993 points
```

Validation:

- Final output had no `NaN`, `Infinity`, `-Infinity`, or `null` values.
- The final output contained 111 data rows and one header row.
- The run completed with `BUILD SUCCESSFUL in 31s`.

Conclusion:

Version 3 successfully processed `datagrams-MiniPilot.csv` with the full distributed Master-Worker workflow. For MiniPilot, partitioning dominated the runtime because the dataset is small enough that worker calculation is quick. This run is mainly a correctness and workflow validation baseline; the full pilot run is the stronger evidence for when distribution becomes necessary.

Detailed analysis is available in `docs/version-3-minipilot-experiment-analysis.md`.

## Version 3 MiniPilot Ice Multi-PC Result

Environment:

```text
Master host: 104m10
Worker endpoint 1: sitm-worker:tcp -h 10.147.17.112 -p 10000
Worker endpoint 2: sitm-worker:tcp -h 10.147.17.104 -p 10000
Version: 3 distributed Master-Worker with ZeroC Ice
```

Input:

```text
Active routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Datagrams: /home/swarch/sitm-data/datagrams-MiniPilot.csv
Output: results/minipilot-ice.csv
Work directory: results/ice-minipilot
```

Method:

- Start two remote Ice worker servers.
- Master partitions MiniPilot into 2 route/bus hash partitions.
- Master sends partition CSV content to workers through Ice.
- Workers process partitions and return partial route/month CSV results.
- Master merges partials into the final deterministic CSV.

Observed metrics:

```text
Active routes: 111
Raw datagrams: 8,145,462
Cleaned datagrams: 7,896,735
Skipped invalid datagrams: 248,727
Valid segments: 7,494,051
Workers: 2
Partitions: 2
Partition runtime: 30,570 ms
Worker runtime: 23,134 ms
Merge runtime: 47 ms
Total runtime: 53,780 ms
Output rows: 111
Build result: BUILD SUCCESSFUL in 55s
```

Conclusion:

The Ice deployment successfully processed MiniPilot with a real networked Master-Worker runtime. Unlike the local `--distributed-master` run, this execution used remote Ice worker servers and confirmed that workers can process assigned partitions without holding a local copy of the full datagram CSV.

## Version 3 Full Pilot Distributed Master-Worker Result

Environment:

```text
Server: swarch@10.147.17.105
Host: 104m05
Project path: ~/sitm-mio-v3
Version: 3 distributed Master-Worker
```

Input:

```text
Active routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Datagrams: /home/swarch/sitm-data/datagrams4Pilot.csv
Output: results/route_month_speeds_pilot_v3.csv
```

Method:

- Partition the full CSV once into 64 route/bus hash partitions.
- Process each partition with a worker JVM.
- Merge 64 partial result CSVs into the final deterministic output.

Observed metrics:

```text
Active routes: 111
Raw datagrams: 806,400,773
Cleaned datagrams: 782,565,720
Skipped invalid datagrams: 23,835,053
Partition count: 64
Partition runtime: 3,180,938 ms
Worker partial files: 64
Worker min runtime: 14,545 ms
Worker max runtime: 23,190 ms
Worker average runtime: 19,133.7 ms
Total worker runtime: 1,224,559 ms
Valid segments: 736,951,733
Partial rows: 56,180
Output rows: 1,443
Output lines including header: 1,444
Merge runtime: 170 ms
Estimated total runtime: 4,405,667 ms
Estimated total runtime: about 73.43 minutes
```

Validation:

- Final output had no `NaN`, `Infinity`, `-Infinity`, or `null` values.
- The final output contained 1,443 data rows and one header row.
- Partial result merge completed successfully from 64 worker outputs.

Conclusion:

Version 3 successfully processed the full `datagrams4Pilot.csv` dataset that failed under the monolithic all-in-memory strategy. The distributed Master-Worker design made the workload memory-bounded by partitioning datagrams by `routeId + busId`, processing each partition independently, and merging compact partial route/month aggregates.

Detailed analysis is available in `docs/version-3-experiment-analysis.md`.

## Version 3 Full Pilot Ice Multi-PC Result

Environment:

```text
Master session: swarch@104m10
Worker endpoint 1: sitm-worker:tcp -h 10.147.17.112 -p 10000
Worker endpoint 2: sitm-worker:tcp -h 10.147.17.104 -p 10001
Version: 3 distributed Master-Worker with ZeroC Ice
Java heap: JAVA_TOOL_OPTIONS=-Xmx8g
```

Input:

```text
Active routes: /home/swarch/sitm-data/lines-241-ActiveGT.csv
Datagrams: /home/swarch/sitm-data/datagrams4Pilot.csv
Output: results/pilot-ice.csv
Work directory: results/ice-pilot
```

Method:

- Master partitioned the full pilot into 1024 route/bus hash partitions.
- Master sent partition CSV content to remote Ice workers.
- Workers processed partitions and returned compact partial route/month CSVs.
- Master merged the partials into the final deterministic output.

Observed metrics:

```text
Active routes: 111
Raw datagrams: 806,400,773
Cleaned datagrams: 782,565,720
Skipped invalid datagrams: 23,835,053
Valid segments: 736,951,733
Workers: 2
Partitions: 1024
Partition runtime: 3,237,629 ms
Worker runtime: 1,861,148 ms
Merge runtime: 493 ms
Total runtime: 5,099,359 ms
Total runtime: about 84.99 minutes
Output rows: 1,443
Build result: BUILD SUCCESSFUL in 1h 25m
```

Conclusion:

The Ice deployment successfully processed the full `datagrams4Pilot.csv` dataset with remote workers. This run demonstrates the required networked Master-Worker deployment: the master owns the input dataset, sends bounded partitions to workers over Ice, receives partial aggregates, and writes the final output.
