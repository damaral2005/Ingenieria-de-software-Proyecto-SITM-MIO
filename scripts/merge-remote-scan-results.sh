#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${DATA_DIR:-$HOME/sitm-data}"
LINES_FILE="${LINES_FILE:-$DATA_DIR/lines-241-ActiveGT.csv}"
PARTIALS_DIR="${PARTIALS_DIR:-results/remote-scan-partials}"
OUTPUT="${OUTPUT:-results/route_month_speeds_pilot_v3_remote.csv}"
LOG_OUTPUT="${LOG_OUTPUT:-results/route_month_speeds_pilot_v3_remote_merge.log}"

if [[ ! -f "$LINES_FILE" ]]; then
  echo "ERROR: lines file not found: $LINES_FILE" >&2
  exit 1
fi

if [[ ! -d "$PARTIALS_DIR" ]]; then
  echo "ERROR: partial results directory not found: $PARTIALS_DIR" >&2
  exit 1
fi

echo "== Merging Version 3 remote scan partial results =="
echo "Lines: $LINES_FILE"
echo "Partials: $PARTIALS_DIR"
echo "Output: $OUTPUT"

./gradlew run --args="--distributed-merge --lines $LINES_FILE --partial-results-dir $PARTIALS_DIR --output $OUTPUT --active-route-col LINEID" \
  2>&1 | tee "$LOG_OUTPUT"

echo
echo "Remote scan merge completed."
echo "Output CSV: $OUTPUT"
echo "Log: $LOG_OUTPUT"
