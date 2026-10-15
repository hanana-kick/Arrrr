#!/usr/bin/env bash
# Keep evidence even if a device test fails; preserve the original failing exit code.
set -euo pipefail
layout=${1:?layout required}
size=${2:?size required}
adb shell rm -rf /sdcard/Download/ArrrrScreenshots
adb shell wm size "$size"
adb shell wm density 320
set +e
./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.layout=$layout" --stacktrace
test_status=$?
set -e
mkdir -p ui-screenshots
pull_status=0
adb pull /sdcard/Download/ArrrrScreenshots/. ui-screenshots/ || pull_status=$?
if [ "$test_status" -ne 0 ]; then
  adb logcat -d -t 2000 > ui-screenshots/failure-logcat.txt || true
  find app/build/outputs/androidTest-results -name 'TEST-*.xml' -exec cat {} \; || true
  exit "$test_status"
fi
if [ "$pull_status" -ne 0 ]; then
  echo 'Tests passed but screenshot export failed.' >&2
  exit "$pull_status"
fi
count=$(find ui-screenshots -name '*.png' -size +4k | wc -l)
if [ "$count" -lt 5 ]; then
  echo "Expected at least five non-empty screenshots; found $count." >&2
  exit 1
fi
echo "Tests and $count screenshots verified for $layout."
