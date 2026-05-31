# Remote Validation Checklist

Use this checklist to run Version 1 on the university Linux server. Local Windows is only for build/test/code review.

## 1. Environment

Validated server:

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-speed
Java: OpenJDK 11.0.26
Gradle wrapper: 8.12
```

Required:

```text
Java 11 or newer
Gradle wrapper 8.12
```

## 2. Upload Without GitHub

From Windows Git Bash:

```bash
cd /c/Users/damar/OneDrive/Escritorio/sitm-mio-speed
tar --exclude='.git' --exclude='.gradle' --exclude='build' --exclude='data' --exclude='results/*.csv' --exclude='results/*.log' -czf sitm-mio-speed.tar.gz .
scp sitm-mio-speed.tar.gz swarch@10.147.17.103:~/
```

On the server:

```bash
ssh swarch@10.147.17.103
rm -rf ~/sitm-mio-speed
mkdir -p ~/sitm-mio-speed
tar -xzf ~/sitm-mio-speed.tar.gz -C ~/sitm-mio-speed
cd ~/sitm-mio-speed
chmod +x gradlew scripts/*.sh
sed -i 's/\r$//' scripts/*.sh
```

## 3. Data Paths

```text
Active routes:             /opt/sitm-mio/lines-241-ActiveGT.csv
MiniPilot zip:             /opt/sitm-mio/datagrams-MiniPilot.zip
Extracted MiniPilot CSV:   /home/swarch/sitm-data/datagrams-MiniPilot.csv
Large Pilot zip:           /opt/sitm-mio/datagrams4Pilot.zip
Extracted large Pilot CSV: /home/swarch/sitm-data/datagrams4Pilot.csv
Data dictionary:           /opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

Prepare MiniPilot:

```bash
ls -lh /opt/sitm-mio
mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data
```

## 4. Headerless Datagram Mapping

Official schema:

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

CLI mapping:

```text
active route column = LINEID
route-index         = 7
bus-index           = 11
timestamp-index     = 10
latitude-index      = 4
longitude-index     = 5
coordinate-scale    = 10000000
```

Index `3` is `odometer`, not route.

## 5. Build And Test

```bash
java -version
./gradlew --version
./gradlew build
./gradlew test
```

## 6. Run MiniPilot

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Or:

```bash
scripts/run-monolithic-remote.sh
```

The script requires MiniPilot. It skips datagrams4Pilot when `/home/swarch/sitm-data/datagrams4Pilot.csv` is missing.

## 7. Validate Output

```bash
ls -lh results
head -10 results/route_month_speeds_minipilot.csv
wc -l results/route_month_speeds_minipilot.csv
grep -Ei "nan|infinity|null" results/route_month_speeds_minipilot.csv | head
```

Validate output routes against active `LINEID`:

```bash
tail -n +2 results/route_month_speeds_minipilot.csv | cut -d, -f1 | sort -u > /tmp/output_routes.txt
awk -F, 'NR==1 { for (i=1; i<=NF; i++) if ($i=="LINEID") c=i; next } { gsub(/^[ \t]+|[ \t]+$/, "", $c); if ($c!="") print $c }' /opt/sitm-mio/lines-241-ActiveGT.csv | sort -u > /tmp/active_routes.txt
comm -23 /tmp/output_routes.txt /tmp/active_routes.txt
```

Expected: no output from `comm`. `route_id=-1` is valid because active routes include `LINEID=-1` with `SHORTNAME=TESTGT1`.

## 8. Successful MiniPilot Evidence

Validated on `104m03`:

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

Output checks:

```text
No NaN, Infinity, or null values.
All output route IDs are active LINEID values.
route_id=-1 is expected.
```

## 9. Optional datagrams4Pilot

Check size and disk space before extraction:

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

Observed Version 1 result:

```text
Extracted CSV: /home/swarch/sitm-data/datagrams4Pilot.csv
Extracted size: 67 GB
Failure time: about 10m39s
Failure: java.lang.OutOfMemoryError: Java heap space
Log: results/route_month_speeds_pilot.log
```

Interpretation: MiniPilot validation remains successful. The large pilot failure is evidence that the current monolithic Version 1 memory strategy is not enough for the 67 GB dataset.

## 10. Report Evidence

Save:

- Build/test output.
- Runtime metrics.
- First 10 output rows.
- Output row count.
- No `NaN`/`Infinity`/`null` validation.
- Active route validation.
- `results/route_month_speeds_minipilot.log`.
- datagrams4Pilot memory failure and `results/route_month_speeds_pilot.log`.

Do not commit input datasets or generated result CSV/log files unless explicitly required.
