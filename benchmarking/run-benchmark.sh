#!/usr/bin/env bash
set -euo pipefail

ITERATIONS="${ITERATIONS:-30}"
WARMUP="${WARMUP:-5}"
BASE_URL="${BASE_URL:-http://localhost:8080}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --iterations)
      ITERATIONS="$2"
      shift 2
      ;;
    --warmup)
      WARMUP="$2"
      shift 2
      ;;
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      echo "Usage: $0 [--iterations N] [--warmup N] [--base-url URL]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
CSV_PATH="${SCRIPT_DIR}/benchmark-${TIMESTAMP}.csv"
JSON_PATH="${SCRIPT_DIR}/benchmark-${TIMESTAMP}.json"
TMP_CSV="$(mktemp)"
RESP_FILE="$(mktemp)"
trap 'rm -f "${TMP_CSV}" "${RESP_FILE}"' EXIT

assert_app_running() {
  if ! curl -sS --max-time 3 "${BASE_URL}/api-docs" >/dev/null; then
    echo "Application is not reachable at ${BASE_URL}. Start the app with preloaded MariaDB data and Lucene/OpenSearch indexes, then rerun."
    exit 1
  fi
}

post_json_elapsed_ms() {
  local url="$1"
  local body="$2"
  curl -sS -o "${RESP_FILE}" -w "%{time_total}" \
    -H "Content-Type: application/json" \
    -X POST "${url}" \
    -d "${body}"
}

extract_total_hits() {
  local hits
  hits="$(grep -Eo '"totalHits"[[:space:]]*:[[:space:]]*[0-9]+' "${RESP_FILE}" | head -n1 | grep -Eo '[0-9]+' || true)"
  if [[ -z "${hits}" ]]; then
    echo 0
  else
    echo "${hits}"
  fi
}

compute_stats() {
  local scenario="$1"
  local engine="$2"
  mapfile -t vals < <(awk -F, -v s="${scenario}" -v e="${engine}" 'NR>1 && $1==s && $2==e {print $3}' "${TMP_CSV}" | sort -n)
  local count="${#vals[@]}"
  local avg="0.000"
  local p50="0.000"
  local p95="0.000"
  local min="0.000"
  local max="0.000"
  if [[ "${count}" -gt 0 ]]; then
    avg="$(printf "%s\n" "${vals[@]}" | awk '{sum+=$1} END {printf "%.3f", sum/NR}')"
    min="$(awk -v x="${vals[0]}" 'BEGIN {printf "%.3f", x}')"
    max="$(awk -v x="${vals[count-1]}" 'BEGIN {printf "%.3f", x}')"
    local idx50=$(( ((50 * count + 99) / 100) - 1 ))
    local idx95=$(( ((95 * count + 99) / 100) - 1 ))
    (( idx50 < 0 )) && idx50=0
    (( idx95 < 0 )) && idx95=0
    (( idx50 >= count )) && idx50=$((count - 1))
    (( idx95 >= count )) && idx95=$((count - 1))
    p50="$(awk -v x="${vals[idx50]}" 'BEGIN {printf "%.3f", x}')"
    p95="$(awk -v x="${vals[idx95]}" 'BEGIN {printf "%.3f", x}')"
  fi
  local max_hits
  max_hits="$(awk -F, -v s="${scenario}" -v e="${engine}" 'NR>1 && $1==s && $2==e {if($4>m)m=$4} END {print m+0}' "${TMP_CSV}")"
  echo "${scenario},${engine},${ITERATIONS},${WARMUP},${avg},${p50},${p95},${min},${max},${max_hits}"
}

SCENARIOS=(
  "baseline_natural_light|{\"queryText\":\"Natural Light\",\"city\":\"Nuremberg\",\"propertyType\":\"APARTMENT\",\"limit\":20}"
  "modern_area_berlin|{\"queryText\":\"modern\",\"city\":\"Berlin\",\"minAreaInSquareMeter\":50,\"maxAreaInSquareMeter\":150,\"limit\":20}"
  "geo_modern_berlin_5km|{\"queryText\":\"modern\",\"city\":\"Berlin\",\"centerLatitude\":52.520008,\"centerLongitude\":13.404954,\"radiusInKilometers\":5,\"limit\":20}"
)

assert_app_running
echo "Running benchmark using preloaded MariaDB data and existing Lucene/OpenSearch indexes."

echo "scenario,engine,elapsed_ms,total_hits" > "${TMP_CSV}"

for entry in "${SCENARIOS[@]}"; do
  scenario="${entry%%|*}"
  body="${entry#*|}"
  for engine in lucene opensearch mariadb-fts; do
    url="${BASE_URL}/api/${engine}/search"
    echo "Warmup: scenario=${scenario}, engine=${engine}"
    for ((i=0; i<WARMUP; i++)); do
      post_json_elapsed_ms "${url}" "${body}" >/dev/null
    done
    echo "Benchmark: scenario=${scenario}, engine=${engine}"
    for ((i=1; i<=ITERATIONS; i++)); do
      elapsed="$(post_json_elapsed_ms "${url}" "${body}")"
      hits="$(extract_total_hits)"
      elapsed_ms="$(awk -v s="${elapsed}" 'BEGIN {printf "%.3f", s*1000.0}')"
      echo "${scenario},${engine},${elapsed_ms},${hits}" >> "${TMP_CSV}"
    done
  done
done

{
  echo "scenario,engine,iterations,warmup,avgMs,p50Ms,p95Ms,minMs,maxMs,totalHits"
  while IFS='|' read -r scenario _; do
    compute_stats "$scenario" "lucene"
    compute_stats "$scenario" "opensearch"
    compute_stats "$scenario" "mariadb-fts"
  done < <(printf '%s\n' "${SCENARIOS[@]}")
} > "${CSV_PATH}"

awk -F, '
BEGIN {
  print "["
}
NR==1 { next }
{
  if (n > 0) {
    print ","
  }
  printf "  {\"scenario\":\"%s\",\"engine\":\"%s\",\"iterations\":%s,\"warmup\":%s,\"avgMs\":%s,\"p50Ms\":%s,\"p95Ms\":%s,\"minMs\":%s,\"maxMs\":%s,\"totalHits\":%s}",
    $1,$2,$3,$4,$5,$6,$7,$8,$9,$10
  n++
}
END {
  print ""
  print "]"
}
' "${CSV_PATH}" > "${JSON_PATH}"

echo ""
echo "Benchmark complete"
echo "CSV: ${CSV_PATH}"
echo "JSON: ${JSON_PATH}"
cat "${CSV_PATH}"
