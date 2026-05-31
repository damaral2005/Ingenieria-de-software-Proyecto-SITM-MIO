#!/usr/bin/env bash
set -euo pipefail

MINI_OUTPUT="results/route_month_speeds_minipilot.csv"
PILOT_OUTPUT="results/route_month_speeds_pilot.csv"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: expected output file not found: $path" >&2
    echo "Run scripts/run-monolithic-remote.sh first on a university Linux server." >&2
    exit 1
  fi
}

print_preview_and_count() {
  local label="$1"
  local path="$2"
  echo "== $label first 10 rows =="
  head -n 10 "$path"
  echo
  echo "== $label row count =="
  wc -l "$path"
  echo
}

check_average_speed_values() {
  local label="$1"
  local path="$2"
  echo "== $label avg_speed_kmh sanity check =="
  awk -F',' '
    NR == 1 {
      for (i = 1; i <= NF; i++) {
        if ($i == "avg_speed_kmh") {
          col = i
        }
      }
      if (!col) {
        print "ERROR: avg_speed_kmh column not found" > "/dev/stderr"
        exit 2
      }
      next
    }
    $col == "" || $col == "NaN" || $col == "Infinity" || $col == "-Infinity" {
      print "ERROR: invalid avg_speed_kmh at line " NR ": " $0 > "/dev/stderr"
      exit 3
    }
    END {
      if (NR <= 1) {
        print "ERROR: no data rows found" > "/dev/stderr"
        exit 4
      }
    }
  ' "$path"
  echo "OK: no empty, NaN, or Infinity avg_speed_kmh values found."
  echo
}

require_file "$MINI_OUTPUT"
require_file "$PILOT_OUTPUT"

print_preview_and_count "MiniPilot" "$MINI_OUTPUT"
print_preview_and_count "Full pilot" "$PILOT_OUTPUT"
check_average_speed_values "MiniPilot" "$MINI_OUTPUT"
check_average_speed_values "Full pilot" "$PILOT_OUTPUT"

echo "Real-data output checks completed."
