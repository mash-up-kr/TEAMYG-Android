---
id: screen-resume-refetch
title: 화면 재진입 재조회 — G-001·C-001 `Enter` 인텐트 + 새로고침 실패 토스트 (Resume Refetch)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-04
related_code:
  - GroupListRoute.kt#GroupListRoute
  - GroupListViewModel.kt#GroupListIntent.Enter
  - GroupListViewModel.kt#GroupListSideEffect.ShowRefreshError
  - GroupListViewModel.kt#updateToday
  - GroupListViewModel.kt#loadGroups
  - GroupListViewModel.kt#handleLoadFailure
  - GroupListViewModelTest
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainViewModel.kt#CanvasMainIntent.Enter
  - CanvasMainViewModel.kt#handleEnter
  - CanvasMainViewModel.kt#syncToday
  - CanvasMainViewModel.kt#loadTodayCanvas
  - CanvasMainViewModel.kt#loadParfaitHistories
  - CanvasMainViewModelTest
  - YGScaffoldV2.kt#YGScaffoldV2
  - YGToastPolicy.kt#rememberYGToastPolicy
  - YGToastPolicy.kt#showError
related_adr: ADR-0005, ADR-0006, ADR-0020
related_spec: g001-group-list, c001-canvas-today-detail, c201-canvas-calendar-server, ygscaffold-v2-common-loading-error, group-ssot
related_architecture: state-management, navigation-flow, design-system
supersedes:
superseded_by:
tags: [spec, parfait, groups, group-list, canvas, state, g-001, c-001]
---

# Spec: 화면 재진입 재조회 (`Enter` 인텐트)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> **사후 기록(post-hoc)** — 선작성 스펙 없이 develop 머지(PR #297, 브랜치 `feature/#288-group-list-refresh`,
> 2026-08-17). as-built 역기록이고 **코드가 SoT**다.

## 목표

백스택 아래에 깔린 화면은 컴포지션에서 빠지지만 **ViewModel은 살아 있다.** 그래서 `init`에서 한 번
조회하는 화면은 다른 화면에 갔다 오면 낡은 값을 그대로 보여 준다 — 그룹을 만들고 목록으로 복귀해도
새 그룹이 안 보이던 것(OQ-P-169)이 그 증상이다. 두 화면(G-001 그룹 목록·C-001 캔버스)에
**`Enter` 인텐트**를 두고 `LifecycleResumeEffect`가 화면이 앞에 설 때마다 그것을 보낸다.

재조회가 필요한 근거는 **다른 멤버가 바꾸기 때문**이다. 목록의 최근 사진도, 캔버스의 토핑·멤버도 내
앱 안의 변경만 좇아서는 최신이 되지 않는다. 그래서 복귀 관용구(`goToSingleClearTop` vs
`clearBackStack()`+`goTo`)를 바꾸는 쪽이 아니라 **화면이 스스로 다시 묻는** 쪽을 골랐다.

## 범위

**포함**

- 두 화면에 `Enter` 인텐트 + Route의 `LifecycleResumeEffect` 배선
- G-001: 날짜 헤더 재계산·목록 재조회를 `init`에서 `Enter`로 이동, **조회 실패 규칙 교체**
  (낡은 목록을 남기고 토스트로 알린다)
- G-001 Route를 `YGScaffoldV2`로 이관(토스트 호스트가 필요해서)
- C-001: 오늘 캔버스·올해 달력 기록 재조회를 `Enter`로 이동 + 자정 넘김 재계산(`syncToday`)

**제외**

- 폴링·푸시 기반 갱신. 화면이 앞에 서는 순간만 트리거다.
- 공유 캐시. 두 화면은 여전히 각자 조회하고 각자 `UiState`에 담는다 —
  그것을 캐시 구독으로 바꾸는 것은 [group-ssot 스펙](2026-08-17-group-ssot.md)이다.
- C-001의 실패 표현. 캔버스 조회 실패는 이 라운드에서도 로그뿐이다.

## API / 인터페이스

```kotlin
// G-001
sealed interface GroupListIntent : UiIntent {
    data object Enter : GroupListIntent          // #297 신설
    …
}
sealed interface GroupListSideEffect : UiSideEffect {
    data object ShowRefreshError : GroupListSideEffect   // #297 신설
    …
}

// C-001
sealed interface CanvasMainIntent : UiIntent {
    data object Enter : CanvasMainIntent         // #297 신설
    …
}
```

- `GroupListViewModel`의 `init` 블록은 **통째로 사라졌다.** 날짜 산출(`updateToday()`)과
  첫 조회(`loadGroups(isRefresh = false)`)를 `Enter`가 한다.
- `CanvasMainViewModel`의 `init`에는 `loadCanvasMainInfo()`(그룹명 mock)와 `loadParfaitYears()`만 남는다 —
  연도 목록은 해가 바뀔 때만 늘어나 재진입마다 물어볼 값이 아니다.
- 문자열 1건 신설: `feature/groups/list/impl` `strings.xml`의 `group_list_refresh_error`.

