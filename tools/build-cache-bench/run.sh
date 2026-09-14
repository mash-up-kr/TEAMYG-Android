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
PAIR_A=""
PAIR_B=""

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
    git -C "$tree" checkout --detach "$commit" >/dev/null
}

# S3·S4 는 커밋 쌍이 없으면 의미가 없다.
#
# 여기서 full SHA 로 고정하는 것이 핵심이다. `HEAD~1` 같은 상대 참조를 그대로 들고 다니면
# 러너가 옮기는 트리 HEAD 를 따라 가리키는 커밋이 회차마다 달라진다 — 그러면 B 를 잰다고
# 하고 A 를 재는 일이 에러 없이 일어난다.
validate_pair() {
    case ",$SCENARIOS," in
        *,S3,*|*,S4,*) ;;
        *) return 0 ;;
    esac
    [[ "$PAIR" == *:* ]] || { echo "S3·S4 는 --pair <A>:<B> 가 필요하다" >&2; return 1; }
    PAIR_A=$(git -C "$ROOT" rev-parse --verify --quiet "${PAIR%%:*}^{commit}") \
        || { echo "커밋을 찾을 수 없다: ${PAIR%%:*}" >&2; return 1; }
    PAIR_B=$(git -C "$ROOT" rev-parse --verify --quiet "${PAIR##*:}^{commit}") \
        || { echo "커밋을 찾을 수 없다: ${PAIR##*:}" >&2; return 1; }
    [[ "$PAIR_A" != "$PAIR_B" ]] \
        || { echo "A 와 B 가 같은 커밋이다: $PAIR_A" >&2; return 1; }
    if git -C "$ROOT" diff --quiet "$PAIR_A" "$PAIR_B"; then
        echo "두 커밋의 트리가 완전히 같다. S3·S4 가 같은 값이 된다: $PAIR_A..$PAIR_B" >&2
        return 1
    fi
    # 이후 보고에 찍히는 값도 고정된 SHA 여야 한다.
    PAIR="$PAIR_A:$PAIR_B"
}

# :app:assembleDebug 는 서명·google-services 를 탄다. 한 시간짜리 측정이 중간에 죽지 않게 먼저 본다.
precheck() {
    local tree="$1" missing=0 dirty
    # 미커밋 수정은 두 방향으로 측정을 망친다. 충돌하지 않으면 checkout --detach 가 A·B
    # 양쪽으로 이월시켜 둘 다 오염시키고, 충돌하면 checkout 이 거부돼 측정이 죽는다.
    dirty="$(git -C "$tree" status --porcelain)"
    if [[ -n "$dirty" ]]; then
        echo "대상 트리에 미커밋 변경이 있다: $tree" >&2
        printf '%s\n' "$dirty" >&2
        missing=1
    fi
    [[ -f "$tree/local.properties" ]] ||{ echo "없음: $tree/local.properties (템플릿 local.default.properties)" >&2; missing=1; }
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
    # --build-cache 를 상수로 준다. 켜짐 여부를 체크아웃된 커밋의 gradle.properties 에
    # 맡기면, 그 줄이 없던 시절의 커밋을 쌍으로 골랐을 때 적중 0 건이 "캐시는 값어치가
    # 없다" 로 조용히 둔갑한다.
    if ! (cd "$tree" && ./gradlew "$@" --offline --build-cache \
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

# 사전 상태 빌드는 한 태그에 최대 4번이라 로그 파일명을 공유하면 마지막 하나만 남는다.
# S4 에서 진단 가치가 가장 큰 "B 를 캐시에 굽는" 빌드가 바로 그 덮어써지는 쪽이다.
PREPARE_STEP=0
prepare_run() {
    local tree="$1" tag="$2"; shift 2
    PREPARE_STEP=$((PREPARE_STEP + 1))
    gradle_run "$tree" "$OUT/discard.csv" "$OUT/logs/$tag.prepare$PREPARE_STEP.log" "$@"
}

# 시나리오가 요구하는 "빌드 직전 상태"를 만든다. 이 단계의 시간은 측정하지 않는다.
# 캐시를 채우는 빌드 앞에는 반드시 clean 이 온다 — 출력이 남아 있으면 그 빌드가
# UP_TO_DATE 로 끝나 캐시에 아무것도 안 담긴다.
prepare_state() {
    local scenario="$1" tree="$2" tag="$3"
    local -a targets
    targets=(${TARGETS//,/ })
    PREPARE_STEP=0
    case "$scenario" in
        S0)
            wipe_cache
            prepare_run "$tree" "$tag" clean
            prepare_run "$tree" "$tag" "${targets[@]}"
            ;;
        S1)
            wipe_cache
            prepare_run "$tree" "$tag" clean
            prepare_run "$tree" "$tag" "${targets[@]}"
            prepare_run "$tree" "$tag" clean
            ;;
        S2)
            wipe_cache
            prepare_run "$tree" "$tag" clean
            ;;
        S3)
            wipe_cache
            checkout_commit "$tree" "$PAIR_A"
            prepare_run "$tree" "$tag" clean
            prepare_run "$tree" "$tag" "${targets[@]}"
            checkout_commit "$tree" "$PAIR_B"
            ;;
        S4)
            wipe_cache
            # B 를 먼저 구워 캐시에 담는다. 이것이 "CI 가 이미 B 를 빌드해 뒀다" 를 대역한다.
            checkout_commit "$tree" "$PAIR_B"
            prepare_run "$tree" "$tag" clean
            prepare_run "$tree" "$tag" "${targets[@]}"
            # 출력을 A 기준으로 되돌린다. S3 와 출력 상태가 같아야 캐시만의 차이가 남는다.
            checkout_commit "$tree" "$PAIR_A"
            prepare_run "$tree" "$tag" clean
            prepare_run "$tree" "$tag" "${targets[@]}"
            checkout_commit "$tree" "$PAIR_B"
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

    # prepare_state 의 첫 동작이 캐시 디렉토리 rm -rf 다. 직전 회차 데몬이 살아 있는 채로
    # 지우면 그 데몬이 열어 둔 캐시를 발밑에서 치우는 꼴이다. 먼저 내린다.
    (cd "$tree" && ./gradlew --stop >/dev/null 2>&1) || true
    prepare_state "$scenario" "$tree" "$tag"

    # 사전 상태 시점의 캐시다. 측정 빌드 뒤에 재면 그 빌드가 밀어 넣은 항목이 섞여,
    # 캐시를 비우고 시작하는 S2 가 entries=1 처럼 보인다.
    local pre_cache
    pre_cache=$(cache_stats)

    # 사전 빌드 횟수가 시나리오마다 달라 데몬 온도가 갈린다. 측정 직전에 고정 횟수로 덥혀
    # 출발선을 맞춘다.
    gradle_run "$tree" "$OUT/discard.csv" "$OUT/logs/$tag.warmup.log" help

    local start end daemon
    start=$(now_ms)
    gradle_run "$tree" "$csv" "$OUT/logs/$tag.log" "$target"
    end=$(now_ms)
    daemon=$(cat "$csv.daemon" 2>/dev/null || echo "unknown")

    echo "$scenario,$target,${PAIR:-none},$iteration,$((end - start)),$daemon" >> "$OUT/builds.csv"
    echo "$scenario,$target,$iteration,$pre_cache,$(cache_stats)" >> "$OUT/cache-size.csv"
}

