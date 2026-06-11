# Version 3 Ice Deployment

## Purpose

This runbook deploys Version 3 as a real distributed Master-Worker system with ZeroC Ice.

The old `--distributed-master` mode starts worker JVMs from the master machine. That is useful for local validation, but it does not prove the deployment expected in the lab. The Ice flow runs one master on the central PC and two remote worker servers on separate PCs.

## Deployment Shape

```text
PC 110: Ice master
  - Reads CLI options.
  - Sends partition ids to remote workers through Ice.
  - Receives partial CSV results through Ice.
  - Merges final route/month output.

PC 112: Ice worker server
  - Listens on tcp -h 0.0.0.0 -p 10000.
  - Receives partition assignments from the master.
  - Reads its local copy of the datagram CSV.
  - Calculates partial route/month aggregates.

PC 104: Ice worker server
  - Same role as PC 112.
```

The master sends partition assignments, not local Java processes. Each worker must already be running and reachable by IP before the master starts.

## Required Files

On all three PCs:

```text
~/sitm-mio-v3/
```

Only the master PC needs the input data:

```text
~/sitm-data/lines-241-ActiveGT.csv
~/sitm-data/datagrams4Pilot.csv
```

Workers receive partition CSV content from the master through Ice, so they do not need the full datagram CSV.

## Prepare Data On The Master

Create the data directory on the master PC:

```bash
mkdir -p ~/sitm-data
```

If the data exists on one of the lab machines under `/opt/sitm-mio`, make that machine the master or copy the data to the chosen master. Example from the master:

```bash
cp /opt/sitm-mio/lines-241-ActiveGT.csv ~/sitm-data/
cp /opt/sitm-mio/datagrams-MiniPilot.csv ~/sitm-data/
cp /opt/sitm-mio/datagrams4Pilot.csv ~/sitm-data/
```

If the data is on another PC, copy it to the master:

```bash
scp swarch@10.147.17.104:/opt/sitm-mio/lines-241-ActiveGT.csv ~/sitm-data/
scp swarch@10.147.17.104:/opt/sitm-mio/datagrams-MiniPilot.csv ~/sitm-data/
scp swarch@10.147.17.104:/opt/sitm-mio/datagrams4Pilot.csv ~/sitm-data/
```

If `datagrams4Pilot` is compressed, extract it first according to the file type:

```bash
tar -xzf datagrams4Pilot.tar.gz -C ~/sitm-data
unzip datagrams4Pilot.zip -d ~/sitm-data
```

## Install From Scratch

If a PC does not have `~/sitm-mio-v3`, create a fresh package from the PC that has the updated project:

```bash
cd ~/sitm-mio-v3
bash scripts/create-ice-deployment-package.sh
```

From Windows PowerShell, use:

```powershell
.\scripts\create-ice-deployment-package.ps1
```

That creates:

```text
.\sitm-mio-v3-ice.zip
```

Copy and extract it on each target PC:

```bash
scp /tmp/sitm-mio-v3-ice.tar.gz swarch@10.147.17.112:/tmp/
scp /tmp/sitm-mio-v3-ice.tar.gz swarch@10.147.17.104:/tmp/
scp /tmp/sitm-mio-v3-ice.tar.gz swarch@10.147.17.110:/tmp/

ssh swarch@10.147.17.112 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && tar -xzf /tmp/sitm-mio-v3-ice.tar.gz -C ~/sitm-mio-v3'
ssh swarch@10.147.17.104 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && tar -xzf /tmp/sitm-mio-v3-ice.tar.gz -C ~/sitm-mio-v3'
ssh swarch@10.147.17.110 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && tar -xzf /tmp/sitm-mio-v3-ice.tar.gz -C ~/sitm-mio-v3'
```

If the package was created from Windows PowerShell as ZIP, use:

```powershell
scp .\sitm-mio-v3-ice.zip swarch@10.147.17.112:~/
scp .\sitm-mio-v3-ice.zip swarch@10.147.17.104:~/
scp .\sitm-mio-v3-ice.zip swarch@10.147.17.110:~/
```

Then extract on Linux:

```bash
ssh swarch@10.147.17.112 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && unzip -oq ~/sitm-mio-v3-ice.zip -d ~/sitm-mio-v3'
ssh swarch@10.147.17.104 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && unzip -oq ~/sitm-mio-v3-ice.zip -d ~/sitm-mio-v3'
ssh swarch@10.147.17.110 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && unzip -oq ~/sitm-mio-v3-ice.zip -d ~/sitm-mio-v3'
```

If `unzip` is unavailable, use Java:

```bash
ssh swarch@10.147.17.104 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && cd ~/sitm-mio-v3 && jar xf ~/sitm-mio-v3-ice.zip'
```

For a clean deploy to all three lab PCs from Windows PowerShell:

```powershell
.\scripts\deploy-ice-project-to-lab.ps1
```

Default targets:

```text
swarch@10.147.17.112
swarch@10.147.17.104
swarch@10.147.17.110
```

