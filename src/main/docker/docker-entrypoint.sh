#!/bin/sh
# Materialize an HS256 JWK for SmallRye JWT verification from JWT_SECRET when
# JWT_JWK_PATH does not already point at an existing file.
# Auth signs with JWT_SECRET; content/courses must use the same secret (or JWK).
# Supports both Quarkus Native (/work/application) and JVM (run-java.sh) images.
set -eu

JWK_FILE="${JWT_JWK_FILE:-/tmp/jwt-secret.jwk}"

materialize_jwk_from_secret() {
  secret="$1"
  # Base64 then Base64URL (no padding) — matches SmallRye oct JWK "k"
  if k=$(printf '%s' "${secret}" | base64 -w0 2>/dev/null); then
    :
  else
    k=$(printf '%s' "${secret}" | base64 | tr -d '\n')
  fi
  k=$(printf '%s' "${k}" | tr '+/' '-_' | tr -d '=')
  printf '{"kty":"oct","k":"%s","alg":"HS256"}' "${k}" > "${JWK_FILE}"
  JWT_JWK_PATH="file:${JWK_FILE}"
  export JWT_JWK_PATH
  echo "docker-entrypoint: materialized HS256 JWK at ${JWK_FILE}"
}

existing="${JWT_JWK_PATH:-}"
existing_file=$(printf '%s' "${existing}" | sed 's/^file://')
if [ -n "${existing_file}" ] && [ -f "${existing_file}" ]; then
  echo "docker-entrypoint: using existing JWK at ${existing_file}"
elif [ -n "${JWT_SECRET:-}" ]; then
  materialize_jwk_from_secret "${JWT_SECRET}"
else
  echo "docker-entrypoint: ERROR — set JWT_SECRET (preferred) or JWT_JWK_PATH to an existing JWK file." >&2
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
