#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# mvnw.sh — project Maven wrapper using IntelliJ-bundled Maven.
#
# Usage: ./scripts/mvnw.sh <goals>
#   ./scripts/mvnw.sh clean install -DskipTests
#   ./scripts/mvnw.sh test -pl refinej-cli
# ---------------------------------------------------------------------------
set -euo pipefail

MVN_HOME="/Users/alex.mondshain/Library/Application Support/JetBrains/IntelliJIdea2025.3/plugins/maven/lib/maven3"

# Build classpath from boot jar + all lib jars
CLASSPATH="$MVN_HOME/boot/plexus-classworlds-2.9.0.jar"
for jar in "$MVN_HOME"/lib/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

exec java -cp "$CLASSPATH" \
    -Dclassworlds.conf="$MVN_HOME/bin/m2.conf" \
    -Dmaven.home="$MVN_HOME" \
    -Dmaven.multiModuleProjectDirectory="$(pwd)" \
    org.codehaus.plexus.classworlds.launcher.Launcher \
    "$@"
