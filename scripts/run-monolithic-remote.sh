#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="/opt/sitm-mio"
LINES_FILE="$DATA_DIR/lines-241-ActiveGT.csv"
MINI_FILE="/home/swarch/sitm-data/datagrams-MiniPilot.csv"
PILOT_FILE="/home/swarch/sitm-data/datagrams4Pilot.csv"
RESULTS_DIR="results"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    echo "Run this script directly on one of the university Linux servers where /opt/sitm-mio exists." >&2
    exit 1
  fi
}

print_header() {
  local label="$1"
  local path="$2"
  echo "== $label header =="
  head -n 1 "$path"
  echo
}

require_file "$LINES_FILE"
if [[ ! -f "$MINI_FILE" ]]; then
  echo "ERROR: required MiniPilot CSV not found: $MINI_FILE" >&2
  echo "Extract it on the university server with:" >&2
  echo "mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data" >&2
  exit 1
fi

mkdir -p "$RESULTS_DIR"

print_header "lines-241-ActiveGT.csv" "$LINES_FILE"
print_header "datagrams-MiniPilot.csv first row (headerless file)" "$MINI_FILE"
if [[ -f "$PILOT_FILE" ]]; then
  print_header "datagrams4Pilot.csv" "$PILOT_FILE"
else
  echo "== datagrams4Pilot.csv =="
  echo "Skipping full pilot run: optional file not found at $PILOT_FILE"
  echo "The large dataset currently exists as /opt/sitm-mio/datagrams4Pilot.zip and is not extracted automatically."
  echo
fi

echo "== Running MiniPilot monolithic calculation =="
./gradlew run --args="--lines $LINES_FILE --datagrams $MINI_FILE --output $RESULTS_DIR/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
  2>&1 | tee "$RESULTS_DIR/route_month_speeds_minipilot.log"

if [[ -f "$PILOT_FILE" ]]; then
  echo
  echo "== Running full pilot monolithic calculation =="
  ./gradlew run --args="--lines $LINES_FILE --datagrams $PILOT_FILE --output $RESULTS_DIR/route_month_speeds_pilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
    2>&1 | tee "$RESULTS_DIR/route_month_speeds_pilot.log"
fi

echo
echo "Remote monolithic validation runs completed."
echo "Outputs:"
echo "  $RESULTS_DIR/route_month_speeds_minipilot.csv"
if [[ -f "$PILOT_FILE" ]]; then
  echo "  $RESULTS_DIR/route_month_speeds_pilot.csv"
else
  echo "  full pilot output skipped because $PILOT_FILE is missing"
fi
echo "Logs:"
echo "  $RESULTS_DIR/route_month_speeds_minipilot.log"
if [[ -f "$PILOT_FILE" ]]; then
  echo "  $RESULTS_DIR/route_month_speeds_pilot.log"
fi
