#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${PARTITION_ID:-}" ]]; then
  echo "ERROR: PARTITION_ID is required." >&2
  echo "Example: PARTITION_ID=0 PARTITIONS=16 scripts/run-scan-worker-remote.sh" >&2
  exit 1
fi

PARTITIONS="${PARTITIONS:-4}"
DATA_DIR="${DATA_DIR:-$HOME/sitm-data}"
LINES_FILE="${LINES_FILE:-$DATA_DIR/lines-241-ActiveGT.csv}"
DATAGRAMS_FILE="${DATAGRAMS_FILE:-$DATA_DIR/datagrams4Pilot.csv}"
RESULTS_DIR="${RESULTS_DIR:-results/remote-scan-partials}"
PARTIAL_OUTPUT="$RESULTS_DIR/partial-$(printf "%05d" "$PARTITION_ID").csv"
LOG_OUTPUT="$RESULTS_DIR/partial-$(printf "%05d" "$PARTITION_ID").log"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    exit 1
  fi
}

require_file "$LINES_FILE"
require_file "$DATAGRAMS_FILE"
mkdir -p "$RESULTS_DIR"

echo "== Running Version 3 scan worker =="
echo "Partition id: $PARTITION_ID"
echo "Partitions: $PARTITIONS"
echo "Lines: $LINES_FILE"
echo "Datagrams: $DATAGRAMS_FILE"
echo "Partial output: $PARTIAL_OUTPUT"

./gradlew run --args="--distributed-scan-worker --lines $LINES_FILE --datagrams $DATAGRAMS_FILE --partial-output $PARTIAL_OUTPUT --partition-id $PARTITION_ID --partitions $PARTITIONS --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
  2>&1 | tee "$LOG_OUTPUT"

echo
echo "Scan worker completed."
echo "Partial CSV: $PARTIAL_OUTPUT"
echo "Log: $LOG_OUTPUT"