## 동작 / 상태

### 트리거

Route가 `LifecycleResumeEffect { processIntent(Enter); onPauseOrDispose { } }`로 보낸다. 인텐트가
Route에 있는 이유는 **ViewModel이 자기 화면이 앞에 섰는지 모르기 때문**이고, `LaunchedEffect(Unit)`이
아닌 이유는 그것이 컴포지션 1회라 `init`과 같아지기 때문이다.

⚠️ 두 화면의 key 관용구가 갈렸다 — C-001은 `LifecycleResumeEffect(viewModel)`, G-001은
`LifecycleResumeEffect(Unit)`이다. 둘 다 화면 수명 동안 안 바뀌는 값이라 동작은 같다.

### G-001 그룹 목록

- `Enter` → `updateToday()` + `loadGroups(isRefresh = false)`. 날짜 헤더까지 다시 세는 이유는 앱을 켜 둔
  채 자정을 넘겨도 머리말이 어제에 머물지 않게 하기 위함이다.
- 조회는 그대로 `launch(key = KEY_LOAD_GROUPS)` 가드 안에서 돌고, `isRefreshing`은 당김일 때만 켠다.
- **실패 규칙이 뒤집혔다.**

| 상황 | #248~#287 | #297 |
|---|---|---|
| 조회 실패 + 목록 있음 | `isError = true` (전면 에러 화면) | 목록 유지. 당김이었으면 `ShowRefreshError` 토스트, `Enter`였으면 조용히 |
| 조회 실패 + 목록 없음 | `isError = true` | `isError = true` (같음, 토스트는 겹치지 않는다) |

  뒤집은 근거는 **조회 빈도가 달라졌다**는 것이다 — 재진입마다 조회가 나가므로 실패마다 전면 교체하면
  뒤로 온 것만으로 보던 목록이 통째로 사라진다. 대신 남겨 두면 실패가 화면에서 사라지므로, 사용자가
  **직접 시킨** 새로고침에만 자리를 따로 만든다(목록이 그대로인 것은 "새 소식이 없다"와 구분되지 않는다).

