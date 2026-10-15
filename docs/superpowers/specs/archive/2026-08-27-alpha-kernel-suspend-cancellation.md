---
id: alpha-kernel-suspend-cancellation
title: 알파 후처리 커널의 취소 확인을 콜백에서 suspend + ensureActive 로 옮긴다
status: implemented
category: refactor-spec
platforms: android
verified: 2026-08-27
related_code: AlphaComponents.kt#downscaleMask, AlphaComponents.kt#applyAreaOpening, AlphaComponents.kt#dilateMask, AlphaPostProcessor.kt#postProcessAlpha, AlphaPostProcessor.kt#applyKeepMask, AlphaPostProcessor.kt#measureAlpha, AlphaPostProcessor.kt#erodeEdge, AlphaPostProcessor.kt#refineWithin, AlphaRefine.kt#refineAlpha, AlphaRefine.kt#boxMean, SegmentationMask.kt#maskSubjectAlpha, ImageSegmentationRepositoryImpl#toCandidatePairs, ImageSegmentationRepositoryImpl#buildCandidatePair, ImageSegmentationRepositoryImpl#postProcess, ImageSegmentationRepositoryImpl#toForegroundCandidate, CountingJob.kt
related_adr:
related_spec: segmentation-mask-postprocessing, segmentation-alpha-refinement
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait, coroutines]
---

# Spec: 알파 후처리 커널의 취소 확인 방식 전환

