#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${PARTITION_ID:-}" ]]; then
  echo "ERROR: PARTITION_ID is required." >&2
  echo "Example: PARTITION_ID=0 WORK_DIR=results/distributed-pilot-v3 scripts/run-partition-worker.sh" >&2
  exit 1
fi

WORK_DIR="${WORK_DIR:-results/distributed-pilot-v3}"
PARTITION_FILE="$WORK_DIR/partitions/partition-$(printf "%05d" "$PARTITION_ID").csv"
PARTIAL_OUTPUT="$WORK_DIR/partial-results/partial-$(printf "%05d" "$PARTITION_ID").csv"
LOG_OUTPUT="$WORK_DIR/partial-results/partial-$(printf "%05d" "$PARTITION_ID").log"

if [[ ! -f "$PARTITION_FILE" ]]; then
  echo "ERROR: partition file not found: $PARTITION_FILE" >&2
  exit 1
fi

mkdir -p "$WORK_DIR/partial-results"

echo "== Running Version 3 partition worker =="
echo "Partition id: $PARTITION_ID"
echo "Partition file: $PARTITION_FILE"
echo "Partial output: $PARTIAL_OUTPUT"

./gradlew run --args="--distributed-worker --partition $PARTITION_FILE --partial-output $PARTIAL_OUTPUT" \
  2>&1 | tee "$LOG_OUTPUT"

echo
echo "Partition worker completed."
echo "Partial CSV: $PARTIAL_OUTPUT"
echo "Log: $LOG_OUTPUT"
