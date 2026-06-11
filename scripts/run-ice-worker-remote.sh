#!/usr/bin/env bash
set -euo pipefail

WORK_DIR="${WORK_DIR:-results/ice-worker}"
ICE_HOST="${ICE_HOST:-0.0.0.0}"
ICE_PORT="${ICE_PORT:-10000}"
ICE_IDENTITY="${ICE_IDENTITY:-sitm-worker}"
APP_JAVA_OPTS="${APP_JAVA_OPTS:--Xmx8g}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-$APP_JAVA_OPTS}"

mkdir -p "$WORK_DIR"

echo "== Starting SITM-MIO Ice worker =="
echo "Identity:  $ICE_IDENTITY"
echo "Endpoint:  tcp -h $ICE_HOST -p $ICE_PORT"
echo "Work dir:  $WORK_DIR"
echo "Java opts: $JAVA_TOOL_OPTIONS"

bash ./gradlew run --args="--ice-worker-server --work-dir $WORK_DIR --ice-host $ICE_HOST --ice-port $ICE_PORT --ice-identity $ICE_IDENTITY"
