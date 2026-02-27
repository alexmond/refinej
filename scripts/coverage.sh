#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# coverage.sh — run the full test suite and print a JaCoCo coverage summary.
#
# Usage:
#   ./scripts/coverage.sh                    # run tests + print summary
#   ./scripts/coverage.sh --open             # also open the HTML report
#   ./scripts/coverage.sh --module refinej-core   # single module
# ---------------------------------------------------------------------------
set -euo pipefail

OPEN=false
MODULE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --open) OPEN=true ;;
        --module) MODULE="$2"; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

MVN_ARGS="-f $ROOT_DIR/pom.xml verify"
[[ -n "$MODULE" ]] && MVN_ARGS="$MVN_ARGS -pl $MODULE"

MVN="$ROOT_DIR/mvnw"

echo "▶ Running tests + JaCoCo..."
"$MVN" $MVN_ARGS 2>&1 | grep -E "Tests run:|BUILD"

echo ""
echo "── JaCoCo coverage summary ──────────────────────────────────────────"

# Parse XML reports for line + branch coverage per module
find "$ROOT_DIR" -path "*/target/site/jacoco/jacoco.xml" | sort | while read -r report; do
    module=$(echo "$report" | sed 's|.*/\(refinej-[^/]*\)/target.*|\1|')
    missed_lines=$(grep -o 'type="LINE"[^/]*/>' "$report" | grep -oP 'missed="\K[0-9]+' | awk '{s+=$1} END{print s+0}')
    covered_lines=$(grep -o 'type="LINE"[^/]*/>' "$report" | grep -oP 'covered="\K[0-9]+' | awk '{s+=$1} END{print s+0}')
    total=$((missed_lines + covered_lines))
    if [[ $total -gt 0 ]]; then
        pct=$(awk "BEGIN{printf \"%.1f\", $covered_lines * 100 / $total}")
        echo "  $module: ${pct}% lines ($covered_lines/$total)"
    else
        echo "  $module: no coverage data"
    fi
done

echo ""
echo "── HTML reports ─────────────────────────────────────────────────────"
find "$ROOT_DIR" -path "*/target/site/jacoco/index.html" | sort | while read -r f; do
    module=$(echo "$f" | sed 's|.*/\(refinej-[^/]*\)/target.*|\1|')
    echo "  $module → $f"
done

if [[ "$OPEN" == "true" ]]; then
    find "$ROOT_DIR" -path "*/target/site/jacoco/index.html" | sort | while read -r f; do
        open "$f" 2>/dev/null || xdg-open "$f" 2>/dev/null || echo "Cannot open: $f"
    done
fi