This script recreates the ZIP, copies it to each remote home directory, deletes the old remote `~/sitm-mio-v3`, extracts the new project, and verifies the Ice scripts exist.

The scripts can be run with `bash scripts/name.sh`, so executable permissions are not required.

## Check IPs And Ports

On each PC, see its IP:

```bash
hostname -I
```

Check whether port `10000` is already listening:

```bash
ss -ltnp | grep ':10000' || echo 'port 10000 is free'
```

From the master PC, check whether a worker port is reachable:

```bash
nc -vz 10.147.17.112 10000
nc -vz 10.147.17.104 10000
```

In this deployment, the worker port is not discovered automatically. We choose it with `ICE_PORT`; default is `10000`.

## Start Workers

On PC 112:

```bash
cd ~/sitm-mio-v3
bash scripts/run-ice-worker-remote.sh
```

On PC 104:

```bash
cd ~/sitm-mio-v3
bash scripts/run-ice-worker-remote.sh
```

Leave both terminals open. Each worker should print:

```text
SITM-MIO Ice worker listening
Identity: sitm-worker
Endpoint: tcp -h 0.0.0.0 -p 10000
```

If port `10000` is busy, use a different port:

```bash
ICE_PORT=11000 bash scripts/run-ice-worker-remote.sh
```

Then update the master `ICE_WORKERS` value accordingly.

For the successful full-pilot run, the workers were:

```text
sitm-worker:tcp -h 10.147.17.112 -p 10000
sitm-worker:tcp -h 10.147.17.104 -p 10001
```

## Run Master

On PC 110:

```bash
cd ~/sitm-mio-v3
bash scripts/run-ice-master-remote.sh
```

Default worker endpoints:

```text
sitm-worker:tcp -h 10.147.17.112 -p 10000
sitm-worker:tcp -h 10.147.17.104 -p 10000
```

Override endpoints if the room IPs change:

```bash
ICE_WORKERS="sitm-worker:tcp -h 10.147.17.112 -p 10000;sitm-worker:tcp -h 10.147.17.104 -p 10000" \
  bash scripts/run-ice-master-remote.sh
```

Expected output:

```text
results/route_month_speeds_pilot_v3_ice.csv
results/ice-distributed-pilot-v3/partial-results/
results/ice-distributed-pilot-v3/ice-worker-metrics.csv
```

For the full pilot Ice run, use a higher partition count to keep each Ice request bounded:

```bash
export ICE_WORKERS="sitm-worker:tcp -h 10.147.17.112 -p 10000;sitm-worker:tcp -h 10.147.17.104 -p 10001"

JAVA_TOOL_OPTIONS="-Xmx8g" bash ./gradlew run --args="--ice-master --lines /home/swarch/sitm-data/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams4Pilot.csv --output results/pilot-ice.csv --work-dir results/ice-pilot --partitions 1024 --ice-workers \"$ICE_WORKERS\" --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

### How Ice Work Is Exchanged

The Ice master does not ask workers to read the full datagram CSV. Instead:

1. The master reads and cleans the full input.
2. The master partitions cleaned points by `routeId + busId`.
3. The master sends one partition CSV payload to a remote worker with `processPartitionCsv`.
4. The worker writes that payload to a local temporary file.
5. The worker calculates partial route/month aggregates.
6. The worker returns a compact partial CSV to the master.
7. The master merges all returned partial CSVs.

For MiniPilot, 2 partitions were enough. For the full pilot, 1024 partitions were used because 64 and 512 partitions produced partition payloads large enough to cause memory pressure or lost worker connections.

## MiniPilot Smoke Test

Use MiniPilot first so connectivity problems show up quickly.

On both workers:

```bash
bash scripts/run-ice-worker-remote.sh
```

On the master:

```bash
DATAGRAMS_FILE=/home/swarch/sitm-data/datagrams-MiniPilot.csv \
OUTPUT_FILE=results/route_month_speeds_minipilot_v3_ice.csv \
WORK_DIR=results/ice-distributed-minipilot-v3 \
bash scripts/run-ice-master-remote.sh
```

## Validation

On PC 110:

```bash
head -10 results/route_month_speeds_pilot_v3_ice.csv
wc -l results/route_month_speeds_pilot_v3_ice.csv
grep -Ei "nan|infinity|null" results/route_month_speeds_pilot_v3_ice.csv | head
cat results/ice-distributed-pilot-v3/ice-worker-metrics.csv
```

For MiniPilot, compare against the existing Version 2 or local Version 3 output:

```bash
diff -u results/route_month_speeds_minipilot_v2.csv results/route_month_speeds_minipilot_v3_ice.csv
```

## Troubleshooting

- If the master cannot connect, confirm each worker terminal is still running and that the IP addresses match the current lab PCs.
- If a worker fails with missing file errors, copy `lines-241-ActiveGT.csv` and the datagram CSV into the same path used by the master command.
- If port `10000` is blocked or busy, restart the worker with `ICE_PORT=<port>` and update `ICE_WORKERS` on the master.
- If the final result is incomplete, confirm `PARTITIONS` equals the number of intended partition ids and every master invocation completed.
