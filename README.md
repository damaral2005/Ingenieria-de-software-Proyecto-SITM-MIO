# SITM-MIO Average Speed Experiment

Java CLI project for calculating average SITM-MIO bus speeds by route and month from GPS datagrams.

Version 1 is intentionally monolithic:

- Single Java process and single JVM.
- Local filesystem inputs and outputs.
- No concurrency.
- No distributed architecture.
- No database.
- No services, APIs, brokers, sockets, RMI, or Spring Boot.

Correctness and reproducible measurement come before optimization.

## Environment

The project targets the university server environment:

```text
Java: 11 or newer
Gradle wrapper: 8.12
```

Validated remotely on:

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-speed
Java: OpenJDK 11.0.26
Gradle wrapper: 8.12
```

## Repository Layout

```text
src/main/java/edu/icesi/sitmmio     Java CLI implementation
src/test/java/edu/icesi/sitmmio     JUnit tests
specs/                              Spec-driven development notes
scripts/                            Remote validation scripts
docs/                               Supporting documentation
results/                            Generated CSV/log outputs, ignored by Git
```

## Local Development On Windows

The local Windows machine is for build, tests, and code review only. Do not try to validate `/opt/sitm-mio` from PowerShell; it maps to `C:\opt\sitm-mio`.

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat run --args="--help"
```

## Remote Deployment Without GitHub

From Windows Git Bash, create an archive without build outputs, local caches, results, or datasets:

```bash
cd /c/Users/damar/OneDrive/Escritorio/sitm-mio-speed
tar --exclude='.git' --exclude='.gradle' --exclude='build' --exclude='data' --exclude='results/*.csv' --exclude='results/*.log' -czf sitm-mio-speed.tar.gz .
scp sitm-mio-speed.tar.gz swarch@10.147.17.103:~/
```

On the remote server:

```bash
ssh swarch@10.147.17.103
rm -rf ~/sitm-mio-speed
mkdir -p ~/sitm-mio-speed
tar -xzf ~/sitm-mio-speed.tar.gz -C ~/sitm-mio-speed
cd ~/sitm-mio-speed
chmod +x gradlew scripts/*.sh
sed -i 's/\r$//' scripts/*.sh
```

## Remote Data Paths

```text
Active routes:             /opt/sitm-mio/lines-241-ActiveGT.csv
MiniPilot zip:             /opt/sitm-mio/datagrams-MiniPilot.zip
Extracted MiniPilot CSV:   /home/swarch/sitm-data/datagrams-MiniPilot.csv
Large Pilot zip:           /opt/sitm-mio/datagrams4Pilot.zip
Extracted large Pilot CSV: /home/swarch/sitm-data/datagrams4Pilot.csv
Data dictionary:           /opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

Prepare MiniPilot if needed:

```bash
ls -lh /opt/sitm-mio
mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data
```

## Datagram Dictionary Mapping

`datagrams-MiniPilot.csv` is headerless. Official schema:

```text
0  eventType
1  registerdate
2  stopId
3  odometer
4  latitude
5  longitude
6  taskId
7  lineId
8  tripId
9  unknown1
10 datagramDate
11 busId
```

Correct CLI mapping:

```text
active route column = LINEID
route id            = lineId       = index 7
bus id              = busId        = index 11
timestamp           = datagramDate = index 10
latitude            = index 4
longitude           = index 5
coordinate scale    = 10000000
```

Index `3` is `odometer`, not route.

## Remote Build And Test

```bash
java -version
./gradlew --version
./gradlew build
./gradlew test
```

## Remote MiniPilot Run

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Or run the script:

```bash
scripts/run-monolithic-remote.sh
```

MiniPilot is required. The large pilot CSV is optional and is skipped by the script when `/home/swarch/sitm-data/datagrams4Pilot.csv` is missing.

## Output Validation

```bash
ls -lh results
head -10 results/route_month_speeds_minipilot.csv
wc -l results/route_month_speeds_minipilot.csv
grep -Ei "nan|infinity|null" results/route_month_speeds_minipilot.csv | head
```

Validate that every output route exists in active `LINEID`:

```bash
tail -n +2 results/route_month_speeds_minipilot.csv | cut -d, -f1 | sort -u > /tmp/output_routes.txt
awk -F, 'NR==1 { for (i=1; i<=NF; i++) if ($i=="LINEID") c=i; next } { gsub(/^[ \t]+|[ \t]+$/, "", $c); if ($c!="") print $c }' /opt/sitm-mio/lines-241-ActiveGT.csv | sort -u > /tmp/active_routes.txt
comm -23 /tmp/output_routes.txt /tmp/active_routes.txt
```

No output from `comm` means all output routes are active. `route_id=-1` is expected because `lines-241-ActiveGT.csv` includes `LINEID=-1` with `SHORTNAME=TESTGT1`.

## Successful MiniPilot Validation

Observed on `swarch@10.147.17.103`:

```text
Active routes: 111
Raw datagrams: 8,145,462
Cleaned datagrams: 8,145,462
Skipped invalid datagrams: 0
Valid segments: 7,658,225
Output rows: 111
Runtime: 29,716 ms
Output: results/route_month_speeds_minipilot.csv
```

Validation results:

- Output had no `NaN`, `Infinity`, or `null` values.
- All output route IDs were validated against active `LINEID` routes.
- `route_id=-1` is expected for `SHORTNAME=TESTGT1`.

## Optional datagrams4Pilot

Do not extract the large dataset automatically. Check size and disk space first:

```bash
unzip -l /opt/sitm-mio/datagrams4Pilot.zip | tail
df -h ~
```

If there is enough space:

```bash
mkdir -p ~/sitm-data
unzip /opt/sitm-mio/datagrams4Pilot.zip -d ~/sitm-data
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams4Pilot.csv --output results/route_month_speeds_pilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" 2>&1 | tee results/route_month_speeds_pilot.log
```

Observed Version 1 result after extraction:

```text
Extracted CSV: /home/swarch/sitm-data/datagrams4Pilot.csv
Extracted size: 67 GB
Command log: results/route_month_speeds_pilot.log
Failure after about 10m39s:
java.lang.OutOfMemoryError: Java heap space
```

This does not invalidate the MiniPilot run. It is evidence that the current monolithic Version 1 implementation, which loads all cleaned datagrams before sorting and aggregation, does not scale to the 67 GB dataset under the available heap. Version 2 should address this with a more scalable processing strategy before adding concurrency or distributed architecture.