# BSD awk 에는 asort 가 없다. 정렬은 sort 에 맡긴다.
# 짝수 개일 때는 아래쪽 값을 쓴다(평균 아님). README 에 적어 둔다.
median_ms() {
    local scenario="$1" target="$2"
    awk -F, -v s="$scenario" -v t="$target" \
        'NR>1 && $1==s && $2==t {print $5}' "$OUT/builds.csv" \
        | sort -n \
        | awk '{v[n++]=$1} END {if (n) print v[int((n-1)/2)]}'
}

hit_rate() {
    local scenario="$1" target="$2" slug="$scenario-${target//:/_}"
    # 실행하지 않은 시나리오는 tasks/*.csv 가 아예 없다 — 그 경우만 조용히 건너뛴다.
    # nullglob 없이도(bash 3.2) glob 이 안 풀리면 files[0] 은 리터럴 글롭 문자열이라 -e 가 실패한다.
    # 파일이 있는데 집계(awk)가 깨지는 경우는 삼키지 않고 그대로 실패시켜 드러나게 한다.
    local files=("$OUT/tasks/$slug"-*.csv)
    [[ -e "${files[0]}" ]] || return 0
    # up_to_date 와 executed 를 따로 낸다. 적중률만 보면 "전부 UP_TO_DATE(=쌍이 무신호)"와
    # "미스인데 빨랐다"가 똑같이 0.0% 로 보여 무신호를 알아볼 수 없다.
    cat "${files[@]}" | awk -F, -v s="$scenario" -v t="$target" '
        $1=="task_path" { next }
        $1 ~ /^:build-logic:/ { next }
        $2=="SKIPPED" || $2=="NO-SOURCE" { next }
        { n++ }
        $2=="FROM_CACHE" { hit++ }
        $2=="UP_TO_DATE" { utd++ }
        $2=="EXECUTED"   { ex++ }
        END {
            if (n) printf "| %s | %s | %d | %d | %d | %d | %.1f%% |\n", \
                s, t, hit, utd, ex, n, 100*hit/n
        }
    '
}

