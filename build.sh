#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: ./build.sh [debug|release|all] [--test]

Defaults to debug. --test includes local unit tests.
BUILD_WORKERS sets Gradle concurrency (default: 2).

Release builds are unsigned unless all four signing variables are set:
  ANIMEPAHE_KEYSTORE, ANIMEPAHE_STORE_PASSWORD,
  ANIMEPAHE_KEY_ALIAS, ANIMEPAHE_KEY_PASSWORD
EOF
}

variant=debug
if [[ ${1:-} == -h || ${1:-} == --help ]]; then
    usage
    exit 0
fi
if [[ $# -gt 0 && $1 != --test ]]; then
    variant=$1
    shift
fi
case "$variant" in
    debug|release|all) ;;
    *) usage >&2; exit 2 ;;
esac
run_tests=false
if [[ ${1:-} == --test ]]; then
    run_tests=true
    shift
fi
if [[ $# -gt 0 ]]; then
    usage >&2
    exit 2
fi
workers=${BUILD_WORKERS:-2}
if [[ ! $workers =~ ^[1-9][0-9]*$ ]]; then
    echo 'BUILD_WORKERS must be a positive integer.' >&2
    exit 2
fi

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"
if [[ $variant != debug && -z ${ANIMEPAHE_KEYSTORE:-} && -f .signing/release.env ]]; then
    source .signing/release.env
    # A migrated checkout can retain the old machine's absolute keystore path.
    if [[ -n ${ANIMEPAHE_KEYSTORE:-} && ! -f $ANIMEPAHE_KEYSTORE ]]; then
        local_keystore="$project_dir/.signing/$(basename -- "$ANIMEPAHE_KEYSTORE")"
        if [[ -f $local_keystore ]]; then export ANIMEPAHE_KEYSTORE=$local_keystore; fi
    fi
fi
release_apk=app/build/outputs/apk/release/app-release-unsigned.apk
if [[ $variant != debug ]]; then
    signing_count=0
    for name in ANIMEPAHE_KEYSTORE ANIMEPAHE_STORE_PASSWORD ANIMEPAHE_KEY_ALIAS ANIMEPAHE_KEY_PASSWORD; do
        if [[ -n ${!name:-} ]]; then
            signing_count=$((signing_count + 1))
        fi
    done
    if (( signing_count > 0 && signing_count < 4 )); then
        echo 'Release signing requires all four ANIMEPAHE_* signing variables. See --help.' >&2
        exit 2
    fi
    if (( signing_count == 4 )); then
        release_apk=app/build/outputs/apk/release/app-release.apk
    else
        echo 'Building an unsigned release APK. Configure signing variables for an installable release.'
    fi
fi

tasks=()
if $run_tests; then tasks+=(:app:testDebugUnitTest); fi
if [[ $variant != release ]]; then tasks+=(:app:assembleDebug); fi
if [[ $variant != debug ]]; then tasks+=(:app:assembleRelease); fi
bash ./gradlew "${tasks[@]}" --max-workers="$workers" --console=plain

if [[ $variant != debug ]]; then
    version_code=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' app/build.gradle.kts | head -n 1)
    version_name=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' app/build.gradle.kts | head -n 1)
    if [[ -n $version_code && -n $version_name ]]; then
        versioned_release="${release_apk%/*}/Anilili-Short-Drama-v${version_name}.${version_code}-release.apk"
        ln -sfn "$(basename -- "$release_apk")" "$project_dir/$versioned_release"
        release_apk=$versioned_release
    fi
fi

echo
echo 'Build complete. APK outputs:'
if [[ $variant != release ]]; then
    printf '  %s/app/build/outputs/apk/debug/app-debug.apk\n' "$project_dir"
fi
if [[ $variant != debug ]]; then
    printf '  %s/%s\n' "$project_dir" "$release_apk"
fi
