#!/usr/bin/env bash
# 로컬 빌드 캐시 측정 러너. 설계 근거는
# parfait/specs/2026-09-14-build-cache-measurement-harness.md 에 있다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
INIT="$ROOT/tools/build-cache-bench/cache-report.init.gradle.kts"
SCENARIO_ORDER="S0 S1 S2 S3 S4"

TREE=""
SCENARIOS="S0,S1,S2"
TARGETS=":app:assembleDebug,test"
ITERATIONS=3
PAIR=""
CACHE_DIR=""
OUT=""
DRY_RUN=0

usage() {
    cat <<'USAGE'
사용법: run.sh [옵션]
  --tree <경로>       측정 대상 트리. 생략하면 worktree 를 새로 만든다.
  --scenarios <목록>  S0,S1,S2,S3,S4 중 쉼표 구분. 실행 순서는 러너가 고정한다.
  --targets <목록>    Gradle 태스크 경로, 쉼표 구분. 기본 :app:assembleDebug,test
  --iterations <N>    시나리오당 반복 횟수. 기본 3
  --pair <A:B>        S3·S4 가 쓸 커밋 쌍. 두 커밋을 콜론으로 잇는다.
  --cache-dir <경로>  전용 빌드 캐시 경로. 기본 <out>/cache
  --out <경로>        결과 디렉토리. 기본 tools/build-cache-bench/runs/<timestamp>
  --dry-run           실행 계획만 출력한다.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --tree) TREE="$2"; shift 2 ;;
        --scenarios) SCENARIOS="$2"; shift 2 ;;
        --targets) TARGETS="$2"; shift 2 ;;
        --iterations) ITERATIONS="$2"; shift 2 ;;
        --pair) PAIR="$2"; shift 2 ;;
        --cache-dir) CACHE_DIR="$2"; shift 2 ;;
        --out) OUT="$2"; shift 2 ;;
        --dry-run) DRY_RUN=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "알 수 없는 옵션: $1" >&2; usage; exit 2 ;;
    esac
done

[[ -n "$TARGETS" ]] || { echo "--targets 가 비었다" >&2; exit 2; }
[[ "$ITERATIONS" =~ ^[1-9][0-9]*$ ]] || { echo "--iterations 는 1 이상의 정수여야 한다: $ITERATIONS" >&2; exit 2; }

OUT="${OUT:-$ROOT/tools/build-cache-bench/runs/$(date +%Y%m%d-%H%M%S)}"
CACHE_DIR="${CACHE_DIR:-$OUT/cache}"

OWNED_TREE=""
BORROWED_TREE=""
BORROWED_HEAD=""

cleanup_tree() {
    if [[ -n "$OWNED_TREE" ]]; then
        git -C "$ROOT" worktree remove --force "$OWNED_TREE" >/dev/null 2>&1 || true
        git -C "$ROOT" worktree prune >/dev/null 2>&1 || true
    fi
    # 빌려 쓴 트리는 지우지 않고 HEAD 만 제자리로 돌린다.
    if [[ -n "$BORROWED_TREE" && -n "$BORROWED_HEAD" ]]; then
        git -C "$BORROWED_TREE" checkout "$BORROWED_HEAD" >/dev/null 2>&1 || true
    fi
}
trap cleanup_tree EXIT

# S3·S4 는 커밋을 오간다. 개발자 체크아웃에서 하면 중단 시 detached HEAD 와 지워진 build/ 가 남는다.
# 명령 치환으로 부르면 서브셸이라 위 전역이 부모에 안 잡힌다 — 값을 echo 하지 않고 직접 대입한다.
ensure_tree() {
    if [[ -n "$TREE" ]]; then
        BORROWED_TREE="$TREE"
        BORROWED_HEAD="$(git -C "$TREE" rev-parse --abbrev-ref HEAD)"
        [[ "$BORROWED_HEAD" != "HEAD" ]] || BORROWED_HEAD="$(git -C "$TREE" rev-parse HEAD)"
        return 0
    fi
    OWNED_TREE="$OUT/tree"
    git -C "$ROOT" worktree add --detach "$OWNED_TREE" HEAD >/dev/null
    local f
    for f in local.properties app/google-services.json; do
        [[ -f "$ROOT/$f" ]] && cp "$ROOT/$f" "$OWNED_TREE/$f"
    done
    TREE="$OWNED_TREE"
}

