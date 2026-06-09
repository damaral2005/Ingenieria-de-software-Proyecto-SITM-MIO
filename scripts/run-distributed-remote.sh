#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="/opt/sitm-mio"
LINES_FILE="$DATA_DIR/lines-241-ActiveGT.csv"
MINI_FILE="/home/swarch/sitm-data/datagrams-MiniPilot.csv"
PILOT_FILE="/home/swarch/sitm-data/datagrams4Pilot.csv"
RESULTS_DIR="results"
WORKERS="${WORKERS:-4}"
PARTITIONS="${PARTITIONS:-8}"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    exit 1
  fi
}

run_distributed() {
  local label="$1"
  local datagrams="$2"
  local output="$3"
  local work_dir="$4"
  local log="$5"

  echo "== Running Version 3 distributed calculation: $label =="
  echo "Workers: $WORKERS"
  echo "Partitions: $PARTITIONS"
  ./gradlew run --args="--distributed-master --lines $LINES_FILE --datagrams $datagrams --output $output --workers $WORKERS --partitions $PARTITIONS --work-dir $work_dir --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
    2>&1 | tee "$log"
}

require_file "$LINES_FILE"
require_file "$MINI_FILE"

mkdir -p "$RESULTS_DIR"

run_distributed \
  "MiniPilot" \
  "$MINI_FILE" \
  "$RESULTS_DIR/route_month_speeds_minipilot_v3.csv" \
  "$RESULTS_DIR/distributed-minipilot-v3" \
  "$RESULTS_DIR/route_month_speeds_minipilot_v3.log"

if [[ -f "$PILOT_FILE" ]]; then
  echo
  run_distributed \
    "datagrams4Pilot" \
    "$PILOT_FILE" \
    "$RESULTS_DIR/route_month_speeds_pilot_v3.csv" \
    "$RESULTS_DIR/distributed-pilot-v3" \
    "$RESULTS_DIR/route_month_speeds_pilot_v3.log"
else
  echo
  echo "Skipping datagrams4Pilot: optional extracted CSV not found at $PILOT_FILE"
  echo "If needed, extract it with:"
  echo "mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams4Pilot.zip -d ~/sitm-data"
fi

echo
echo "Distributed validation completed."
echo "MiniPilot output: $RESULTS_DIR/route_month_speeds_minipilot_v3.csv"
echo "MiniPilot log:    $RESULTS_DIR/route_month_speeds_minipilot_v3.log"
