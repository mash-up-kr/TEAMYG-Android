---
id: canvas-today-ssot-polling
title: 오늘 캔버스 인메모리 SSoT · 배경 탭 토핑 렌더링 · 주기 폴링 (Canvas Today SSoT & Polling)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-31
related_code: CanvasBGEditScreen, CanvasBGEditViewModel, CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect, CanvasBGEditError, CanvasBGEditRoute, YGScaffoldV2, CanvasToppingItem, CanvasMainViewModel, CanvasMainUiState, CanvasMainIntent, CanvasMainScreen, CanvasToppingLayer, CanvasToppingPlaceViewModel, CanvasToppingPlaceUiState, BaseViewModel, ParfaitRepository, ParfaitRepositoryImpl, ParfaitRemoteDataSource, CanvasLocalDataSource, CanvasLocalDataSourceImpl, CanvasPoller, GetTodayParfaitFlowUseCase, RefreshTodayParfaitDetailUseCase, RequestTodayParfaitRefreshUseCase, ObserveTodayParfaitRefreshFailureUseCase, ObserveParfaitDayBoundaryUseCase, CanvasPoller, GetParfaitDetailUseCase, CanvasVO, CanvasToppingVO, ToppingTransform, ToppingDraft, ToppingDraftRepository, AddToppingUseCase, UpdateToppingUseCase, DeleteToppingUseCase, ChangeCanvasBackgroundUseCase, LogoutUseCase, TokenAuthenticator, ParfaitDay, parfaitToday
related_adr: ADR-0029, ADR-0023, ADR-0026, ADR-0025, ADR-0020, ADR-0009
related_spec: group-ssot, c001-canvas-today-detail, c001-canvas-main, c106-topping-place, c201-canvas-calendar-server, c202-canvas-spotlight, c301-canvas-background-edit, c301-topping-edit-tab, screen-resume-refetch, server-delta-nametag-chip-day-boundary
related_architecture: data-layer, state-management
supersedes:
superseded_by:
tags: [spec, parfait, canvas, state, cache, polling]
---

# Spec: 오늘 캔버스 인메모리 SSoT · 배경 탭 토핑 렌더링 · 주기 폴링