checkout_commit() {
    local tree="$1" commit="$2"
    git -C "$tree" checkout --detach "$commit" >/dev/null 2>&1
}

# S3·S4 는 커밋 쌍이 없으면 의미가 없다. 콜론이 빠지면 A 와 B 가 같은 커밋이 되어
# 조용히 무의미한 숫자가 나오므로 형식까지 본다.
validate_pair() {
    case ",$SCENARIOS," in
        *,S3,*|*,S4,*) ;;
        *) return 0 ;;
    esac
    [[ "$PAIR" == *:* ]] || { echo "S3·S4 는 --pair <A>:<B> 가 필요하다" >&2; return 1; }
    git -C "$ROOT" rev-parse --verify --quiet "${PAIR%%:*}^{commit}" >/dev/null \
        || { echo "커밋을 찾을 수 없다: ${PAIR%%:*}" >&2; return 1; }
    git -C "$ROOT" rev-parse --verify --quiet "${PAIR##*:}^{commit}" >/dev/null \
        || { echo "커밋을 찾을 수 없다: ${PAIR##*:}" >&2; return 1; }
}

# :app:assembleDebug 는 서명·google-services 를 탄다. 한 시간짜리 측정이 중간에 죽지 않게 먼저 본다.
precheck() {
    local tree="$1" missing=0
    [[ -f "$tree/local.properties" ]] || { echo "없음: $tree/local.properties (템플릿 local.default.properties)" >&2; missing=1; }
    grep -q '^sdk.dir' "$tree/local.properties" 2>/dev/null || { echo "local.properties 에 sdk.dir 없음" >&2; missing=1; }
    if [[ "$TARGETS" == *"assembleDebug"* ]]; then
        [[ -f "$tree/app/google-services.json" ]] || { echo "없음: $tree/app/google-services.json" >&2; missing=1; }
    fi
    # 다른 데몬이 돌면 수치가 흔들린다. 막지는 않고 알린다.
    if "$tree/gradlew" --status 2>/dev/null | grep -qi idle; then
        echo "경고: 유휴 Gradle 데몬이 있다. IDE 를 닫고 돌리는 편이 낫다." >&2
    fi
    return "$missing"
}

plan_runs() {
    local scenario target i
    for target in ${TARGETS//,/ }; do
        for scenario in $SCENARIO_ORDER; do
            case ",$SCENARIOS," in *,"$scenario",*) ;; *) continue ;; esac
            for ((i = 1; i <= ITERATIONS; i++)); do
                echo "$scenario|$target|$i"
            done
        done
    done
}

# BSD date 는 %N 을 모르면서 오류도 내지 않는다 — %3N 을 리터럴로 뱉고 exit 0 이다.
# 그래서 date 를 시도한 뒤 폴백하는 구조가 성립하지 않는다. python3 으로 직행한다.
now_ms() {
    python3 -c 'import time;print(int(time.time()*1000))'
}

# 전용 캐시를 통째로 지운다. 빈 변수로 rm -rf 가 나가지 않게 막는다.
wipe_cache() {
    [[ -n "$CACHE_DIR" && "$CACHE_DIR" != "/" ]] || { echo "캐시 경로가 비었다" >&2; exit 3; }
    rm -rf "${CACHE_DIR:?}"
    mkdir -p "$CACHE_DIR"
}

gradle_run() {
    local tree="$1" csv="$2" log="$3"; shift 3
    mkdir -p "$(dirname "$log")"
    if ! (cd "$tree" && ./gradlew "$@" --offline \
        -I "$INIT" \
        -PcacheReport.csv="$csv" \
        -PcacheReport.cacheDir="$CACHE_DIR" >"$log" 2>&1); then
        echo "빌드 실패: $* (로그: $log)" >&2
        return 1
    fi
}

cache_stats() {
    local entries kb
    # gc.properties·cache.lock 은 캐시 항목이 아니다.
    entries=$(find "$CACHE_DIR" -type f ! -name 'gc.properties' ! -name '*.lock' | wc -l | tr -d ' ')
    kb=$(du -sk "$CACHE_DIR" 2>/dev/null | awk '{print $1}')
    echo "$entries,${kb:-0}"
}

