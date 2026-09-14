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
    # --build-cache 는 상수다. 체크아웃된 커밋의 gradle.properties 에 맡기지 않는다.
    (cd "$tree" && ./gradlew "$@" --offline --build-cache \
        -I "$INIT" \
        -PcacheReport.csv="$csv" \
        -PcacheReport.cacheDir="$CACHE" >/dev/null)
}

# 1) seed 트리에서 캐시를 채운다.
run_in "$SEED_TREE" "$WORK/seed.csv" "$TARGET" --rerun-tasks

# 2) probe 트리에서 같은 캐시를 읽는다. clean 없이는 UP_TO_DATE 가 나와 판정이 무의미하다.
run_in "$PROBE_TREE" "$WORK/probe-clean.csv" clean
run_in "$PROBE_TREE" "$WORK/probe.csv" "$TARGET"

# @DisableCachingByDefault 로 선언된 태스크는 어떤 캐시로도 안 줄어든다. 판정에 섞으면
# "캐시 불가"가 "이식 불가"로 둔갑한다.
NOT_CACHEABLE=':jar$|:bundleLibCompileToJar'
# included build 는 run.sh 의 hit_rate 도 뺀다. 여기서 안 빼면 seed 의 --rerun-tasks 가
# 캐시에 담은 build-logic 태스크들이 probe 에서 적중해, 정작 $TARGET 이 EXECUTED 인데도
# 적중 건수가 0 이 아니게 되어 게이트가 통과한다.
INCLUDED='^:build-logic:'

echo "target:     $TARGET"
echo "seed tree:  $SEED_TREE"
echo "probe tree: $PROBE_TREE"
echo
echo "-- probe outcomes (캐시 가능 태스크만) --"
awk -F, -v re="$NOT_CACHEABLE" -v inc="$INCLUDED" \
    'NR>1 && $1 !~ re && $1 !~ inc {c[$2]++} END {for (o in c) printf "%-12s %d\n", o, c[o]}' "$WORK/probe.csv"
echo
echo "-- 판정 제외: 캐시 불가 태스크 / included build --"
awk -F, -v re="$NOT_CACHEABLE" -v inc="$INCLUDED" \
    'NR>1 && ($1 ~ re || $1 ~ inc) {printf "%-12s %s\n", $2, $1}' "$WORK/probe.csv"
echo
echo "-- not reused (executed despite warm cache) --"
awk -F, -v re="$NOT_CACHEABLE" -v inc="$INCLUDED" \
    'NR>1 && $1 !~ re && $1 !~ inc && $2=="EXECUTED" {printf "%s  %s\n", $1, $4}' "$WORK/probe.csv"
echo

# 게이트는 두 조건을 함께 본다. 측정하려는 바로 그 태스크가 적중했는가(target_outcome),
# 그리고 판정 대상 전체에 적중이 하나라도 있는가(hits).
target_outcome=$(awk -F, -v t="$TARGET" 'NR>1 && $1==t {print $2; exit}' "$WORK/probe.csv")
hits=$(awk -F, -v re="$NOT_CACHEABLE" -v inc="$INCLUDED" \
    'NR>1 && $1 !~ re && $1 !~ inc && $2=="FROM_CACHE" {n++} END {print n+0}' "$WORK/probe.csv")

if [[ "$target_outcome" != "FROM_CACHE" ]]; then
    echo "FAIL: $TARGET 이 FROM_CACHE 가 아니다 (${target_outcome:-probe.csv 에 없음})." >&2
    exit 1
fi
if [[ "$hits" -eq 0 ]]; then
    echo "FAIL: 판정 대상 중 FROM_CACHE 가 0 건이다. 다른 경로에서 만든 캐시가 재사용되지 않는다." >&2
    exit 1
fi
echo "PASS: $TARGET FROM_CACHE, 판정 대상 적중 $hits 건."
