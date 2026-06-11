#!/usr/bin/env bash
set -euo pipefail

PACKAGE_PATH="${PACKAGE_PATH:-/tmp/sitm-mio-v3-ice.tar.gz}"

mkdir -p "$(dirname "$PACKAGE_PATH")"

tar \
  --exclude='.git' \
  --exclude='.gradle' \
  --exclude='build' \
  --exclude='results/*.csv' \
  --exclude='results/*.log' \
  --exclude='*.tar.gz' \
  --exclude='swarch@*' \
  -czf "$PACKAGE_PATH" \
  .

echo "Created deployment package: $PACKAGE_PATH"
echo
echo "Copy it to each lab PC, for example:"
echo "  scp $PACKAGE_PATH swarch@10.147.17.104:/tmp/"
echo "  ssh swarch@10.147.17.104 'rm -rf ~/sitm-mio-v3 && mkdir -p ~/sitm-mio-v3 && tar -xzf /tmp/$(basename "$PACKAGE_PATH") -C ~/sitm-mio-v3'"
