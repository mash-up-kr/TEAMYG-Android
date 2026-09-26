---
id: build-cache-measurement-harness
title: 로컬 빌드 캐시 측정 하니스 구현 계획 (7 Task)
status: done
type: work-order
created: 2026-09-14
updated: 2026-09-16
platforms: android
owner: android
related_adr: ADR-0003
related_spec: build-cache-measurement-harness, ci-gradle-cache-seeding
related_code: tools/build-cache-bench/run.sh, tools/build-cache-bench/check-relocatability.sh, tools/build-cache-bench/cache-report.init.gradle.kts, tools/build-cache-bench/report.py, gradle.properties
archived_reason: develop 머지(PR #499 `f37a76540`, 2026-09-16)
tags: [plan, parfait, build, ci]
---

# 로컬 빌드 캐시 측정 하니스 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 리모트 빌드 캐시가 실제로 얼마를 사 오는지 재는 로컬 측정 하니스를 만든다.

**Architecture:** Gradle init script가 빌드 캐시 디렉토리를 하니스 전용 경로로 돌리고 태스크
outcome을 CSV로 떨군다. 셸 러너가 시나리오별 사전 상태(빌드 출력·캐시 내용물·체크아웃 커밋)를
세우고 반복 측정한다. 핵심 지표는 커밋 쌍 `(A, B)`에 대해 전용 캐시가 `A`만 담은 상태로 `B`를
빌드한 시간과 `A`·`B` 둘 다 담은 상태로 빌드한 시간의 차이이며, 뒤쪽이 CI가 이미 구운 항목을
개발자가 받는 상태를 대역한다.

**Tech Stack:** Gradle 9.5 init script (Kotlin DSL), `BuildEventsListenerRegistry` +
`OperationCompletionListener`, bash 3.2, `git worktree`.

**Spec:** [`parfait/specs/2026-09-14-build-cache-measurement-harness.md`](../../specs/archive/2026-09-14-build-cache-measurement-harness.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 위키 저장소가 아니다. 로컬 절대경로는
  `wiki/personal-private/project-paths.md`에 있고, 리모트는 `mash-up-kr/TEAMYG-Android`다.
- **브랜치는 `build/build-cache-measurement`.** 이미 존재하며 `develop`과 같은 자리에 있다.
- **태스크마다 커밋한다.** `git push`와 PR 생성은 사용자 승인 전까지 하지 않는다.
- **새 도구 의존성을 들이지 않는다.** `bats`·`gradle-profiler`·`jq`·`gawk` 설치를 요구하지 않는다.
  검증은 실제 Gradle 실행과 러너의 dry-run 출력으로 한다.
- **이 머신은 macOS이고 셸 도구가 BSD 계열이다.** bash 3.2, BSD `awk`(`asort` 없음),
  BSD `date`(`%N` 없음). GNU 전용 기능을 전제하지 않는다.
- **개발자의 워킹 트리와 Gradle User Home을 파괴하지 않는다.** `~/.gradle/caches/build-cache-1`에
  쓰지 않고, 측정 대상 트리에서 `git stash`와 `git checkout -f`를 쓰지 않는다. 측정 빌드는
  개발자 체크아웃이 아니라 전용 worktree에서 돈다.
- **절대경로를 스크립트에 박지 않는다.** 경로는 인자나 스크립트 위치 기준 상대경로로 받는다.
- **주석 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다).
- **Gradle 9.5 / JDK 17.** `gradle/wrapper/gradle-wrapper.properties`와 CI의 `setup-java` 설정 기준.

---

### Task 1: init script — 태스크 outcome과 실행 사유 수집

**Files:**
- Create: `tools/build-cache-bench/cache-report.init.gradle.kts`

**Interfaces:**
- Consumes: 없음.
- Produces: Gradle 프로퍼티 `cacheReport.csv`(출력 파일 절대경로)를 받아 헤더
  `task_path,outcome,duration_ms,execution_reasons`를 가진 CSV를 쓴다. 같은 경로에 `.daemon`을
  덧붙인 파일에 이 빌드를 실행한 Gradle 데몬의 PID를 쓴다. 이후 모든 태스크가 이 init script를
  `-I`로 주입한다.

**배경:** `TaskExecutionListener`와 `gradle.taskGraph.afterTask`는 Gradle 9.5에 아직 있지만
deprecated이고, configuration cache를 켜면 `Listener registration ... is unsupported.`로 빌드가
깨진다. 지원되는 경로인 `BuildEventsListenerRegistry`를 쓴다.

**함정:** init script 스코프에는 `objects`가 없다. `objects.newInstance` + `@Inject`로 서비스를
얻으려 하면 `Unresolved reference 'objects'`로 죽는다. `org.gradle.kotlin.dsl.support.serviceOf`를
쓴다.

**타입 계층(실측 확인):** `TaskSuccessResult`와 `TaskFailureResult`는 `TaskExecutionResult`를
상속하지만 `TaskSkippedResult`는 상속하지 않는다. 그래서 실행 사유는 `as?`로 받아야 하고,
스킵된 태스크는 빈 문자열이 된다.

- [ ] **Step 1: init script를 작성한다**

```kotlin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.io.File

abstract class TaskOutcomeRecorder :
    BuildService<TaskOutcomeRecorder.Params>, OperationCompletionListener, AutoCloseable {

    interface Params : BuildServiceParameters {
        val csv: RegularFileProperty
    }

    private val rows = StringBuilder()

    override fun onFinish(event: FinishEvent) {
        if (event !is TaskFinishEvent) return
        val result = event.result
        // getSkipMessage() 가 돌려주는 "NO-SOURCE" 는 하이픈이다. 원본 대조를 위해 정규화하지 않는다.
        val outcome = when (result) {
            is TaskSuccessResult -> when {
                result.isFromCache -> "FROM_CACHE"
                result.isUpToDate -> "UP_TO_DATE"
                else -> "EXECUTED"
            }
            is TaskSkippedResult -> result.skipMessage
            is TaskFailureResult -> "FAILED"
            else -> "UNKNOWN"
        }
        // 왜 실행됐는지를 남긴다. S3 에서 EXECUTED 로 남은 태스크의 사유가 판단 재료다.
        // 쉼표와 따옴표는 CSV 구조를 깨므로 미리 지운다.
        val reasons = (result as? TaskExecutionResult)?.executionReasons.orEmpty()
            .joinToString(";") { it.replace(',', ' ').replace('"', '\'') }
        rows.append(event.descriptor.taskPath).append(',')
            .append(outcome).append(',')
            .append(result.endTime - result.startTime).append(',')
            .append('"').append(reasons).append('"').append('\n')
    }

    override fun close() {
        val target = parameters.csv.get().asFile
        target.parentFile.mkdirs()
        target.writeText("task_path,outcome,duration_ms,execution_reasons\n" + rows)
        // 데몬 온도 편향을 사후에 확인하려면 어느 데몬이 이 빌드를 돌렸는지 알아야 한다.
        File(target.path + ".daemon").writeText(ProcessHandle.current().pid().toString())
    }
}

val csvPath = providers.gradleProperty("cacheReport.csv")
    .getOrElse(File(gradle.startParameter.currentDir, "build/task-outcomes.csv").absolutePath)

val recorder = gradle.sharedServices.registerIfAbsent(
    "taskOutcomeRecorder",
    TaskOutcomeRecorder::class,
) { parameters.csv.set(File(csvPath)) }

gradle.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(recorder)
```

- [ ] **Step 2: 실행해서 CSV와 데몬 파일이 나오는지 확인한다**

Run (TJYG-Android 루트에서):
```bash
./gradlew help \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-smoke.csv"
head -3 build/bench-smoke.csv
cat build/bench-smoke.csv.daemon
```

Expected: 빌드 성공. 헤더가 `task_path,outcome,duration_ms,execution_reasons`이고 `:help` 행이
있다. included build 태스크(`:build-logic:convention:*`)도 행으로 잡힌다. `.daemon` 파일에 숫자
PID가 있다.

- [ ] **Step 3: outcome 네 값과 실행 사유가 실제로 갈리는지 확인한다**

Run:
```bash
./gradlew :core:util:jvm:compileKotlin --rerun-tasks \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-run1.csv"
./gradlew :core:util:jvm:compileKotlin \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-run2.csv"
grep ':core:util:jvm:compileKotlin' build/bench-run1.csv build/bench-run2.csv
grep -c 'NO-SOURCE' build/bench-run2.csv
awk -F, 'NF != 4' build/bench-run1.csv | head
```

Expected: 첫 실행이 `EXECUTED`이고 마지막 컬럼에 사유 문자열이 비어 있지 않다. 두 번째 실행이
`UP_TO_DATE`다. `NO-SOURCE` 행이 하나 이상 있다(하이픈 표기 확인). 마지막 `awk`가 아무것도 찍지
않는다 — 컬럼 수가 깨진 행이 없다는 뜻이다.

- [ ] **Step 4: 스모크 산출물을 지우고 커밋한다**

```bash
rm -f build/bench-smoke.csv* build/bench-run1.csv* build/bench-run2.csv*
git add tools/build-cache-bench/cache-report.init.gradle.kts
git commit -m "feat: 빌드 캐시 측정용 태스크 outcome 수집 init script를 추가한다"
```

---

### Task 2: init script — 전용 빌드 캐시 디렉토리 전환

**Files:**
- Modify: `tools/build-cache-bench/cache-report.init.gradle.kts`

**Interfaces:**
- Consumes: Task 1의 init script.
- Produces: Gradle 프로퍼티 `cacheReport.cacheDir`(디렉토리 절대경로). 주면 그 경로를 로컬 빌드
  캐시로 쓰고, 안 주면 Gradle 기본값을 그대로 둔다.

**왜 필요한가:** 시나리오가 요구하는 "캐시에 커밋 `A`만" / "`A`와 `B` 모두" 상태를 디렉토리
교체로 만든다. 동시에 개발자의 `~/.gradle/caches/build-cache-1`을 건드리지 않는다.

- [ ] **Step 1: 전환 코드를 파일 끝에 추가한다**

```kotlin
// 측정이 개발자의 ~/.gradle 캐시를 쓰거나 더럽히지 않게 한다.
val benchCacheDir = providers.gradleProperty("cacheReport.cacheDir").orNull
if (benchCacheDir != null) {
    gradle.settingsEvaluated(
        Action<Settings> {
            buildCache.local.directory = File(benchCacheDir)
        },
    )
}
```

import 두 줄을 상단 import 블록에 추가한다.

```kotlin
import org.gradle.api.Action
import org.gradle.api.initialization.Settings
```

`Action<Settings>`로 명시하는 이유는 `settingsEvaluated`에 `Closure` 오버로드가 함께 있어서다.
명시하면 어느 해석으로도 흔들리지 않는다.

- [ ] **Step 2: 전용 디렉토리에만 쓰는지 확인한다**

Run:
```bash
BENCH=$(mktemp -d)
BEFORE=$(ls ~/.gradle/caches/build-cache-1 | wc -l)
./gradlew :core:util:jvm:compileKotlin --rerun-tasks \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-cache.csv" \
  -PcacheReport.cacheDir="$BENCH"
AFTER=$(ls ~/.gradle/caches/build-cache-1 | wc -l)
echo "original before=$BEFORE after=$AFTER"
echo "bench entries=$(find "$BENCH" -type f | wc -l)"
```

Expected: `before`와 `after`가 같다. `bench entries`가 1 이상이다.

- [ ] **Step 3: 전용 디렉토리에서 캐시 적중이 나는지 확인한다**

Run (위 `$BENCH`를 그대로 쓴다):
```bash
./gradlew :core:util:jvm:clean \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-clean.csv" \
  -PcacheReport.cacheDir="$BENCH"
./gradlew :core:util:jvm:compileKotlin \
  -I tools/build-cache-bench/cache-report.init.gradle.kts \
  -PcacheReport.csv="$PWD/build/bench-hit.csv" \
  -PcacheReport.cacheDir="$BENCH"
grep ':core:util:jvm:compileKotlin' build/bench-hit.csv
```

Expected: outcome이 `FROM_CACHE`다. 아니면 전용 디렉토리 전환이 먹지 않은 것이므로 여기서 멈춘다.

- [ ] **Step 4: 정리하고 커밋한다**

```bash
rm -rf "$BENCH" build/bench-cache.csv* build/bench-clean.csv* build/bench-hit.csv*
git add tools/build-cache-bench/cache-report.init.gradle.kts
git commit -m "feat: 측정용 전용 빌드 캐시 디렉토리 전환을 init script에 추가한다"
```

---
### Task 3: 게이트 G0 — 캐시 항목 이식성 확인

**Files:**
- Create: `tools/build-cache-bench/check-relocatability.sh`

**Interfaces:**
- Consumes: Task 1·2의 init script.
- Produces: 이식 가능/불가 태스크 목록과 적중 건수. **이 게이트가 실패하면 이후 태스크를 진행하지
  않고 사용자에게 보고한다.**

**왜 첫 관문인가:** 리모트 캐시는 다른 머신이 만든 항목을 받는 구조다. 절대경로가 다르다는
이유만으로 적중하지 않는다면, 핵심 값이 얼마로 나오든 리모트 캐시의 가치는 0이다.

**설계상 반드시 지킬 것 셋** — 초안이 셋 다 틀려서 게이트가 거짓 실패했다.

- worktree는 `local.properties`가 없으면 configuration 단계에서 죽는다. `app/build.gradle.kts`가
  그 파일을 조건 없이 열고, configuration-on-demand가 꺼져 있어 `:core:*` 타깃도 `:app` 구성을
  탄다. 두 트리 모두에 복사해야 한다.
- probe 트리를 `clean`하지 않으면 빌드 출력이 남아 `FROM_CACHE`가 아니라 `UP_TO_DATE`가 나온다.
  이식성을 확인하지 못한 채 통과처럼 보인다.
- **개발자 체크아웃에서 빌드하지 않는다.** seed와 probe 둘 다 별도 worktree여야 Global
  Constraint를 지키면서 서로 다른 절대경로라는 조건도 만족한다.

- [ ] **Step 1: 확인 스크립트를 작성한다**

```bash
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
```

- [ ] **Step 2: 실행 권한을 주고 돌린다**

Run:
```bash
chmod +x tools/build-cache-bench/check-relocatability.sh
./tools/build-cache-bench/check-relocatability.sh
```

Expected: `probe outcomes`에 `FROM_CACHE`가 1건 이상 잡힌다. probe 트리에서 빌드한 적 없는
캐시인데도 적중한다는 뜻이다.

- [ ] **Step 3: 더 넓은 그래프로 한 번 더 돌린다**

Run:
```bash
./tools/build-cache-bench/check-relocatability.sh :data:compileDebugKotlin
```

Expected: `FROM_CACHE`가 다수다. `not reused` 목록이 길면 그 태스크들이 이식 불가 후보이므로
사유와 함께 기록한다.

> `:domain`은 JVM 모듈이라 `compileDebugKotlin`이 없다(`:domain:compileKotlin`이다). Android
> variant 태스크를 보려면 `:data`나 `:feature:*` 계열을 쓴다.

- [ ] **Step 4: 개발자 체크아웃이 온전한지 확인한다**

Run:
```bash
git status --porcelain
git worktree list
```

Expected: 변경이 스크립트 신규 1건뿐이고, worktree 목록에 임시 트리가 남아 있지 않다.

- [ ] **Step 5: 결과를 판정하고 보고한다**

`FROM_CACHE`가 0건이면 **여기서 멈춘다.** 이후 태스크를 진행하지 않고 `not reused` 목록과 함께
사용자에게 보고한다. 스펙의 전제가 깨진 것이므로 하니스를 더 짓는 것이 의미가 없다.

적중이 나면 커밋한다.

```bash
git add tools/build-cache-bench/check-relocatability.sh
git commit -m "feat: 캐시 항목 이식성 확인 스크립트를 추가한다"
```

---

### Task 4: 러너 뼈대 — 인자·선행 조건·dry-run

**Files:**
- Create: `tools/build-cache-bench/run.sh`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: Task 1·2의 init script.
- Produces: `run.sh`. 인자 `--tree`·`--scenarios`·`--targets`·`--iterations`·`--pair`·`--cache-dir`·
  `--out`·`--dry-run`을 받는다. 셸 함수 `precheck`(선행 조건 검사)와 `plan_runs`(실행 계획 출력)를
  이후 태스크가 쓴다.

**시나리오 순서는 러너가 고정한다.** `--scenarios`에 적은 순서를 그대로 쓰면, 앞 시나리오가
남긴 빌드 출력과 캐시가 뒤 시나리오의 사전 상태 구성에 영향을 준다. 정본 순서
`S0 S1 S2 S3 S4`로 정렬해서 돌린다.

- [ ] **Step 1: 러너 뼈대를 작성한다**

```bash
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
```

- [ ] **Step 2: dry-run 출력이 기대와 맞는지 확인한다**

Run:
```bash
chmod +x tools/build-cache-bench/run.sh
./tools/build-cache-bench/run.sh --dry-run --scenarios S1,S0 --targets test --iterations 2
```

Expected: 첫 줄이 `tree=...` 요약이고 이어서 정확히 4줄이 나온다. **`--scenarios`에 `S1,S0`
순서로 줬어도 `S0`이 먼저 나와야 한다** — 러너가 순서를 고정한다는 뜻이다.
```
S0|test|1
S0|test|2
S1|test|1
S1|test|2
```

- [ ] **Step 3: 커밋 쌍 검증이 걸리는지 확인한다**

Run:
```bash
./tools/build-cache-bench/run.sh --dry-run --scenarios S3 --targets test || echo "rc=$?"
./tools/build-cache-bench/run.sh --dry-run --scenarios S3 --targets test --pair "nosuchref:HEAD" || echo "rc=$?"
```

> `validate_pair`는 Task 6에서 실행 본문에 연결된다. 이 Step은 함수가 문법적으로 올바른지와
> 인자 파싱이 도는지까지만 본다. dry-run이 정상 출력되면 통과다.

- [ ] **Step 4: 결과 디렉토리를 커밋 대상에서 뺀다**

`.gitignore` 끝에 추가한다.

```
# 빌드 캐시 측정 결과 — 머신마다 다르고 저장소에 쌓일 이유가 없다
/tools/build-cache-bench/runs/
```

- [ ] **Step 5: 커밋한다**

```bash
git add tools/build-cache-bench/run.sh .gitignore
git commit -m "feat: 빌드 캐시 측정 러너 뼈대와 선행 조건 검사를 추가한다"
```

- [ ] **Step 6: 커밋 후에 선행 조건 검사를 실증한다**

`precheck`는 clean 트리를 전제로 하므로 **커밋 뒤에** 확인한다.

함수를 파일로 뽑아 `source` 한다. heredoc 으로 넘기면 delimiter 를 인용하지 않는 한
`$1`·`$tree`·`$missing` 이 소싱 전에 바깥 셸에서 먼저 빈 문자열로 치환돼 `precheck` 가 항상
거짓 실패하고 `return: : numeric argument required` 가 난다.

Run:
```bash
TMPTREE=$(mktemp -d)/tree
git worktree add --detach "$TMPTREE" HEAD >/dev/null
cp local.properties "$TMPTREE/local.properties" 2>/dev/null || true
sed -n '/^precheck()/,/^}/p' tools/build-cache-bench/run.sh > /tmp/precheck-fn.sh
TARGETS=":app:assembleDebug" bash -c '
  source /tmp/precheck-fn.sh
  precheck "$1" && echo "precheck: 통과" || echo "precheck: 실패 (위 사유)"
' _ "$TMPTREE"
rm -f /tmp/precheck-fn.sh
git worktree remove --force "$TMPTREE"; git worktree prune
```

Expected: `google-services.json`을 복사하지 않았으므로 `없음: .../app/google-services.json`이
찍히고 `precheck: 실패`가 나온다. 검사가 실제로 동작한다는 뜻이다. 파일까지 복사하면
`precheck: 통과`가 나온다.

---
### Task 5: 시나리오 S0·S1·S2 실행

**Files:**
- Modify: `tools/build-cache-bench/run.sh`

**Interfaces:**
- Consumes: Task 4의 `precheck`·`plan_runs`, Task 1·2의 init script.
- Produces: 셸 함수 `now_ms`·`wipe_cache`·`gradle_run`·`prepare_state`·`cache_stats`·`measure`와
  산출물 `builds.csv`(헤더 `scenario,target,pair,iteration,wall_ms,daemon_pid`)·
  `cache-size.csv`(헤더 `scenario,target,iteration,entries,kb`)·`tasks/<태그>.csv`·`logs/<태그>.log`.
  Task 6이 같은 `prepare_state`에 `S3`·`S4`를 더한다.

**핵심 제약 둘.**

1. **사전 상태는 반복 매 회차마다 다시 세운다.** 시나리오 단위로 한 번만 세우면 `S2`의 2회차는
   캐시가 이미 차 있어 사실상 `S1`이 되고 중앙값이 통째로 무의미해진다.
2. **캐시를 채우는 빌드 앞에는 반드시 `clean`이 온다.** Gradle은 태스크가 **실제로 실행될 때만**
   캐시 항목을 저장한다. 빌드 출력이 남아 있으면 그 빌드가 `UP_TO_DATE`로 끝나 **빈 캐시에
   아무것도 담기지 않는다.** 초안이 이걸 빠뜨려 `S1`·`S3`·`S4`가 전부 `S2`와 같은 상태로
   측정됐고 핵심 값의 부호가 뒤집혔다.

- [ ] **Step 1: 시간·캐시·실행 헬퍼를 추가한다**

`plan_runs` 아래, `--dry-run` 분기 위에 넣는다.

```bash
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
```

- [ ] **Step 2: 사전 상태 구성 함수를 추가한다**

```bash
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
        *)
            echo "알 수 없는 시나리오: $scenario" >&2
            return 4
            ;;
    esac
}
```

`S0`은 무변경 재실행이라 사전 빌드를 그대로 두고 측정한다. `S1`은 캐시를 채운 뒤 출력만
지운다. `S2`는 캐시도 출력도 없는 상태다.

- [ ] **Step 3: 측정 함수를 추가한다**

```bash
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
```

- [ ] **Step 4: 실행 본문으로 dry-run 아래를 교체한다**

```bash
mkdir -p "$OUT" "$CACHE_DIR" "$OUT/logs"
TREE="${TREE:-$ROOT}"
precheck "$TREE" || { echo "선행 조건 미충족" >&2; exit 5; }

echo "scenario,target,pair,iteration,wall_ms,daemon_pid" > "$OUT/builds.csv"
echo "scenario,target,iteration,entries,kb" > "$OUT/cache-size.csv"

while IFS='|' read -r scenario target iteration; do
    echo "[$scenario] $target ($iteration/$ITERATIONS)"
    measure "$scenario" "$target" "$iteration" "$TREE"
done < <(plan_runs)

rm -f "$OUT/discard.csv" "$OUT/discard.csv.daemon"
echo "결과: $OUT"
```

- [ ] **Step 5: 가장 가벼운 구성으로 돌려 본다**

Run:
```bash
./tools/build-cache-bench/run.sh \
  --scenarios S1,S2 --targets :core:util:jvm:compileKotlin --iterations 1
RUN=$(ls -d tools/build-cache-bench/runs/* | tail -1)
cat "$RUN/builds.csv"
grep ':core:util:jvm:compileKotlin' "$RUN"/tasks/*.csv
```

Expected: `builds.csv`에 2행이고 `daemon_pid`가 숫자다. **`S1`의 `compileKotlin`이 `FROM_CACHE`,
`S2`가 `EXECUTED`다.** `S1`이 `EXECUTED`로 나오면 사전 빌드가 캐시를 채우지 못한 것이므로
`prepare_state`의 `clean` 순서를 고친다.

- [ ] **Step 6: 반복 오염이 없는지 확인한다**

Run:
```bash
./tools/build-cache-bench/run.sh \
  --scenarios S2 --targets :core:util:jvm:compileKotlin --iterations 3
RUN=$(ls -d tools/build-cache-bench/runs/* | tail -1)
for f in "$RUN"/tasks/S2-*.csv; do
  echo "$f from_cache=$(awk -F, 'NR>1 && $2=="FROM_CACHE"' "$f" | wc -l)"
done
```

Expected: 세 회차 모두 `from_cache`가 같다(0에 가깝다). 회차가 늘수록 커지면 `wipe_cache`가 매
회차 돌지 않는 것이다.

- [ ] **Step 7: 커밋한다**

```bash
git add tools/build-cache-bench/run.sh
git commit -m "feat: S0·S1·S2 시나리오 측정을 러너에 구현한다"
```

---

### Task 6: 시나리오 S3·S4 — 커밋 쌍과 격리된 트리

**Files:**
- Modify: `tools/build-cache-bench/run.sh`

**Interfaces:**
- Consumes: Task 5의 `prepare_state`·`measure`·`wipe_cache`·`gradle_run`, Task 4의 `validate_pair`.
- Produces: `prepare_state`에 `S3`·`S4` 분기, 전용 트리 확보 함수 `ensure_tree`, 종료 시 복구
  `trap`.

**무엇을 만드는가:**
- `S3` = `T_local`. 전용 캐시에 커밋 `A`의 항목만 있는 상태로 `B`를 빌드한다.
- `S4` = `T_remote`. 캐시에 `A`와 `B`의 항목이 모두 있는 상태로 `B`를 빌드한다.
- **핵심 값은 `S3 − S4`다.**

**두 시나리오의 출력 상태를 맞춘다.** 측정 직전 빌드 출력은 양쪽 모두 **커밋 `A` 기준**이다.
실제 개발자가 어제 `A`까지 빌드해 두고 오늘 `B`를 당겨받은 상태가 그것이고, 한쪽만 `clean`하면
캐시가 아니라 출력 유무가 차이를 만들어 값이 부풀려진다. 그래서 `S4`는 `B`를 먼저 구워 캐시에
담은 뒤 `A`를 다시 빌드해 출력을 `A`로 되돌린다.

**워킹 트리 안전:** `--tree`가 없으면 러너가 `git worktree`로 전용 트리를 만든다. `--tree`로
받았으면 시작 HEAD를 기억했다가 종료 시 되돌린다. `git stash`와 `checkout -f`는 쓰지 않는다.

- [ ] **Step 1: 전용 트리 확보와 복구를 추가한다**

```bash
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
```

- [ ] **Step 2: `prepare_state`에 `S3`·`S4` 분기를 더한다**

`case` 문의 `*)` 앞에 넣는다.

```bash
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
```

- [ ] **Step 3: 실행 본문을 고친다**

`TREE="${TREE:-$ROOT}"`를 두 줄로 바꾼다.

```bash
validate_pair || exit 2
ensure_tree
```

- [ ] **Step 4: 커밋 쌍으로 돌려 본다**

Run:
```bash
PAIR_A=$(git rev-parse HEAD~1)
PAIR_B=$(git rev-parse HEAD)
./tools/build-cache-bench/run.sh \
  --scenarios S3,S4 --pair "$PAIR_A:$PAIR_B" \
  --targets :core:util:jvm:compileKotlin --iterations 1
RUN=$(ls -d tools/build-cache-bench/runs/* | tail -1)
cat "$RUN/builds.csv"
grep ':core:util:jvm:compileKotlin' "$RUN"/tasks/S3-*.csv "$RUN"/tasks/S4-*.csv
```

Expected: `S4`의 `compileKotlin`이 `FROM_CACHE`이고 `S3`는 그렇지 않다. `S4`의 `wall_ms`가
`S3`보다 작다. **부호가 반대로 나오면 `prepare_state`의 `clean` 순서가 틀린 것이다** — 캐시를
채우는 빌드가 `UP_TO_DATE`로 끝나면 캐시에 아무것도 담기지 않는다.

- [ ] **Step 5: 워킹 트리가 온전한지 확인한다**

Run:
```bash
git status --porcelain
git rev-parse --abbrev-ref HEAD
git worktree list
```

Expected: 변경 없음, 브랜치는 `build/build-cache-measurement`, worktree 목록에 측정용 트리가 남아
있지 않다.

- [ ] **Step 6: 중단해도 안전한지 확인한다**

Run: 위 Step 4의 측정을 다시 시작하고 몇 초 뒤 `Ctrl-C`로 끊은 다음 Step 5의 세 명령을 실행한다.

Expected: 같은 결과다. worktree가 남아 있으면 `ensure_tree`가 서브셸에서 불렸거나 `trap`이
걸리지 않은 것이다.

- [ ] **Step 7: 빌려 쓴 트리의 HEAD 복구를 확인한다**

Run:
```bash
BORROW=$(mktemp -d)/tree
git worktree add "$BORROW" -b bench-borrow-check >/dev/null
cp local.properties "$BORROW/local.properties" 2>/dev/null || true
./tools/build-cache-bench/run.sh --tree "$BORROW" \
  --scenarios S3 --pair "$(git rev-parse HEAD~1):$(git rev-parse HEAD)" \
  --targets :core:util:jvm:compileKotlin --iterations 1
git -C "$BORROW" rev-parse --abbrev-ref HEAD
git worktree remove --force "$BORROW"; git branch -D bench-borrow-check; git worktree prune
```

Expected: 측정이 끝난 뒤 `bench-borrow-check`가 찍힌다. `HEAD`가 찍히면 detached로 방치된 것이다.

- [ ] **Step 8: 커밋한다**

```bash
git add tools/build-cache-bench/run.sh
git commit -m "feat: 커밋 쌍 기반 S3·S4 시나리오와 격리 트리를 러너에 구현한다"
```

---
### Task 7: 요약 리포트와 README

**Files:**
- Modify: `tools/build-cache-bench/run.sh`
- Create: `tools/build-cache-bench/README.md`

**Interfaces:**
- Consumes: Task 5·6이 만든 `builds.csv`·`cache-size.csv`·`tasks/*.csv`.
- Produces: `summary.md`. 핵심 값, 참고값, 적중률(actionable 기준, 전 회차 집계), `S3` 미스
  태스크와 사유를 담는다.

**집계 규칙 둘.**

- **적중률의 분모는 actionable 태스크다.** included build(`:build-logic:`)와 `SKIPPED`·`NO-SOURCE`를
  뺀다. 그래야 Gradle이 찍는 `N actionable tasks` 줄과 같은 기준이 된다.
- **모든 회차를 읽는다.** 1회차만 읽으면 변동이 가장 큰 회차만 리포트된다.

- [ ] **Step 1: 중앙값 함수를 추가한다**

```bash
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
    cat "$OUT/tasks/$slug"-*.csv 2>/dev/null | awk -F, -v s="$scenario" -v t="$target" '
        $1=="task_path" { next }
        $1 ~ /^:build-logic:/ { next }
        $2=="SKIPPED" || $2=="NO-SOURCE" { next }
        { n++; if ($2=="FROM_CACHE") hit++ }
        END { if (n) printf "| %s | %s | %d | %d | %.1f%% |\n", s, t, hit, n, 100*hit/n }
    '
}
```

- [ ] **Step 2: 요약 생성 함수를 추가한다**

```bash
# 핵심 값을 맨 앞에 둔다. 나머지는 그 값을 읽기 위한 참고값이다.
write_summary() {
    local md="$OUT/summary.md" target scenario s3 s4 m
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
        echo "분모는 actionable 태스크다. included build 와 SKIPPED·NO-SOURCE 를 뺀 전 회차 집계다."
        echo
        echo "| scenario | target | from_cache | actionable | 적중률 |"
        echo "|---|---|---|---|---|"
        for target in ${TARGETS//,/ }; do
            for scenario in $SCENARIO_ORDER; do
                hit_rate "$scenario" "$target"
            done
        done
        echo
        echo "## S3 에서 캐시 미스로 남은 태스크"
        echo
        echo "사유는 init script 가 getExecutionReasons() 로 받은 값이다."
        echo
        echo '```'
        cat "$OUT/tasks"/S3-*.csv 2>/dev/null \
            | awk -F, '$1!="task_path" && $2=="EXECUTED" {sum[$1]+=$3; why[$1]=$4} END {for (t in sum) printf "%8d ms  %-55s %s\n", sum[t], t, why[t]}' \
            | sort -rn | head -30
        echo '```'
    } > "$md"
    echo "요약: $md"
}
```

실행 본문 끝의 `echo "결과: $OUT"` 앞에서 `write_summary`를 부른다.

- [ ] **Step 3: 요약이 나오는지 확인한다**

Run:
```bash
PAIR_A=$(git rev-parse HEAD~1); PAIR_B=$(git rev-parse HEAD)
./tools/build-cache-bench/run.sh \
  --scenarios S1,S2,S3,S4 --pair "$PAIR_A:$PAIR_B" \
  --targets :core:util:jvm:compileKotlin --iterations 1
