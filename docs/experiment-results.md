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
