#!/usr/bin/env bash
# Imports one position file. Meant to be called by a scheduler (Control-M, Airflow, cron),
# which reads the exit code to decide what happens next:
#   0  loaded cleanly
#   3  loaded, but some records were rejected (see GET /api/imports/{id}/rejections)
#   2  this file was already imported (safe to ignore on a re-run)
#   1  failed (bad file, database down, ...): alert and retry
#
# Usage: bin/import-positions.sh <positions.csv|positions.xml>
# Env:   RISKFEED_JAR (default target/riskfeed-1.0.0.jar), JAVA_OPTS, SPRING_PROFILES_ACTIVE
set -euo pipefail

file="${1:?usage: $0 <positions.csv|positions.xml>}"
if [[ ! -r "$file" ]]; then
  echo "$(date -u +%FT%TZ) ERROR cannot read $file" >&2
  exit 1
fi

jar="${RISKFEED_JAR:-$(dirname "$0")/../target/riskfeed-1.0.0.jar}"
echo "$(date -u +%FT%TZ) INFO importing $file"

set +e
# shellcheck disable=SC2086  # JAVA_OPTS is intentionally split into words
java ${JAVA_OPTS:-} -jar "$jar" --spring.main.web-application-type=none --import="$file"
code=$?
set -e

echo "$(date -u +%FT%TZ) INFO finished $file with exit code $code"
exit "$code"