# 시나리오가 요구하는 "빌드 직전 상태"를 만든다. 이 단계의 시간은 측정하지 않는다.
# 캐시를 채우는 빌드 앞에는 반드시 clean 이 온다 — 출력이 남아 있으면 그 빌드가
# UP_TO_DATE 로 끝나 캐시에 아무것도 안 담긴다.
prepare_state() {
    local scenario="$1" tree="$2" tag="$3"
    local -a targets
    targets=(${TARGETS//,/ })
    local disc="$OUT/discard.csv" dlog="$OUT/logs/$tag.prepare.log"
    case "$scenario" in
        S0)
            wipe_cache
            gradle_run "$tree" "$disc" "$dlog" clean
            gradle_run "$tree" "$disc" "$dlog" "${targets[@]}"
            ;;
        S1)
            wipe_cache
            gradle_run "$tree" "$disc" "$dlog" clean
            gradle_run "$tree" "$disc" "$dlog" "${targets[@]}"
            gradle_run "$tree" "$disc" "$dlog" clean
            ;;
        S2)
            wipe_cache
            gradle_run "$tree" "$disc" "$dlog" clean
            ;;
        S3)
            wipe_cache
            checkout_commit "$tree" "${PAIR%%:*}"
            gradle_run "$tree" "$disc" "$dlog" clean
            gradle_run "$tree" "$disc" "$dlog" "${targets[@]}"
            checkout_commit "$tree" "${PAIR##*:}"
            ;;
        S4)
            wipe_cache
            # B 를 먼저 구워 캐시에 담는다. 이것이 "CI 가 이미 B 를 빌드해 뒀다" 를 대역한다.
            checkout_commit "$tree" "${PAIR##*:}"
            gradle_run "$tree" "$disc" "$dlog" clean
            gradle_run "$tree" "$disc" "$dlog" "${targets[@]}"
            # 출력을 A 기준으로 되돌린다. S3 와 출력 상태가 같아야 캐시만의 차이가 남는다.
            checkout_commit "$tree" "${PAIR%%:*}"
            gradle_run "$tree" "$disc" "$dlog" clean
            gradle_run "$tree" "$disc" "$dlog" "${targets[@]}"
            checkout_commit "$tree" "${PAIR##*:}"
            ;;
        *)
            echo "알 수 없는 시나리오: $scenario" >&2
            return 4
            ;;
    esac
}

measure() {
    local scenario="$1" target="$2" iteration="$3" tree="$4"
    local tag="$scenario-${target//:/_}-$iteration"
    local csv="$OUT/tasks/$tag.csv"
    mkdir -p "$OUT/tasks" "$OUT/logs"

    prepare_state "$scenario" "$tree" "$tag"

    # 사전 상태를 만드는 빌드 횟수가 시나리오마다 달라서 데몬 온도가 갈린다.
    # 상태를 다 만든 뒤에 재기동하고 고정 횟수로 덥혀야 모든 측정이 같은 조건에서 출발한다.
    (cd "$tree" && ./gradlew --stop >/dev/null 2>&1) || true
    gradle_run "$tree" "$OUT/discard.csv" "$OUT/logs/$tag.warmup.log" help

    local start end daemon
    start=$(now_ms)
    gradle_run "$tree" "$csv" "$OUT/logs/$tag.log" "$target"
    end=$(now_ms)
    daemon=$(cat "$csv.daemon" 2>/dev/null || echo "unknown")

    echo "$scenario,$target,${PAIR:-none},$iteration,$((end - start)),$daemon" >> "$OUT/builds.csv"
    echo "$scenario,$target,$iteration,$(cache_stats)" >> "$OUT/cache-size.csv"
}

if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "tree=${TREE:-<새 worktree>} out=$OUT cache=$CACHE_DIR pair=${PAIR:-<없음>}"
    plan_runs
    exit 0
fi

mkdir -p "$OUT" "$CACHE_DIR" "$OUT/logs"
validate_pair || exit 2
ensure_tree
precheck "$TREE" || { echo "선행 조건 미충족" >&2; exit 5; }

echo "scenario,target,pair,iteration,wall_ms,daemon_pid" > "$OUT/builds.csv"
echo "scenario,target,iteration,entries,kb" > "$OUT/cache-size.csv"

while IFS='|' read -r scenario target iteration; do
    echo "[$scenario] $target ($iteration/$ITERATIONS)"
    measure "$scenario" "$target" "$iteration" "$TREE"
done < <(plan_runs)

rm -f "$OUT/discard.csv" "$OUT/discard.csv.daemon"
echo "결과: $OUT"
