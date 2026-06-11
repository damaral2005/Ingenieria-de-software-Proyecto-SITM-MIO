#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${DATA_DIR:-/home/swarch/sitm-data}"
LINES_FILE="${LINES_FILE:-$DATA_DIR/lines-241-ActiveGT.csv}"
DATAGRAMS_FILE="${DATAGRAMS_FILE:-$DATA_DIR/datagrams4Pilot.csv}"
RESULTS_DIR="${RESULTS_DIR:-results}"
OUTPUT_FILE="${OUTPUT_FILE:-$RESULTS_DIR/route_month_speeds_pilot_v3_ice.csv}"
WORK_DIR="${WORK_DIR:-$RESULTS_DIR/ice-distributed-pilot-v3}"
PARTITIONS="${PARTITIONS:-2}"
WORKER_RETRIES="${WORKER_RETRIES:-2}"
ICE_WORKERS="${ICE_WORKERS:-sitm-worker:tcp -h 10.147.17.112 -p 10000;sitm-worker:tcp -h 10.147.17.104 -p 10000}"
APP_JAVA_OPTS="${APP_JAVA_OPTS:--Xmx8g}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-$APP_JAVA_OPTS}"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    exit 1
  fi
}

require_file "$LINES_FILE"
mkdir -p "$RESULTS_DIR" "$WORK_DIR"

echo "== Running SITM-MIO Ice distributed master =="
echo "Lines:      $LINES_FILE"
echo "Datagrams:  $DATAGRAMS_FILE"
echo "Workers:    $ICE_WORKERS"
echo "Partitions: $PARTITIONS"
echo "Retries:    $WORKER_RETRIES"
echo "Output:     $OUTPUT_FILE"
echo "Work dir:   $WORK_DIR"
echo "Java opts:  $JAVA_TOOL_OPTIONS"

bash ./gradlew run --args="--ice-master --lines $LINES_FILE --datagrams $DATAGRAMS_FILE --output $OUTPUT_FILE --work-dir $WORK_DIR --partitions $PARTITIONS --worker-retries $WORKER_RETRIES --ice-workers \"$ICE_WORKERS\" --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
