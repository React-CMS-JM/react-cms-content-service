#!/bin/sh
# Require JWT_SECRET for HMAC JWT (HmacJwtAuthContextInfoProducer).
# Supports both Quarkus Native (/work/application) and JVM (run-java.sh) images.
set -eu

if [ -z "${JWT_SECRET:-}" ]; then
  echo "docker-entrypoint: ERROR — JWT_SECRET is required." >&2
  exit 1
fi

if [ -x /work/application ]; then
  exec /work/application -Dquarkus.http.host=0.0.0.0 "$@"
fi
if [ -x /opt/jboss/container/java/run/run-java.sh ]; then
  exec /opt/jboss/container/java/run/run-java.sh "$@"
fi

echo "docker-entrypoint: ERROR — no Quarkus runtime found (expected /work/application or run-java.sh)." >&2
exit 1
