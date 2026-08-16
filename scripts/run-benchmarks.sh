#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/android"

if [[ -x ./gradlew ]]; then
  ./gradlew :benchmark:run
  ./gradlew :shared:allTests
  ./gradlew :shared:jsBrowserProductionWebpack
else
  echo "Gradle wrapper not found. Install JDK 17, Android SDK 35, and Gradle 8.9+, then run:" >&2
  echo "  ./gradlew :benchmark:run" >&2
  echo "  ./gradlew :shared:allTests" >&2
  echo "  ./gradlew :shared:jsBrowserProductionWebpack" >&2
  exit 2
fi
