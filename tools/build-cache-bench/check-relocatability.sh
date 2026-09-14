#!/usr/bin/env bash
# 다른 절대경로에서 만들어진 캐시 항목이 적중하는지 본다.
# 여기서 깨지면 리모트 빌드 캐시는 무의미하다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INIT="$ROOT/tools/build-cache-bench/cache-report.init.gradle.kts"
TARGET="${1:-:core:util:jvm:compileKotlin}"

WORK="$(mktemp -d)"
CACHE="$WORK/cache"
SEED_TREE="$WORK/seed"
PROBE_TREE="$WORK/probe"

cleanup() {
    git -C "$ROOT" worktree remove --force "$SEED_TREE" >/dev/null 2>&1 || true
    git -C "$ROOT" worktree remove --force "$PROBE_TREE" >/dev/null 2>&1 || true
    git -C "$ROOT" worktree prune >/dev/null 2>&1 || true
    rm -rf "$WORK"
}
trap cleanup EXIT

# local.properties 가 없으면 :app 구성에서 죽는다. app 타깃이 아니어도 마찬가지다.
seed_tree_files() {
    local tree="$1" f
    for f in local.properties app/google-services.json; do
        [[ -f "$ROOT/$f" ]] && cp "$ROOT/$f" "$tree/$f"
    done
}

git -C "$ROOT" worktree add --detach "$SEED_TREE" HEAD >/dev/null
git -C "$ROOT" worktree add --detach "$PROBE_TREE" HEAD >/dev/null
seed_tree_files "$SEED_TREE"
seed_tree_files "$PROBE_TREE"

run_in() {
    local tree="$1" csv="$2"; shift 2
    (cd "$tree" && ./gradlew "$@" --offline \
        -I "$INIT" \
        -PcacheReport.csv="$csv" \
        -PcacheReport.cacheDir="$CACHE" >/dev/null)
}

# 1) seed 트리에서 캐시를 채운다.
run_in "$SEED_TREE" "$WORK/seed.csv" "$TARGET" --rerun-tasks

# 2) probe 트리에서 같은 캐시를 읽는다. clean 없이는 UP_TO_DATE 가 나와 판정이 무의미하다.
run_in "$PROBE_TREE" "$WORK/probe-clean.csv" clean
run_in "$PROBE_TREE" "$WORK/probe.csv" "$TARGET"

echo "target:     $TARGET"
echo "seed tree:  $SEED_TREE"
echo "probe tree: $PROBE_TREE"
echo
echo "-- probe outcomes --"
awk -F, 'NR>1 {c[$2]++} END {for (o in c) printf "%-12s %d\n", o, c[o]}' "$WORK/probe.csv"
echo
echo "-- not reused (executed despite warm cache) --"
awk -F, 'NR>1 && $2=="EXECUTED" {printf "%s  %s\n", $1, $4}' "$WORK/probe.csv"