> 🔁 **표의 오른쪽 열이 다시 바뀌었다(2026-09-04, PR #440 develop 머지)** — 갈림의 기준이 "목록이
> 남아 있는가"에서 **"사용자가 당겼는가"**로 옮겼다. 같은 라운드가 당기는 동안 목록을 비우게 만들어,
> 당긴 새로고침 실패는 목록이 있어도 받아 줄 자리가 에러 화면뿐이 됐다. 그래서 이 스펙이 신설한
> `ShowRefreshError`와 문자열 `group_list_refresh_error`가 **도달 불가가 되어 함께 삭제**됐고,
> 그것 때문에 Route가 쥐고 있던 토스트 호스트도 사라졌다(`YGScaffoldV2` 이관 자체는 그대로다).
> **재진입 조회 실패가 조용한 것은 그대로**이고, `isError`에는 규칙이 하나 붙었다 —
> 켜기만 하고 끄는 것은 성공한 조회뿐이다 → OQ-P-348 · [g001 스펙](2026-08-01-g001-group-list.md).
- 실패 사유(네트워크·서버·그 외)는 여전히 **로그로만** 갈린다. 토스트 문구는 하나다.

### C-001 캔버스

- `Enter` → `syncToday()` → **오늘을 보고 있을 때만** `loadTodayCanvas()` + `loadParfaitHistories(올해)`.
  지난 날은 마감돼 더 바뀌지 않으므로 이미 받아 둔 것이 그대로 맞다.
- 달력 기록을 함께 받는 이유: 다른 멤버가 오늘 토핑을 올리면 오늘 칸에 점이 생기는데 연 단위 캐시는
  그것을 스스로 알 방법이 없다. **바뀔 수 있는 해는 올해뿐**이라 올해만 다시 받는다.
- 오늘 조회는 부작용 있는 GET(`/parfaits/today`가 없으면 만든다)인데 **재진입마다 부른다** — 첫 진입에서
  이미 만들어진 것을 받을 뿐이라 캔버스가 늘어나지는 않는다는 판단이고, `LOAD_TODAY_CANVAS_KEY` 가드는
  중복 동시 호출만 막는 역할로 남았다. 부수 효과로 **ViewModel이 만들어지는 것만으로는 캔버스가 생기지
  않는다**(화면이 실제로 앞에 설 때까지 미뤄진다).
- `syncToday()` — 화면을 열어 둔 채 자정을 넘긴 경우 `parfaitToday()`를 다시 세고, 달라졌을 때만 쓴다.
  - 오늘을 보고 있었으면 `today`·`selectedDate`·`displayedMonth`를 새 날로 옮기고 `todayCanvas`·
    `viewedCanvas`를 **비운다** — 어제 것을 오늘로 착각해 그 위에 토핑을 올리는 일을 막기 위함이고,
    바로 뒤따르는 오늘 조회가 채운다.
  - 지난 날을 보고 있었으면 그 캔버스는 그대로 유효하므로 `today`만 갱신한다(이 경우 뒤의 재조회는
    건너뛴다).
  - ⚠️ 경계는 **KST 자정**이다. 위키 [[캔버스-마감-스케줄]]의 03:00은 여기서도 적용되지 않았다(OQ-P-127).

## 표시·제어 규칙

- G-001 Route 컨테이너가 `YGScaffold` → **`YGScaffoldV2`**(`toastPolicy = rememberYGToastPolicy()`)로
  옮겨졌다. 토스트를 띄우려면 호스트가 필요해서이고, `isLoading`은 넘기지 않는다(첫 조회 로딩 표현은
  여전히 없다).
- 그룹 추가 오버레이의 두 번째 스캐폴드도 V2가 됐지만 `toastPolicy`는 주지 않는다 — 오버레이가 떠 있는
  동안에는 당겨서 새로고침을 할 수 없어 실패가 나지 않는다(코드 주석). 스캐폴드를 겹쳐 오버레이를
  그리는 형태 자체는 그대로다(OQ-P-046).
- 이로써 `YGScaffold` V1 잔여는 **6파일**(camera·gallery·groups canvas/enter·intro·segmentation)이 됐다
  → OQ-P-204.

## 파일 구성

- `.../groups/list/impl/route/GroupListRoute.kt` — `LifecycleResumeEffect` + `YGScaffoldV2` + 토스트 소비.
- `.../groups/list/impl/route/GroupListViewModel.kt` — `init` 삭제, `Enter`·`updateToday`,
  `handleLoadFailure(throwable, isRefresh)`.
- `.../groups/list/impl/src/main/res/values/strings.xml` — `group_list_refresh_error` 추가.
- `.../groups/canvas/impl/route/CanvasMainRoute.kt` — `LifecycleResumeEffect`.
- `.../groups/canvas/impl/viewmodel/CanvasMainViewModel.kt` — `Enter`·`handleEnter`·`syncToday`.
- `.../groups/canvas/impl/build.gradle.kts` — `parfait.test.unit` 적용(**이 모듈 첫 유닛 테스트**).

### 테스트

유닛 456 → **467건**.

| 파일 | 내용 |
|---|---|
| `CanvasMainViewModelTest`(신설 5) | `Enter`가 오늘 캔버스 + 올해 기록을 받는지 · 화면이 서기 전에는 오늘 조회가 **안 나가는지** · 재진입이 오늘 캔버스를 다시 받아 새 멤버 칩이 뜨는지 · 지난 날을 보는 중이면 둘 다 안 부르는지 · 연도 목록은 재진입에 다시 안 묻는지 |
| `GroupListViewModelTest`(8 → 14) | `Enter` 성공·실패·재진입 재조회·헤더 날짜 · 목록이 남은 실패는 화면 유지 · **당김 실패만 토스트**(재진입 실패는 조용) · 에러 화면에서는 토스트를 겹치지 않음 |

## 주의 / 열린 질문

- **주석과 동작이 어긋난다** — `loadParfaitHistories`의 KDoc은 "연 단위로 한 번만 받아 … 이미 본 해로
  돌아올 때도 다시 부르지 않는다"인데, `Enter`가 올해를 매번 다시 받는다.
- **실패 표현이 화면마다 갈렸다** — 같은 라운드에서 G-001만 토스트 자리를 얻었고 C-001의 오늘 조회·
  기록 조회 실패는 그대로 로그뿐이다. 재진입마다 조회가 나가므로 실패 빈도만 늘었다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-167·OQ-P-221.
- **재진입 실패는 조용하다** — G-001도 사용자가 직접 당긴 것만 알리므로, 뒤로 와서 실패하면 낡은 목록을
  낡은 줄 모르고 본다.
- **자정 처리 경로가 둘** — `GetTodayParfaitUseCase`의 응답 뒤 재시도 1회(요청 중 날이 바뀐 경우)와
  화면 재진입 `syncToday`(열어 둔 채 날이 바뀐 경우). 서로 다른 상황을 덮지만 기준 시각은 둘 다 KST 자정이라
  03:00 경계 미적용은 그대로다.
- **관용구가 규약이 아니다** — 두 화면이 같은 형태를 쓰지만 `LifecycleResumeEffect`로 재조회하라는
  규약은 없고, 새로 생기는 화면이 이것을 따르는지 확인할 수단도 없다 → OQ-P-221.
- **후속 라운드와의 관계** — PR #307(2026-08-20 develop 머지)이 이 자리의 **대상을 캐시로 바꿨다**
  (`Enter` → `RefreshMyGroupsUseCase`, 표시는 `Flow` 구독). 재진입 재조회 자체는 그 뒤에도 남고,
  실패 판정만 `isNullOrEmpty()`가 된다 → [group-ssot 스펙](2026-08-17-group-ssot.md).
- **실기기·실서버 확인 없음** — 재진입 재조회·자정 넘김·토스트 노출은 유닛 테스트로 덮은 범위까지다.
