#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# checkstyle.sh — run Checkstyle + Spring Java Format validation across all
#                 modules and print a concise violation summary.
#
# Usage:
#   ./scripts/checkstyle.sh              # validate (fail on violation)
#   ./scripts/checkstyle.sh --fix        # apply spring-javaformat formatting first
#   ./scripts/checkstyle.sh --module refinej-core   # single module
# ---------------------------------------------------------------------------
set -euo pipefail

FIX=false
MODULE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --fix) FIX=true ;;
        --module) MODULE="$2"; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
    shift
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
POM="-f $ROOT_DIR/pom.xml"
MVN="$ROOT_DIR/mvnw"
PL_ARG=""
[[ -n "$MODULE" ]] && PL_ARG="-pl $MODULE"

if [[ "$FIX" == "true" ]]; then
    echo "▶ Applying spring-javaformat..."
    "$MVN" $POM spring-javaformat:apply $PL_ARG
    echo "  Done."
fi

echo "▶ Running Checkstyle + Spring Java Format validate..."
"$MVN" $POM validate $PL_ARG 2>&1 | \
    grep -E "\[ERROR\].*\.java|BUILD SUCCESS|BUILD FAILURE" | \
    grep -v "^\[ERROR\] Failed to execute" | head -50

echo ""
echo "Run './scripts/checkstyle.sh --fix' to auto-apply Spring Java Format."
