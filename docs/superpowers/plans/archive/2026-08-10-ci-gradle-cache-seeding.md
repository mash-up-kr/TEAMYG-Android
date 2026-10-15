# CI Gradle 캐시 시딩 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** PR CI에서 한 번도 적중한 적 없는 Gradle 캐시를 적중시킨다 — `develop` push에 캐시 생산자 job을 세우고, 빌드 캐시를 켜서 캐시에 담길 내용물을 만든다.

**Architecture:** 생산자/소비자 분리다. 새 워크플로 `gradle-cache-seed.yml`이 `develop` push에서 PR 워크플로 둘의 태스크 합집합을 한 번의 Gradle 호출로 돌려 캐시를 채우고(생산자), 기존 `test.yml`·`ktlint.yml`은 한 줄도 고치지 않고 그 캐시를 읽는다(소비자). 이것만으로는 부족한데, `org.gradle.caching`이 꺼져 있어 태스크 출력 저장소가 비기 때문이다 — `gradle.properties`에서 함께 켠다.

**Tech Stack:** GitHub Actions, `gradle/actions/setup-gradle@v4`, Gradle 9.4.1(wrapper), AGP 9.2.1, Kotlin 2.4.0, JDK 17(temurin)

> **실행 결과 (2026-08-10, 전 Task 완료 · PR #227 develop 머지)** — 브랜치 `chore/build-action-cache`,
> 커밋 `1db48217`~`027631a3` + push 이후 커밋. 아래 Task 1의 `gradle.properties` 내용은 **as-built가 아니다.**
> 최종 리뷰 이후 사용자 판단으로 `org.gradle.parallel`·힙 상향·`kotlin.daemon.jvmargs`를 전부
> 되돌렸고, 실제로 남은 변경은 `org.gradle.caching=true` 한 줄이다. 되돌린 근거와 재도입 시
> 필요한 사실은 스펙의 "검토했다가 뺀 것" 절에 있다.
> 진행 원장: `.superpowers/sdd/2026-08-10-ci-gradle-cache-seeding/progress.md`
>
> **Task 2 as-built** — 아래 Step 2의 워크플로 본문에 셋이 더 붙었다: `workflow_dispatch` 트리거,
> `concurrency` 그룹(`cancel-in-progress: true`, 팀 리뷰 P3 반영), `timeout-minutes: 45`,
> `continue-on-error: true`(`--continue`가 종료 코드를 성공으로 바꾸지 않으므로 seed 실패가
> `develop`을 빨갛게 만드는 것을 막는다). 확정본은
> [스펙 §변경 대상](../../specs/archive/2026-08-10-ci-gradle-cache-seeding.md)에 있다.
>
> **Task 3 완료(2026-08-10 머지 후)** — Step 3·4의 관측값은
> [스펙 §머지 후 관측](../../specs/archive/2026-08-10-ci-gradle-cache-seeding.md)에 표로 남겼다.
> 요지: seed 런 `cache-read-only: false`로 캐시 저장 확인, 후속 PR `unit-test`에서
> `Gradle User Home cache not found`가 사라지고 `608 actionable tasks: 240 executed, 368 from cache`,
> 소요 6m16s → 2m45s. **계획의 성공 판정 조건을 둘 다 만족했다.**

## Global Constraints

- **작업 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 wiki 저장소가 아니다. 로컬 경로는 `wiki/personal-private/project-paths.md`에 있고, 리모트는 `mash-up-kr/TJYG-Android`(현재 `mash-up-kr/TEAMYG-Android`로 리다이렉트).
- **브랜치는 기존 `chore/build-action-cache`를 쓴다.** 이미 존재하고 `develop`과 내용이 동일하다(`git diff develop...HEAD`가 빈 출력). 새 브랜치를 만들지 않는다.
- **TJYG-Android 커밋은 기본적으로 하지 않는다.** 각 Task의 커밋 스텝은 **사용자 승인을 받은 뒤에만** 실행한다. 승인 없이 자동 커밋하지 않는다.
- **`git push`와 `gh pr create`는 사용자 확인 필수.** 승인 전까지 실행하지 않는다.
- **`clean`과 빌드 태스크를 한 번의 Gradle 호출에 섞지 않는다.** `org.gradle.parallel=true`가 켜진 뒤에는 `clean`이 다른 태스크와 경쟁할 수 있다. 항상 `./gradlew clean` 따로, 빌드 태스크 따로 호출한다.
- **`configuration cache`는 이번 범위에서 제외다.** `org.gradle.configuration-cache`를 켜지 않는다. 이유는 스펙 "범위" 절에 있다(CI 이득 0 + 시크릿 필요).
- **파일 3개만 건드린다.** `gradle.properties`(수정), `.github/workflows/gradle-cache-seed.yml`(신규). 그 외 워크플로·합성 액션·빌드 스크립트는 수정 대상이 아니다.
- 스펙: [`parfait/specs/archive/2026-08-10-ci-gradle-cache-seeding.md`](../../specs/archive/2026-08-10-ci-gradle-cache-seeding.md)

---

## File Structure

| 파일 | 상태 | 책임 |
|---|---|---|
| `gradle.properties` | 수정 | 빌드 캐시·병렬 실행 활성화, 데몬 힙. 캐시에 **담길 내용물**을 만든다. |
| `.github/workflows/gradle-cache-seed.yml` | 신규 | `develop` push에서 캐시를 채우는 생산자 job. **캐시를 저장하는 유일한 주체.** |
| `.github/workflows/test.yml` | 무변경 | 소비자. PR에서 캐시를 읽기만 한다. |
| `.github/workflows/ktlint.yml` | 무변경 | 소비자. |
| `.github/actions/setup-android-build/action.yml` | 무변경 | `develop`이 기본 브랜치라 쓰기 모드 전환에 추가 입력이 필요 없다. |

Task 1(`gradle.properties`)이 Task 2(워크플로)보다 먼저다. 순서가 뒤바뀌면 시딩 job이 돌아도 태스크 출력이 캐시에 안 들어가 아무것도 데워지지 않는다.

---

### Task 1: 빌드 캐시·병렬 실행 활성화

**Files:**
- Modify: `gradle.properties`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `~/.gradle/caches/build-cache-1`에 태스크 출력이 쌓이는 상태. Task 2의 시딩 워크플로가 이 전제 위에서만 의미를 갖는다.

**주의:** 이 Task는 전체 유닛 테스트를 세 번 돌린다. 한 번에 수 분씩 걸리므로 시간을 잡고 시작한다. 중간에 끊으면 캐시 상태가 애매해져 Step 6 판정이 무의미해진다.

- [x] **Step 1: 작업 저장소·브랜치 확인**

TJYG-Android 저장소 루트에서:

```bash
git branch --show-current
git diff develop...HEAD --stat
git status --short
```

Expected:
- 첫 명령이 `chore/build-action-cache` 출력. 다른 브랜치면 `git checkout chore/build-action-cache`.
- 둘째 명령이 **빈 출력**(develop과 동일).
- 셋째 명령이 빈 출력(작업 트리 클린). 클린하지 않으면 중단하고 사용자에게 보고한다.

- [x] **Step 2: 실패하는 테스트 — 현재 캐시 미적중을 관측**

```bash
./gradlew --stop
./gradlew clean
./gradlew test 2>&1 | tail -5
```

Expected: 마지막 줄들이 아래 형태다.

```
BUILD SUCCESSFUL in Xm Ys
NNN actionable tasks: NNN executed
```

**핵심은 `executed` 개수가 총 개수와 같고 `from cache`가 한 건도 없다는 것.** 이것이 RED다.
`from cache`가 이미 보이면 `org.gradle.caching`이 어딘가(`~/.gradle/gradle.properties` 등)에 이미 켜져 있다는 뜻이므로, 고치지 말고 사용자에게 보고한다.

`NNN` 값을 기록해 둔다. Step 6에서 같은 총량과 비교한다.

- [x] **Step 3: `gradle.properties` 수정**

파일 전체를 아래로 만든다(주석 포함).

```properties
# 병렬 워커와 Kotlin 데몬이 함께 뜨면 기본 힙에서 GC 압박을 받는다.
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8

# 태스크 출력 재사용. CI에서는 setup-gradle 이 Gradle User Home 아래 build-cache-1 을
# 함께 캐시하므로, 이 설정이 켜져 있어야 캐시 시딩(gradle-cache-seed.yml)이 의미를 갖는다.
org.gradle.caching=true

# 독립 모듈 병렬 컴파일.
org.gradle.parallel=true

kotlin.code.style=official
```

- [x] **Step 4: 데몬 재기동 후 콜드 빌드 — 변경이 빌드를 깨지 않는지**

```bash
./gradlew --stop
./gradlew clean
./gradlew test 2>&1 | tail -5
```

`--stop`이 필요한 이유: 기존 데몬은 옛 `org.gradle.jvmargs`로 떠 있어 새 힙 설정이 적용되지 않는다.

Expected: `BUILD SUCCESSFUL`.

**FAIL 시 판정 기준** — 여기서 깨지면 원인은 십중팔구 `org.gradle.parallel=true`이고, 모듈 간에 선언되지 않은 암묵적 의존이 있다는 뜻이다. 임의로 우회하지 말고 실패한 태스크명과 에러 메시지를 사용자에게 보고한다. 병렬을 빼면 캐시 효과 자체는 유지되므로 축소 적용이 가능한 선택지다.

- [x] **Step 5: 빌드 산출물만 지우고 캐시는 남긴다**

```bash
./gradlew clean
```

`clean`은 프로젝트의 `build/` 디렉토리를 지우지만 `~/.gradle/caches/build-cache-1`은 건드리지 않는다. 이 비대칭이 다음 스텝의 관측을 가능하게 한다.

- [x] **Step 6: 통과하는 테스트 — 캐시 적중 관측**

```bash
./gradlew test 2>&1 | tail -5
```

Expected: 요약 줄에 **`from cache`가 나타난다.**

```
BUILD SUCCESSFUL in Xm Ys
NNN actionable tasks: A executed, B from cache
```

`B`가 0보다 커야 GREEN이다. Step 2와 총량 `NNN`은 같고 내역만 갈린다.

`from cache`가 여전히 0이면 `org.gradle.caching`이 먹지 않은 것이다. Task 2로 넘어가지 말고 중단한다 — 시딩 워크플로를 붙여도 아무 효과가 없기 때문이다.

- [x] **Step 7: 커밋 (사용자 승인 후)**

```bash
git add gradle.properties
git commit -m "chore: enable Gradle build cache and parallel execution

CI 캐시 시딩(gradle-cache-seed.yml)의 전제. org.gradle.caching 없이는
Gradle User Home 에 의존성만 쌓이고 태스크 출력 저장소가 비어 있어
시딩을 붙여도 태스크가 매번 재실행된다.

병렬 워커와 Kotlin 데몬 동시 기동에 맞춰 데몬 힙을 함께 올린다."
```

---

### Task 2: 캐시 시딩 워크플로 신설

**Files:**
- Create: `.github/workflows/gradle-cache-seed.yml`

**Interfaces:**
- Consumes: Task 1이 켠 `org.gradle.caching`. 없으면 이 워크플로가 데우는 것은 의존성뿐이다.
- Produces: `develop` push마다 갱신되는 Gradle User Home 캐시 항목. `test.yml`·`ktlint.yml`이 PR에서 이것을 읽는다.

- [x] **Step 1: 실패하는 테스트 — 캐시 저장 job이 없음을 확인**

```bash
grep -l "push:" .github/workflows/*.yml
```

Expected: **빈 출력**(매칭 파일 0건). 워크플로 4종 어디에도 `push` 트리거가 없다는 것이 이 Task가 존재하는 이유다.

빈 출력이 아니면 이미 누가 `push` 트리거를 추가했다는 뜻이므로, 파일을 새로 만들지 말고 사용자에게 보고한다.

- [x] **Step 2: 워크플로 파일 생성**

`.github/workflows/gradle-cache-seed.yml`:

```yaml
name: gradle-cache-seed

# setup-gradle 은 기본 브랜치(develop)의 job 에서만 캐시를 저장한다.
# PR 워크플로는 전부 cache-read-only 라, 저장하는 주체가 이 job 하나다.
on:
  push:
    branches: [ develop ]

permissions:
  contents: read

jobs:
  seed:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: ./.github/actions/setup-android-build

      - name: Restore app secrets
        uses: ./.github/actions/restore-app-secrets
        with:
          kakao-native-app-key: ${{ secrets.KAKAO_NATIVE_APP_KEY }}
          google-services-json: ${{ secrets.GOOGLE_SERVICES_JSON }}

      # PR 워크플로(test·ktlint)가 쓰는 태스크 전부를 한 번의 Gradle 호출로 돌린다.
      # 워크플로마다 push 트리거를 다는 대신 하나로 묶는 이유: job 이 둘이면 같은
      # 캐시 키에 동시에 써서 한쪽 저장이 버려지고, 데몬 기동·의존성 해석도 두 번 친다.
      # --continue: 앞 태스크가 실패해도 나머지 태스크 출력은 캐시에 남긴다.
      #             이 job 의 목적은 게이트가 아니라 캐시 적재다.
      - name: Warm Gradle caches
        run: |
          ./gradlew test ktlintCheck \
            :core:util:android:assembleDebugAndroidTest \
            :core:designsystem:assembleDebugAndroidTest \
            --continue
```

- [x] **Step 3: YAML 파싱 검증**

이 환경에는 `actionlint`도 PyYAML도 없다. macOS 기본 ruby의 psych를 쓴다.

```bash
ruby -ryaml -e 'p YAML.load_file(".github/workflows/gradle-cache-seed.yml").keys'
```

Expected:

```
["name", true, "permissions", "jobs"]
```

`true`가 나오는 것이 정상이다 — YAML 1.1이 `on:` 키를 불리언으로 읽는 잘 알려진 동작이고, GitHub Actions 파서는 영향받지 않는다. 기존 `test.yml`도 같은 결과를 낸다.

파싱 에러가 나면 들여쓰기를 고친다.

- [x] **Step 4: 태스크 경로가 실재하는지 검증**

```bash
./gradlew --dry-run test ktlintCheck \
  :core:util:android:assembleDebugAndroidTest \
  :core:designsystem:assembleDebugAndroidTest
```

`--dry-run`은 태스크 그래프만 계산하고 실행하지 않는다. 오타난 태스크 경로가 여기서 잡힌다.

Expected: `BUILD SUCCESSFUL`. `Task 'xxx' not found` 류 에러가 나면 경로 오타다.

- [x] **Step 5: PR 워크플로 태스크 합집합을 실제로 덮는지 대조**

```bash
grep -h "gradlew" .github/workflows/test.yml .github/workflows/ktlint.yml
```

Expected 출력에 담긴 Gradle 태스크는 아래 셋이다.

- `test`
- `:core:util:android:assembleDebugAndroidTest :core:designsystem:assembleDebugAndroidTest`
- `ktlintCheck`

시딩 워크플로의 `Warm Gradle caches` 스텝이 이 셋을 모두 포함하는지 눈으로 대조한다. 빠진 것이 있으면 그 워크플로는 PR에서 계속 콜드로 돈다.

- [x] **Step 6: 커밋 (사용자 승인 후)**

```bash
git add .github/workflows/gradle-cache-seed.yml
git commit -m "ci: add Gradle cache seeding workflow on develop push

setup-gradle 은 기본 브랜치 job 에서만 캐시를 저장하는데 기존 워크플로 4종이
전부 pull_request 트리거라 저장 주체가 없었다. 그 결과 PR 런마다
'Gradle User Home cache not found' 로 콜드 빌드가 돌았다.

test·ktlint 두 워크플로의 태스크 합집합을 한 번의 Gradle 호출로 돌려
캐시를 데운다."
```

---

### Task 3: 제출과 머지 후 적중 검증

**Files:**
- 코드 변경 없음. 검증과 문서 갱신만.

**Interfaces:**
- Consumes: Task 1·2의 커밋 2개.
- Produces: 실제 CI 런에서의 캐시 적중 증거. 이 계획의 성공/실패 판정.

**주의:** Step 3·4는 `develop` 머지 이후에만 수행 가능하다. 이 Task는 머지를 기다리며 중단되는 것이 정상이다.

- [x] **Step 1: push (사용자 확인 필수)**

사용자에게 먼저 물어보고 승인받는다. 승인 전 실행 금지.

```bash
git push -u origin chore/build-action-cache
```

- [x] **Step 2: PR 생성 (사용자 확인 필수)**

사용자에게 먼저 물어보고 승인받는다. base는 `develop`이다.

```bash
gh pr create --base develop --title "ci: Gradle 캐시 시딩으로 PR CI 콜드 빌드 제거" --body "$(cat <<'EOF'
## 문제

PR CI의 Gradle 캐시가 한 번도 적중한 적이 없다. 두 런 로그(#25 `31323149207`, #26 `31323027198`)가 동일하게 찍은 것:

- `cache-read-only: true`
- `Gradle User Home cache not found. Will initialize empty.`
- `651 actionable tasks: 651 executed` — `FROM-CACHE` 0건

## 원인

캐시 설정 오류가 아니라 **캐시를 저장하는 job이 없다.** `gradle/actions/setup-gradle`은 기본 브랜치 job에서만 저장하는데, 워크플로 4종이 전부 `pull_request` 트리거라 생산자가 0개다. PR job은 존재한 적 없는 캐시를 매번 조회만 했다.

두 번째 원인은 캐시의 내용물이다. `org.gradle.caching`이 꺼져 있어 의존성만 쌓이고 태스크 출력 저장소(`build-cache-1`)가 비어 있었다.

## 변경

- `gradle-cache-seed.yml` 신규 — `develop` push에서 `test` + `ktlintCheck` + `assembleDebugAndroidTest` 2건(= PR 워크플로 둘의 태스크 합집합)을 한 번의 Gradle 호출로 돌려 캐시를 데운다.
- `gradle.properties` — `org.gradle.caching` · `org.gradle.parallel` 활성화, 데몬 힙 상향.
- 기존 `test.yml` · `ktlint.yml` · 합성 액션은 무변경(생산자/소비자 분리).

## 검증

로컬에서 `clean` 후 재빌드 시 `from cache` 항목이 잡히는 것까지 확인했다. 캐시 저장과 PR 적중은 이 PR이 `develop`에 머지된 **이후**에만 확인 가능하다 — 이 PR 자체의 CI는 여전히 콜드로 돈다.

## 범위 밖

configuration cache. Gradle 9·AGP 9에서 동작은 하지만 config cache 데이터는 프로젝트 `.gradle/configuration-cache`에 저장되고 `setup-gradle`의 캐시 대상은 Gradle User Home이라 매 런 새 러너인 CI에서는 이득이 0이다. 살리려면 `cache-encryption-key` 입력과 repo secret이 필요해 별건으로 둔다.
EOF
)"
```

- [x] **Step 3: 머지 후 — seed 런이 캐시를 저장했는지**

`develop` 머지 후:

```bash
gh run list --repo mash-up-kr/TEAMYG-Android --workflow gradle-cache-seed.yml --limit 3
```

런이 잡히면 job 로그를 받아 캐시 관련 줄을 본다(`JOB_ID`는 위 목록에서 얻는다).

```bash
gh api repos/mash-up-kr/TEAMYG-Android/actions/jobs/JOB_ID/logs > /tmp/seed.log
grep -a "cache-read-only\|Gradle User Home\|Caching Gradle state\|actionable tasks" /tmp/seed.log
```

Expected:
- `cache-read-only: false` — 기본 브랜치라 쓰기 모드로 전환됐다는 증거. **이 계획에서 실제 런으로만 확인 가능한 유일한 전제다.**
- 캐시 저장 관련 그룹이 로그에 존재.

`cache-read-only: true`가 나오면 전제가 틀린 것이다. 그 경우 `setup-android-build` 합성 액션에 `cache-read-only: false` 입력을 명시적으로 넘기는 후속 작업이 필요하다 — 사용자에게 보고한다.

`grep`에 `-a`를 붙이는 이유: Actions 로그는 제어문자가 섞여 grep이 바이너리로 판정하면 매칭 결과를 출력하지 않는다.

- [x] **Step 4: 다음 PR — 적중 판정**

seed 런 이후에 생성되거나 재실행된 PR의 `unit-test` job 로그에서:

```bash
gh api repos/mash-up-kr/TEAMYG-Android/actions/jobs/JOB_ID/logs > /tmp/pr.log
grep -a "Gradle User Home\|actionable tasks" /tmp/pr.log
```

Expected:
- `Gradle User Home cache not found`가 **사라진다**.
- `actionable tasks` 요약에 `from cache` 항목이 잡힌다.

이 둘이 최종 성공 판정이다. `Run unit tests` 소요 시간도 함께 기록해 진단 시점(4분대)과 비교한다.

- [x] **Step 5: 문서 갱신 (wiki 저장소)**

이 스텝만 wiki 저장소(`team-yg-pesonal-agent`)에서 수행한다. TJYG-Android가 아니다.

- `parfait/specs/2026-08-10-ci-gradle-cache-seeding.md`의 frontmatter `status`를 `implemented`로 바꾸고 `parfait/specs/archive/`로 옮긴다. `parfait/specs/README.md`의 인덱스 링크 경로도 함께 고친다.
- 같은 문서에 Step 3·4에서 실제 관측한 값(`cache-read-only` 값, `from cache` 유무, 소요 시간)을 결과로 덧붙인다. 예측이 아니라 관측이므로 런 ID와 함께 적는다.
- `parfait/plans/README.md`의 이 계획 행에 완료 결과를 덧붙이고 `parfait/plans/archive/`로 옮긴다.
- 남은 열린 질문 2건(configuration cache CI 활성화, Node 20 deprecation)이 여전히 유효하면 `parfait/synthesis/open-questions.md`에 등록한다.

wiki 저장소 변경은 브랜치 → commit → (사용자 확인 후) push → PR 절차를 따른다.

---

## 실패 시 축소 경로

전부 실패해도 되돌릴 것은 파일 2개뿐이다. 부분 실패 시 선택지:

| 실패 지점 | 축소안 |
|---|---|
| Task 1 Step 4에서 병렬 실행이 빌드를 깸 | `org.gradle.parallel`만 빼고 `org.gradle.caching`·힙은 유지. 캐시 효과 자체는 살아 있다. |
| Task 1 Step 6에서 `from cache` 0건 | 중단. 시딩 워크플로를 붙여도 효과가 없으므로 Task 2로 넘어가지 않는다. |
| Task 3 Step 3에서 `cache-read-only: true` | `setup-android-build`에 `cache-read-only` 입력을 뚫어 seed job에서만 `false`로 넘기는 후속 작업. |
