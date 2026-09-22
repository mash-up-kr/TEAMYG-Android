---
id: state-management
title: 상태 관리 (MVI) · 데이터 흐름
category: architecture
status: living
platforms: android
verified: 2026-09-05
related_spec: c103-multi-subject-selection, c201-canvas-calendar, c201-canvas-calendar-server, session-token-refresh-infra, user-info-ssot, c301-topping-edit-tab, ygscaffold-v2-common-loading-error, s101-group-setting-api, group-ssot, intro-term-agree
related_adr: ADR-0001, ADR-0005, ADR-0009, ADR-0020, ADR-0021, ADR-0022, ADR-0023, ADR-0029
related_architecture: data-layer, navigation-flow
related_code: core:ui, BaseViewModel, MviContract, AppError, GetToppingDraftFlowUseCase, EnsureDraftSubjectRecordedUseCase, PastCanvasAlertRepository, LoginViewModel, AccountInfoViewModel, AppSettingViewModel, GetMyAccountFlowUseCase, GetMyGroupsFlowUseCase, GetGroupDetailUseCase, GroupListViewModel, GroupSettingViewModel, CanvasMainViewModel, CanvasBGEditViewModel, CanvasToppingPlaceViewModel, TermAgreeViewModel, TermAgreeError, GetTutorialVisibleFlowUseCase, CompleteTutorialUseCase, CanvasTutorialStep
tags: [architecture, parfait]
---
# 상태 관리 (MVI) · 데이터 흐름

화면 상태를 `core:ui`의 MVI 베이스로 다룬다. 결정 근거는 [[0005-custom-mvi-baseviewmodel]]. 레이어 흐름은 [[0001-layered-multi-module]]·[[data-layer]].

> 근거는 파일명+심볼명으로만.

## 단방향 흐름

```
사용자 입력
  → Screen: viewModel.processIntent(Intent)
  → ViewModel(BaseViewModel): Intent 처리
       ├─ UseCase 호출 → Repository → DataSource
       ├─ state 갱신  → StateFlow<S>  → Screen 재구성
       └─ 1회성 효과  → SharedFlow<E> → Screen에서 소비(내비게이션 등)
```

