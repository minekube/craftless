#!/usr/bin/env sh
set -eu

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

exec /opt/craftless/bin/craftless server start \
  --port "${CRAFTLESS_PORT:-8080}" \
  --workspace "${CRAFTLESS_WORKSPACE:-/var/lib/craftless}"
