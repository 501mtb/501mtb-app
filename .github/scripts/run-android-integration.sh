#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$PWD"
LOG_FILE="$RUNNER_TEMP/501mtb-firebase-emulators.log"

cd "$ROOT_DIR/tests/firestore-rules"
npx firebase emulators:start --config ../../firebase.json --only auth,firestore --project demo-501mtb > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!
cleanup() {
  kill "$EMULATOR_PID" 2>/dev/null || true
}
trap cleanup EXIT

READY=0
for attempt in $(seq 1 60); do
  if nc -z 127.0.0.1 8080 && nc -z 127.0.0.1 9099; then
    READY=1
    break
  fi
  sleep 2
done

if [ "$READY" -ne 1 ]; then
  cat "$LOG_FILE"
  exit 1
fi

node seed-android-integration-admin.mjs
cd "$ROOT_DIR"
gradle connectedDebugAndroidTest