> ✅ **develop 에 머지됐다(2026-08-31, PR #404 `2c7bb31b`)** — 스택 3단(PR1·PR2·PR3)이 한 머지로
> 들어왔고 머지 커밋의 트리가 브랜치 팁(`6bd21fb7`)과 같아 **충돌 해소 편집이 0건**이다. 아래
> as-built 서술은 그대로 develop 의 코드다. [ADR-0029](../../../adr/0029-canvas-today-ssot-polling.md) 도
> 같은 날 `accepted` 가 됐다.

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> 🔁 **검수 반영본(2026-08-27)** — 초판을 서브에이전트 셋이 검수해 치명 3건·중대 10건이 나왔고
> 그 결과 설계가 세 축에서 바뀌었다. ① **폴링 수명**을 "구독자가 사라지면 저절로 멎는다"에서
> **`state.subscriptionCount` 기반 배선 신설**로 바꿨다 — 이 저장소에는 `stateIn`·`WhileSubscribed`가
> 한 건도 없고 구독이 ViewModel 수명에 걸리도록 규약(`architecture/state-management.md`)이 서 있어,
> 초판대로면 카메라 흐름은 물론 앱이 백그라운드에 있어도 폴링이 계속 돌았다. ② **폴링 경로**를
> 부작용 있는 오늘 조회에서 **부작용 없는 상세 조회**로 바꿨다(오늘 조회는 최초 획득과 하루
> 경계에만). ③ **하루 경계 판정**을 `Flow` 필터 하나에서 **명시적 시간 축(티커)**으로 바꿨다 —
> 캐시가 조용하면 재방출이 없어 필터가 아예 평가되지 않았다.

> ✅ **첫 조회 덮개를 잇는 답이 정해졌다(2026-08-30, 스택 3단을 `27e85d0d` 위로 리베이스).**
> `loadTodayCanvas()` 의 조회를 구독으로 바꾸는 자리가 PR #407 의 **첫 조회 덮개**
> (`CanvasMainUiState.isInitialLoading`)와 부딪혔고, 리베이스 충돌을 푸는 자리에서 닫혔다.
> 덮개는 구독 안에서 파생하되(`try`/`finally` 가 아니다), 그것만으로는 부족해
> **폴러가 갱신 실패를 내보내는 표면을 새로 만들었다** — 아래 「실패 표현」 참고.
> → [open-questions](../../../synthesis/open-questions.md) OQ-P-326 ⑥.

## 목표

같은 오늘 캔버스를 쓰는 세 화면이 지금은 각자 서버를 부르고, 받은 값을 각자의 `UiState`에만
둔다. 이 스펙은 그 값을 `:data`의 인메모리 저장소 한 벌로 옮겨 세 화면이 구독하게 하고, 그
저장소 위에 주기 폴링을 얹어 다른 멤버가 올린 토핑이 화면을 나갔다 오지 않아도 나타나게 한다.
함께, 배경 편집 화면의 배경 탭에서 토핑이 아예 그려지지 않던 것을 고친다.

지금 어긋나 있는 것은 셋이다.

- **같은 사실을 세 번 부른다.** `CanvasMainViewModel`·`CanvasBGEditViewModel`·
  `CanvasToppingPlaceViewModel`이 각자 `GetTodayParfaitUseCase(groupId)`를 부르고, 결과를 각자의
  모양(`CanvasVO` / `CanvasToppingItem` / `CanvasToppingVO`)으로 따로 매핑한다. 세 화면이 공유하는
  지점은 없다.
- **배경 탭에서 토핑이 사라진다.** `CanvasBGEditScreen`이 토핑 레이어 전체를 `selectedTab ==
  CanvasEditTab.TOPPING` 조건으로 감싸고 있어, 배경을 고르는 동안에는 그 캔버스에 무엇이 올라가
  있는지 보이지 않는다. 배경은 토핑과 함께 보고 골라야 하는 값이다.
- **남이 올린 토핑이 늦게 온다.** 갱신 시점이 화면 재진입(`Enter`)뿐이라, 캔버스를 열어 둔 채로는
  다른 멤버의 토핑이 영영 나타나지 않는다.

그룹 정보는 이미 같은 문제를 인메모리 SSoT로 풀었다([ADR-0023](../../../adr/0023-group-in-memory-ssot.md),
[group-ssot](2026-08-17-group-ssot.md)). 그 ADR은 "폴링을 붙일 자리가 저장소 한 곳으로
정해진다"고 적어 두었고, 이 스펙이 그 자리를 캔버스에서 실제로 채운다.

## 범위

**포함**

- `:data`에 `CanvasLocalDataSource`(인메모리) 신설, `ParfaitRepositoryImpl`이 원격·로컬을 조율
- 오늘 캔버스 조회 UseCase를 구독용(`Flow`)과 갱신용(`suspend`)으로 분리
- 세 ViewModel을 구독 방식으로 이관
- 배경 편집 화면의 배경 탭에서 토핑을 반투명·비상호작용으로 렌더링
- `:data`에 `CanvasPoller` 신설과, 구독 수에 수명을 매다는 `BaseViewModel` 배선
- 폴링이 편집 중인 배치·배경 선택을 덮지 않게 하는 병합 규칙
- **쓰기(배경 저장·토핑 추가·토핑 삭제) 성공 뒤 강제 갱신 1회와, 그때의 폴링 주기 재시작**
- 파르페 하루 경계를 시간 축으로 들여 오늘 판정을 갱신
- 토핑 배치 확정 시점의 `positionZ` 재계산
- 세션 종료 시 캔버스 캐시 정리와 폴링 중단
- **(2026-08-28 추가)** 배경 편집 화면의 확인·삭제에 진행 표시와 실패 안내를 달고, 삭제는
  성공했을 때만 화면을 닫게 한다

**제외**

- **지난 날 캔버스의 캐시화.** 저장소가 소유하는 것은 **그룹별 오늘 캔버스 한 벌**뿐이다. 달력으로
  고른 지난 날 상세는 지금처럼 `CanvasMainViewModel`이 조회해 자기 상태에 들고 있는다. 마감된
  날은 바뀌지 않아 공유해 얻을 것이 없고, `Map<ParfaitId, CanvasVO>`로 넓히면 무효화 규칙이
  날짜 축까지 따라온다.
- **영속.** 앱을 껐다 켜면 캐시는 비어 있고 첫 조회를 기다린다(ADR-0023과 같은 판단).
- **푸시 기반 갱신.** 폴링만 다룬다.
- **토핑 테두리의 저장 경로.** 테두리 PATCH는 아직 소비처가 없다(OQ-P-276). 이 스펙은 그 공백을
  닫지 않고 승계한다.
- **`CanvasEditRoute`·`CanvasImageSelectRoute`·`CanvasMoveRoute`.** 도달 불가로 남은 유휴 화면이며
  이미 OQ-P-239로 등록돼 있다.
- **토핑 탭의 z 순서 왜곡.** 토핑 탭은 남의 토핑 전부를 그린 뒤 딤을 얹고 내 토핑을 그려서, 저장된
  `positionZ` 순서가 아니라 **내 것이 항상 위**다. 선택 UI의 요구이고 이번 범위가 아니다.
- **Compose UI 테스트 하니스.** build-logic에 `parfait.test.compose` 관례 플러그인이 이미 있어 비용이
  큰 일은 아니지만, PR1은 ViewModel을 건드리지 않는 순수 렌더링 변경이라 계측 테스트의 값이 낮다.
  `@YGPreview`와 수동 확인으로 대신한다.

## 스택 PR 구성

세 단계를 스택 PR로 쌓는다. 아래 순서는 의존 방향이 아니라 **머지 순서**다 — PR1은 화면
단독 변경이라 PR2·PR3과 겹치는 파일이 사실상 없고, 가장 작아 먼저 나간다.

| 단계 | 내용 | 주로 건드리는 곳 |
|------|------|------------------|
| PR1 | 배경 탭 토핑 렌더링 | `CanvasBGEditScreen` |
| PR2 | 오늘 캔버스 인메모리 SSoT | `:data` 저장소·`ParfaitRepository`·UseCase·세 ViewModel |
| PR3 | 폴링·병합 규칙·하루 경계 티커·`positionZ` 재계산 | `CanvasPoller`·`BaseViewModel`·세 ViewModel |

> **계획 단계에서 더 쪼개도 된다.** `writing-plans`로 구현 계획을 세울 때 한 단계가 너무 커진다고
> 판단되면 `PR1-1`·`PR1-2`처럼 하위 번호로 나눠 스택을 늘려도 된다. 위 표는 최소 경계이지 상한이
> 아니다. 나눌 때 지켜야 할 것은 두 가지다 — **각 PR이 혼자서 빌드·테스트를 통과할 것**, 그리고
> **아래 각 절이 정한 결정을 하위 PR들이 나눠 갖되 바꾸지는 말 것**.

---

## PR1 — 배경 탭 토핑 렌더링

### 목표 동작

배경 탭에서도 그 캔버스에 올라간 토핑이 전부 보인다. 내 것과 남의 것을 가리지 않고 **불투명도
0.5**로 그리며, **어떤 터치 이벤트도 받지 않는다**. 배경을 고르는 화면이지 토핑을 고르는 화면이
아니므로, 토핑은 배경 선택의 참고로만 존재한다.

### 무엇을 바꾸는가

`CanvasBGEditScreen`에서 토핑 레이어를 감싸고 있는 `selectedTab == CanvasEditTab.TOPPING` 조건을
걷어내고, **그리기와 상호작용을 분리**한다. 두 탭이 공유하는 것은 저장된 배치대로 토핑 이미지를
겹쳐 그리는 부분뿐이고, 그 위에 얹히는 것은 전부 토핑 탭 전용이다.

배경 탭에서 붙이지 않는 것을 열거한다.

- 선택 UI의 `YGAtomicColors.Transparency.Black25` 딤 오버레이. 이것은 "남의 토핑과 내 토핑을
  가르는 층"이지 반투명 표현이 아니다.
- 탭·드래그 입력 레이어(`toppingTapInput`·`toppingDragInput`).
- 선택된 토핑의 `ToppingSelectionStroke`와 `ToppingCornerButtons`.
- `CanvasToppingImage`의 `semantics { role = Role.Button; onClick { … } }`. 접근성 서비스에도
  버튼으로 보이면 안 된다 — 실제로 누를 수 없기 때문이다.

배경 탭에서는 `rememberBGEditHitEntries`와 그 안의 `rememberToppingAlphaMasks`를 **호출하지
않는다.** 마스크는 탭 판정에만 쓰이고 그리기에는 `painter`만 있으면 되는데, 마스크 준비는 비트맵
디코딩을 동반한다. 두 탭이 공유할 그리기용 최소 정보(`painter`·중심점·크기·회전)는 별도 함수로
뽑아, 토핑 탭은 그 위에 판정 정보를 얹는 형태로 둔다.

### 불투명도

`CanvasToppingImage`에 `alpha: Float = 1f` 파라미터를 더해, 이미 있는
`graphicsLayer(rotationZ = …)` 호출에 함께 넘긴다. 배경 탭이면 `0.5f`, 토핑 탭이면 `1f`다.
값은 이 화면 파일의 이름 있는 상수로 둔다.

레이어 하나에 `alpha`를 주므로 토핑 이미지와 그 테두리(`YGToppingCutoutImage`가 그리는
`ToppingOutline` 스탬프)가 함께 반투명해진다. 이미지와 테두리가 각각 반투명해져 겹치는 자리만
진해지는 일은 생기지 않는다.

### 배치·크기

토핑의 위치·크기는 전부 Canvas-Area 대비 비율이다(`CanvasToppingItem` 문서 참고). 두 탭에서
캔버스 박스를 감싸는 상하 패딩이 다르지만, 두 탭 모두 캔버스 박스를 같은 방식으로
(`fillMaxWidth` + 좌우 인셋 + `aspectRatio(CANVAS_AREA_ASPECT_RATIO)`) 잡으므로 박스 크기가 달라져도
토핑 자리는 비율로 따라온다. 별도 보정이 없다.

**예외는 테두리 두께다.** `YGToppingCutoutImage`의 `borderWidth`는 화면 기준 dp라 토핑을 키워도
굵기가 그대로다(해당 KDoc). 두 탭에서 박스 실측 크기가 달라지는 기기에서는 토핑 대비 테두리
비중이 미세하게 달라진다. 캔버스 메인과 편집 화면 사이에 이미 있는 성질이라 회귀가 아니다.

### 검증

`@YGPreview`를 배경 탭·토핑 탭 각각으로 늘려 눈으로 확인한다. ViewModel은 건드리지 않으므로
기존 `CanvasBGEditViewModelTest`는 그대로다.

---

## PR2 — 오늘 캔버스 인메모리 SSoT

### 저장소 구조

`CanvasLocalDataSource`는 IO가 없으므로 모든 함수가 non-suspend다.

```kotlin
interface CanvasLocalDataSource {
    /** 미조회면 null. 오늘 캔버스는 서버가 없으면 만들어 주므로 "0건"이라는 상태가 없다 */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    fun saveTodayCanvas(groupId: GroupId, canvas: CanvasVO)

    fun clear()
}
```

구현은 `@Singleton` + `MutableStateFlow<Map<GroupId, CanvasVO>>` 하나이고, 읽기는 그 맵을 한
그룹으로 좁혀 `distinctUntilChanged`를 건다. `GroupLocalDataSourceImpl.groupDetail`과 같은
이유다 — 좁히지 않으면 남의 그룹 캔버스가 저장될 때마다 이 구독자까지 재방출된다.

`CanvasVO`에 `groupId`가 없어 저장 함수가 그것을 따로 받는다.

### Repository

`ParfaitRepository`에서 기존 `suspend fun getTodayCanvas(groupId): Result<CanvasVO>`를 **없애고**
넷으로 가른다.

```kotlin
fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

/** 오늘 조회. ⚠️ 캔버스가 없으면 서버가 만들어 저장한다 */
suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit>

/** 상세 조회로 오늘 캔버스 캐시를 갱신한다. 부작용이 없다 */
suspend fun refreshTodayCanvasDetail(groupId: GroupId, parfaitId: ParfaitId): Result<Unit>

fun clearTodayCanvas()
```

갱신 함수가 `Result<Unit>`만 돌려주는 것은 ADR-0023이 세운 규칙을 그대로 따르는 것이다 — 값을
얻는 길이 둘이면 캐시는 곧 두 번째 출처가 된다. `clearTodayCanvas()`가 필요한 이유는 세션 정리를
부르는 `LogoutUseCase`가 `:domain`에 있어 `:data`의 `CanvasLocalDataSource`를 직접 볼 수 없기
때문이다(`ParfaitGroupRepository.clearGroups()`와 같은 형태).

기존 `getCanvasDetail`은 그대로 둔다 — 달력으로 고른 지난 날 조회이고 캐시를 건드리지 않는다.
`refreshTodayCanvasDetail`과 같은 엔드포인트를 부르지만 저장 여부가 다르므로 표면을 갈라 둔다.
`getYears`·`getPastCanvases`·`changeCanvasBackground`도 그대로다.

> 🔧 **as-built**: 오늘 캔버스 관련 표면은 최종적으로 위 넷이 아니라 아래 넷이다 — 개수는
> 같지만 구성이 다르다.
>
> - `refreshTodayCanvas(groupId)`는 **없앴다.** PR2가 만든 "하루 경계를 넘겨 받으면 한 번 더
>   조회"하는 이 표면은 PR3에서 프로덕션 호출처가 0건이 됐다 — 폴러의 "캐시 날짜가 오늘이
>   아니면 오늘 조회"가 다음 주기(최대 5초)에 같은 결과를 낸다. 함께 만들었던
>   `cachedTodayCanvasDate(groupId): LocalDate?`(캐시 날짜 peek 용 표면)도 그 유일한
>   소비처였던 `RefreshTodayParfaitUseCase`와 함께 지웠다.
> - `requestTodayCanvasRefresh(groupId)` — 되감기 직전처럼 응답을 기다릴 수 없는 자리에서
>   부르는 비동기 갱신 표면이다(PR3, `CanvasToppingPlaceViewModel`의 확인 처리). 즉시
>   반환하고 실제 갱신은 폴러의 스코프에서 끝까지 간다. `refreshTodayCanvasDetail`의 async
>   판이다 — 둘 다 결국 `CanvasPoller.refreshNow(groupId)` 한 줄로 수렴한다.
>
> - `todayCanvasRefreshFailures(groupId): Flow<Unit>` — 폴러의 갱신 **실패**만 흘리는 표면이다
>   (2026-08-30 리베이스에서 신설). 값은 싣지 않아 "값을 얻는 길은 `todayCanvas` 하나"라는
>   ADR-0023 규칙은 그대로다. 첫 조회를 기다리는 화면이 덮개를 내릴 계기로만 쓴다 — 갱신이
>   실패하면 캐시가 아무것도 방출하지 않아, 구독만 보고 있으면 덮개가 풀릴 자리가 없다.
>
> 최종 다섯: `todayCanvas`(구독) · `todayCanvasRefreshFailures`(실패 구독) ·
> `refreshTodayCanvasDetail`(suspend 갱신) · `requestTodayCanvasRefresh`(async 갱신) ·
> `clearTodayCanvas`(세션 정리).

### 갱신·무효화 규칙

캐시가 바뀌는 시점은 아래 여섯뿐이다.

| 시점 | 캐시 동작 | 실패 시 |
|------|-----------|---------|
| 최초 획득 (캐시가 비었을 때) — 오늘 조회 | 그 그룹 엔트리 저장 | 캐시 불변, `Result.failure` |
| 폴링 주기가 캐시 날짜를 확인해 오늘이 아님을 본다 — 오늘 조회 | 같음 | 캐시 불변, 다음 주기가 재시도 |
| 폴링 주기 갱신(캐시 날짜가 오늘) — 상세 조회 | 같음 | 캐시 불변, 조용히 다음 주기 |
| 쓰기 성공 뒤 강제 갱신 — 상세 조회 | 같음 | 캐시 불변, 다음 주기가 덮는다 |
| 폴링 시작(구독 0 → 1) 즉시 1회 | 위 규칙대로(캐시 유무·캐시 날짜가 오늘인지에 따라) | 같음 |
| 로그아웃·강제 로그아웃 | `clearTodayCanvas()` | — |

**쓰기 성공 뒤 강제 갱신을 넣는 이유**는 되감기와 화면 갱신 사이의 빈틈을 없애기 위해서다.
배경 저장·토핑 추가·토핑 삭제가 성공하면 그 결과를 폴링 주기만큼 기다리지 않고 즉시 한 번
가져온다. 그 갱신도 폴러를 통과하므로 **주기가 그 시점부터 다시 세어진다**(아래 「폴링」 참고).

### UseCase

`GetTodayParfaitUseCase`는 **하루 경계에서 어제 캔버스를 받으면 한 번만 다시 부르는** 판단을
들고 있다. 그 판단은 갱신 쪽에 남는다.

- `RefreshTodayParfaitUseCase` — 기존 `GetTodayParfaitUseCase`의 내용을 그대로 옮기고 반환만
  `Result<Unit>`으로 바꾼다. 성공하면 저장소에 실린다.
- `GetTodayParfaitFlowUseCase` — `repository.todayCanvas(groupId)`를 흘리되, `canvas.date`가 오늘이
  아니면 null로 거른다. `invoke`가 `clock: Clock = Clock.System`을 받는다 —
  `RefreshTodayParfaitUseCase`가 이미 그렇고, 시계를 주입하지 않으면 이 필터를 테스트로 고정할 수
  없다(`server-delta-nametag-chip-day-boundary`가 같은 이유로 그 파라미터를 들였다).

> ⚠️ **이 필터만으로는 하루 경계를 못 막는다.** 필터는 업스트림이 방출할 때만 평가되는데 캐시에
> `distinctUntilChanged`가 걸려 있어, 활동 없는 그룹의 캔버스를 열어 둔 채 경계를 넘기면 재방출이
> 없어 필터가 아예 돌지 않는다. 실제 보장은 PR3의 `CanvasPoller`가 만든다 — 티커가 아니라
> **캐시에 실린 날짜가 오늘인지**를 매 주기 다시 본다(「폴링을 어디에 두는가」 참고).
> **PR2 단계에서는 기존과 같이 `Enter` 시점에만 오늘 판정이 갱신된다** — 이 절을 PR2의 검증
> 기준으로 쓰지 말 것.

### 화면 이관

**`CanvasMainViewModel`**

`loadTodayCanvas()`의 조회가 구독으로 바뀐다. 진입 재조회(`CanvasMainIntent.Enter`)는 PR2에서는
그대로 남되 `RefreshTodayParfaitUseCase`를 부른다. PR3에서 폴러의 즉시 1회 조회가 그 역할을
가져가면서 이 호출은 사라진다(아래 「폴링」 참고).

지금 `todayCanvas`와 `viewedCanvas` 두 필드가 오늘을 볼 때는 같은 값을 들고 있다. 이관하면서
이 중복을 없앤다 — `todayCanvas`는 구독 값이고, 지난 날 캔버스를 담는 필드는 `pastCanvas`이며,
화면이 그리는 것은 파생값이다.

```kotlin
val displayedCanvas: CanvasVO?
    get() = if (isViewingToday) todayCanvas else pastCanvas
```

`canvasBackground`·`toppings`·`isCanvasEmpty`·`spotlightedTopping`이 전부 이 파생값을 본다.
`syncToday()`는 날짜·달력 위치만 옮기고 캔버스를 비우는 일은 하지 않는다.

이름을 바꾸는 이유는 의미가 뒤집히기 때문이다. `viewedCanvas`는 `c201-canvas-calendar-server`가
"지금 화면에 그려지는 캔버스"로 정의한 필드였고, `pastCanvas`는 지난 날 전용이다. 지금 그려지는
것은 `displayedCanvas`가 맡는다.

`memberChips`는 구독 값이 바뀔 때마다 다시 세우고, 색은 서버가 배정한 값을 그대로 쓰는 기존
규칙을 유지한다.

**실패 표현.** `todayCanvas`가 `null`인 것은 "아직 못 받음"과 "갱신 실패"를 합친 상태다. 화면은
둘을 구분하지 않고 기존 규칙을 그대로 쓴다 — 토핑 추가 버튼 비활성(ADR-0026), 보여 줄 캔버스가
없을 때만 `ShowTodayCanvasError`. **폴링 실패는 어디에도 표현하지 않는다** — 사용자가 시키지
않은 조회이고, 보여 줄 캐시 값이 이미 있다(`group-ssot`이 `Enter` 재조회 실패를 조용히 넘기는
것과 같은 판단).

> 🔧 **as-built(2026-08-30 리베이스) — 「첫 조회를 기다리는 동안」만 예외다.** 위 규칙은 캐시
> 값이 이미 있다는 전제 위에 서 있는데, 첫 조회는 그 전제가 깨진 자리다. 갱신 하나가 실패하면
> 캐시가 아무것도 방출하지 않아 덮개(`isInitialLoading`)가 풀릴 계기가 없고, 화면이 로딩에
> 갇힌다. 그래서 폴러가 `todayCanvasRefreshFailures` 로 실패를 내보내고 화면이 그것을 받는다.
>
> **덮개를 켜고 내리는 자리는 셋이다.** ① 구독이 열릴 때 `todayCanvas` 가 `null` 이면 켠다
> (`onStart`). 이미 받아 둔 캔버스가 있으면 켜지 않는다 — 그러지 않으면 화면에 다시 붙을
> 때마다 그려진 캔버스 위로 덮개가 번쩍인다. ② 구독이 캔버스를 실어 오면 내린다.
> ③ 실패 신호가 오면 내리고 `ShowTodayCanvasError` 를 낸다.
>
> ⚠️ **실패 신호를 받는 조건은 「덮개가 걸려 있을 때」로 좁힌다.** 폴링은 5초마다 돌아 조건 없이
> 알리면 실패가 이어지는 동안 토스트가 쌓인다. 이 가드가 위 「폴링 실패는 표현하지 않는다」를
> 그대로 지킨다 — 덮개가 없다는 것은 보여 줄 캔버스가 이미 있다는 뜻이다.

**`CanvasBGEditViewModel`**

`loadCanvas()`가 구독으로 바뀐다. **편집을 연 캔버스와 구독 값의 `parfaitId`가 다르면 구독
쪽으로 옮기는** 기존 판단은 살리되 **최초 방출에만** 적용한다 — 매 방출마다 돌면 편집 중에
저장 대상이 조용히 옮겨간다.

PR2 단계의 병합 규칙은 **통째 대입**이다. 이 단계엔 폴링이 없어 방출 계기가 최초 로드뿐이고,
지켜야 할 로컬 편집이 없다. PR3이 이 자리를 병합 규칙으로 대체한다.

**`CanvasToppingPlaceViewModel`**

`loadCanvasIfNeeded(groupId)`와 `canvasLoadedForGroupId` 가드가 사라지고, 초안이 알려 준
`groupId`로 구독한다. 캔버스를 못 받아도 토핑 배치 자체는 막지 않는 기존 규칙(기본 배경·빈 토핑
목록으로 그대로 둔다)을 유지하며, **`null` 방출은 무시하고 마지막 값을 지킨다** — 비우면 배경이
흰색으로 튄다.

### 세션 종료 정리

인메모리라 프로세스가 살아 있는 계정 전환에서 이전 계정의 캔버스가 남는 것이 실제 위험이다.
`LogoutUseCase`가 그룹 캐시를 지우는 자리에서 `ParfaitRepository.clearTodayCanvas()`를 함께 부르고,
강제 로그아웃 경로(`TokenAuthenticator`)는 `CanvasLocalDataSource.clear()`를 직접 부른다(그쪽은
`:data`라 로컬 데이터소스를 바로 본다). 정리 순서는 group-ssot이 정한 as-built를 따른다 — 계정
정보 정리는 DataStore IO라 던질 수 있으므로 **인메모리 캐시 정리를 그 앞에 둔다.**

> 🔧 **as-built**: `clearTodayCanvas()`는 `clear()` 단독이 아니라 **`CanvasPoller.stopAll()`이
> 먼저다.** 순서가 반대면(먼저 비우고 나중에 폴러를 세우면) 이미 출발한 갱신 응답이 도착해
> 방금 비운 캐시를 되살릴 수 있다 — PR3의 세대(`generation`) 메커니즘이 이 순서에 의존한다
> (「폴링을 어디에 두는가」·「세션 정리와 진행 중인 갱신」 참고).

### 검증

`CanvasLocalDataSourceImpl`은 미조회(`null`)와 저장 후 값을 구분해 단언하고, 한 그룹의 저장이
다른 그룹 구독자를 재방출시키지 않는 것을 고정한다. `GetTodayParfaitFlowUseCase`는 주입한 시계로
날짜가 어긋난 캔버스를 null로 거르는 것을 고정한다. 세 ViewModel 테스트는 조회 목킹을 구독
목킹으로 바꾸고, 배경 편집의 `parfaitId` 이동이 최초 방출에만 도는 것을 단언한다.

---

## PR3 — 폴링 · 병합 규칙 · 하루 경계 · `positionZ` 재계산

### 폴링을 어디에 두는가

**폴링 로직은 `:data`의 `CanvasPoller`(`@Singleton`) 하나가 소유한다.** 저장소는 값의 소유자로
두고 트리거는 트리거대로 갈라야, 나중에 푸시로 갈아 끼울 때 바뀌는 자리가 하나로 남는다.
`CanvasPoller`가 `CanvasLocalDataSource`와 패키지(`local/`)를 같이 쓰는 것은 위치가 겹쳐서일 뿐,
`CanvasPoller` 자신은 로컬 데이터소스가 아니다 — 값은 `CanvasLocalDataSource`가 들고,
`CanvasPoller`는 그 값을 언제 다시 받을지 정하는 **트리거 소유자**다.

`CanvasPoller`는 `@Singleton CoroutineScope`(`SupervisorJob + Dispatchers.IO`)를 주입받고, 그룹별로
참조 계수와 폴링 `Job`을 든다. **계수 조작은 코루틴 `Mutex`가 아니라 스레드 락인 `synchronized`로
보호한다.** 이유는 둘이다 — `release`가 `Flow`의 `onCompletion`에서 불리는데 그 블록은 **취소된
코루틴에서 돈다**, 거기서 `Mutex.withLock`처럼 서스펜드하면 취소된 코루틴이라 재개되지 않아
계수가 안 내려간다. 또 `stopAll()`은 `TokenAuthenticator`가 OkHttp 스레드의 `runBlocking` 안에서
부르므로, 코루틴 전용인 `Mutex`가 아니라 어느 스레드에서도 먹는 락이 필요하다.

- 계수가 0 → 1이 되면 **즉시 1회** 갱신하고 이어서 주기 루프에 든다.
- 계수가 1 → 0이 되면 그 그룹의 `Job`을 취소한다.
- 진행 중인 갱신이 있으면 **이번 주기를 건너뛴다.** 강제 갱신도 같은 가드를 지난다.
- 주기 갱신이 `Result.failure`면 조용히 넘기고 다음 주기를 기다린다.
- **갱신이 나갈 때마다 주기를 처음부터 다시 센다.** 쓰기 뒤 강제 갱신·하루 경계 전환·구독 시작
  즉시 조회가 전부 폴러를 통과하므로, 갱신 직후에 주기 타이머가 또 터지는 일이 없다.

주기는 5초이고 이름 있는 상수 하나로 둔다.

**부르는 엔드포인트는 상황에 따라 갈린다.** `CanvasPoller`는 하루 경계 티커를 구독하지 않는다 —
**캐시에 실린 날짜가 오늘인지**만 보고 오늘 조회 여부를 정한다. 경계를 넘기면 캐시의 날짜가
저절로 어제로 남으므로, 별도 신호 없이 다음 주기가 그 사실만으로 오늘 조회를 고른다.

| 상황 | 부르는 것 |
|------|-----------|
| 캐시가 비었다(최초 획득) | 오늘 조회 |
| 캐시에 실린 날짜가 오늘이 아니다(하루 경계를 넘겼다) | 오늘 조회 |
| 그 밖의 모든 갱신 | 상세 조회(groupId, 캐시의 parfaitId) |

> 🔧 **as-built**: 이 표는 "무엇을 부르는가"를 다루지 "누가 누구를 부르는가"가 아닌데, 애초
> 표현이 뒤집혀 읽혔다. **실제 호출 방향은 반대다.** `ParfaitRepositoryImpl`의
> `refreshTodayCanvasDetail`·`requestTodayCanvasRefresh`가 `CanvasPoller.refreshNow(groupId)`를
> 부르고, `CanvasPoller`가 저장소를 거치지 않고 `ParfaitRemoteDataSource.getTodayCanvas`·
> `getCanvasDetail`을 **직접** 부른다 — 저장소 표면 이름(`refreshTodayCanvas`·
> `refreshTodayCanvasDetail`)이 폴러 내부에서 다시 불리는 일은 없다(`refreshTodayCanvas`
> 표면 자체도 이후 없앴다, 위 Repository 절의 as-built 참고). 위 표는 폴러가 그 갈래에서
> 실제로 여는 원격 호출을 가리킨다.

호출부가 `refreshTodayCanvasDetail`에 넘기는 `parfaitId`는 "지금 캐시에 실린 그 캔버스"를
갱신하겠다는 **의도 표시**일 뿐이다. 실제로 무엇을 갱신할지는 호출 시점에 **캐시가 다시** 정한다
(위 표의 갈래 자체가 캐시를 다시 읽어 판단한 결과다) — 구현에서 이 파라미터는 완전히 무시된다.

오늘 조회는 서버에 해당 날짜 파르페가 없으면 **만들어 저장하는 부작용**이 있고, 그 날이 캘린더·
연도 목록에 즉시 나타난다(`api/parfait.md`). 주기 갱신마다 그것을 부르면 아무도 안 쓴 날의
캔버스가 생기고, 경계 직후 그 그룹의 여러 클라이언트가 동시에 생성을 태운다. 캔버스를 만들
필요가 있는 것은 최초 획득과 캐시 날짜가 오늘이 아닐 때뿐이므로 그 둘에만 남긴다.

**진입 시점의 명시적 갱신 호출은 세 화면 모두에서 사라진다.** `CanvasMainViewModel`의 `Enter`
갱신 제거(아래 「폴링 수명을 무엇에 매다는가」)뿐 아니라, `CanvasBGEditViewModel`의 `loadCanvas()`와
`CanvasToppingPlaceViewModel`의 `loadCanvasIfNeeded(groupId)`도 별도 조회를 걸지 않고 구독만
연다 — 구독이 붙는 순간(계수 0 → 1) 폴러가 이미 즉시 1회를 부르므로, 화면이 또 걸면 갱신이
두 번 나간다.

### 폴링 수명을 무엇에 매다는가

**폴러의 참조 계수는 화면이 실제로 보고 있는 동안에만 올라간다.** 이 저장소에는 지금
`stateIn`·`SharingStarted`·`WhileSubscribed`가 한 건도 없고, `architecture/state-management.md`가
"구독은 ViewModel 수명에 걸린다"를 규약으로 못 박고 있다. `Navigator.goTo`는 백스택에 쌓기만
하므로, 그대로 두면 카메라·갤러리 흐름 내내는 물론 앱이 백그라운드에 있어도 폴링이 계속 돈다.

그래서 **배선을 새로 세운다.** `BaseViewModel`에 노출한 `state`의 구독자 수에 수명을 매다는
헬퍼를 더한다.

```kotlin
protected fun <T> launchWhileSubscribed(
    stopTimeout: Duration = SUBSCRIPTION_STOP_TIMEOUT,
    source: () -> Flow<T>,
    collector: suspend (T) -> Unit,
): Job
```

라우트는 이미 `collectAsStateWithLifecycle()`로 `state`를 구독한다. 그래서 화면이 백그라운드로
가거나 컴포지션에서 빠지면 구독자 수가 0이 되고, 이 헬퍼가 여는 업스트림 구독도 함께 끊긴다.
정지 유예(`stopTimeout`)를 두는 이유는 화면 전환과 구성 변경의 짧은 공백을 건너뛰기 위해서다 —
유예가 없으면 캔버스 메인에서 배경 편집으로 옮기는 사이 계수가 0을 찍고, 새 구독이 붙을 때
즉시 조회가 한 번 더 나간다.

`ParfaitRepositoryImpl.todayCanvas(groupId)`가 `onStart`·`onCompletion`에서 폴러의 계수를
올리고 내린다. 화면은 폴러의 존재를 모른다.

이 배선은 `core:ui`의 공용 표면이므로 `architecture/state-management.md`에 규약 한 절을 함께
더한다 — 기존 "구독은 ViewModel 수명에 걸린다"와 나란히 "화면이 보는 동안만 살아야 하는 구독은
이 헬퍼를 쓴다"를 적어, 다음 사람이 두 방식을 임의로 고르지 않게 한다.

**그 결과 `CanvasMainIntent.Enter`의 명시적 갱신 호출은 없앤다.** 폴러의 즉시 1회 조회가 그
역할을 그대로 한다. 남겨 두면 재진입마다 갱신이 두 번 나간다. `Enter`가 계속 담당하는 것은
달력 기록 재조회(`loadParfaitHistories`)뿐이다.

**지난 날을 보는 동안에는 구독을 끊어** 폴링을 멈춘다. 마감된 날은 바뀌지 않으므로 오늘 캔버스를
계속 부를 이유가 없다. 끊는 동안 **마지막 `todayCanvas` 값은 그대로 둔다** — 오늘로 돌아오면
구독이 다시 붙으면서 즉시 1회 조회가 덮으므로 낡은 값이 보이는 창은 왕복 한 번이고, 비우면 빈
캔버스가 깜빡이고 토핑 추가 버튼이 잠깐 비활성이 된다.

### 하루 경계

파르페 하루 경계(새벽 3시)를 **명시적 시간 축**으로 들인다. 다음 경계까지 `delay`한 뒤 방출하는
티커를 두는데, 보는 곳은 **`CanvasMainViewModel`** 하나다 — 발화하면 `today`·`selectedDate`·
`displayedMonth`를 다시 센다. 이 티커가 필요한 이유는 지금 `syncToday()`를 부르는 곳이
`handleEnter()` 하나뿐이라서다 — 화면을 열어 둔 채 경계를 넘기면 `today`·`selectedDate`가 어제로
남고, 그러면 폴링이 받아 온 **오늘 캔버스가 어제 날짜 헤더 아래 그려진다.**

**`CanvasPoller`는 이 티커를 구독하지 않는다.** 「폴링을 어디에 두는가」에 적었듯 폴러는 캐시에
실린 날짜가 오늘인지만으로 오늘 조회 여부를 정한다 — 경계를 넘기면 캐시 날짜가 저절로 어제가
되어 별도 신호 없이 다음 주기가 오늘 조회를 고른다. 값 스트림 하나에 매달지 않는 이유도 같은
맥락이다. 캐시에 `distinctUntilChanged`가 걸려 있어 활동 없는 그룹에서는 재방출이 없고, 재방출이
없으면 필터도 평가되지 않는다. 필터가 도는 순간은 이미 캐시가 바뀐 뒤라 필터가 필요 없는
순간이다.

티커는 UseCase가 소유하고 `clock`을 받아, 테스트에서 가상 시간으로 경계 앞뒤를 고정할 수 있게
한다.

### 배경 편집 화면의 병합

배경 편집 화면은 **구독 방출을 받을 때마다** 아래 규칙으로 병합한다. 최초 방출도 예외가 아니다 —
그때는 집합이 비어 있어 결과가 통째 대입과 같아진다. 화면은 그 방출이 폴링에서 왔는지 강제
갱신에서 왔는지 구분하지 않고, 구분할 필요도 없다.

**토핑.** `CanvasBGEditUiState`에 손댄 토핑을 추적하는 집합을 둔다.

```kotlin
val dirtyToppingIds: Set<Long> = emptySet()
val deletedToppingIds: Set<Long> = emptySet()
```

`Set<Long>`인 것은 같은 화면의 `selectedToppingId`가 `Long`이라 그것에 맞춘 것이다.
`ParfaitImageId`로 감싸는 자리는 지금처럼 API 호출 직전 한 곳뿐이다.

`dirtyToppingIds`는 **아직 서버에 반영되지 않은 로컬 변경**만 담는다. 이동
(`OnToppingMoveDrag`)·크기조절(`OnToppingResize`)·회전(`OnToppingRotate`)·테두리 편집
결과(`OnToppingEditResult`)가 대상 id를 넣는다.

**삭제는 여기 넣지 않는다.** 삭제는 모달 확인이 곧 DELETE라 이미 서버에 반영돼 있고
(`c301-topping-edit-tab`의 as-built), 확인 버튼은 삭제를 다루지 않는다. 대신 성공한 삭제의 id를
`deletedToppingIds`에 넣는다 — 삭제 직전에 출발한 갱신 응답이 뒤늦게 도착하면 그 토핑이 아직
서버 목록에 있어, 툼스톤이 없으면 **방금 지운 토핑이 되살아난다.**

병합 규칙은 넷이다.

- `deletedToppingIds`에 있는 토핑은 서버 목록에 있어도 **화면에서 뺀다.** 서버 목록에서 사라지면
  그 id를 툼스톤에서도 뺀다.
- `dirtyToppingIds`에 **있는** 토핑은 로컬 값을 지킨다.
- `dirtyToppingIds`에 **없는** 토핑은 서버 값으로 갈아 끼운다. 남이 올린 새 토핑도 이 경로로
  화면에 나타난다.
- 서버 목록에서 사라진 토핑은 화면에서 빼고, 두 집합에서도 뺀다.

테두리 재편집도 `dirtyToppingIds`에 넣는다. 그 토핑의 로컬 값이 갱신에 덮이면 안 되는 것은 같다.

⚠️ **이 스펙이 쓰인 다음 날 테두리 PATCH에 소비처가 생겼다**(2026-08-27, PR #369 — OQ-P-276 ①③
해소). 그래서 확인 시 나가는 요청은 위치 하나가 아니라 **위치와 테두리 둘**이고, 아래 「확인 시
PATCH 대상」이 스냅샷 대조를 집합으로 바꿀 때 **테두리 갈래를 함께 옮겨야 한다** — 그러지 않으면
방금 붙은 저장이 되돌아간다 → OQ-P-326 ①.

✅ **as-built(2026-08-28 스택 리베이스)** — 테두리 갈래를 함께 옮겼다. 자세한 형태는 아래
「확인 시 PATCH 대상」의 as-built 각주에 적는다.

**배경.** `withCanvas`가 지금 `selectedImageUri`·`selectedImageSource`·`selectedColor`를 서버 값으로
통째 대입한다. 구독으로 바뀌면 이것이 매 방출마다 돌아, **사용자가 갤러리에서 고른 배경이 주기마다
서버 배경으로 되돌아간다.** 그래서 이 셋은 **최초 방출에만 시딩하고 이후 방출은 무시한다.**
`parfaitId` 이동도 같다.

⚠️ **같은 함수에 시딩이 둘 더 늘었다**(2026-08-27, PR #400) — `selectedTab`과 `selectedToppingId`를
`initialToppingId`로 채운다(본인 토핑을 탭해 들어온 경우). 조회가 진입 시 한 번뿐인 지금은 무해하지만,
구독으로 바뀌면 **사용자가 탭을 옮기거나 선택을 풀어도 다음 방출에 되돌아간다.** 위 "최초 방출에만
시딩한다" 목록에 이 둘도 넣는다 → OQ-P-326 ②.

✅ **as-built(2026-08-28 스택 리베이스)** — 그 둘을 시딩 목록에 넣었다. `withCanvas`가 시딩 가드
(`hasSeededFromCanvas`) 뒤에서 `selectedTab`·`selectedToppingId`까지 함께 채우고, 이후 방출에서는
토핑 목록만 `mergeToppings`가 갈아 끼운다.

**확인 시 PATCH 대상**은 `toppings` 중 `dirtyToppingIds`에 든 것이다. 지금 `confirmedToppings`
스냅샷과 대조해 골라내던 방식이 이것으로 대체되므로, `confirmedToppings`는 **제거한다** —
`CanvasBGEditUiState`의 필드가 아니라 ViewModel의 `private var`였고, 렌더링에 쓰이지 않던
값이라 화면에 영향이 없다.

✅ **as-built(2026-08-28 스택 리베이스) — 스냅샷이 사라진 것이 아니라 대조 범위가 좁아졌다.**
`confirmedToppings`는 예정대로 없앴지만, 그 자리를 `serverToppings`가 받는다. 둘의 역할이 다르다:
전자는 **목록 전체**를 견줘 "무엇이 바뀌었나"를 가려냈고, 후자는 **집합이 이미 고른 토핑 안에서만**
"어느 축이 바뀌었나"를 가린다. 남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡히던 문제는 집합 필터가
앞에서 막으므로 다시 생기지 않는다.

이 대조가 필요한 이유는 **요청이 둘로 갈려 있기 때문**이다. 집합은 어느 축을 만져서 dirty 가 됐는지
기억하지 않으므로, 대조 없이는 위치만 옮긴 토핑에도 테두리 PATCH 가 따라 나간다. develop 의
`onClickConfirm_toppingBorderEdited_savesOnlyTheBorder` 가 그 반대 방향("테두리만 바꾸면 위치 PATCH 는
안 나간다")을 이미 잠그고 있어, 축별 판정을 빼면 그 테스트가 깨진다.

> 대조 방식을 바꾸는 것은 부수 효과도 닫는다. 지금 `updateToppingIfChanged`는 `toppings` 전체를
> 순회하고 스냅샷에 없으면 무조건 바뀐 것으로 보므로, 갱신이 들어오면 남의 새 토핑이 그 조합에
> 걸려 **남의 토핑에 PATCH를 쏜다.** ⚠️ PR #369 이후로는 그 자리에서 **위치와 테두리 둘 다**
> 나간다(`original == null`이 두 판정을 동시에 참으로 만든다).

**집합에서 빠지는 시점은 셋뿐이다.** 확인 PATCH가 성공한 토핑, 확인 PATCH가 실패한 토핑(화면이
이미 되감긴 뒤라 되살릴 자리가 없다 — 현 as-built의 "토핑 저장 실패는 화면에 닿지 않는다"를
승계하며 그 공백은 OQ-P-276 소관이다), 서버 목록에서 사라진 토핑. 그 밖에는 화면이 살아 있는
동안 집합이 줄지 않는다.

🔁 **as-built(2026-08-28 브랜치 `feature/canvas-polling`) — 실패한 토핑은 집합에 남는다.**
위 규칙이 선 전제("화면이 이미 되감긴 뒤라 되살릴 자리가 없다")가 같은 브랜치에서 뒤집혔다.
**확인은 하나라도 실패하면 화면을 닫지 않는다.** 그래서 되살릴 자리가 생겼고, 못 보낸 토핑을
집합에서 빼면 다시 누른 확인이 그 토핑을 건너뛴다. 빠지는 시점은 **성공한 토핑과 서버 목록에서
사라진 토핑 둘**이다. 승계하려던 OQ-P-275·OQ-P-270의 공백도 이 브랜치에서 함께 닫혔다
(OQ-P-276 ②는 그대로다).

> 이 병합 규칙은 [OQ-P-219](../../../synthesis/open-questions.md)의 **항목 ②(통째 대입을 병합으로 바꿀지)**
> 를 캔버스 화면 상태 층에서 답한 것이다. **미결 자체는 닫히지 않는다** — 항목 ①·③(낡은 응답
> 순서, 조작 직후 진행 중인 조회 취소)은 저장소 캐시 층의 문제이고 아래 「주의」에 남긴다.

### 배경 편집 화면의 진행·실패 표현

🔁 **as-built(2026-08-28 브랜치 `feature/canvas-polling`) — 스펙 밖에서 더해진 동작이다.**
초판은 이 화면의 실패 표현을 "현 as-built 승계"로만 적었는데, 그 as-built가 **확인도 삭제도
진행 표시가 없고 실패가 로그 한 줄**이라 폴링이 얹히면 더 나빠진다 — 갱신이 5초마다 화면을
다시 그리는 동안 사용자는 자기가 누른 것이 도는 중인지 실패한 것인지 구분할 방법이 없다.
그래서 같은 브랜치에서 다음 셋을 더했다.

- **진행 중에는 화면을 덮는다.** `CanvasBGEditUiState.isLoading`을 확인과 삭제가 나눠 쓰고
  라우트가 `YGScaffoldV2(isLoading = …)`에 넘긴다. 덮개가 입력을 삼키므로 두 흐름이 겹치지
  않고, 그래서 깃발도 하나면 된다. 오버레이·입력 차단·접근성 숨김은 스캐폴드가 이미 하던 것이다.
- **삭제는 성공했을 때만 나간다.** 강제 갱신을 기다린 뒤 `NavigateBack`을 쏜다(먼저 나가면
  라우트가 되감기며 `viewModelScope`가 취소돼 그 갱신이 끊긴다 — 배경 저장이 이미 같은 순서다).
  실패하면 화면에 남고 토스트만 나간다.
- **확인은 하나라도 실패하면 닫지 않는다.** 토핑 PATCH와 배경 저장을 모두 시도하되, 배경만
  저장되고 토핑이 남은 채 닫으면 사용자는 방금 옮긴 토핑이 되돌아간 캔버스를 보게 된다.

`CanvasBGEditError`가 3종에서 5종이 됐다 — `NETWORK`·`UNSUPPORTED_IMAGE`는 그대로이고
`UNKNOWN`이 요청별로 갈렸다(`BACKGROUND_SAVE_UNKNOWN`·`TOPPING_SAVE_UNKNOWN`·
`TOPPING_DELETE_UNKNOWN`). 문구가 "배경을 저장하지 못했어요"라 삭제 실패에 그대로 쓸 수
없어서다. `NETWORK`만 요청을 가리지 않는다.

⚠️ **배경과 토핑이 함께 실패하면 배경 쪽 토스트만 나간다.** 둘을 겹쳐 띄우지 않기로 한
결과이고, 부분 실패를 한 문구로 접는 것과 같은 판단이다(OQ-P-275 ②).

### 캔버스 메인의 스포트라이트

강조 중이던 토핑이 갱신으로 사라지면 파생 `spotlightedTopping`만 null이 되고 상태값
`spotlightedToppingId`는 남는다. 딤이 그려지지 않아 Dim 탭이라는 해제 계기가 사라지고,
`handleOnClickTopping`은 `spotlightedToppingId != null`이면 즉시 반환하므로 **겉보기는 Default인데
토핑 탭이 전부 먹지 않는 화면**이 된다. 탈출구는 앱을 백그라운드로 보냈다 오는 것뿐이다.

폴링 이전에는 `toppings`가 `Enter` 시점에만 바뀌었고 같은 resume에서 `OnAppReturnedFromBackground`가
이미 해제했으므로 도달 불가능한 상태였다. 폴링이 새로 여는 경로다.

**구독 값을 받을 때 `spotlightedToppingId`가 새 목록에 없으면 `resetSpotlight()`를 부른다.**

### 토핑 배치 화면의 `positionZ`

확인을 누르는 시점에 구독 중인 캔버스에서 `max(positionZ) + 1`로 다시 센다. 흐름 진입 때 초안에
못 박은 값은 카메라·누끼를 거치는 사이 남이 토핑을 올리면 낡는다.

**재계산의 전제는 구독 값의 `parfaitId`가 초안의 `parfaitId`와 같은 것이다.** 다르면 초안 값으로
물러선다. 구독 값은 "오늘"로 걸러진 캔버스라 하루 경계를 넘기면 초안이 가리키는 캔버스와 다른
것일 수 있는데, 그때 재계산한 z를 초안의 캔버스에 실으면 **사용자가 들어간 캔버스가 아닌 곳의
z로 쓰게 된다.** ADR-0026이 "초안 없이 배치 시점에 재조회"를 기각한 것과 같은 이유다. 캔버스를
아직 못 받은 경우에도 초안 값으로 물러선다.

**이것은 해결이 아니라 완화다.** 재계산이 읽는 값은 최대 폴링 주기만큼 낡았으므로, 두 사람이 그
안에 확인을 누르면 여전히 같은 `max(z)`를 읽어 z가 겹친다. 근본 해결은 서버가 z를 배정하는
것이고 아래 「주의」에 미결로 남긴다.

초안이 `nextPositionZ`를 계속 싣는 것은 그대로 둔다 — 위 두 폴백의 값이고, 초안이 흐름의 대상
캔버스를 못 박는다는 ADR-0026의 결정은 바뀌지 않는다.

### 세션 정리와 진행 중인 갱신

`clearTodayCanvas()`/`clear()`는 **폴러의 모든 그룹 `Job`을 함께 취소한다.** 지우기만 하면
그 시점에 이미 출발한 갱신의 응답이 뒤늦게 도착해 `saveTodayCanvas`를 불러, **직전 계정의
캔버스가 캐시에 되살아난다.** 인메모리라 프로세스가 살아 있는 계정 전환에서 남는데, 같은 그룹에
두 계정이 속해 있으면 새 계정이 이전 계정 기준으로 계산된 `isMine`을 보고 남의 토핑을 자기
것으로 선택해 PATCH를 쏘다 403을 맞는다.

### 검증

폴링은 가상 시간(`runTest` + 테스트 디스패처)으로 고정한다.

- 구독이 생기면 즉시 1회, 이어서 주기마다 갱신이 나간다
- 구독이 끊기면(정지 유예 뒤) 더 나가지 않는다
- 두 구독자가 붙어도 주기당 한 번만 나간다
- 강제 갱신이 들어오면 그 시점부터 주기가 다시 세어진다
- 캐시 날짜가 오늘이면 상세 조회를, 캐시가 비었거나 캐시 날짜가 오늘이 아니면(하루 경계를
  넘겼다) 오늘 조회를 부른다
- 진행 중인 갱신이 있으면 이번 주기를 건너뛴다(강제 갱신도 같은 가드를 지난다)
- 정리 뒤 도착한 응답이 캐시를 되살리지 않는다

병합은 `CanvasBGEditViewModelTest`에서 dirty 토핑이 덮이지 않는 것, dirty 아닌 토핑이 갱신되는 것,
툼스톤에 든 토핑이 서버 목록에 있어도 안 되살아나는 것, 배경 선택이 최초 방출에만 시딩되는 것을
각각 고정한다. 스포트라이트 해제와 `positionZ` 재계산의 세 경로(같은 `parfaitId` / 다른
`parfaitId` / 캔버스 없음)도 단언한다. 하루 경계는 두 갈래로 나눠 확인한다 — 폴러 쪽은 캐시에
실린 날짜를 가상 시간으로 밀어 엔드포인트 전환을 확인하고, `CanvasMainViewModel` 쪽은 티커를
가상 시간으로 밀어 오늘 판정이 함께 도는 것을 확인한다.

---

## 파일 구성

**신설**

| 파일 | 역할 |
|------|------|
| `data/source/parfait/local/CanvasLocalDataSource.kt` | 인터페이스 |
| `data/source/parfait/local/CanvasLocalDataSourceImpl.kt` | `@Singleton` 인메모리 구현 |
| `data/source/parfait/local/CanvasPoller.kt` | 그룹별 참조 계수 + 주기 갱신 루프(로컬 데이터소스가 아니라 트리거 소유자 — 패키지만 `local/`을 공유) |
| `domain/usecase/parfait/GetTodayParfaitFlowUseCase.kt` | 구독 + 날짜 낡음 필터 |
| `domain/usecase/parfait/ObserveParfaitDayBoundaryUseCase.kt` | 하루 경계 티커 |
| `domain/usecase/parfait/RefreshTodayParfaitDetailUseCase.kt` | 부작용 없는 상세 조회로 갱신(쓰기 직후 등 응답을 기다리는 자리) |
| `domain/usecase/parfait/RequestTodayParfaitRefreshUseCase.kt` | `RefreshTodayParfaitDetailUseCase`의 async 판(되감기 직전 등 기다릴 수 없는 자리) |
| `domain/usecase/parfait/ObserveTodayParfaitRefreshFailureUseCase.kt` | 폴러의 갱신 실패 구독(2026-08-30 리베이스 신설, 첫 조회 덮개를 내리는 계기) |
| `data/model/qualifier/ApplicationScope.kt` | 프로세스 수명 스코프 한정자 |
| `data/di/ApplicationScopeModule.kt` | `@Singleton CoroutineScope`(`SupervisorJob + Dispatchers.IO`) 제공 — `CanvasPoller`가 주입받는다 |
| `data/di/ClockModule.kt` | 전역 `Clock` 싱글턴 바인딩(`ADR-0029` 「영향」 참고) |

> 🔧 **as-built**: `domain/usecase/parfait/RefreshTodayParfaitUseCase.kt`(기존 `GetTodayParfaitUseCase`
> 이관분)는 신설된 뒤 PR3에서 다시 지웠다 — 아래 「삭제」 참고.

**변경**

| 파일 | 변경 |
|------|------|
| `CanvasBGEditScreen.kt` | 탭 게이트 분리, `alpha` 파라미터, 배경 탭 비상호작용 |
| `BaseViewModel.kt` | 구독 수에 수명을 매다는 헬퍼 추가 |
| `ParfaitRepository.kt` / `ParfaitRepositoryImpl.kt` | `getTodayCanvas` → 구독·갱신·상세갱신·정리 넷으로 분리, 폴러 계수 연동 |
| `CanvasMainViewModel.kt` | 구독 이관, `viewedCanvas` → `pastCanvas` + `displayedCanvas` 파생, 경계 티커 구독, 스포트라이트 해제, `Enter` 갱신 제거 |
| `CanvasBGEditViewModel.kt` | 구독 이관, 병합 규칙, `confirmedToppings` → `serverToppings`(대조 범위 축소), 배경·탭·선택 최초 시딩 |
| `CanvasToppingPlaceViewModel.kt` | 구독 이관, 확인 시 `positionZ` 재계산 |
| `LogoutUseCase.kt` / `TokenAuthenticator` | 캔버스 캐시 정리 추가 |
| `architecture/state-management.md` | 구독 수명 헬퍼 규약 한 절 |
| DI 모듈 | `CanvasLocalDataSource`·`CanvasPoller` 바인딩, `@Singleton CoroutineScope` 제공 |

**삭제**

`GetTodayParfaitUseCase.kt`는 `RefreshTodayParfaitUseCase.kt`로 옮겨 가며 사라진다. 도메인
테스트의 `ParfaitRepository` 페이크 셋(`GetTodayParfaitUseCaseTest`·`GetParfaitHistoriesUseCaseTest`·
`GetParfaitYearsUseCaseTest`)이 그 함수를 override하고 있어 함께 고친다. `ParfaitRepository`와
`GetParfaitDetailUseCase`의 KDoc이 삭제 대상 심볼을 링크하고 있어 그것도 고친다.

> 🔧 **as-built**: `RefreshTodayParfaitUseCase.kt`는 PR3에서 **다시 지운다.** PR3의 폴러가
> "캐시 날짜가 오늘이 아니면 오늘 조회"를 매 주기 스스로 판단하면서 이 UseCase의 프로덕션
> 호출처가 0건이 됐다 — 동작 손실은 없다(다음 폴링 주기, 최대 5초 안에 폴러가 같은 결과를
> 낸다). 함께 지운 것: `ParfaitRepository`의 `refreshTodayCanvas`·`cachedTodayCanvasDate` 두
> 표면과 그 구현, `CanvasPoller.refreshNow`/`refreshNowAsync`/`refresh`의 `forceToday`
> 파라미터, `CanvasMainViewModel`의 그 주입, `:domain` 테스트 페이크 셋의 해당 override.
>
> ⚠️ **`ShowTodayCanvasError` 는 지웠다가 되살렸다(2026-08-30 리베이스).** PR3 시점에는 발행하는
> 곳이 없어 이펙트·화면 처리·문자열 리소스를 함께 지웠는데, 첫 조회 덮개를 잇느라 폴러의 실패
> 신호가 생기면서 트리거가 돌아왔다. 셋 다 복원돼 있다 — 조건만 "보여 줄 캔버스가 없을 때"에서
> "첫 조회를 기다리는 동안"으로 좁혀졌다(위 「실패 표현」 as-built).

문서 산출물은 이 스펙과 [ADR-0029](../../../adr/0029-canvas-today-ssot-polling.md), 그리고 두
`README.md` 인덱스 행이다(각각 같은 커밋에 등록).

## 검증 못 한 것

- 배경 탭에서 토핑이 반투명으로 그려지고 터치가 전혀 먹지 않는지(실기기)
- 한 기기에서 올린 토핑이 다른 기기 캔버스에 폴링 주기 안에 나타나는지
- 배경 편집 중 남이 토핑을 올렸을 때 내 배치·배경 선택이 안 밀리고 남의 토핑만 나타나는지
- 카메라·갤러리로 나갔다 오는 동안 폴링이 멎고 복귀 첫 프레임이 최신인지
- 앱을 백그라운드로 보냈을 때 폴링이 실제로 멎는지
- 계정 전환 시 이전 계정 캔버스가 남지 않는지

## 주의 / 열린 질문

- **폴링 주기 5초는 실측 전 값이다.** 한 그룹에 몰리는 요청은 그 그룹에서 동시에 캔버스를 보는
  사람 수에 비례하고 주기에 반비례한다(정원 상한은 12명이다). 상수 하나로 두므로 서버 부하를
  보고 조정한다. 조정 트리거와 실측 주체는 정하지 않았다([OQ-P-320](../../../synthesis/open-questions.md)).
- **낡은 응답 순서가 완전히 닫히지는 않는다.** 폴러 자신의 중첩은 "진행 중인 갱신이 있으면 이번
  주기를 건너뛴다"로 막지만, 다른 경로에서 나간 갱신과 겹치는 창은 남는다. 이것이
  [OQ-P-219](../../../synthesis/open-questions.md)의 항목 ①·③이 캔버스에서 나타난 형태다
  ([OQ-P-321](../../../synthesis/open-questions.md)로 따로 등록하고 OQ-P-219에 상호 참조를 남겼다).
- **`positionZ` 겹침은 완화만 된다.** 폴링 주기 안에 두 사람이 확인을 누르면 같은 `max(z)`를 읽는다.
  근본 해결은 서버가 z를 배정하는 것이라 앱만으로 닫히지 않는다([OQ-P-322](../../../synthesis/open-questions.md)).
- **경계 직후 오늘 조회가 한 그룹에서 동시에 나갈 수 있다.** 캔버스를 열어 둔 클라이언트가 여럿이면
  경계 전환에서 각자 한 번씩 생성을 태운다. 폴링이 백그라운드에서 멎으므로 그 수가 "그 순간 실제로
  캔버스를 보고 있는 사람"으로 줄지만 0은 아니다. 서버가 유니크 제약으로 막는지 확인하지 못했다
  ([OQ-P-323](../../../synthesis/open-questions.md)).
- **테두리 편집 결과는 여전히 저장되지 않는다**(OQ-P-276). 이 스펙은 그 토핑을 갱신에서 지키는
  것까지만 하고 저장 경로는 열지 않는다.
- **`CanvasEditRoute`·`CanvasImageSelectRoute`·`CanvasMoveRoute`가 유휴 상태로 남는다.** 이미
  [OQ-P-239](../../../synthesis/open-questions.md)에 등록돼 있다. 배경 편집과 이름이 비슷해 다음 사람이
  헷갈릴 자리라는 점만 덧붙인다.
