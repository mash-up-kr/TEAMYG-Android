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

if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "tree=${TREE:-<새 worktree>} out=$OUT cache=$CACHE_DIR pair=${PAIR:-<없음>}"
    plan_runs
    exit 0
fi

echo "아직 시나리오 실행이 구현되지 않았다. --dry-run 으로 계획만 볼 수 있다." >&2
exit 1
