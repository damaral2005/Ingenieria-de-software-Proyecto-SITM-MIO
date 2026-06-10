#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${DATA_DIR:-$HOME/sitm-data}"
LINES_FILE="${LINES_FILE:-$DATA_DIR/lines-241-ActiveGT.csv}"
DATAGRAMS_FILE="${DATAGRAMS_FILE:-$DATA_DIR/datagrams4Pilot.csv}"
WORK_DIR="${WORK_DIR:-results/distributed-pilot-v3}"
PARTITIONS="${PARTITIONS:-64}"
LOG_OUTPUT="${LOG_OUTPUT:-results/distributed-pilot-v3-partition.log}"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    exit 1
  fi
}

require_file "$LINES_FILE"
require_file "$DATAGRAMS_FILE"
mkdir -p "$(dirname "$LOG_OUTPUT")"

echo "== Creating Version 3 distributed partition files =="
echo "Lines: $LINES_FILE"
echo "Datagrams: $DATAGRAMS_FILE"
echo "Work directory: $WORK_DIR"
echo "Partitions: $PARTITIONS"

./gradlew run --args="--distributed-partition --lines $LINES_FILE --datagrams $DATAGRAMS_FILE --work-dir $WORK_DIR --partitions $PARTITIONS --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
  2>&1 | tee "$LOG_OUTPUT"

echo
echo "Partitioning completed."
echo "Manifest: $WORK_DIR/manifest.csv"
echo "Partitions directory: $WORK_DIR/partitions"
echo "Partial results directory: $WORK_DIR/partial-results"
