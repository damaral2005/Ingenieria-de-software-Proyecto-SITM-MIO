# Version 3 Multi-PC Scan Worker Flow

## Purpose

This flow supports the large `datagrams4Pilot.csv` experiment when the master machine does not have enough free disk space to create local partition files.

Instead of having the master create large partition CSVs, each PC has a local copy of the raw datagram CSV and runs a scan worker:

1. The worker scans the full CSV locally.
2. It keeps only rows whose `routeId + busId` hash belongs to its assigned partition id.
3. It calculates partial route/month aggregates.
4. It writes one partial result CSV.
5. The partial result CSV is copied back to the master.
6. The master merges all partial result CSV files.

This keeps the Version 3 architecture under the distributed Master-Worker pattern while avoiding large intermediate partition files on the master.

## Required Files On Each Worker PC

Each worker PC needs:

```text
~/sitm-mio-v3/                         Project code
~/sitm-data/lines-241-ActiveGT.csv     Active routes file
~/sitm-data/datagrams4Pilot.csv        Large datagram CSV
```

If the worker has no `/opt/sitm-mio`, copy the data from a PC that has it.

Example from the worker PC:

```bash
mkdir -p ~/sitm-data
scp swarch@10.147.17.103:/home/swarch/sitm-data/lines-241-ActiveGT.csv ~/sitm-data/
scp swarch@10.147.17.103:/home/swarch/sitm-data/datagrams4Pilot.tar.gz ~/sitm-data/
cd ~/sitm-data
tar -xzf datagrams4Pilot.tar.gz
```

If only `datagrams4Pilot.csv` exists on the source machine, copy that file instead. Copying the compressed file is usually faster and uses less network traffic.

## Worker Command

Run one scan worker partition with:

```bash
PARTITION_ID=0 PARTITIONS=16 scripts/run-scan-worker-remote.sh
```

Important variables:

```text
PARTITION_ID   Current partition id. Starts at 0.
PARTITIONS     Total partition count. Must be identical for every worker.
DATA_DIR       Optional. Defaults to ~/sitm-data.
```

Example for four workers:

```bash
# PC 1
PARTITION_ID=0 PARTITIONS=4 scripts/run-scan-worker-remote.sh

# PC 2
PARTITION_ID=1 PARTITIONS=4 scripts/run-scan-worker-remote.sh

# PC 3
PARTITION_ID=2 PARTITIONS=4 scripts/run-scan-worker-remote.sh

# PC 4
PARTITION_ID=3 PARTITIONS=4 scripts/run-scan-worker-remote.sh
```

If only two PCs are available, start with:

```bash
# PC 1
PARTITION_ID=0 PARTITIONS=2 scripts/run-scan-worker-remote.sh

# PC 2
PARTITION_ID=1 PARTITIONS=2 scripts/run-scan-worker-remote.sh
```

Using more partitions reduces memory per partition, but every partition id requires a full CSV scan. For very large data, prefer one partition per available PC first, then increase only if a worker runs out of memory.

## Copy Partials Back To Master

On the master PC, create a partials directory:

```bash
mkdir -p ~/sitm-mio-v3/results/remote-scan-partials
```

Copy each worker partial result back:

```bash
scp swarch@10.147.17.105:~/sitm-mio-v3/results/remote-scan-partials/partial-00001.csv \
  ~/sitm-mio-v3/results/remote-scan-partials/
```

Repeat for every worker partition.

## Merge On Master

After all partial CSV files are present on the master:

```bash
cd ~/sitm-mio-v3
scripts/merge-remote-scan-results.sh
```

Expected final output:

```text
results/route_month_speeds_pilot_v3_remote.csv
```

## Validation

Check the final output:

```bash
head -10 results/route_month_speeds_pilot_v3_remote.csv
wc -l results/route_month_speeds_pilot_v3_remote.csv
grep -Ei "nan|infinity|null" results/route_month_speeds_pilot_v3_remote.csv | head
```

## Notes

- All workers must use the same `PARTITIONS` value.
- Every partition id from `0` to `PARTITIONS - 1` must be processed exactly once.
- Missing partition ids will produce incomplete final results.
- Duplicate partition ids will double count those route/bus groups.
- This flow trades runtime for disk safety: every scan worker reads the full CSV, but the master avoids creating huge partition files.

## Faster Alternative: Partition Once, Then Distribute Partitions

If one PC has enough free disk space, prefer the partition-once flow:

1. Create partition files once on the PC with enough disk.
2. Process partition files locally or copy some partition files to other PCs.
3. Copy partial result CSVs back.
4. Merge partial result CSVs.

Create partitions:

```bash
PARTITIONS=64 scripts/create-distributed-partitions.sh
```

Run one partition worker:

```bash
PARTITION_ID=0 WORK_DIR=results/distributed-pilot-v3 scripts/run-partition-worker.sh
```

This is faster than scan-worker mode because the 67 GB datagram CSV is read once during partitioning, not once per partition.