> **2026-08-15 갱신** — `BaseViewModel`이 확장됐다([ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md),
> **PR #241로 develop 머지 완료**). 이펙트 전달이 `Channel` 기반으로 바뀌고
> `launch(key, onError, block)`가 생겼다. **3분할 계약은 그대로다** — 설계 도중 4번째
> 스트림(`error`)이 생겼다가 철회됐다.
>
> **화면 밖 이벤트는 이 계약 밖이다**(2026-08-15, PR #260) — 강제 로그아웃은 ViewModel이 아니라
> `:data`의 `SessionEventBusImpl`이 발행하고 앱 루트가 수집한다(#450에서 인터페이스가 `SessionEventBus`, 구현이 `~Impl`로 개명됐다 — [data-layer](data-layer.md) 「이벤트 버스 개명」). 채널 선택 근거(`Channel` + 단일 소비자)는
> 같지만 소유자가 화면이 아니라 세션이다 → [navigation-flow](navigation-flow.md) "세션 종료 이동".

## 3분할 계약 (`MviContract`)
- **UiState** — 불변. 화면이 그리는 전부. `StateFlow<S>`로 노출.
- **UiIntent** — 사용자 행위/이벤트. `processIntent(intent)` 진입.
- **UiSideEffect** — 내비게이션·토스트 등 1회성. `SharedFlow<E>`로 노출.

예: `LoginState` / `LoginIntent`(`LoginWithKakao`, `LoginWithKakaoSuccess`) / `LoginSideEffect`(`NavigateToGroupList`, `NavigateToTermAgree`, `RequestLoginWithKakao`).

### 이펙트 전달은 `Channel`이다
`Channel(BUFFERED).receiveAsFlow()`. 구독자가 없는 순간 발행해도 버퍼에 남았다가 전달되고, 이미
소비한 이펙트는 재구독해도 다시 오지 않는다. `SharedFlow` + `replay`는 후자를 깨서 화면 재진입·
Activity 재생성 때 내비게이션이 저절로 다시 실행된다 — 명시적으로 기각된 설계다.

대신 **단일 소비자**다. `effect` 수집은 **화면당 한 곳(Route)**이며, 두 곳에서 수집하면 이펙트가
한쪽에만 간다. 조용히 넘어가지 않도록 베이스가 동시 구독자 수를 세어 error 로그를 남긴다.

### 작업 실행은 `launch(key, onError, block)`
```
protected fun launch(key: Any? = null, onError: ((AppError) -> Unit)? = null, block: suspend CoroutineScope.() -> Unit): Job?
```
- 같은 `key`의 작업이 살아 있으면 **새로 시작하지 않고 `null` 반환** — 버튼 연타 차단.
- 블록이 던지면 `AppError.Unexpected`로 감싸 `onError`로. **없으면 로그만.**
- `CancellationException`은 재던진다. `Result.failure`는 값이라 잡지 않는다 — 호출부가 처리한다.

**실패도 이펙트다.** 공용 에러 스트림을 따로 두지 않는다 — 스트림을 나누면 이펙트와의 순서
보장이 사라지고, 실패 경로가 없는 화면까지 빈 채널을 달게 된다. 표현이 필요한 화면이 자기
`SideEffect`에 케이스를 두고 `onError`에서 발행한다.

```kotlin
launch(key = …, onError = { postSideEffect(XxxSideEffect.ShowError(it)) }) { … }
```

**SDK 다이얼로그처럼 `launch` 바깥에서 뜨는 조작**은 `launch(key)` 가드가 못 막는다. State의
`isLoading` 가드를 한 겹 더 둔다 → [a002-kakao-login-api](../superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md).

### 화면이 보는 동안만 살아야 하는 구독은 `launchWhileSubscribed`

`BaseViewModel.launch`로 연 구독은 **ViewModel 수명**에 걸린다 — 백스택 아래에 깔린 화면에서도
계속 돈다. 그것이 맞는 경우가 대부분이지만, 업스트림이 주기적으로 서버를 부르는 종류라면
보이지 않는 화면 때문에 요청이 계속 나간다.

그런 구독은 `launchWhileSubscribed`로 연다. 노출한 `state`의 구독자 수가 0보다 큰 동안에만
업스트림이 살아 있고, 라우트가 `collectAsStateWithLifecycle()`을 쓰므로 화면이 백그라운드로
가거나 컴포지션에서 빠지면 함께 끊긴다. 마지막 구독자가 떠난 뒤 유예를 두어 화면 전환의 짧은
공백에서 업스트림이 껐다 켜지지 않게 한다.

⚠️ **`source` 안에서 `state`를 수집하면 안 된다** — 열린 업스트림 자신이 구독자로 세어져
계수가 0으로 내려가지 않는다. 화면 조건으로 업스트림을 가르려면 별도 flow를 둔다.

**둘 중 하나를 임의로 고르지 않는다.** 기준은 "이 구독이 서버를 계속 부르는가"다.
근거는 [ADR-0029](../adr/0029-canvas-today-ssot-polling.md).

> ✅ **코드가 들어왔다(2026-08-31, PR #404)** — 이 절은 구현 전에 쓰였고, 이제 `BaseViewModel`이
> 실제로 `launchWhileSubscribed(stopTimeout, source, collector)`를 들고 있다. 활성 판정은
> `state.subscriptionCount > 0`이고 유예는 상수 하나다. 쓰는 곳은 캔버스 세 화면
> (`CanvasMainViewModel`·`CanvasBGEditViewModel`·`CanvasToppingPlaceViewModel`)이며, 폴러의
> 참조 계수는 저장소 층이 구독의 `onStart`/`onCompletion`에 걸어 올리고 내린다 — 화면은 폴러의
> 존재를 모른다.

> 📌 **서버를 부르지 않는 구독에도 쓰이기 시작했다(2026-09-05, PR #449)** — 튜토리얼 노출 여부
> (`GetTutorialVisibleFlowUseCase`)는 DataStore 한 키를 읽는 구독인데 소비 셋
> (`CanvasMainViewModel`·`CustomGalleryPickerViewModel`·`SegmentationConfirmViewModel`)이 모두
> `launchWhileSubscribed`로 연다. **위 기준("이 구독이 서버를 계속 부르는가")으로는 `launch` 쪽**이고,
> 실제로 얻는 것은 화면이 안 보일 때 로컬 구독이 잠깐 끊기는 것뿐이다. 쓰는 곳이 캔버스 세 화면에서
> **여섯**이 됐고 그중 셋이 기준 밖이라, 문서의 기준과 코드의 관행이 갈렸다
> → [open-questions](../synthesis/open-questions.md) OQ-P-368.
>
> 딸려 오는 것이 하나 있다 — 이 구독은 **화면이 `state`를 보는 동안에만** 열리므로 ViewModel 테스트가
> `backgroundScope`에서 `state`를 수집해 라우트의 `collectAsStateWithLifecycle()`을 흉내 내야 한다.
> 세 ViewModel 테스트가 각자 `shownViewModel()` 헬퍼로 같은 준비를 적는다.

## 신규 화면 추가 체크리스트
1. **api 모듈**: `NavKeyXxx`(@Serializable) 정의([[navigation-flow]]).
2. **impl 모듈**:
   - `XxxState : UiState`, `XxxIntent : UiIntent`, `XxxSideEffect : UiSideEffect` 정의.
   - `@HiltViewModel class XxxViewModel @Inject constructor(...) : BaseViewModel<XxxState, XxxIntent, XxxSideEffect>(초기상태)` — `processIntent` 구현.
   - `XxxScreen`/`XxxRoute` Composable: `state` 수집·렌더, `effect` 수집·처리(내비게이션은 `Navigator`).
   - 엔트리 빌더(`featureXxxEntryBuilder()`) 노출 + DI 등록([[navigation-flow]]).
3. 필요한 도메인 동작은 **UseCase**로([[0009-usecase-injectable-invoke]]), 데이터 접근은 Repository로.

## UI State가 담는 것 / 담지 않는 것

- **표시 문자열·리소스 ID를 State에 담지 않는다.** State는 도메인 의미를 들고, 표시 변환은 화면이 렌더 시점에 한다. 유효성 결과가 대표 사례다 — `NameValidResult.Error?`를 담고 화면이 `core:ui`의 `toStringResource(fieldType)` 확장으로 문자열을 얻는다([ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md), 원안 수렴 — #223 develop 머지 2026-08-13). ViewModel이 `@StringRes Int`를 산출해 담던 과도기 형태는 같은 매핑을 feature마다 복제해 폐기됐다.
- ⚠️ **리소스 ID를 든 enum이 State에 들어왔다**(2026-09-05, PR #449) — `CanvasMainUiState.tutorialStep`이
  `CanvasTutorialStep?`인데, 그 enum이 `@DrawableRes`·`@StringRes` 상수를 프로퍼티로 든다. State가
  직접 `Int`를 담지는 않아도 **표시 리소스가 State를 타고 흐르는 것은 같다.** 같은 라운드의 한 장짜리
  튜토리얼 둘은 `isTutorialVisible: Boolean`만 담고 리소스는 화면이 고른다 — **같은 기능이 화면 수에
  따라 두 형태로 갈렸다** → [open-questions](../synthesis/open-questions.md) OQ-P-369.
- **in-flight는 원인별로 나눠 들고 화면 덮개는 그 OR로 판단한다**(2026-08-17, PR #285·#287) —
  `GroupSettingUiState`가 `isLoadingDetail`·`isSubmittingNickname`·`isSubmittingDialogAction` 셋을
  따로 두고 `isLoading`이 파생이다. 합쳐 하나로 두면 "첫 조회 중"과 "왕복 중"을 가르지 못해 버튼
  활성 기준(`isConfirmEnabled`)을 만들 수 없다. 셋 다 `finally`에서 내려 **예외·취소 어느 경로로
  빠져나가도 로딩이 걸린 채 남지 않는다** → [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md).
  - 📌 **같은 형태가 S-001에도 생겼다(2026-08-19, PR #306)** — `AppSettingState`가 `isLoggingOut`·
    `isWithdrawing`을 따로 들고 `isLoading`이 둘의 OR다. **화면을 덮는 기준은 하나여도 항목 비활성
    기준은 원인별로 갈리기 때문에** 합칠 수 없다 — `YGActionItem(enabled = !isLoggingOut)`은 로그아웃
    항목 하나만 가리키므로, 필드가 하나면 탈퇴 왕복이 엉뚱한 항목을 비활성으로 만든다. 둘 다
    `finally`에서 내려 예외·취소로 빠져나가도 가드가 걸린 채 남지 않는다.
- ⚠️ **화면 덮개 판정 일부가 State 밖 Route 컴포지션에 산다**(2026-09-10, PR #482) — C-001이
  "이 캔버스를 이미 한 번 그렸는가"를 `CanvasMainRoute`의 `remember` 상태 셋(`paintedCanvasIds`·
  `sawLoading`·`observedCanvasId`)으로 들고, `isLoading`을 그것과 `UiState`를 함께 보아 만든다.
  판정 재료가 **이미지 로드 상태**라 ViewModel이 알 수 없는 것이 이 배치의 이유다. 대가는
  테스트다 — ViewModel 테스트가 닿지 않고 계측도 없어 **덮개가 다시 뜨는 회귀도, 아예 안 뜨는
  회귀도 자동으로는 안 잡힌다**([open-questions](../synthesis/open-questions.md) OQ-P-395)
  → [canvas-feedback-fixes 스펙](../superpowers/specs/archive/2026-09-10-canvas-feedback-fixes.md).
- **도메인 VO 보유는 허용**하되 강제는 아니다. S-101(`GroupSettingUiState`, #223 develop 머지)이 `GroupName`·`GroupNickname`·`InviteCode`를 State에 들인 첫 사례다. 단 **편집 중 입력값처럼 유효성이 보장되지 않는 값은 원시 타입으로 둔다** — VO로 감싸면 "타입은 맞는데 유효하지 않다"는 모순이 생긴다.
- 표시 규칙에 따른 분기(문구 선택·상태 enum 산출)는 화면의 private 헬퍼가 갖는다. State가 계산 프로퍼티로 들 이유가 없다.
  - ⚠️ **이탈 사례(2026-08-16, PR #259)** — C-201 캘린더의 `CanvasMainUiState.selectableMonths`가
    State 안 계산 프로퍼티다. 화면만 쓰는 값이 아니라 ViewModel의 연도 이동 계산도 읽어서 화면 헬퍼로
    내리면 로직이 갈린다 → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md) ·
    [open-questions](../synthesis/open-questions.md) [2026-08-16].
  - ⚠️ **같은 State가 계산 프로퍼티 일곱으로 늘었다(2026-08-17, PR #279)** — 저장 필드를
    **원천 둘**(`todayCanvas`·`viewedCanvas` — 뒤엣것은 PR #404에서 `pastCanvas`로 개명되고
    `displayedCanvas` 파생이 갈라져 나왔다)과 **캐시 하나**(`parfaitHistoriesByYear`)로 줄이고
    배경·토핑·날짜 라벨·빈 여부·오늘 여부·그 해 목록을 전부 파생으로 돌렸다. 방향 자체는 원천을
    하나로 모으는 것이라 이 규약의 취지와 어긋나지 않지만, **파생이 캐시가 가른 구분을 다시 뭉개는
    자리**(`parfaitHistories`의 `orEmpty()`)가 생겼다 → OQ-P-214 ·
    [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md).
- **앱 전역 사실은 State가 소유하지 않고 구독한다**(2026-08-16, PR #263). 계정 정보는 화면의 소유물이
  아니라 앱 수명 동안 하나뿐인 사실이라 `:data` SSoT에 살고, S-001·S-002는 `GetMyAccountFlowUseCase`를
  `init`에서 수집만 한다 — 화면 진입마다 다시 조회하지 않고, 한 화면의 닉네임 변경이 구독 중인 모든
  화면에 그대로 전파된다([ADR-0022](../adr/0022-user-info-local-ssot.md)).
  - **세 번째 구독자 G-001이 그 경계를 다시 보여 준다**(2026-08-20, PR #312). 같은 ViewModel이
    그룹 목록은 `Enter`(화면에 설 때마다)로, 닉네임은 `init`(한 번)으로 가져온다 — **남이 바꾸는
    값과 앱 전역 사실이 갈리는 자리**다. 이 화면은 닉네임을 그리지도 않는다(A-005로 넘기는 인자).
    ⚠️ 그래서 `null`을 로딩으로 다룰 화면 표현이 없고, 값이 아직 없으면 이동을 **조용히 건너뛴다**
    → OQ-P-253.
  - **`null`은 빈 문자열이 아니라 로딩이다.** mock 문자열을 지우면 기본값이 없어지므로 State가
    nullable을 들고 화면이 그것을 다룬다 — S-002는 레이아웃을 그대로 두고 **입력 필드만 비활성**한다
    (자리를 다른 것으로 바꾸면 값이 도착할 때 화면이 튄다).
  - **편집 화면은 저장값과 입력 버퍼를 나눠 갖는다** — `savedNickname`(SSoT가 준 값)과 `nickname`(입력).
    "서버 값과 다른가"(`isDirty`)를 알아야 확인 버튼 활성·뒤로가기 확인 모달이 성립하는데, 버퍼 하나로는
    사용자가 무엇을 바꿨는지 알 수 없다.
  - **낙관적 갱신을 하지 않는다.** 변경 성공 시에도 State에 직접 쓰지 않고 SSoT 구독이 새 값을
    되돌려준다 — 직접 쓰면 저장이 실패해도 화면만 바뀐 상태가 된다.
  - **그룹 정보도 같은 규약을 탄다**(2026-08-20, PR #307 develop 머지).
    G-001·C-001·S-101이 `GetMyGroupsFlowUseCase`·`GetGroupDetailUseCase`를 `init`에서 수집하고,
    서버 조회는 `Enter`(화면이 앞에 설 때)·`Refresh`가 따로 부른다 — 갱신 함수가 `Result<Unit>`이라
    화면이 조회 결과를 State에 넣을 길 자체가 없다([ADR-0023](../adr/0023-group-in-memory-ssot.md)).
    S-101은 그 덕에 닉네임 변경 성공 후 멤버 목록을 손으로 고치던 코드를 버렸다.
    - **여기서도 `null`은 빈 목록이 아니라 미조회다.** `GroupListUiState.groupList`가 nullable이고,
      조회 실패 시 에러 화면 판정(`isNullOrEmpty()`)과 0건 온보딩 툴팁 분기가 그 구분에 걸린다.
    - **구독은 `viewModelScope.launch`가 아니라 `BaseViewModel.launch`로 연다.** 무한 구독이라 key
      가드가 의미 없어 보이지만, 구독 시작부에서 다른 SSoT(계정 정보, DataStore)를 읽으면 그 실패가
      가드 없는 코루틴에서는 그대로 크래시가 된다 — S-101이 실제로 그 회귀를 냈다.
      (화면이 보는 동안만 살아야 하는 구독은 대신 [`launchWhileSubscribed`](#화면이-보는-동안만-살아야-하는-구독은-launchwhilesubscribed)다.)
- **화면이 앞에 설 때마다 다시 묻는다**(2026-08-17, PR #297). `init` 조회는 화면 수명이 아니라
  **ViewModel 수명**에 걸린다 — 백스택 아래 엔트리는 컴포지션에서 빠져도 ViewModel이 살아 있어 돌아온
  화면이 낡은 값을 그대로 보여 준다. G-001·C-001은 `Enter` 인텐트를 두고 Route의
  `LifecycleResumeEffect`가 그것을 보낸다. 근거는 **남이 바꾸기 때문**이다(목록의 최근 사진, 캔버스의
  토핑·멤버) — 내 앱 안의 변경만 좇는 구독으로는 최신이 되지 않아 SSoT 구독과 **별개 축**이다.
  - **재조회 빈도가 실패 표현을 바꾼다.** 조회가 재진입마다 나가면 실패마다 전면 에러 화면으로 넘기는
    규칙은 "뒤로 온 것만으로 목록이 사라진다"가 된다. G-001은 그래서 **보여 줄 것이 있으면 화면을
    유지**하고 사용자가 직접 당긴 새로고침 실패만 따로 알렸다(`ShowRefreshError` 토스트).
    - 🔁 **갈림의 기준이 바뀌었다(2026-09-04, PR #440 develop 머지)** — "목록이 남아 있는가"에서
      **"사용자가 당겼는가"**로 옮겼다. 당긴 새로고침은 화면이 이미 목록을 비운 뒤라 실패를 받아 줄
      자리가 에러 화면뿐이고, 재진입 조회 실패만 낡은 목록을 남긴다. 그래서 `ShowRefreshError`가
      도달 불가가 되어 이펙트·문자열과 함께 걷혔고, G-001 Route는 토스트 호스트를 다시 안 갖는다.
      `isError`에 규칙이 하나 붙었다 — **켜기만 하고, 끄는 것은 성공한 조회뿐**이다(끄면 실패한
      재진입 조회가 앞선 실패를 덮어 낡은 목록이 표시 없이 돌아온다). 덮개를 내리는 자리도 옮겼다:
      조회가 반환한 시점이 아니라 **캐시가 목록을 실제로 낸 시점**이다(구독은 시작하자마자 `null`을
      한 번 내므로 그때는 안 내리고, 조회가 실패하면 캐시가 아무것도 안 내므로 실패 갈래에서 따로
      내린다) → OQ-P-348.
  - **재진입에 다시 부를 값과 아닐 값을 가른다** — C-001은 오늘 캔버스와 **올해** 달력 기록만 다시 받고,
    연도 목록(해가 바뀔 때만 늘어난다)과 지난 날 캔버스(마감돼 안 바뀐다)는 그대로 둔다.
  - **재진입은 시간이 흐른 지점이기도 하다** — 두 화면 다 이때 오늘을 다시 센다(G-001은 날짜 헤더,
    C-001은 `syncToday()`가 보던 캔버스까지 정리). 기준은 KST 자정이라 03:00 경계는 여전히 미적용이다.
  - ⚠️ 관용구일 뿐 규약이 아니다 — 새 화면이 이것을 따르는지 확인할 수단이 없다 → OQ-P-221 ·
    [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md).
  - 📌 **같은 수명을 반대로 쓰는 자리가 생겼다**(2026-09-01, PR #411). C-001 환영 배너는 재진입마다
    다시 뜨면 안 되므로, `init`이 **ViewModel 수명에 걸린다는 성질 자체를 1회 보장으로** 쓴다 —
    `welcomeGroupName`이 `null`이 아니면 `init`에서 `ShowWelcome` 이펙트를 한 번 쏘고 그만이다.
    재조회 관용구가 이 수명을 **결함으로 보고 `Enter`로 우회한 것과 정반대 방향**이고, 그래서 같은
    성질이 화면 안에서 두 뜻으로 쓰인다. 다만 이 보장은 상태가 아니라 수명에 기대므로 키가 다시
    살아나는 경로에서는 성립하지 않는다 → OQ-P-339.
  - **같은 화면에 두 번째 수명 이펙트가 붙었다**(2026-08-20, PR #298). C-001은 `LifecycleResumeEffect`로
    재조회를 보내고 `LifecycleStartEffect`로 **Spotlight를 해제**한다 — 되묻는 것과 화면 상태를 되돌리는
    것이 서로 다른 시점에 걸린다(C-202 정책이 "백그라운드 복귀 시 Default"를 규정한다)
    → [c202-canvas-spotlight 스펙](../superpowers/specs/archive/2026-08-20-c202-canvas-spotlight.md).
- **서버 실패 갈래는 feature 로컬 enum**이다 — S-002의 `GlobalNicknameError` 4종. 형식 오류
  (`NameValidResult.Error`, 요청 전 검사)와 **별개 축**이라 State가 둘을 따로 들고 화면이
  `nicknameError ?: submitError` 순으로 보여준다. 문구 매핑은 같은 모듈의 `@Composable` 확장이 갖는다 —
  소비처가 하나면 feature 로컬이 맞다는 [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)
  애드덤이고, `feature` `impl`은 서로를 의존하지 않는 leaf라 형제 화면의 `GroupNickNameError`를 재사용할
  수도 없다(문구 중복은 [open-questions](../synthesis/open-questions.md)가 추적 중이다).
  > 📌 **두 번째 사례 — A-002 `LoginError` 4종(PR #267 develop 머지).** 같은 형태이고, 여기서 규약이
  > 하나 더 드러났다: **로그 분기는 8갈래인데 사용자 갈래는 4개다.** 502·503·SDK 실패가 사용자에겐
  > "잠시 후 다시"로 같아서 한 갈래로 묶인다 — enum이 세는 것은 서버 에러 코드가 아니라 **문구가
  > 갈리는 지점**이다. `SideEffect`가 실어 보내는 것도 문구가 아니라 **사유**(`ShowError(error: LoginError)`)이고,
  > 문구는 Route가 `LoginError.entries.associateWith { it.toStringResource() }`로 **컴포지션에서 미리 뽑아
  > 둔다** — 이펙트 수집은 코루틴이라 `stringResource`를 부를 수 없고, `LocalContext.getString` 우회는
  > 로케일 변경 때 갱신되지 않는다(`LocalContextResourcesRead` 린트).
  > 함께 **`launch(onError = …)`를 붙여야 한다** — 안 붙이면 `Result.failure`만 알려지고 UseCase가
  > 예외를 던지는 경로가 조용해진다([ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md)이 "이 자리가
  > 통로"라고 지정한 곳이다).
  > 📌 **가장 좁은 사례 — 온보딩 약관의 `TermAgreeError` 2종(2026-08-20, PR #315 develop 머지).**
  > 갈래 넷(`SignUpException.RequiredPolicyNotAgreed`·`AppError.Network`·`AppError.Server`·그 외)이
  > `NETWORK`·`UNKNOWN` **둘로 접힌다.** `LoginError`가 보여 준 "enum이 세는 것은 에러 코드가 아니라
  > 문구가 갈리는 지점"을 끝까지 민 형태이고, **결함으로 로그를 남기는 갈래까지 사용자에겐 같은
  > 문구**라는 것이 새로 드러난 점이다(화면 가드가 뚫린 경우도 처분이 "잠시 후 다시"로 같다).
  > 같은 화면이 **enum에 넣지 않은 실패**도 함께 보여 준다 — 약관 조회 실패는 `isLoadFailed`로
  > 화면에 남는다. 갈림길은 **재시도 동선이 화면 안에 있는가**이고, 있으면 State가 들고 없으면
  > 사유 enum + 토스트다 → [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md) "실패 표현".
- **요청 중 플래그는 `finally`로 내린다** — S-001 로그아웃(`isLoggingOut`, PR #260)이 `launch(key)` 중복
  가드 위에 State 플래그를 한 겹 더 두는 사례다. `launch(key)`는 두 번째 탭을 삼킬 뿐 버튼이 눌리는
  것처럼 보이므로 비활성은 State로 드러낸다(아래 "안티패턴" 1번의 반대 사례).
  > 📌 **네 번째 사례 — C-301 배경 저장의 `CanvasBGEditError` 3종(2026-08-22, PR #329).** 앞의 셋과
  > 같은 형태이고(사유 enum + `entries.associateWith` 문구 사전 + 토스트), 새로 드러난 것은 **갈래
  > 하나가 서버가 아니라 기기에서 온다**는 점이다 — `AppError.UnsupportedImage`가 "이 사진 자체가
  > 안 된다"를 뜻하고, 그 갈래만 **재시도가 무의미**해서 문구가 "다른 사진을 골라 주세요"로 갈린다.
  > 나머지 실패는 전부 `UNKNOWN`으로 접히는데, 그래서 마감된 캔버스의 409도 "잠시 후 다시"가 된다
  > → [open-questions](../synthesis/open-questions.md) OQ-P-261.
  > 📌 **다섯째 사례와 전달 축 하나가 한 라운드에 함께 들어왔다(2026-08-27, PR #393·#394).**
  > A-005 그룹 생성이 `GroupCreateError` 2종(`NETWORK`·`UNKNOWN`)을 새로 들었고, 이로써 **실패를
  > 로그로만 남기던 화면이 사라졌다**(OQ-P-167 ④). 둘로 접은 근거는 `TermAgreeError`와 같다 —
  > 서버 400 세 갈래는 사용자가 손쓸 수 없어 문구를 나눠도 할 일이 달라지지 않는다.
  > S-102 그룹 참여는 갈래가 아니라 **전달 축**이 바뀌었다. `GroupNickNameUiState.submitError`가
  > 사라지고 `ShowError(error)` 이펙트가 그 자리를 받아, 입력칸 아래는 형식 오류(`nicknameError`)
  > 전용이 됐다 — 한 화면이 인라인과 토스트를 함께 쓰는 형태로 S-101과 같아졌다.
  > ⚠️ 그래서 [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)이 적어 둔
  > "둘을 동시에 들고 `nicknameError ?: submitError`로 형식 오류를 먼저 보여준다"는 이제
  > **S-002 계정 정보에만 남는 서술**이다.
  > **새로 드러난 것은 갈래 하나가 실패가 아니라는 점이다** — `NICKNAME_NOT_APPLIED`는 참여가 이미
  > 끝난 뒤 닉네임 `PATCH`만 실패한 경우라 흐름을 멈추지 않는다. 그래서 이펙트를 쏜 뒤
  > **토스트가 스스로 사라질 때까지 기다렸다가 이동한다** — 토스트 호스트가 이 화면에 매여 있어
  > 곧바로 넘기면 뜨자마자 함께 사라지기 때문이다. 실패 안내가 이동을 지연시키는 첫 사례이고,
  > 기다리는 시간이 `YGToastPolicy`와 별개 상수라는 점은
  > [open-questions](../synthesis/open-questions.md) OQ-P-328이 쥔다.
- **이동 전에 끝내야 하는 일이 있으면 순서가 계약이 된다** — C-103 후보 선택(2026-08-24, PR #342)이
  그 사례다. 탭 하나가 **저장 → 초안 기록 완료 → `isLoading` 해제 → `GoToConfirm` post** 순으로
  돌고, 이 순서를 지키는 이유는 다음 화면에 있다: `SegmentationConfirmViewModel`은 정상 진입에서
  스스로 초안을 적지 않고 **구독만** 하므로 이 화면이 초안의 **유일한 writer**이고, 기록보다 이동이
  앞서면 그 화면이 첫 방출에서 `DraftMissing`으로 "다음"을 잠근 채 뜬다. 기록이 실패하거나 흐름
  미개시를 알리면 **이동하지 않고** 실패 이펙트로 접는다.
  - **로딩 해제가 갈래마다 따로 놓인다** — 성공·`Result.failure`·`launch(onError)` 셋이다.
    이동이 `goTo`라 이 화면과 ViewModel이 백스택에 남고, 켠 채 나가면 **돌아왔을 때 오버레이에
    갇힌다.** 아래 "안티패턴" 1번이 금지하는 것은 마지막 줄 한 곳에 몰아 두는 형태이고, 여기서는
    세 갈래가 각각 내린다.
  - **중복 탭 방어는 `launch(key)`만으로 끝냈다** — 위 로그아웃 사례와 달리 State 플래그를 한 겹
    더 두지 않는다. 눌리는 것이 버튼이 아니라 사진 위 점선 박스이고, `isLoading` 오버레이 자체가
    이미 "받는 중"을 그리기 때문이다 → [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md).
- ⚠️ **UI 타입 보유 사례(2026-08-15, PR #231)** — C-301 배경 편집의 `CanvasBGEditUiState`가 Compose
  `Color`를, `CanvasBGEditEffect.ConfirmBackground`가 디자인시스템 타입 `YGCanvasBackground`를 든다.
  선택 팔레트(`CanvasBackgroundPaletteColors`)도 ViewModel 파일의 public 상수다. 위 "표시 문자열을
  담지 않는다"의 같은 결에서 보면 이탈이지만, 배경색은 **도메인 의미가 아직 없는 값**(저장·서버 계약이
  없다)이라 대체 표현도 정해져 있지 않다 → [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) ·
  [open-questions](../synthesis/open-questions.md) [2026-08-15].
  > 📌 **전제 하나가 사라졌다(2026-08-22, PR #329)** — 이제 배경에는 저장·서버 계약이 있다
  > (`CanvasBackgroundEdit`·`CanvasBackground`). 그럼에도 **화면 타입은 그대로 남겼다** — 확인은
  > `Color.toRgbHex()`로 경계에서만 도메인 값을 만들고, 저장 성공 이펙트는 여전히
  > `YGCanvasBackground`를 싣는다(실은 Route가 그 값을 쓰지도 않는다 — 돌아간 캔버스 메인이 재조회로
  > 그린다). 즉 이탈의 근거는 "도메인 의미가 없다"에서 **"화면이 도메인 타입을 들 이유가 아직
  > 없다"**로 바뀌었다 → [open-questions](../synthesis/open-questions.md) OQ-P-194 ①.

## 안티패턴 (금지)
- `launch` 블록의 **마지막 줄**에서 로딩 플래그를 되돌리기 → 던지거나 취소되면 도달하지 못해
  버튼이 영구 비활성으로 남는다. `finally`에 둔다.
- 한 ViewModel의 `effect`를 **두 곳에서 수집** → 이펙트가 한쪽에만 간다(로그로 드러난다).
- side effect(내비게이션 등)를 **state에 담기** → 재구성 시 중복 실행. 반드시 `SharedFlow<E>`.
- Screen에서 Repository/UseCase 직접 호출 → 반드시 ViewModel 경유.
- **ViewModel이 Repository를 직접 주입** → 반드시 UseCase 경유(2026-09-09, PR #479로 마지막 넷이
  정리됐다 — `feature/*/impl` ViewModel 21개 중 위반이 넷이었고 전부 `ToppingDraftRepository`
  하나였다). ⚠️ **컴파일러가 검사하지 않는다** — `feature/*/impl`은 `:domain` 전체를 보므로 새
  ViewModel이 다시 직접 받아도 빌드가 통과한다
  → [topping-draft-usecase-extraction 스펙](../superpowers/specs/archive/2026-09-09-topping-draft-usecase-extraction.md).
- 표시 문자열 매핑을 **feature마다 복제** → 공유 도메인 규칙엔 공유 매핑([ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)).
