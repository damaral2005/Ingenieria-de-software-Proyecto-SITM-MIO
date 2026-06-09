#!/usr/bin/env bash
set -euo pipefail

V2_OUTPUT="${V2_OUTPUT:-results/route_month_speeds_minipilot_v2.csv}"
V3_OUTPUT="${V3_OUTPUT:-results/route_month_speeds_minipilot_v3.csv}"

if [[ ! -f "$V2_OUTPUT" ]]; then
  echo "ERROR: V2 output not found: $V2_OUTPUT" >&2
  exit 1
fi

if [[ ! -f "$V3_OUTPUT" ]]; then
  echo "ERROR: V3 output not found: $V3_OUTPUT" >&2
  exit 1
fi

echo "== Comparing V2 and V3 MiniPilot outputs =="
echo "V2: $V2_OUTPUT"
echo "V3: $V3_OUTPUT"
echo

if cmp -s "$V2_OUTPUT" "$V3_OUTPUT"; then
  echo "OK: V2 and V3 outputs are byte-for-byte identical."
  exit 0
fi

echo "Outputs differ. Showing unified diff preview:"
diff -u "$V2_OUTPUT" "$V3_OUTPUT" | head -n 80
exit 2