# 핵심 값을 맨 앞에 둔다. 나머지는 그 값을 읽기 위한 참고값이다.
write_summary() {
    local md="$OUT/summary.md" target scenario s3 s4 m s3_files
    {
        echo "# 빌드 캐시 측정 결과"
        echo
        echo "- 커밋 쌍: ${PAIR:-none}"
        echo "- 반복: $ITERATIONS (중앙값, 짝수면 아래쪽 값)"
        echo
        echo "## 핵심 값 — T_local(S3) 빼기 T_remote(S4)"
        echo
        echo "| target | S3 ms | S4 ms | 차이 ms |"
        echo "|---|---|---|---|"
        for target in ${TARGETS//,/ }; do
            s3=$(median_ms S3 "$target")
            s4=$(median_ms S4 "$target")
            [[ -n "$s3" && -n "$s4" ]] && echo "| $target | $s3 | $s4 | $((s3 - s4)) |"
        done
        echo
        echo "## 참고값"
        echo
        echo "| scenario | target | 중앙값 ms |"
        echo "|---|---|---|"
        for target in ${TARGETS//,/ }; do
            for scenario in $SCENARIO_ORDER; do
                m=$(median_ms "$scenario" "$target")
                [[ -n "$m" ]] && echo "| $scenario | $target | $m |"
            done
        done
        echo
        echo "## 캐시 적중률"
        echo
        echo "분모(actionable)는 included build 와 SKIPPED·NO-SOURCE 를 뺀 전 회차 집계다."
        echo "S3 가 사실상 전부 up_to_date 면 캐시 효과가 아니라 커밋 쌍이 무신호라는 뜻이다."
        echo
        echo "| scenario | target | from_cache | up_to_date | executed | actionable | 적중률 |"
        echo "|---|---|---|---|---|---|---|"
        for target in ${TARGETS//,/ }; do
            for scenario in $SCENARIO_ORDER; do
                hit_rate "$scenario" "$target"
            done
        done
        echo
        echo "## S3 에서 캐시 미스로 남은 태스크"
        echo
        echo "사유는 init script 가 getExecutionReasons() 로 받은 값이다."
        echo "ms 는 $ITERATIONS 회차의 **합**이다 — 1회 빌드 시간이 아니다. target 당 상위 30개만 싣는다."
        # target 을 섞으면 같은 태스크가 두 그래프에서 각각 실행된 시간이 한 줄로 합쳐져
        # 어느 쪽이 비싼지 알 수 없게 된다. 적중률 표와 같은 slug 로 좁힌다.
        for target in ${TARGETS//,/ }; do
            # S3 를 이번 실행에서 안 돌렸으면 그 slug 의 csv 가 아예 없다 — 그 경우만 건너뛴다.
            # 집계(awk)가 깨지는 경우는 삼키지 않고 그대로 실패시켜 드러나게 한다.
            s3_files=("$OUT/tasks/S3-${target//:/_}"-*.csv)
            [[ -e "${s3_files[0]}" ]] || continue
            echo
            echo "### $target"
            echo
            echo '```'
            # head -30 은 쓰지 않는다 — head 가 30줄을 받고 먼저 끝내면 pipefail 아래서
            # sort 가 SIGPIPE(141)로 죽어 write_summary 전체가 죽는다. S3 는 미스가 많은
            # 사유 문자열(수백 바이트)이 정상이라 몇백 줄만 돼도 이 경합에 걸린다.
            # awk 는 입력을 끝까지 읽어 앞 단계에 SIGPIPE 를 보내지 않는다.
            cat "${s3_files[@]}" \
                | awk -F, '$1!="task_path" && $2=="EXECUTED" {sum[$1]+=$3; why[$1]=$4} END {for (t in sum) printf "%8d ms  %-55s %s\n", sum[t], t, why[t]}' \
                | sort -rn | awk 'NR<=30'
            echo '```'
        done
    } > "$md"
    echo "요약: $md"
}

# dry-run 보다 앞이다. 쌍이 어느 SHA 로 고정됐는지 확인할 값싼 창구가 dry-run 뿐이다.
validate_pair || exit 2

if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "tree=${TREE:-<새 worktree>} out=$OUT cache=$CACHE_DIR"
    echo "pair A=${PAIR_A:-<없음>} B=${PAIR_B:-<없음>}"
    plan_runs
    exit 0
fi

mkdir -p "$OUT" "$CACHE_DIR" "$OUT/logs"
ensure_tree
precheck "$TREE" || { echo "선행 조건 미충족" >&2; exit 5; }

echo "scenario,target,pair,iteration,wall_ms,daemon_pid" > "$OUT/builds.csv"
echo "scenario,target,iteration,pre_entries,pre_kb,post_entries,post_kb" > "$OUT/cache-size.csv"

while IFS='|' read -r scenario target iteration; do
    echo "[$scenario] $target ($iteration/$ITERATIONS)"
    measure "$scenario" "$target" "$iteration" "$TREE"
done < <(plan_runs)

rm -f "$OUT/discard.csv" "$OUT/discard.csv.daemon"
write_summary
echo "결과: $OUT"