cat "$(ls -d tools/build-cache-bench/runs/* | tail -1)/summary.md"
```

Expected: 핵심 값 표의 차이가 **양수**다. 적중률 표에서 `S2`가 0%에 가깝고 `S1`이 훨씬 높으며
`S4`가 `S3`보다 높다. 미스 태스크 목록은 시간 내림차순이고 각 줄 끝에 실행 사유가 붙는다.

- [ ] **Step 4: 교차 검증을 한다**

Run:
```bash
RUN=$(ls -d tools/build-cache-bench/runs/* | tail -1)
awk -F, '$1!="task_path" && $1 !~ /^:build-logic:/ && $2=="EXECUTED"' "$RUN"/tasks/S2-*.csv | wc -l
grep -c 'actionable tasks' "$RUN"/logs/S2-*.log || true
tail -3 "$RUN"/logs/S2-*.log
```

Expected: 위 `EXECUTED` 건수가 로그 마지막의 `N actionable tasks: X executed, ...`의 X와 맞는다.
**대조 전에 `:build-logic:` 접두 태스크를 걸러야 한다** — 그 줄은 actionable 태스크만 세는데
리스너는 included build 태스크와 lifecycle 태스크까지 받는다.

- [ ] **Step 5: 개발자 캐시가 그대로인지 확인한다**

Run:
```bash
find ~/.gradle/caches/build-cache-1 -type f | wc -l
```

Expected: 측정 전에 센 값과 같다. 전체 측정을 한 바퀴 돌린 뒤에 확인하는 것이 요점이다.

- [ ] **Step 6: README를 작성한다**

`tools/build-cache-bench/README.md`에 아래를 담는다.

- 이 하니스가 답하려는 질문 한 줄과 스펙 문서 링크.
- **선행 조건**: `local.properties`(`sdk.dir`·`kakao.native.app.key`·debug 키스토어 3종),
  `app/google-services.json`. 전부 `.gitignore` 대상이라 클론마다 각자 채운다는 사실.
- 시나리오 5종 표(스펙과 같은 표)와 각 시나리오의 사전 상태.
- **경고 셋**: `clean`이 대상 트리의 빌드 출력을 지운다는 것, `S3`·`S4`가 전용 worktree를
  만들어 돌기 때문에 측정 중 그 디렉토리를 건드리면 안 된다는 것, `--tree`로 트리를 빌려주면
  그 트리의 HEAD가 측정 중 커밋 사이를 오간다는 것.
- 실행 예시 둘(가벼운 확인용, 전체 측정용).
- **결과 읽는 법**: 핵심 값은 `S3 − S4`이고 `S2 − S1`이 아니라는 것과 그 이유.
- 측정 조건: `--offline` 고정, 회차마다 Gradle 데몬 재기동 후 `help` 1회 워밍업, IDE를 닫고
  돌릴 것, 반복이 짝수면 중앙값이 아래쪽 값이라는 것.
- **한계 다섯**:
  - OS 이식성 미확인(CI는 `ubuntu-latest`, 개발자는 macOS).
  - 전송 시간 미반영이라 `S3 − S4`는 낙관적 상한이다.
  - **Kotlin 데몬은 `./gradlew --stop`으로 죽지 않는다.** Gradle 데몬만 재기동되므로 Kotlin
    컴파일 쪽 웜업 곡선은 통제되지 않는다. `builds.csv`의 `daemon_pid`로 Gradle 데몬 교체만
    사후 확인할 수 있다.
  - `clean`이 지우지 않는 상태가 남는다 — `<tree>/.gradle`의 파일 해시·실행 이력,
    `~/.gradle/caches/<ver>/transforms-*`. `S2`는 "캐시 축출 직후"이지 "새 머신"이 아니다.
  - 머신마다 수치가 다르므로 서로 다른 머신의 값을 직접 비교하지 않는다.

- [ ] **Step 7: 커밋한다**

```bash
git add tools/build-cache-bench/run.sh tools/build-cache-bench/README.md
git commit -m "feat: 측정 요약 리포트 생성과 사용 문서를 추가한다"
```

---

## 실행 후

측정을 실제로 돌리기 전에 **도입 문턱값을 먼저 정한다.** `S3 − S4`가 얼마 이상이면 리모트
캐시를 세울 값어치가 있다고 볼 것인지를 측정 후에 정하면 결과에 맞춰 기준이 움직인다.
스펙의 열린 질문 둘(커밋 쌍 `P1`·`P2` 선정 기준, 문턱값)을 닫고 나서 측정에 들어간다.

---

## As-built 정정 (2026-09-14 실행 후)

실행 중 이 계획의 코드·검증 명령에서 결함이 드러났다. 최종 산출물은 아래대로이며,
같은 것을 다시 쓸 사람이 계획 본문을 그대로 옮기면 같은 자리에서 넘어진다.

### 계획이 틀렸던 곳

| 위치 | 무엇이 틀렸나 | 최종 형태 |
|---|---|---|
| Task 4 Step 6 | `source /dev/stdin <<SH` 의 delimiter 를 인용하지 않아 `$1`·`$tree`·`$missing` 이 소싱 전에 바깥 셸에서 치환됐다. `precheck` 가 항상 거짓 실패하고 `return: : numeric argument required` 가 났다 | 함수를 파일로 뽑아 `source` 한다(본문 반영 완료) |
| Task 5 `now_ms` | "BSD `date` 는 `%3N` 에 실패하므로 폴백이 걸린다"가 틀렸다. **실패하지 않고 `%3` 를 리터럴로 뱉으며 exit 0** 이라 폴백이 영영 안 걸리고 `$((end - start))` 가 터진다 | `date` 경로를 없애고 `python3` 로 직행(본문 반영 완료) |
| Task 7 `hit_rate`·S3 집계 | `cat … \| awk` 가 `set -euo pipefail` 아래서, 실행하지 않은 시나리오의 tasks 파일이 없을 때 `cat` 비영 종료로 `write_summary` 전체를 죽였다 | 파일 존재를 먼저 보고 없으면 건너뛴다. `\|\| true` 로 파이프라인 전체를 삼키면 진짜 집계 오류까지 묻힌다 |
| Task 7 S3 집계 | `\| sort -rn \| head -30` 이 `pipefail` 아래서 `head` 의 조기 종료로 `sort` 를 SIGPIPE(141)로 죽인다. 사유 문자열이 길어 수백 줄에서도 걸린다 | `awk 'NR<=30'` — 입력을 끝까지 읽어 앞 단계에 SIGPIPE 를 보내지 않는다 |
| Task 4 `validate_pair` | 커밋 존재만 확인하고 **해석 결과를 버렸다.** `--pair HEAD~1:HEAD` 를 주면 러너가 트리 HEAD 를 옮기는 동안 다시 해석되어 측정 빌드가 `B` 가 아니라 `A` 에서 돈다 | `rev-parse` 로 full SHA 를 고정하고 이후 전부 그 변수를 쓴다. A==B 와 트리 동일 쌍도 거부한다 |
| Task 5 `gradle_run` | `--build-cache` 를 안 붙여 캐시 활성화가 체크아웃된 커밋의 `gradle.properties` 에 달려 있었다 | `--offline --build-cache` 를 상수로 붙인다 |
| Task 4 `precheck` | 스펙이 요구한 대상 트리 clean 검사가 없었다 | `git status --porcelain` 검사를 넣는다 |
| Task 5 `measure` | `--stop` 이 한 곳뿐이라, 사전 빌드 횟수가 다른 시나리오(`S4` 2회·`S3` 1회)의 데몬 온도 차이가 측정에 그대로 남았다 | `--stop` 을 사전 상태 수립 **앞뒤로 한 번씩** 둔다 |

### 계획이 비어 있던 곳

- 적중률 표에 `up_to_date`·`executed` 컬럼이 없어 "무신호"와 "캐시 미스인데 빨랐다"가 구분되지 않았다.
- `cache-size.csv` 를 측정 빌드 **뒤에만** 재서, 캐시를 비우는 `S2` 가 `entries=1` 로 보였다. 사전·사후 둘 다 남긴다.
- 이식성 게이트가 어떤 조건에서도 비0 으로 끝나지 않아 사람이 눈으로 읽어야만 게이트였다. 게다가
  included build 태스크의 적중만으로 통과할 수 있었다 — 판정을 `$TARGET` 자신의 `FROM_CACHE` 로 좁혔다.
- `prepare.log` 가 매 사전 빌드마다 덮어써져 `S4` 의 "B 를 캐시에 굽는" 빌드 로그가 사라졌다. 단계별로 나눈다.
- `precheck` 가 스펙 선행 조건표의 절반(키스토어 3종·`kakao.native.app.key`)을 안 봤다.

### 실행 기록

커밋 `ef3b083`~`dbe395a` 18개. 태스크 리뷰 7회, 수정 라운드 3회, 최종 whole-branch 리뷰 1회와
그 수정 wave 2회. 최종 실측: `S3 − S4 = +3209ms`(`:core:util:jvm:compileKotlin`,
쌍 `6ac5011:0d48491`), 이식성 게이트 PASS.