> ✅ **as-built(2026-08-27, PR #363 `4da18230` develop 머지)** — 세 브랜치가 rebase 를 거쳐 정련
> 브랜치 하나로 접혔고, 그 브랜치가 `develop` 으로 들어왔다. 그래서 **`--merges` 목록에는 이 스택이
> 한 줄로만 뜬다**(커밋은 44개다). 「변환 규칙」·「전염 방향」 표는 아래 as-built 주석까지 포함해
> 코드와 일치하고, `CountingJob` 도 테스트 소스에 그대로 있다. 이 회차가 새로 고친 자리는 없다 —
> 「브랜치 배분」이 예고한 rebase 위험(`refineWithin` 이 커널 PR 에 없다)은 실행 중에 이미 처리됐다.

## 목표

알파 후처리 커널이 취소를 확인하는 방법을 **호출부가 넘기는 `checkCancelled` 콜백**에서
**`suspend` 함수 + `ensureActive()`** 로 바꾼다.

프로덕션 동작은 바뀌지 않는다. 취소를 확인하는 지점과 빈도가 그대로이고, 호출부가 이미
`{ job?.ensureActive() }` 를 넘기고 있어 커널이 보게 될 `Job` 인스턴스도 같다. 바뀌는 것은
**그 확인 수단을 어떻게 커널에 전달하느냐**다. 테스트 쪽에는 소멸하는 것이 하나 있다(「테스트」).

## 근거 등급 (이 스펙의 성격)

앞선 두 라운드가 실기기 관측에서 출발했다면 이 스펙은 **코드 리뷰에서 출발한다.** 리뷰가 대안을
제시했고, 그 대안의 비용을 마이크로벤치마크로 재서 채택 범위를 좁혔다.

측정은 데스크톱 JVM 에서 했고 실기기 절대값은 다르다. 아래 「측정」에서 **재현된 결론과 재현되지
않은 결론을 구분해 적었다.** 채택 판단에 쓴 것은 재현된 쪽뿐이다.

## 배경

리뷰는 이렇게 물었다. `downscaleMask` 같은 함수가 CPU 를 오래 점유하니, 콜백 대신 이 함수들을
`suspend` 로 만들고 중단 지점에 `yield()` 를 두면 취소가 구조적 동시성으로 자연스럽게
전파되지 않겠느냐.

지적의 방향은 옳다. 정련 브랜치 기준으로 `checkCancelled` 는 `postProcessAlpha` → `refineWithin`
→ `refineAlpha` → `guidedCoefficients` → `boxMean` 까지 네 홉을 손수 전달된다. 새 커널을
추가하는 사람이 이 파라미터를 잊으면 그 단계는 취소를 확인하지 않고, 그 누락은 조용하다.
`AlphaPostProcessorTest` 가 확인 호출 수에 하한을 걸어 둔 이유가 그것이다.

⚠️ **`suspend` 로 바꾼다고 취소가 저절로 전파되지는 않는다.** 취소 전파는 중단 지점에서만
일어나는데 이 커널들은 순수 CPU 루프라 중단 지점이 없다. `ensureActive()` 를 손으로 넣어야
하는 부담은 콜백과 똑같다. **사라지는 것은 파라미터 배관뿐이고, 그것이 이 전환의 전부이자
유일한 이득이다.** 이 문장을 근거 삼아 확인 호출을 생략하면 안 된다.

다만 **`yield()` 는 이 워크로드에 맞지 않는다.** 그래서 리뷰의 구조는 받고 프리미티브만 바꾼다.

## 범위

**포함**

- 취소 확인을 하는 커널과 **그 호출 사슬 위쪽 전부**를 `suspend` 로 전환
- `checkCancelled` 파라미터와 기본값 `= {}` 제거
- 관련 테스트의 `runTest` 전환과 취소 검증 재구성

**제외**

- 취소 확인 지점·빈도 변경. 지금처럼 행 경계마다 확인한다
- 커널 알고리즘·수치·출력의 변경. 이 전환으로 결과 픽셀이 달라지면 안 된다
- `yield()` 도입(아래 「배제한 대안」)
- 확인 빈도를 낮추는 스로틀링(아래 「배제한 대안」)
- `GuidanceProvider.pixelsIn` 의 `suspend` 화. 커널에 진짜 중단 지점을 만드는 변경이고, 그러면
  테스트 하니스 전제와 `refineElapsedNanos` 의 의미가 함께 흔들린다(「미결」)

## 설계

### 변환 규칙

취소를 확인하는 함수는 `suspend` 가 되고, 진입 시 `Job` 을 한 번 꺼내 루프에서 쓴다.

```kotlin
internal suspend fun downscaleMask(
    alpha: ByteArray, width: Int, height: Int, factor: Int, threshold: Int,
): BooleanArray {
    val job = currentCoroutineContext().job
    ...
    for (y in 0 until height) {
        job.ensureActive()
```

**`currentCoroutineContext().job` 을 쓰는 것이 계약이다.** 이 확장은 컨텍스트에 `Job` 이 없으면
`IllegalStateException` 을 던진다. 반면 `get(Job)?.ensureActive()` 계열은 — `job?.ensureActive()`
든 `currentCoroutineContext().ensureActive()` 든 — `Job` 이 없으면 **조용히 아무 것도 하지
않는다.** 취소 확인이 통째로 no-op 이 되고도 테스트가 초록으로 남는 실패 모양이라, 진입에서 한 번
터지는 편이 낫다. 프로덕션 호출부는 전부 `withContext(Dispatchers.Default)` 안이고 테스트
하니스도 항상 `Job` 을 넣으므로 정당한 no-Job 경로는 없다.

루프 밖에서 한 번만 꺼내는 것도 계약이다. 근거는 성능이 아니라 이 fail-fast 를 **함수당 한 번**만
치르기 위해서다(성능 근거는 「측정」 3번 참고).

조기 반환 가드가 있는 함수(`erodeEdge` 의 `if (width <= 0 || height <= 0) return false`)에서도
호이스팅은 가드 **앞**에 둔다. 조기 반환 경로에는 확인 루프가 없어 그 자리에서 `Job` 부재를 미리
잡을 실익은 없지만, 실익이 없다는 관찰만으로 위치를 바꿀 근거는 되지 못한다 — `erodeEdge` 는
커널·결선·정련 세 브랜치가 차례로 손대는 함수라, 어느 한 브랜치에서 자리를 바꾸면 나머지와
어긋난다(실제로 정련 브랜치가 rebase 충돌을 풀면서 이 함수를 다시 건드려 가드 뒤로 옮겨진 적이
있고, 스택 일관성을 근거로 가드 앞으로 되돌렸다). **같은 함수는 스택 전체에서 같은 형태여야 한다**는
것이 개별 함수의 미세한 실익보다 우선한다.

### 전염 방향

**잎 방향으로는 전염시키지 않는다.** 취소 확인이 없는 순수 함수(`ceilDiv` · `countRuns` ·
`fillRuns` · `findRoot` · `union` · `bilinear` · `luminanceOf`)는 그대로 둔다. 확인 지점이 없는
함수까지 `suspend` 가 되면 어느 함수가 취소를 존중하는지가 시그니처에서 읽히지 않는다.

**호출 사슬 위쪽으로는 전염이 강제된다.** 전환 대상을 부르는 함수는 예외 없이 `suspend` 가
된다. 아래 표의 「전염」 행이 그것이다 — 자체 확인 지점은 없지만 대상 함수를 부르기 때문에 바뀐다.

| 파일 | 자체 확인 | 전염 |
|---|---|---|
| `AlphaComponents.kt` | `downscaleMask` · `unionAdjacentRows` · `dilateMask` | `applyAreaOpening` |
| `AlphaPostProcessor.kt` | `applyKeepMask` · `measureAlpha` · `erodeEdge` | `postProcessAlpha` · `refineWithin` |
| `AlphaRefine.kt` | `boxMean` · `downscale` · `guidedCoefficients` · `applyCoefficients` | `downscaleLuminance` · `downscaleAlpha` · `refineAlpha` |
| `SegmentationMask.kt` | — | `maskSubjectAlpha` |
| `ImageSegmentationRepositoryImpl` | — | `toCandidatePairs` · `buildCandidatePair` · `postProcess` · `toForegroundCandidate` |

as-built: `applyAreaOpening` · `postProcessAlpha` · `downscaleLuminance` · `downscaleAlpha` 는
설계 당시 「자체 확인」에 있었으나 실제로는 확인 루프가 없고 하위에 전달만 한다. 구현도 이 넷에는
`Job` 을 꺼내지 않았다 — 위 표는 그 결과를 반영한다.

`refineWithin` 과 `maskSubjectAlpha` 는 `checkCancelled` 를 자기 시그니처에 갖고 있으므로
파라미터 제거 대상이기도 하다.

`downscale` 은 `private inline fun` 이므로 `suspend inline` 이 된다. 값 추출 람다는 `suspend` 가
아니어도 되고 `noinline`·`crossinline` 이 필요한 자리는 생기지 않는다(Kotlin 2.4.10 에서
최소 예제로 확인). `FloatArray(size) { ... }` 같은 인라인 생성자 안에서 확인을 부르는 자리도
깨지지 않는다.

### KDoc

`postProcessAlpha` 의 `@param checkCancelled 행 경계마다 불린다. 이 함수는 코루틴을 모르므로
호출부가 넣어 준다` 는 삭제한다. 대신 본문 설명에 **행 경계마다 취소를 확인하고 취소 시
`CancellationException` 을 던진다**는 한 줄을 남긴다.

이 문장이 없으면 왜 `suspend` 인지가 코드에서 읽히지 않는다. 순수 CPU 루프는 중단 지점이 없어
`suspend` 표시만으로는 취소를 존중한다는 사실이 드러나지 않는다.

### 배제한 대안

**`yield()`** — 리뷰의 원안이다. `Dispatchers.Default` 는 `isDispatchNeeded` 가 참이라
`yield()` 가 예외 없이 재디스패치하고(`Yield.kt` → `SchedulerCoroutineDispatcher.dispatchYield`
→ `CoroutineScheduler.dispatch(fair = true)`), 그 태스크가 큐 꼬리로 들어가 다른 워커에게
훔쳐진다. 원본 알파 배열을 순차로 훑는 루프가 코어를 옮겨 다니며 캐시 지역성을 잃는다.
얻는 것은 같은 디스패처의 다른 코루틴에 대한 공정성인데, 이 후처리는 사용자가 기다리는 전경
작업이라 양보할 이유가 없다.

**`Job` 을 파라미터로 전달** — 시그니처에서 `checkCancelled` 를 지우고 `job: Job?` 을 넣는 안.
전달할 값의 타입만 바뀌고 배관은 그대로 남는다. 이 전환의 유일한 이득이 배관 제거이므로
의미가 없다.

**`currentCoroutineContext().ensureActive()` 를 루프 안에서 매번** — 이 확장은 내부적으로
`get(Job)?.ensureActive()` 라 no-Job 을 조용히 통과시킨다. `.job` 을 쓰는 호이스팅 판본이 같은
일을 하면서 fail-fast 이므로 이쪽을 택할 이유가 없다.

**콜백 유지** — 성능상 근거로는 정당화되지 않는다(「측정」 1번). 남는 근거는 "바꾸지 않는 것이
가장 싸다"뿐인데, 리뷰가 지적한 누락 위험이 실재하고 그 위험을 잡으려고 테스트를 따로 쓰고 있다.

**확인 빈도 스로틀링** — "짧은 작업에서는 확인을 건너뛴다"는 제안. 아래 측정에서 확인 전체가
커널 실행 시간의 한 자릿수 퍼센트에 그쳤고, 카운터와 임계값이 새 튜닝 부채가 된다. 취소 반응
지연도 늘어난다.

### 측정

`downscaleMask` 본문을 그대로 복사해 확인 지점만 바꿔 비교했다. JVM 17 / 12 코어 / 코루틴
1.11.0 / `Dispatchers.Default` 이고, 워밍업 후 반복 측정한 중앙값이다.

| 확인 방식 | 원본 해상도 커널 | 확인만 반복 |
|---|---|---|
| 콜백 `ensureActive`(현행) | 4.08 ms | 0.065 ms |
| `suspend` + `ensureActive`(호이스팅) | 4.02 ms | 0.106 ms |
| `suspend` + `ensureActive`(매번 조회) | 4.47 ms | 0.127 ms |
| `suspend` + `yield` | 7.94 ms | 3.565 ms |
| 확인 없음(기준선) | 4.07 ms | — |

**재현된 결론 둘.**

1. **콜백과 호이스팅은 기준선과 구분되지 않는다.** 확인만 반복하면 콜백이 근소하게 빠르지만
   실제 커널에서는 순서가 뒤집혔다. 이 규모는 JIT 인라이닝 결정에 따라 부호가 바뀐다. 즉
   **성능은 콜백을 유지할 근거가 되지 못한다.**
2. **`yield` 는 실제 커널에서 회당 비용이 몇 배 커진다.** 행마다 실행 스레드가 바뀌는 것을
   기록해 확인했고, 행당 작업량이 작은 축소판 단계일수록 배수가 커진다.

**재현되지 않은 결론 하나.** 위 표에서 "매번 조회"가 커널 안에서 0.45 ms 손해로 보이는데, 같은
비교를 다른 조건(더 큰 판, 컨텍스트 원소 수 증가)으로 재측정했을 때는 그 손해가 나타나지 않았다.
"확인만" 열의 델타(0.021 ms)와 "커널" 열의 델타(0.45 ms)가 같은 횟수에 대해 스무 배 넘게
어긋나는 것도 구조적으로 설명되지 않는다. **호이스팅을 계약으로 삼는 근거는 이 수치가 아니라
「변환 규칙」의 fail-fast 다.**

측정 코드는 저장소 밖 임시 위치에 있고 산출물이 아니다.

## 테스트

### `CountingJob` 헬퍼

확인 호출 수를 세는 테스트 더블을 test 소스셋에 둔다. `Job` 을 실제 `Job` 에 위임하고
`isActive` 를 가로챈다. 이 더블을 커널에 닿게 하는 데 **함정이 넷이고 전부 실험으로 확인했다.**
넷 모두 증상이 "테스트가 조용히 통과한다"이므로 헬퍼 주석에 남긴다.

1. **`withContext(job)` 으로 넣으면 안 된다.** 새 `ScopeCoroutine` 이 만들어져 컨텍스트의 `Job`
   자리를 차지하므로 커널은 그 코루틴을 보고 더블을 못 본다. `Continuation` 의 컨텍스트로 직접
   지정해 코루틴 래퍼 없이 실행한다.
2. **`Job by delegate` 는 `CoroutineContext` 의 `get`·`fold`·`minusKey`·`plus` 까지 위임한다.**
   앞의 셋을 오버라이드해도 `plus` 가 남아 `countingJob + 무엇` 이 위임 대상을 왼쪽에 놓는다.
   넷 다 오버라이드하거나 위임을 버리고 필요한 멤버만 명시 구현한다.
3. **하니스 컨텍스트에 `ContinuationInterceptor` 를 넣으면 안 된다.** 디스패처가 있으면
   `startCoroutine` 이 본문을 비동기로 제출하고 즉시 반환해, 중단 지점이 없는 커널에서도 호출
   수가 0 인 채로 단언이 통과한다. 컨텍스트는 `CountingJob` 단독이 계약이다.
4. **`startCoroutine` 반환 직후 완료를 단언한다.** 위 3번과, 커널에 진짜 중단 지점이 생기는
   경우를 함께 잡는다. 미완이면 실패시키고 "이 하니스는 중단 없는 커널만 검증한다"는 메시지를
   남긴다.

취소를 만들 때는 **위임 `Job` 을 먼저 취소하고 그 다음에 `isActive` 가 거짓을 반환하게 한다.**
`Job.ensureActive()` 는 `if (!isActive) throw getCancellationException()` 인데,
`getCancellationException()` 은 아직 활성인 `Job` 에서 부르면 `CancellationException` 이 아니라
`IllegalStateException` 을 던진다. 순서를 지키면 `isActive` 반환값을 조작할 필요조차 없다.

`Job` 인터페이스에는 `@SubclassOptInRequired(InternalForInheritanceCoroutinesApi::class)` 가
붙어 있어 위임 구현에 `@OptIn` 이 필요하다. 라이브러리가 장래 에러 승격을 예고했으므로 헬퍼
KDoc 에 코루틴 버전을 올릴 때 막힐 수 있다는 것을 남긴다.

### 기존 테스트

`AlphaComponentsTest` · `AlphaPostProcessorTest` · `AlphaRefineTest` · `SegmentationMaskTest` 를
`runTest` 로 전환한다. 커널이 `suspend` 가 되므로 선택지가 없는 기계적 변경이고, 단언과 픽스처는
그대로 둔다. `runTest` 기본 타임아웃은 넉넉하고 현재 픽스처가 작아 느려지거나 불안정해질 위험은
없다.

`kotlinx-coroutines-test` 는 이미 테스트 번들에 있어 새로 추가할 의존성이 없다.

**두 테스트만 기계적 전환이 아니다.**

- `postProcessAlpha_cancelledMidway_propagatesTheCallersThrow` 는 **삭제한다.** 콜백에
  `error("cancelled")` 를 심어 임의의 `IllegalStateException` 이 전파되는지를 보는 테스트인데
  ("순수 커널에는 중단점이 없다. 콜백이 유일한 탈출구다"), 전환 후에는 호출부가 임의 예외를
  주입할 통로 자체가 사라진다. 아래 커널별 취소 테스트가 그 자리를 대신한다.
- `postProcessAlpha_countingCancelledCallback_isCalledPastTheDownscaleStage` 는 `CountingJob`
  으로 옮기되 **현행 하한 단언을 그대로 유지한다.** 이 테스트는 원래 정확한 값 고정이었으나
  `test: checkCancelled 전파 테스트의 단언을 정확한 값 대신 하한으로 완화한다` 커밋이 의도적으로
  하한으로 바꿨다. 근거는 "정확한 값은 단계 구성·순회 입도·픽스처 크기가 바뀌면 기능 회귀 없이도
  깨진다"이고, 이 전환이 바로 단계 구성을 건드리는 변경이라 그 근거가 지금 더 강하게 적용된다.
  값을 되돌리려면 새 근거가 필요하다.

### 커널별 취소 테스트

각 커널이 취소를 존중하는지 함수 단위로 검증한다. `CountingJob` 으로 N 번째 확인에서 취소시키고
`CancellationException` 이 나오는지, 그 시점 이후로 진행하지 않았는지를 본다.

대상은 `downscaleMask` · `applyAreaOpening` · `dilateMask` · `applyKeepMask` · `measureAlpha` ·
`erodeEdge` · `postProcessAlpha` · `boxMean` · `guidedCoefficients` · `applyCoefficients` ·
`refineAlpha` · `maskSubjectAlpha` 다.

전체 파이프라인 카운팅만으로는 회귀가 났을 때 어느 함수인지 특정되지 않는다. 함수 단위 테스트가
그 자리를 지목한다.

### 개별 회귀 그물의 한계

확인 루프가 **없는** 함수(`applyAreaOpening` · `postProcessAlpha` · `maskSubjectAlpha` ·
`refineAlpha`)의 위 커널별 취소 테스트는 **서브트리가 확인을 전부 잃었을 때만** 실패한다. 한
단계만 확인을 잃어도 다음 단계의 확인이 "첫 확인"이 되어 그대로 통과하므로, 이 넷의 취소 테스트는
"함수가 취소를 어떻게든 존중하는가"만 보고 **개별 단계의 회귀는 잡지 못한다.**

개별 단계의 회귀를 잡는 것은 **카운팅 테스트 둘뿐**이다.

- `postProcessAlpha_countingChecks_isCalledPastTheDownscaleStage` — 하한 `> 40`(8×8·배율1 픽스처
  실측 47)
- `refineAlpha_countingChecks_visitsEveryStage` — 하한 `> 60`(4×4·배율1·반경1 픽스처 실측 64,
  지점별 확인 제거 시 56/40/40/60/60)

이 성질을 적어 두지 않으면 다음 사람이 카운팅 테스트를 커널별 취소 테스트와 "중복"으로 보고
지운다. 위임 전용 함수의 회귀 감지선은 이 둘이 유일하다.

## 브랜치 배분

취소 확인 콜백은 세그멘테이션 스택 PR 의 **커널 PR** 에서 도입되어 위 두 단계에서 확장됐다.
리뷰가 달린 곳이자 원본이 있는 커널 PR 에서 고치고 위를 rebase 한다. 스택의 실제 바닥은 그보다
한 단 아래(후보 판정 범위 PR)이지만 그 브랜치에는 이 콜백이 없다.

| 단계 | 작업 |
|---|---|
| 커널 PR | `AlphaComponents.kt` 넷 · `AlphaPostProcessor.kt` 넷 전환, `CountingJob` 신설, `AlphaComponentsTest`·`AlphaPostProcessorTest` 전환(임의 예외 테스트 삭제 포함) |
| 결선 PR | `maskSubjectAlpha` 전환, `ImageSegmentationRepositoryImpl` 의 콜백 생성 제거와 private 넷 전환, `SegmentationMaskTest` 전환 |
| 정련 PR | `AlphaRefine.kt` 일곱 전환, **`AlphaPostProcessor.kt#refineWithin` 전환**, `AlphaRefineTest` 전환 |

⚠️ `refineWithin` 은 커널 PR 에 존재하지 않고 정련 PR 에서 `AlphaPostProcessor.kt` 에 추가된다.
그래서 **정련 PR 이 커널 PR 에서 이미 손댄 파일을 다시 건드린다.** 이 줄을 놓치면 정련 PR 의
rebase 가 컴파일 에러로 막힌다.

`checkCancelled` 가 상위 브랜치 전체에 퍼져 있어 rebase 충돌은 확실히 발생한다. 대부분은
"지워진 파라미터를 계속 넘기는 코드"라 해결이 기계적이지만, `refineWithin` 과 결선 PR 의
private 넷은 시그니처를 새로 `suspend` 로 바꿔야 하므로 기계적이지 않다.

⚠️ rebase 후 force push 가 필요하다. 리모트로 나가는 작업이므로 실행 직전에 승인을 받는다.

> ✅ **as-built** — 셋이 순서대로 rebase 되어 **정련 브랜치 하나로 접힌 채** PR #363 으로 머지됐다.
> 위 표의 배분 자체는 커밋 단위로 그대로 남아 있으나(`refactor: 알파 커널의 취소 확인을 콜백에서
> suspend 로 옮긴다` · `refactor: 결선 경로의 취소 확인을 suspend 로 옮긴다` · `refactor: 정련 커널의
> 취소 확인을 suspend 로 옮긴다`), **`develop` 이 본 머지는 하나**다.

## 미결

- 실기기에서 콜백과 `suspend` 방식의 차이를 잴 필요가 있는가. 측정된 차이가 프로파일러 해상도
  아래라 통상적인 프로파일링으로는 판정되지 않는다. 재려면 마이크로벤치마크 하니스가 필요하고,
  이 크기의 차이를 위해 그것을 세우는 비용이 정당한지는 정하지 않았다.
- 커널 전체가 `suspend` 가 된 뒤 `GuidanceProvider.pixelsIn` 도 `suspend fun interface` 로
  바꾸자는 제안이 나올 수 있다. 이 스펙은 그것을 범위 밖에 둔다. 받아들이면 **커널에 진짜 중단
  지점이 생겨** ① 테스트 하니스의 전제가 깨지고(위 함정 4번의 단언이 이를 잡는다)
  ② `refineElapsedNanos` 가 디스패치 대기까지 포함하는 벽시계 값으로 의미가 달라진다. 지금
  구조에서는 `ensureActive()` 가 중단 지점이 아니라 그 값의 의미가 보존된다.
- **확인 없이 오래 도는 루프가 여전히 남아 있다.** 이 스펙은 「범위 · 제외」에서 확인 지점·빈도
  변경을 범위 밖에 뒀으므로 이 전환에서는 고치지 않고 사실만 남긴다. `AlphaComponents.kt` 의
  `applyAreaOpening` 은 `countRuns` 와 `fillRuns` 로 마스크 전체를 **두 번** 훑은 뒤에야 첫
  확인(`unionAdjacentRows`)에 닿고, union 이후의 성분 집계·마스크 소거 루프에도 확인이 없다.
  `AlphaRefine.kt` 의 `downscale` 마지막 나눗셈 루프와 `guidedCoefficients` 의 배열 생성
  람다도 같다. 큰 판에서는 첫 확인 전에 전체 두 패스가 그대로 지나가므로, 확인 지점을 더
  촘촘히 할지는 이 스펙과 별도의 판단이 필요하다.
