#!/bin/sh
# Usage: sh scripts/check-credential-shortcut.sh [--build]
# Runs the credential shortcut tests; --build also assembles the complete debug APK.
# CREDENTIAL_GRADLE can select an installed Gradle executable instead of the wrapper.
# Set CREDENTIAL_OFFLINE=1 to use cached dependencies only.
set -eu

cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
case "${1:-}" in
    '') build_task='' ;;
    --build) build_task=':app:assembleDebug' ;;
    *) echo "Usage: sh scripts/check-credential-shortcut.sh [--build]" >&2; exit 2 ;;
esac
if [ "$#" -gt 1 ]; then
    echo "Usage: sh scripts/check-credential-shortcut.sh [--build]" >&2
    exit 2
fi

# Gradle updates version.properties during configuration, including test runs.
saved_version=$(mktemp "${TMPDIR:-/tmp}/luckytool-version.XXXXXX")
cp app/version.properties "$saved_version"
restore_version() {
    cp "$saved_version" app/version.properties
    rm -f "$saved_version"
}
trap restore_version EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [ -n "${CREDENTIAL_GRADLE:-}" ]; then
    set -- "$CREDENTIAL_GRADLE"
else
    set -- sh ./gradlew
fi
"$@" ${CREDENTIAL_OFFLINE:+--offline} :app:testDebugUnitTest \
    --tests 'com.luckyzyx.luckytool.hook.scopes.systemui.*' \
    --tests '*LockScreenCredentialShortcutPageTest*' \
    ${build_task:+"$build_task"}
