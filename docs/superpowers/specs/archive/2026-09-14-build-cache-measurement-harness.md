---
id: build-cache-measurement-harness
title: 로컬 빌드 캐시 측정 하니스 (Local build cache measurement harness)
status: implemented
category: build-ci
platforms: android
verified: 2026-09-16
related_code:
  - gradle.properties
  - gradle-cache-seed.yml
  - tools/build-cache-bench/run.sh
  - tools/build-cache-bench/check-relocatability.sh
  - tools/build-cache-bench/report.py
  - tools/build-cache-bench/cache-report.init.gradle.kts
related_adr:
related_spec:
  - ci-gradle-cache-seeding
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, build, ci, performance]
---

# Spec: 로컬 빌드 캐시 측정 하니스

> ✅ **구현 완료·develop 머지(2026-09-16, PR #499 `f37a76540` — 머지 트리가 브랜치 팁 `6600f9efa`와 같다, 충돌 해소 편집 0건).**
> 6파일 · 삽입 1200줄, 커밋 19개. 코드 변경이 `tools/`와 `.gitignore`뿐이라 테스트 수는 그대로다
> (유닛 1298 · 계측 46, 두 수 모두 같은 라운드의 PR #497이 올린 값이다).
> **구현이 이 스펙보다 두 자리 넓다** — 게이트 G0를 실행하는 `check-relocatability.sh`가 별도 파일이 됐고,
> 요약을 브라우저로 보는 `report.html`과 그것을 조립하는 `report.py`가 계획에도 없던 산출물로 붙었다.
> 아래 「파일 배치와 실행 인터페이스」·「출력물」·「게이트 G0」는 머지본을 기준으로 고쳐 적었다.
>
> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

리모트 빌드 캐시를 도입할 값어치가 있는지 판단할 **근거를 만든다.** 산출물은 리모트 캐시가
아니라 그 결정을 수치로 뒷받침하는 측정 도구다.

## 진단 근거

현재 캐시 구성은 두 층이다.

- `gradle.properties`의 `org.gradle.caching=true` — 팀원 각자 머신의 로컬 빌드 캐시.
- `gradle-cache-seed.yml` — `develop` push마다 GitHub Actions 캐시에 Gradle User Home을 굽고,
  PR 워크플로가 읽기 전용으로 소비한다. 경위와 실측은
  [ci-gradle-cache-seeding](2026-08-10-ci-gradle-cache-seeding.md)에 있다.

리모트 빌드 캐시가 **추가로** 파는 것은 하나다. **CI가 이미 구운 태스크 출력을 개발자 머신이
당겨 쓰는 것.** GitHub Actions 캐시는 러너 안에서만 닿기 때문에 지금은 그 경로가 없다.

그래서 재야 할 값은 "캐시가 있을 때와 없을 때의 차이"가 아니다. 그 값은 로컬 캐시가 이미
대부분 소진했다. 재야 할 것은 **개발자가 새 커밋을 당겨받은 순간, 로컬 캐시에는 없고 CI
캐시에는 있는 태스크들의 실행 시간**이다. 그것이 리모트 캐시가 파는 물건의 크기다.

## 범위

- 포함: 로컬 전용 측정 하니스 신규 1식(러너 스크립트 · init script 리스너 · README).
- 포함: `.gitignore`에 측정 결과 디렉토리 제외 1줄.
- 제외: **리모트 빌드 캐시 도입 자체.** 이 스펙은 측정까지다.
- 제외: **CI에서 도는 측정.** 러너 시간을 쓰고, 측정 대상이 러너 환경이라 개발자 머신의
  체감과 벌어진다.
- 제외: configuration cache와 `org.gradle.parallel`. 이 저장소는 둘 다 켜 두지 않았고
  (`gradle.properties`에 해당 키가 없다), 측정 축에 섞으면 캐시 효과를 가를 수 없다.
  빌드 성능 후속 과제로 [open-questions](../../../synthesis/open-questions.md)에 이미 올라 있다.
  하니스 자체는 configuration cache를 켜도 동작하므로 나중에 다시 만들 필요는 없다.

## 설계

### 핵심 지표

리모트 캐시의 이상적 상태를 **전용 캐시 디렉토리의 내용물로 대역**한다. 서버를 세우지 않고도
효과를 시뮬레이션할 수 있다.

커밋 쌍 `(A, B)`를 잡고 같은 빌드를 두 조건에서 돌린다. `A`는 개발자가 어제까지 작업하던
커밋이고, `B`는 방금 당겨받은 커밋이다.

- **`T_local`** — 전용 캐시가 `A`의 항목만 담은 상태에서 `B`를 빌드한 시간.
  리모트 캐시가 **없는** 지금의 개발자다.
- **`T_remote`** — 전용 캐시가 `A`와 `B`의 항목을 모두 담은 상태에서 `B`를 빌드한 시간.
  빌드 출력은 삭제한다. CI가 `B`를 이미 구웠고 개발자가 그 항목을 받는 상태다.

**핵심 값은 `T_local − T_remote`다.** 이것이 리모트 캐시가 실제로 사 오는 몫이다.

**두 조건의 빌드 출력 상태는 커밋 `A` 기준으로 같아야 한다.** 어제 `A`까지 빌드해 두고 오늘
`B`를 당겨받은 상태가 실제 개발자이고, 한쪽만 출력을 지우면 캐시가 아니라 출력 유무가 차이를
만들어 값이 부풀려진다. 그래서 `T_remote` 쪽은 `B`를 먼저 구워 캐시에 담은 뒤 `A`를 다시
빌드해 출력을 되돌린다.

**캐시를 채우는 빌드 앞에는 반드시 출력 삭제가 온다.** Gradle은 태스크가 실제로 실행될 때만
캐시 항목을 저장한다. 출력이 남아 있으면 그 빌드가 up-to-date로 끝나 빈 캐시에 아무것도 담기지
않고, 두 조건이 똑같이 빈 캐시로 측정되어 핵심 값의 부호가 뒤집힌다.

전송 시간을 0으로 가정하므로 이 값은 **낙관적 상한**이다. 실제 리모트 적중은 태스크 실행
대신 다운로드와 압축 해제를 치른다. 그 간격을 재는 수단은 이 하니스에 없다(→ 한계).

커밋 쌍은 두 종류를 잡는다.

- **P1 — 일상 커밋 쌍.** `develop`의 인접한 두 머지 커밋. 하루에 여러 번 겪는 경우다.
- **P2 — 코어 모듈 ABI 변경 쌍.** `:core:*` 또는 `:domain`의 공개 시그니처가 바뀐 머지 커밋과
  그 부모. 다운스트림 컴파일 태스크가 최대 폭으로 무효화되는, 리모트 캐시가 가장 크게 이기는
  자리다.

**개발자가 자기 코드를 방금 고친 경우는 범위 밖이다.** 그 키는 아무도 빌드한 적이 없어
로컬에도 CI에도 항목이 없고, 리모트 캐시가 개입할 여지가 없다.

### 시나리오

| ID | 사전 상태 | 무엇을 말해주는가 |
|---|---|---|
| S0 | 아무것도 지우지 않고 재실행 | 하한선. 어떤 캐시도 이 아래로 못 내려간다. |
| S1 | 빌드 출력 삭제, 캐시는 같은 커밋으로 채워진 상태 | 로컬 캐시의 최상 조건. 여기서 남는 미스는 캐시 불가 태스크다. |
| S2 | 빌드 출력 삭제 + 캐시 비움 | 총 캐시 가능 작업량. 참고값이다. |
| S3 | 커밋 `B`, 캐시에 `A`만, 출력은 `A` 기준 | `T_local` |
| S4 | 커밋 `B`, 캐시에 `A`와 `B` 모두, 출력은 `A` 기준 | `T_remote` |

`S3`와 `S4`가 핵심이고 나머지 셋은 해석용 참고값이다. `S0`은 바닥, `S1`은 로컬 캐시가 낼 수
있는 최선, `S2`는 전체 작업량이라 `S3 − S4`가 그 사이 어디에 놓이는지 읽게 해 준다.

대상 그래프는 `:app:assembleDebug`와 `test` 둘이다. 앞은 개발자가 실제로 기다리는 빌드이고,
뒤는 태스크 성격이 달라(컴파일과 테스트 실행의 비중) 캐시 값어치가 다르게 나올 수 있다.

> **`test`를 CI 실측치와 직접 대조하지 않는다.** `test.yml`의 PR job은 `test` 외에
> `:core:util:android:assembleDebugAndroidTest`·`:core:designsystem:assembleDebugAndroidTest`를
> 따로 돌리고, CI는 `ubuntu-latest`에 콜드 데몬이며 하니스는 macOS에 웜 데몬이다. 대조할 수
> 있는 것은 actionable 태스크 수 정도이고 시간은 아니다.

### 게이트 G0 — 캐시 항목 이식성

**이것이 첫 번째로 확인할 것이다. 여기서 깨지면 핵심 값이 얼마든 리모트 캐시의 가치는 0이다.**

다른 절대경로에서 만들어진 캐시 항목이 적중하는지 본다. 클론 두 벌을 두고, 한쪽에서 채운
전용 캐시 디렉토리를 다른 쪽이 가리키게 한 뒤 `FROM_CACHE`가 잡히는지 확인한다. 적중률이
낮으면 어느 태스크가 이식 불가인지 `tasks.csv`로 특정한다.

구현은 러너와 별도 스크립트 `check-relocatability.sh`다. seed와 probe 둘 다 worktree이고 각각
`local.properties`를 복사받으며, probe는 `clean` 뒤에 잰다. 인자로 준 태스크가 probe에서
`FROM_CACHE`가 아니거나 판정 대상에 `FROM_CACHE`가 한 건도 없으면 종료 코드 1이다.
**판정에서 빼고 따로 출력하는 것이 둘 있다** — `@DisableCachingByDefault` 태스크는 어떤 캐시로도
줄지 않아 섞으면 "캐시 불가"가 "이식 불가"로 읽히고, included build(`:build-logic:*`)는 seed가
`--rerun-tasks`로 담아 둔 것이 probe에서 적중해 게이트를 거짓 통과시킨다(러너의 적중률 집계도
같은 이유로 뺀다).

**한계를 명시한다.** 이 게이트가 재는 것은 절대경로 이식성뿐이다. 실제 리모트 캐시에서는
**CI(`ubuntu-latest`)가 만든 항목을 개발자 머신(macOS)이 받는다.** OS와 JDK 벤더가 캐시 키에
들어가므로 적중하지 않을 수 있는데, 그 확인은 리모트 캐시를 실제로 세워 보기 전에는 불가능하다.
경로 이식성이 깨지면 OS 이식성은 볼 것도 없다는 의미에서 G0는 필요조건 검사다.

### 측정을 개발자 워킹 트리에서 떼어낸다

**측정은 전용 클론에서 돈다.** 러너가 `git worktree`로 측정 전용 작업 트리를 만들거나, 이미
있는 전용 클론 경로를 인자로 받는다. 개발자의 워킹 트리에서는 돌지 않는다.

이유가 셋이다. 첫째, `S3`·`S4`가 커밋을 오가야 하는데 개발자 트리에서 `git checkout`을 하면
측정이 중단됐을 때 detached HEAD와 사라진 작업물을 남긴다. 둘째, `clean`이 개발자의 실제
빌드 출력과 IDE 증분 상태를 날린다. 셋째, 전용 클론은 절대경로가 다르므로 G0 게이트를
측정과 같은 자리에서 확인할 수 있다.

러너는 시작 전에 대상 트리가 clean한지 확인하고, `git stash`와 `checkout -f`를 쓰지 않으며,
중단 시 원래 HEAD로 되돌리는 `trap`을 건다.

### 전용 빌드 캐시 디렉토리

init script가 `settingsEvaluated` 시점에 `buildCache.local.directory`를 하니스 전용 경로로
돌린다. 개발자의 `~/.gradle/caches/build-cache-1`에는 쓰지 않는다. `S3`·`S4`가 요구하는
"캐시에 `A`만" / "`A`와 `B` 모두" 상태도 이 디렉토리를 통째로 교체하는 것으로 만든다.

**비파괴 주장은 여기까지다.** 옮겨지는 것은 빌드 캐시뿐이고, 의존성(`modules-2`)과 AGP
아티팩트 변환 결과(`transforms-*`), Kotlin DSL 캐시, 데몬 로그는 개발자의 Gradle User Home에
계속 쌓인다. 데이터를 잃지는 않지만 디스크는 먹는다.

전용 디렉토리는 러너가 인자로 받고 기본값을 명시한다. 시스템이 청소할 수 있는 경로는 쓰지
않는다. 삭제 동작에는 빈 변수 가드를 둔다.

### 측정 조건

- **반복 매 회차마다 사전 상태를 다시 세운다.** 시나리오 단위가 아니다. `S2`의 2회차는
  캐시를 다시 비우지 않으면 사실상 `S1`이 되고, `S3`는 회차를 거듭할수록 `B`의 항목이 캐시에
  쌓여 `S4`로 수렴한다. 이 두 오염이 결과를 통째로 무효화한다.
- **시나리오 실행 순서를 러너가 고정한다.** 부분집합 실행을 허용하되, 각 시나리오가 자기
  사전 상태를 독립적으로 세운다. `S1`만 단독으로 돌려도 의미가 있어야 한다.
- **데몬 온도를 기록한다.** 데몬은 빌드를 거듭하며 JIT와 파일 해시를 축적해서 같은 데몬 안의
  N번째 빌드가 1번째보다 체계적으로 빠르다. 단조 편향이라 중앙값으로 지워지지 않는다.
  `builds.csv`에 데몬 식별자를 남기고, **`--stop`을 사전 상태 수립 앞뒤로 한 번씩** 건다.
  앞의 것은 캐시 디렉토리를 지울 때 직전 회차 데몬이 그것을 붙잡고 있지 않게 하고, 뒤의 것은
  사전 빌드 횟수가 시나리오마다 다른 것(`S4`는 2회, `S3`는 1회)이 데몬 온도 차이로 남지 않게 한다.
  뒤의 `--stop`이 빠지면 `S4` 쪽이 항상 더 더워 핵심 값이 한 방향으로 부풀려진다. Kotlin 데몬도 별도 프로세스로 자체 웜업 곡선을 가지므로
  같은 취급을 받는다.
- **`--offline`과 `--build-cache`로 고정한다.** 앞은 플러그인 마커 재확인이나 툴체인 해석에서
  네트워크가 끼는 것을 막는다. 뒤가 없으면 캐시가 켜지는 유일한 근거가 체크아웃된 커밋의
  `gradle.properties`가 되어, `org.gradle.caching`이 들어오기 전 커밋을 쌍으로 고르면 전
  시나리오가 캐시 없이 돌고 **"리모트 캐시는 값어치가 없다"는 결론이 조용히 만들어진다.**
- 각 시나리오를 기본 3회 반복하고 중앙값을 쓴다. 반복 횟수는 러너 인자다.
- 러너는 시작 전에 선행 조건을 검사한다(→ 선행 조건). 한 시간짜리 측정이 중간에 죽지 않게 한다.

### 선행 조건

`:app:assembleDebug`는 아래를 요구한다. 전부 `.gitignore` 대상이라 클론마다 각자 채워야 한다.

| 필요한 것 | 없으면 | 근거 |
|---|---|---|
| 루트 `local.properties` | configuration 단계에서 즉시 실패 | `app/build.gradle.kts`가 `rootProject.file("local.properties")`를 무조건 연다. 템플릿은 `local.default.properties` |
| `sdk.dir` | Android SDK를 못 찾는다 | `local.default.properties` |
| `kakao.native.app.key` | 빌드는 되지만 placeholder가 잘못 박힌다 | 같은 파일 |
| debug 서명 4종 값(`YG_DEBUG_STORE_FILE`·`YG_DEBUG_STORE_PASSWORD`·`YG_DEBUG_KEY_ALIAS`·`YG_DEBUG_KEY_PASSWORD`)과 키스토어 파일 실존 | `validateSigningDebug`가 `error(...)`로 실패 | `build-logic`의 `AndroidConfig`·`PropertySettingManager`. `YG_DEBUG_STORE_FILE`이 상대경로면 `app/` 기준으로 풀리므로 새 worktree에서는 절대경로가 안전하다 |
| `app/google-services.json` | `processDebugGoogleServices` 실패 | `app/build.gradle.kts`의 `com.google.gms.google-services` |

`YG_BASE_URL`은 필요 없다. `PropertySettingManager`에 폴백이 있다. `test` 그래프는 서명과
google-services를 타지 않으므로 요구 조건이 더 얕다.

### 수집 지표

- **빌드 단위** — 시나리오 ID, 커밋 쌍 ID, 대상 그래프, 반복 인덱스, wall time, 데몬 식별자,
  데몬 안에서의 빌드 순번.
- **태스크 단위** — 태스크 경로, outcome, 소요 시간, 그리고 **캐시 미스 사유**.
- **캐시 규모** — 각 시나리오 종료 후 전용 캐시 디렉토리의 항목 수와 총 용량. 리모트 캐시의
  저장 비용과 전송 시간을 추정할 유일한 근거다.

태스크 outcome은 init script가 `BuildEventsListenerRegistry`에 `OperationCompletionListener`를
등록해 받는다. `TaskSuccessResult`의 `isFromCache`와 `isUpToDate`로 세 상태를 가르고,
`TaskSkippedResult.getSkipMessage()`가 나머지를 준다. **이 경로는 Gradle 9.5와 이 저장소에서
동작을 확인했다.**

`TaskExecutionResult.getExecutionReasons()`가 "왜 이 태스크가 실행됐는가"를 문자열로 돌려준다.
`S3`에서 미스가 난 태스크가 왜 미스인지를 별도 분석 없이 바로 기록한다.

**`TaskExecutionListener`와 `afterTask`는 쓰지 않는다.** 둘 다 Gradle 9.5에 남아 있고 이
저장소에서는 configuration cache가 꺼져 있어 지금 당장은 동작하지만, deprecated이고
configuration cache를 켜는 순간 `Listener registration ... is unsupported.`로 빌드가 깨진다.
나중에 그 축을 켤 때 하니스를 다시 만들지 않으려고 처음부터 지원되는 경로를 쓴다.

**outcome 값은 API가 돌려주는 문자열을 그대로 쓴다.** `getSkipMessage()`는 밑줄이 아니라
하이픈이 들어간 `NO-SOURCE`를 돌려준다. 정규화하면 원본 대조가 어려워진다.

### 파일 배치와 실행 인터페이스

```
tools/build-cache-bench/
├── run.sh                        # 시나리오 러너
├── check-relocatability.sh       # 게이트 G0 (러너와 별도로 먼저 돌린다)
├── cache-report.init.gradle.kts  # 태스크 outcome 수집 + 전용 캐시 디렉토리 전환
├── report.py                     # CSV → report.html 조립
├── README.md                     # 사용법·선행 조건·측정 조건·해석 방법
└── runs/                         # 측정 결과 (.gitignore 대상)
```

저장소에 `tools/`·`scripts/` 최상위 디렉토리는 없다. `docs/`·`http/`·`baselineprofile/`처럼
용도별 최상위 디렉토리를 두는 관례를 따른다.

```
./tools/build-cache-bench/run.sh \
  --scenarios S0,S1,S2,S3,S4 \
  --targets :app:assembleDebug,test \
  --pair "$(git rev-parse <A>):$(git rev-parse <B>)" \
  --iterations 3
```

`--targets`는 Gradle에 그대로 넘기는 태스크 경로다. `test`는 전 모듈, `:app:assembleDebug`는
해당 모듈이라는 차이가 인자 표기에 드러나야 한다.

머지본의 인자는 위 넷에 셋을 더한 일곱이다. **`--tree`는 선택이다** — 생략하면 러너가 worktree를
새로 만들고 저장소 루트의 `local.properties`·`app/google-services.json`을 복사한다. `--cache-dir`는
전용 캐시 경로(기본 `<out>/cache`), `--out`은 결과 디렉토리(기본 `runs/<타임스탬프>/`), `--dry-run`은
빌드 없이 실행 계획과 고정된 쌍 SHA만 출력한다. **커밋 쌍은 `--pair <A>:<B>` 한 인자로 받는다** —
스펙 초판이 쓴 `--pair P1` 같은 쌍 이름은 러너가 모른다. `S3`·`S4`를 안 돌리면 `--pair`는 필요 없다.

### 출력물

`--out`(기본 `runs/<타임스탬프>/`) 아래에 여섯을 남긴다.

- `builds.csv` — 빌드 1행. 컬럼 `scenario,target,pair,iteration,wall_ms,daemon_pid`.
- `tasks/<시나리오>-<그래프>-<회차>.csv` — 태스크 1행. 컬럼 `task_path,outcome,duration_ms,execution_reasons`.
  빌드마다 한 파일이다.
- `cache-size.csv` — 시나리오별 캐시 항목 수와 용량. 컬럼
  `scenario,target,iteration,pre_entries,pre_kb,post_entries,post_kb`. **`pre_*`가 측정 빌드가 읽으려는
  캐시이고 `post_*`에는 그 빌드가 새로 밀어 넣은 항목이 섞인다** — 저장 비용·전송량 추정에는 `pre_*`를 쓴다.
- `logs/<태그>.log`(측정 빌드)·`logs/<태그>.prepare<N>.log`(사전 상태를 만든 빌드들)·
  `logs/<태그>.warmup.log`. `<N>`은 그 시나리오 안에서의 순번이다(`S4`는 1=`clean`, 2=B 굽기,
  3=`clean`, 4=A 굽기).
- `summary.md` — 사람이 읽는 요약. **핵심 값 `T_local − T_remote`를 커밋 쌍과 대상 그래프별로**
  먼저 싣고, 참고값(`S0`·`S1`·`S2`)과 `S3`에서 미스로 남은 태스크를 사유별로 묶어 잇는다.
  적중률 표에는 `from_cache` 외에 `up_to_date`·`executed`도 싣는다 — 셋을 다 보지 않으면 "쌍이
  무신호라 전부 `UP_TO_DATE`"와 "캐시 미스인데 빨랐다"가 똑같이 `0.0%`로 읽힌다.
- `report.html` — **스펙 초판에 없던 산출물이다.** 같은 내용을 브라우저로 본다. 핵심 값을 맨 위에 두고
  시나리오별 소요 시간과 태스크 결과 구성을 차트로 그리며, `S3` 미스 태스크의 긴 사유는 접어 둔다.
  외부 라이브러리를 쓰지 않는 단일 파일이고, 조립(`report.py`)이 실패해도 러너는 죽지 않는다 —
  CSV와 `summary.md`는 이미 디스크에 있다.

**적중률의 분모는 actionable 태스크로 고정한다.** 그래야 Gradle 요약 줄과 같은 기준이 된다.

**하니스는 커밋하고 `runs/`는 커밋하지 않는다.** 측정치는 머신마다 다르다. 판단에 실제로 쓴
수치만 후속 스펙으로 옮긴다.

## 검증

1. **게이트 G0 — 이식성.** 다른 절대경로에서 채운 캐시가 적중하는지 확인한다. 적중률이 낮으면
   여기서 멈추고 결과를 보고한다. 하니스의 나머지를 짓기 전에 통과해야 한다.
2. **교차 검증.** `tasks.csv`의 집계가 Gradle의 `N actionable tasks: X executed, Y from cache`
   줄과 일치하는지 대조한다. **대조 전에 필터를 건다** — 그 줄은 actionable 태스크만 세는데
   리스너는 lifecycle 태스크와 included build(`build-logic`)의 태스크까지 받는다. 필터 없이
   대조하면 리스너 누락이 아니라 정의 차이로 어긋난다.
3. **방향성 확인.** `S1`에서 `FROM_CACHE`가 지배적이고 `S2`에서 `EXECUTED`가 지배적인지,
   `S4`의 적중이 `S3`보다 많은지 본다. 반대로 나오면 사전 상태 구성이 틀린 것이다.
4. **반복 오염 확인.** 같은 시나리오의 1회차와 3회차 outcome 분포가 유사한지 본다. 회차를
   거듭하며 `FROM_CACHE`가 늘어나면 사전 상태 재수립이 동작하지 않는 것이다.
5. **비파괴 확인.** 전체 측정 1회 후 개발자의 `~/.gradle/caches/build-cache-1` 항목 수가
   그대로인지, 그리고 측정 대상 트리의 HEAD가 원래 자리로 돌아왔는지 확인한다. 측정을 중간에
   끊었을 때도 같은지 본다.

## 주의 / 열린 질문

- **OS 이식성은 이 하니스로 확인할 수 없다.** CI는 `ubuntu-latest`이고 개발자는 macOS다.
  CI가 만든 항목이 개발자 머신에서 적중하는지는 리모트 캐시를 실제로 세워야 알 수 있다.
  G0는 그 필요조건만 검사한다.
- **전송 비용이 지표에 없다.** `T_remote`는 전송 시간을 0으로 놓는다. 실제 이득은 이 값보다
  작다. `cache-size.csv`의 용량이 전송 시간 추정의 재료다.
- **측정 1회의 시간 비용이 크다.** 커밋 쌍 2종 × 대상 그래프 2종 × 시나리오 5종 × 3회에
  워밍업까지 더하면 빌드 수가 백 단위로 간다. 기본 구성으로 한 번에 다 돌리는 것은 현실적이지
  않으므로, 러너는 커밋 쌍과 그래프를 하나씩 돌릴 수 있어야 한다.
- **`S4`가 사전 상태에서 풀빌드를 `S3`보다 2회 더 돈다 — 한 방향 편향이다.** 사전 상태 수립 직후의
  `--stop`이 균등화하는 것은 **Gradle 데몬뿐**이고, OS 페이지 캐시와 Kotlin 데몬은 그것을 넘겨 남는다.
  그래서 `S4` 쪽이 더 덥혀진 상태로 측정에 들어가 핵심 값 `S3 − S4`를 **부풀린다.** 회차마다 같은
  방향이라 중앙값으로 지워지지 않는다(구현 중에 드러나 README 「한계」에도 적었다).
- **Kotlin 데몬은 통제되지 않는다.** `./gradlew --stop`은 Gradle 데몬만 죽이고 Kotlin 컴파일
  데몬은 살려 둔다. 회차마다 Gradle 데몬을 새로 띄워도 Kotlin 쪽 웜업 곡선은 이어진다.
  Gradle 데몬이 실제로 교체됐는지는 `builds.csv`의 `daemon_pid`로 사후 확인한다.
- **머신 노이즈를 통제할 수 없다.** IDE·다른 Gradle 데몬·전원 상태·발열이 수치를 흔든다.
  러너가 `gradle --status`로 다른 데몬을 확인하고 경고한다. 서로 다른 머신의 수치는 직접
  비교하지 않는다.
- **열린 질문: 커밋 쌍 `P1`·`P2`를 무엇으로 고정할 것인가.** 두 커밋의 거리가 결과를 좌우한다.
  선정 기준(머지 커밋만 쓸 것인지, `P2`의 "코어 모듈 ABI 변경"을 어떻게 식별할 것인지)을
  정해야 한다. **구현 중에 확정된 것 셋**: ① 쌍은 러너가 `rev-parse`로 **full SHA로 고정**한다 —
  `HEAD~1` 같은 상대 참조를 그대로 넘기면 러너가 트리 HEAD를 옮기는 동안 다시 해석되어 측정
  빌드가 `B`가 아니라 `A`에서 돈다. ② 두 SHA가 같거나 트리가 동일한 쌍은 러너가 거부한다.
  ③ 대상 모듈의 입력이 실제로 바뀌는지는 러너가 알 수 없다 — `git diff --stat <A> <B> -- <모듈 경로>`로
  사람이 먼저 확인한다. `S3`가 사실상 전부 `UP_TO_DATE`면 캐시 효과가 없는 것이 아니라 쌍이
  무신호라는 뜻이다.
- **열린 질문: 도입을 정당화하는 문턱값.** `T_local − T_remote`가 얼마 이상이면 리모트 캐시를
  세울 값어치가 있다고 볼 것인지 합의된 선이 없다. 측정 후에 정하면 결과에 맞춰 기준이
  움직인다. **측정을 돌리기 전에 정한다.** 이 스펙이 아카이브로 가면서 추적은
  [open-questions](../../../synthesis/open-questions.md) OQ-P-402가 잇는다 — 하니스는 머지됐지만
  **측정은 아직 아무도 돌리지 않았다.**
