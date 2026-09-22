---
id: navigation-flow
title: 내비게이션 흐름 (Navigation3 + Navigator)
category: architecture
status: living
platforms: android
verified: 2026-09-21
related_spec: c103-multi-subject-selection, segmentation-pipeline-hardening, designsystem-ygscreen-scaffold, a005-group-create, a004-group-invite-code, s102-group-nickname, g001-group-list, c101-camera-picture-confirm, c102-custom-gallery-picker, intro-term-agree, a002-login-onboarding, c001-canvas-main, a002-kakao-login-api, c301-canvas-background-edit, session-token-refresh-infra, c201-canvas-calendar, user-info-ssot, c301-topping-edit-tab, ygscaffold-v2-common-loading-error, s101-group-setting-api, canvas-save-preview-capture-holder
related_adr: ADR-0002, ADR-0006, ADR-0013, ADR-0021, ADR-0022
related_architecture:
related_code: core:navigation, Navigator, Navigator.kt#popUpTo, NavTransition
tags: [architecture, parfait]
---
# 내비게이션 흐름 (Navigation3 + Navigator)

Navigation3 위에 자체 Navigator·엔트리 빌더를 얹는다. 결정 근거는 [[0006-navigation3-custom-navigator]]·[[0002-feature-api-impl-split]].

> 근거는 파일명+심볼명으로만.

## 구성 요소
- **Navigator**(`core:navigation`, `@ActivityRetainedScoped`) — 백스택 = `SnapshotStateList<NavKey>`. `goTo()`, `goToSingleClearTop()`, `goToAndPopCurrent()`, `replaceAll()`, `popUpTo()`, `onBack()`.
  - `replaceAll(destination)`(#260 신설, **`clearBackStack()` 대체**) — 백스택을 비우고 목적지 하나만
    남긴다. 비우기와 채우기를 나눠 노출하면 그 사이에 **빈 백스택**이 생기고 채우는 것은 호출부의
    규약일 뿐이라, 빈 상태를 만들 수 있는 API 자체를 없앴다(빈 백스택은 `onBack`이 이미 방어하고 있는
    크래시 원인이다). `clearBackStack()`은 제거됐고 기존 호출부 3곳(`SplashRoute`·`TermAgreeRoute`·
    `LoginRoute`)이 함께 옮겨졌다. `NavigatorTest`가 이 성질을 잠근다 — 저장소에서 `Navigator`에
    테스트가 붙은 것도 이번이 처음이다.
  - `goToSingleClearTop(destination)`(#224 신설) — 대상이 백스택에 있으면 **그 위를 한 번에 잘라내(`removeRange`) 기존 엔트리를 재사용**하고, 없으면 `goTo`처럼 새로 쌓는다. 한 칸씩 빼면 스냅샷 변경이 그만큼 쌓이므로 범위 삭제로 처리한다. 엔트리 재사용이므로 대상 화면의 상태·ViewModel이 그대로 살아난다(`init` 조회는 다시 돌지 않는다 — 돌아온 화면이 다시 조회하려면 화면 쪽에 `Enter` 인텐트가 있어야 하고, G-001·C-001이 #297에서 그것을 갖췄다 → [state-management](state-management.md)).
  - `goToAndPopCurrent(destination)`(#221 신설) — 지금 화면을 대상으로 **치환**한다(마지막 칸에 덮어쓰기).
    백스택 깊이가 늘지 않고 뒤로 가면 지금 화면을 건너뛴다. 스택이 비어 있으면 그냥 쌓는다.
    확인·경유 화면처럼 되돌아올 이유가 없는 자리에 쓴다(첫 사용처: C-101-confirm → C-103).
  - `popUpTo<T>()` / `popUpTo(type: KClass<out NavKey>)`(`Navigator.kt#popUpTo`, PR #309 develop 머지,
    2026-08-20) — 백스택에서 `T` 타입 키를 뒤에서부터 찾아 있으면 그 위를 전부 걷어내고
    `true`를, 없으면 아무것도 하지 않고 `false`를 준다. **`goToSingleClearTop` 대신 이것을 쓰는 경우는
    호출부가 목적지 키의 인자를 모를 때다** — `goToSingleClearTop`은 키 동등성 비교라 `NavKeyCanvasMain`의
    `groupId`를 알아야 하는데, 카메라·세그멘테이션 쪽 닫기 콜백은 그 값을 들고 있지 않다. NavKey 다섯
    개에 `groupId`를 실어 나르는 대안은 배경 편집처럼 그 값이 무의미한 경로에도 인자를 붙이게 돼
    기각했다. reified 버전은 호출부 편의이고 `KClass` 버전이 실제 구현·테스트 대상이다.
    **대상을 못 찾으면 아무것도 걷지 않고 `false`를 주므로 "백스택에 있는지" 확인이 반환값으로 끝난다** —
    별도 조회 API를 두지 않은 이유다. 소비처는 Route 넷이다: `PictureConfirmRoute`(`returnResultOnly = false`)·
    `SegmentationRoute`·`SegmentationConfirmRoute`의 닫기 → `popUpTo<NavKeyCanvasMain>()`,
    `PictureConfirmRoute`(`returnResultOnly = true`)의 확인·닫기 → `popUpTo<NavKeyCanvasBGEdit>()`,
    `CanvasToppingPlaceRoute`의 배치 완료 → `popUpTo<NavKeyCanvasMain>()`
    ([segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)).
- **NavKey**(각 feature `:api`, `@Serializable`) — 목적지 식별. 예: `NavKeyLogin`, `NavKeySegmentation`, `NavKeyCameraCustom`. groups·app 계열은 목적지가 많다: `NavKeyGroupList`·`NavKeyGroupSetting`·`NavKeyGroupInviteCode`, canvas의 `NavKeyCanvasMain`·`NavKeyCanvasBGEdit`(#231)·`NavKeyCanvasToppingPlace`(#290)·`NavKeyCanvasImageSave`(#445), `NavKeyAppSetting` 등(`NavKeyCanvasEdit`·`NavKeyCanvasImageSelect`·`NavKeyCanvasMove` 셋은 도달 불가로 남아 있다가 #514에서 삭제됐다). 전체 목록은 `feature/*/api`에서 확인(모듈 목록은 [module-structure](module-structure.md)).
- **엔트리 빌더**(각 feature `:impl`) — `entry<NavKeyXxx> { ... }`를 등록하는 함수(예: `featureLoginEntryBuilder()`). Hilt 멀티바인딩 `Set<EntryProviderScope<NavKey>.(Navigator) -> Unit>`로 주입. **빌더 하나가 여러 entry를 등록할 수 있다** — 예: `featureCanvasEntryBuilder()`는 canvas NavKey(`Main`·`BGEdit`·`ImageSave`(#445)·`ToppingPlace`) entry를 한 함수에서 등록(`Edit`·`ImageSelect`·`Move` 셋은 #514에서 삭제).
- **NavTransition**(`core:navigation`, #326 신설) — 화면 전환 한 벌(`push`·`pop`·`predictivePop`)을
  묶은 값. `metadata`로 NavEntry에 실어 화면별로 앱 기본을 덮는다 → 아래 [화면 전환](#화면-전환-2026-08-22-pr-326).
- **MainRoute**(`app`) — 주입된 빌더 집합을 `entryProvider { }` DSL로 순회 등록. NavEntry 데코레이터 적용:
  - `rememberSaveableStateHolderNavEntryDecorator` — 엔트리별 상태 보존.
  - `rememberViewModelStoreNavEntryDecorator` — 엔트리별 ViewModel 수명.
  - `rememberResultEventBusNavEntryDecorator` — 엔트리 간 결과 전달.
- **백스택은 저장되지 않는다** — `Navigator`가 백스택을 `mutableStateListOf`로 들고
  `@ActivityRetainedScoped`로 살 뿐이다. `MainRoute`는 그것을 그대로 `NavDisplay`에 넘기며
  `rememberNavBackStack`도 `SavedStateHandle`도 쓰지 않고, `MainActivity.onCreate`도
  `savedInstanceState`를 읽지 않는다. 그래서 **프로세스가 죽고 돌아오면 백스택은
  `NavigatorConst.INITIAL_NAVIGATION_KEY`(=`NavKeySplash`) 하나로 리셋된다.** NavKey가
  `@Serializable`인 것은 Navigation3의 관용구이고, 이 앱이 그것을 실제로 직렬화해 저장하는 자리는
  없다. **이 성질은 아래 모든 흐름의 전제다** — "프로세스 사망 뒤 복원"을 전제한 서술은 이 앱에서
  성립하지 않는다. `NavKeyCanvasImageSave`의 KDoc("NavKey 는 직렬화돼 오간다")과 OQ-P-364 ①이 그
  전제로 쓰여 있다 → [open-questions](../synthesis/open-questions.md) OQ-P-364.

## 이동/뒤로
- 이동: ViewModel의 side effect → Screen이 소비 → `navigator.goTo(NavKeyXxx(...))`.
- 뒤로: `navigator.onBack()`. **빈 백스택 접근 가드 필수**(과거 크래시 이력) — `backStack.size <= 1`이면 no-op.
- feature 간 이동은 상대 `:impl`이 아니라 **`:api`의 NavKey만** 참조.

## 화면 전환 (2026-08-22, PR #326)

전환이 처음으로 앱의 결정이 됐다. 그전까지는 Navigation3 기본(페이드 + 축소)을 그대로 썼고,
지금은 **새 화면이 오른쪽에서 들어와 지금 화면을 덮고 뒤로 가면 오른쪽으로 빠진다.**

- **한 벌로 묶는다** — `NavTransition`(`core:navigation`)은 `push`·`pop`·`predictivePop` 셋을 한
  값으로 들고, 셋을 각각 넘기지 않는 이유가 **방향이 짝을 이뤄야 하나의 동작으로 읽히기** 때문이다.
  한쪽만 갈아 끼우면 오른쪽에서 들어와 놓고 아래로 빠지는 식이 되기 쉽다. `metadata`는 세 슬롯
  (`NavDisplay.transitionSpec`·`popTransitionSpec`·`predictivePopTransitionSpec`)을 한 번에 채운
  맵이라 엔트리 쪽에서는 `entry<K>(metadata = ….metadata)` 한 줄이다.
- **`predictivePop`만 인자를 받는다** — 끌어당긴 가장자리(`swipeEdge`)에 따라 화면이 빠지는 방향이
  반대라 `pop`과 같은 모양을 쓸 수 없다. `targetSdk`가 36이라 시스템 predictive back은 opt-out을
  하지 않는 한 켜져 있지만, **이 경로가 실기기에서 도는 것을 본 기록은 없다**(OQ-P-260).
- **어느 화면에 붙는지가 함정이다** — `NavDisplay`는 **위에 놓이는 화면**, 즉 새로 쌓이거나 지금
  걷히는 화면의 메타데이터만 본다. A → B 전환의 모양은 **B에 붙인 것**이 정하고, B에서 A로 되돌아올
  때도 마찬가지로 B의 것이 쓰인다. "이 화면을 떠나는 모양"을 A에 붙이려는 것이 자연스러운 오해다.
- **앱 기본은 `NavDisplay` 인자로 직접 물린다** — `NavTransition.Default`의 세 함수를
  `transitionSpec`·`popTransitionSpec`·`predictivePopTransitionSpec`에 넘긴다. ⚠️ **같은 세 줄이
  `app`의 `MainRoute`와 `app-preview`의 `RootRoute` 두 곳에 있다** → OQ-P-259.
  📌 **두 루트가 얇아졌다(2026-09-20, PR #514)** — 둘 다 `NavDisplay`를 감싸던
  `SharedTransitionLayout` + `CompositionLocalProvider` 껍질을 벗고 `NavDisplay`를 바로 부르며,
  `modifier`가 껍질 대신 `NavDisplay`로 내려갔다. **복제 자체는 그대로다** — 데코레이터 셋과 전환
  세 줄은 여전히 두 파일에 나란히 있다.
- 🔁 **예외가 없어졌다(2026-09-20, PR #514)** — 유일한 예외는 `NavKeyCanvasEdit` 엔트리의
  `NavTransition.Fade.metadata`였다. 그 화면과 `NavKeyCanvasImageSelect`가 **사진 하나를 공유 요소로
  이었고**(`LocalSharedTransitionScope`, 두 Screen이 `sharedElement`), 화면 전체가 옆으로 밀리면 정작
  봐야 할 사진의 이동이 묻히기 때문이었다. 도달 불가 화면 셋이 삭제되면서 그 예외도 함께 걷혔고,
  **`LocalSharedTransitionScope`와 두 루트의 `SharedTransitionLayout` 껍질도 사라졌다**(공유 요소를
  쓰는 화면이 하나도 남지 않았다). 지금 develop에 `NavTransition.Fade`를 다는 엔트리는 **0건**이고
  모든 화면이 `Default`를 쓴다 → OQ-P-260 ③ 해소. ⚠️ 그래서 **`Fade` 프리셋 자체가 사용처 0으로
  남았다** — `NavTransitionTest`가 슬롯만 잠그므로 컴파일도 테스트도 이것을 잡지 못한다 → OQ-P-404.
- **`Fade`를 고르는 기준**(브랜치 KDoc에 있다가 최종 커밋에서 지워져 여기로 옮긴다 —
  [parfait/CLAUDE.md](../code-conventions.md) 최소 보존선): **방향을 말할 수 없는 전환**에 쓴다. 앞뒤 관계가
  없는 경계(스플래시 → 첫 화면)이거나, 공유 요소가 자리를 옮기는 전환이다.
  ⚠️ 기준은 남았지만 **그 기준에 해당하는 화면이 지금 하나도 없다**(위 🔁).
- **`NavTransitionTest`가 잠그는 것은 슬롯이 다 찼는지 하나다** — 프리셋마다 세 키가 모두 있고,
  `copy()`로 한 슬롯만 갈아도 나머지 둘이 그대로 실려 나가는지. 하나라도 비면 **그 방향만 라이브러리
  기본으로 튀어** 앞뒤가 안 맞는다. 전환의 모양 자체는 단언 대상이 아니다(실기기 몫).

## 앱 진입 체인 (2026-08-09, PR #220)

시작 목적지는 `NavigatorConst.INITIAL_NAVIGATION_KEY = NavKeySplash`(`core:navigation`)다.
그 뒤 체인이 이 PR에서 처음 끝까지 이어졌다:

`NavKeySplash` → `NavKeyLogin` → `NavKeyTermAgree` → `NavKeyGroupList`

> 📌 **첫 화면이 갈림길이 됐다(2026-08-16, PR #263)** — 스플래시가 더는 무조건 로그인으로 가지 않는다.
> `BootstrapSessionUseCase`가 저장된 refresh token 유무를 보고, 있으면 `users/me`로 세션을 검증하면서
> 계정 정보 SSoT를 채운 뒤 **`NavKeySplash` → `NavKeyGroupList`**로 바로 넘긴다. 목적지 판단은 전부
> 도메인이 하고(`SessionBootstrap.ToGroupList`/`ToLogin`) 화면은 그것을 이펙트로 옮기기만 한다 —
> 스플래시가 "토큰이 있나", "조회가 됐나"를 알지 않는다. 실패는 종류와 무관하게 `ToLogin`이고
> 갈리는 것은 정리 범위뿐이다(인증 거절만 세션 파기) →
> [user-info-ssot 스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md).
> ⚠️ **네트워크 실패도 로그인 화면으로 보낸다** — 오프라인에서 앱을 켜면 토큰이 남아 있어도 로그인
> 화면이다(그룹 목록을 캐시로 그릴 수단이 아직 없다) → [open-questions](../synthesis/open-questions.md).
> 📌 **떠나는 시점에 조건이 하나 더 붙었다(2026-08-18, PR #305)** — 스플래시가 로띠를 재생하게 되면서
> **부트스트랩 응답과 애니메이션 종료가 모두 끝나야** 이동 이펙트가 나간다. 둘은 서로를 기다리지 않고
> 각자 끝나므로 순서가 정해져 있지 않고, `SplashState(destination, isAnimationFinished)`에 각각 남겨
> 나중에 끝난 쪽이 이동을 일으킨다. 화면은 "다음이 어디인가"를 여전히 모르고 **"내 애니메이션이
> 끝났다"만 인텐트로 올린다**(`SplashIntent.AnimationFinished`). 같은 신호가 두 번 와도 첫 번만
> 받아들인다 — 컴포지션이 다시 서면 백스택을 두 번 갈아 끼우게 된다. 로띠 **파싱 실패도 '끝'으로
> 넘긴다**(그 신호가 없으면 스플래시를 벗어날 방법이 아예 없다). 대기 상한이 없고 실기기 확인도
> 없다 → [open-questions](../synthesis/open-questions.md).

- 이전에는 로그인이 `NavKeyGroupHome`(`ResultEventBus` 시연용 임시 화면)으로 갔고, `NavKeyTermAgree`·
  `NavKeyGroupList`는 entry만 등록된 **도달 불가 화면**이었다. 체크리스트 6번의 사례가 하나 닫힌 것.
- **백스택 리셋 관용구**: 되돌아가면 안 되는 경계에서 **`navigator.replaceAll(...)`** 한 줄을 부른다 —
  `SplashRoute`(→ 로그인 **또는 그룹 목록**), `TermAgreeRoute`(→ 그룹 목록),
  `LoginRoute`(기존 회원 → 그룹 목록) 3곳.
  결과적으로 그룹 목록에서는 백스택이 1개라 뒤로가기가 no-op이다.
  > 🔁 **2026-08-15(PR #260)** — 그전까지는 `clearBackStack()` + `goTo(...)` 두 줄이었고 "반드시 같은
  > 블록에서 `goTo`가 따라와야 한다"가 암묵 규약이었다. 그 규약을 타입에서 지우려고 두 함수를
  > `replaceAll`로 합치고 `clearBackStack()`을 제거했다 → [open-questions](../synthesis/open-questions.md) [2026-08-12].
  > 📌 **네 번째 자리가 생겼다(2026-08-17, PR #287)** — S-101 그룹 나가기·신고 성공 시
  > `replaceAll(NavKeyGroupList)`다. 앞의 셋과 달리 **되돌아갈 화면이 없어서가 아니라 되돌아가면
  > 전부 403이라서**다 — 백스택에 쌓인 화면이 방금 떠난 그룹의 것이고, 그 그룹의 상세·닉네임 변경·
  > 신고는 나간 뒤 `GROUP_NOT_JOINED`로 떨어진다. 부수 효과로 목록이 새 엔트리라 다시 조회된다.
- 의존 방향은 규약대로 `:api`만: `feature/login/impl` → `feature/intro/api`,
  `feature/intro/impl` → `feature/groups/list/api`.
- ~~**화면 전이만 결선됐다**~~ → ✅ **데이터까지 이어졌다(2026-08-15, PR #241·#242)**. 로그인이
  카카오 `idToken`으로 서버 인증을 하고 `isNewUser`로 갈라(기존 회원은 목록, 신규는 약관) 약관 화면이
  `POST /auth/signup`으로 동의를 보내고 **세션을 저장한 뒤** 목록으로 넘어간다. 즉 이 체인의 세 구멍
  (인증·회원 분기·동의 저장)이 닫혔다 → [open-questions](../synthesis/open-questions.md) [2026-08-10].
  **기존 회원은 약관 화면을 거치지 않으므로 백스택 리셋 지점이 두 곳**이다 — `LoginRoute`가 기존 회원이면
  `replaceAll(NavKeyGroupList)`, 신규면 리셋 없이 `goTo(NavKeyTermAgree(registrationToken))`이라
  약관 화면에서 뒤로 가면 로그인으로 돌아간다. 리셋은 그 뒤 `TermAgreeRoute`가 한다.
- 📌 **체인 첫 화면이 실물이 됐다(2026-08-11, PR #218)** — A-002 로그인의 온보딩 자리가 placeholder
  박스에서 일러스트 3장으로 채워졌다. 전이·인증 구조는 그대로다(카카오 토큰은 여전히 `LoginState`
  안에서 끝난다) → [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md).

## 세션 종료 이동 (2026-08-15, PR #260)

화면이 아니라 **네트워크 계층이 이동을 일으키는 첫 경로**다. 상세는
[session-token-refresh-infra 스펙](../superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md).

```
TokenAuthenticator(재발급 거절) → SessionEventBusImpl.postForcedLogout()
    → MainRoute의 LaunchedEffect 단일 수집 → navigator.replaceAll(NavKeyLogin)
```

- **수집 지점은 앱 루트 `MainRoute` 한 곳**이다(`NavDisplay` 상위 `LaunchedEffect`). 화면마다 구독하면
  한 이벤트로 이동이 여러 번 일어난다. `SessionEventBus`(#450 개명, 구 `SessionEventSource`)는 `MainActivity`가 주입받아 내려준다.
- 통로는 `Channel(CONFLATED)`이라 **단일 소비자**이고, 401이 여러 건 터져도 이동은 한 번이다. 이
  성질이 규약이 아니라 타입에서 나온다는 점이 화면 이펙트(`BaseViewModel`)와 같다([ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md)).
- 사용자가 직접 누르는 로그아웃은 같은 목적지를 화면 이펙트로 간다 — S-001 앱 설정의
  `AppSettingSideEffect.NavigateToLogin` → `replaceAll(NavKeyLogin)`. 즉 **같은 이동에 진입점이 둘**이고
  둘 다 백스택을 비운다.
- ⚠️ **수집 지점이 하나라는 것은 규약일 뿐 기계 검사가 없다** → [open-questions](../synthesis/open-questions.md) [2026-08-16].

## 푸시 딥링크 이동 (2026-09-05, PR #446·#447 · 2026-09-06, PR #456)

**네트워크 계층에 이어 두 번째로 화면 밖이 이동을 일으키는 경로**이고, 구조는 위 세션 종료 이동을
그대로 본떴다. 근거는 [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정,
계약 대조는 [api/notification.md](../api/notification.md) "푸시 수신·딥링크".

```
알림 탭 → MainActivity(onCreate | onNewIntent) → Intent.toPushDeepLinkOrNull()
    → PushDeepLinkEventBus.post() → MainRoute의 LaunchedEffect 단일 수집
    → 스플래시 이탈 대기 → 세션 없으면 버림
    → AddTopping: navigator.goTo(NavKeyCanvasMain(groupId))
    → GroupList: navigator.goTo(NavKeyGroupList)
```

- **수집 지점은 앱 루트 `MainRoute` 한 곳**이다(세션 사건과 같은 이유 — 화면마다 구독하면 한 번의
  탭으로 이동이 여러 번 일어난다). 통로도 같은 `Channel(CONFLATED)`이라 **단일 소비자**이고, 알림을
  연달아 탭하면 **마지막 것 하나로 접힌다**.
- **발행은 `MainActivity`가 두 자리에서 한다** — 콜드 스타트는 `onCreate`의 `intent`, 이미 떠 있으면
  `onNewIntent`다. 뒤엣것이 성립하려면 매니페스트의 `launchMode="singleTop"`이 필요하고 이 PR이
  그것을 함께 붙였다(앱 전 화면에 걸리는 매니페스트 변경이다 — 단일 액티비티라 그렇다).
- **같은 딥링크가 두 번 발행되는 경로가 둘이고, 막는 수단도 둘이다.**
  - **액티비티 재구성** — 화면 회전이 없어도 재구성이 일어나는 경로(다크모드 토글·폰트/언어 변경·
    폴더블 리사이즈)에서 `onCreate`가 같은 `intent`를 다시 받는다. 소비한 `Intent`를
    `setIntent(Intent())`로 비워 막는다.
  - **태스크 되살리기** — 알림이 만든 인텐트는 그것이 띄운 **태스크의 base intent**로 남는다.
    뒤로가기로 액티비티가 끝나도 태스크 기록은 남으므로, 최근 앱이나 런처로 그 태스크를 되살리면
    시스템이 **같은 extras를 다시 실어** `onCreate`를 부른다. `setIntent(Intent())`는 액티비티
    인스턴스의 필드만 비우므로 이 경로에는 닿지 못한다. 되살린 인텐트에만 붙는
    `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`로 가르고, 판정은 `toPushDeepLinkOrNull`이 한다
    (`PushDeepLinkIntentTest` 2건이 잠근다).

  > 실기기 관측값(Pixel 4, API 29) — 알림 탭은 `flags=0x14000000`, 되살리기는 `0x14100000`이고
  > extras는 양쪽이 같다. 차이는 `0x00100000` 한 비트다.
- **목적지는 `route` 하나가 정한다.** `type`(`TOPPING`·`REMIND_AM`·`REMIND_PM`)은 라우팅에 안 쓰고
  탭 분석 용도로 `PushDeepLink`에 실려만 간다. 모르는 `route`·잘못된 `groupId`(숫자 아님·0 이하)는
  `null`이라 **평범한 실행과 구분되지 않는다** — 파싱 실패가 화면에 보이지 않는다는 뜻이다.
  두 문자열 축은 각각 `PushNotificationRouteType`(`canvas`·`group`)과 `PushNotificationType` enum이
  들고 있고, `groupId`를 양수로 읽는 규칙은 `PushDeepLink.AddTopping.parse`에 있다.
- **이동 수단은 `goTo`다** — 위 세 관용구(`replaceAll`·`goToSingleClearTop`·`popUpTo`)를 쓰지 않으므로
  딥링크로 연 화면 아래에 **그때까지의 백스택이 그대로 남는다.**

✅ **소비 시점에 두 게이트가 걸린다(2026-09-06, PR #456 `618ead927`).**

- **스플래시 이탈을 기다린 뒤 소비한다.** `MainRoute`의 수집은 `LaunchedEffect(Unit)`이라 첫 컴포지션에
  곧바로 시작하는데, 그 시점의 백스택은 `NavKeySplash` 하나이고
  [앱 진입 체인](#앱-진입-체인-2026-08-09-pr-220)의 `SplashRoute`는 부트스트랩이 끝나면
  `replaceAll(...)`로 백스택을 갈아 끼운다. **딥링크가 먼저 쌓이고 리셋이 뒤따르는 순서**가 가능하므로,
  `snapshotFlow { navigator.backStack.lastOrNull() }.first { it != NavKeySplash }`로 이탈을 기다린 뒤
  소비한다. 이미 벗어난 웜 스타트에서는 그대로 통과한다. 그 순서가 수정 전에 실제로 났는지는 관측하지
  않았다 — 창을 없애는 쪽을 택했다.
- **세션이 없으면 딥링크를 버린다.** `AddTopping`·`GroupList` 두 목적지 모두 로그인이 필요하다. 대기
  뒤에 `HasActiveSessionUseCase`(근거는 `BootstrapSessionUseCase`와 같은 `AuthRepository.hasSession`)가
  false면 이동하지 않고, **로그인을 마쳐도 원래 목적지로 이어가지 않는다.** 판정이 대기 뒤에 있는 이유는
  부트스트랩이 인증 거절을 받으면 토큰을 지우기 때문이다.
- **알림 상태바 아이콘이 전용 에셋으로 갈렸다** — `ic_launcher_monochrome`은 어댑티브 세이프존 여백을
  안고 있어 24dp 규격에서 콘텐츠가 작게 찍힌다. `ic_notification`을 새로 두고 그 안에서 여백을 조여
  라이브 영역을 채운다. ⚠️ **여백 보정이 벡터의 `group` 변환으로 들어가 있다** — 디자인이 여백을 조인
  에셋을 다시 주면 걷어낼 자리다 → [open-questions](../synthesis/open-questions.md) OQ-P-374.
- 남은 미결은 이동 수단이다 — `goTo`라 딥링크로 연 화면 아래에 백스택이 남는다
  → [open-questions](../synthesis/open-questions.md) OQ-P-360 ②.

> 🔁 **여기 있던 "Android 13+에서는 알림 표시 자체가 막힌다"는 문장을 걷었다** — `POST_NOTIFICATIONS`를
> 묻는 자리가 PR #450으로 들어왔다(`NotificationPermissionGate`, A-004·A-005 완료 직후). OQ-P-358은
> 그때 이미 해소됐는데 이 문서만 옛 상태로 남아 있었다.

📌 **푸시가 이동 말고 하는 일이 하나 더 생겼다**(2026-09-10, PR #482) —
`ParfaitFirebaseMessagingService`가 알림을 띄운 뒤 같은 `data` 를 다시 읽어, **토핑 알림이면**
`RequestTodayParfaitRefreshUseCase` 로 오늘 캔버스 갱신을 요청한다(폴링 주기를 기다리지 않는다).
**`PushDeepLinkEventBus` 는 쓰지 않는다** — 그것은 `Channel` 기반 단일 소비자이고 **탭했을 때의
이동**을 나르는 축이라, 도착 신호로 재사용하면 푸시가 올 때마다 화면이 옮겨 간다. 토핑만 고르는
판정은 `Map<String, String>.toppingGroupIdOrNull()`(`PushDeepLinkIntent.kt`)이 하고 리마인드는
`route=group` 이라 파서가 이미 갈라 준다. **포그라운드 전용**이다(백그라운드·종료 상태에서는 시스템이
알림을 직접 띄우고 이 콜백을 거치지 않는다) — 다만 폴러 수명이 화면 구독에 매여 있어 잃는 것이 없다
→ [canvas-adaptive-polling 스펙](../superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md).

## 그룹 생성·참여 플로우 (2026-08-12, PR #224)

그룹 목록에서 갈라진 두 갈래가 **목록으로 되돌아오며 닫혔다**. 이전에는 양쪽 끝이 stub이라 들어가면 나올 수 없었다.

```
NavKeyGroupList ─┬─ 생성 ─▶ NavKeyGroupCreate(nickName) ──(확인 모달 = POST 생성)──┐
                 └─ 참여 ─▶ NavKeyGroupInviteCode ─(GET 미리보기)─▶ NavKeyGroupNickName(inviteCode, groupName, nickName) ─(확인 모달 = POST 참여 + PATCH 닉네임)─┤
                                                                                                                                                        └─▶ replaceAll(NavKeyGroupList) → goTo(NavKeyCanvasMain(groupId, welcome*))
```

> 📌 **실서버 결선(2026-08-15, PR #243·#244)** — 두 갈래의 mock UseCase가 전부 걷혔다. 그때는 **합류 시점이
> 앞당겨져** A-004 확인 모달이 `POST /api/parfait-groups/join`으로 합류하고 그 `groupId`를 다음 화면에 넘겼다.
> 🔁 **되돌아왔다(2026-08-16, PR #261)** — 확인 모달이 통째로 S-102로 옮겨가 **참여와 닉네임 적용이 한 확인
> 뒤에 연달아** 일어난다. A-004는 미리보기까지만 하고 **초대코드·그룹명**을 NavKey 인자로 넘기므로,
> 닉네임 화면에서 이탈하면 참여 자체가 없다(OQ-P-166 해소). 참여 성공 뒤 닉네임 PATCH가 실패해도 흐름은
> 멈추지 않는다 — 전역 닉네임을 쓴 채 목록으로 간다.

- ~~복귀는 `goToSingleClearTop(NavKeyGroupList)`다~~ → 🔁 **종착지가 목록에서 캔버스로 옮겨졌다
  (2026-09-01, PR #411)**. 두 갈래 다 `replaceAll(NavKeyGroupList)`로 흐름 화면을 통째로 걷어낸 뒤
  **곧바로 `goTo(NavKeyCanvasMain(groupId, welcomeGroupName, welcomeInviteCode))`**를 쌓는다. 목록을
  깔아 두는 이유를 두 Route가 같은 주석으로 적는다 — 캔버스만 남기면 뒤로가기가 앱 종료가 된다.
  그래서 이 경계의 관용구는 이제 `replaceAll` 하나가 아니라 **`replaceAll` + `goTo` 두 줄 조합**이고,
  `goToSingleClearTop`의 소비처는 그룹 생성·참여에서 사라졌다.
  📌 **되돌아온 목록이 다시 묻는 경로는 그대로다**(#297) — `GroupListIntent.Enter`가
  `LifecycleResumeEffect`로 걸려 있어, 새로 만든 엔트리든 뒤로가기로 앞에 선 엔트리든 재조회한다
  (OQ-P-169 해소 유지). 다만 이번에는 목록 엔트리 자체가 새것이라 `init` 성격의 첫 조회도 함께 돈다.
  즉 develop에 **백스택 리셋 관용구가 셋**이다: 되돌아갈 화면이 없는 경계는 `replaceAll`
  (Splash·TermAgree·Login·강제 로그아웃·S-101 나가기/신고), 이미 스택에 있는 화면으로 복귀는
  `goToSingleClearTop`(#224 이후 남은 소비처는 없다), 흐름을 걷어내고 **다른 화면으로 들어가는**
  경계는 `replaceAll` + `goTo`(그룹 생성·참여) → [open-questions](../synthesis/open-questions.md) [2026-08-12].
- **확인 모달이 전이의 게이트**다 — 각 갈래의 **마지막 입력 화면**(생성은 A-005, 참여는 S-102)에서 확인 버튼이
  곧바로 이동하지 않고 `YGModalPopup`을 띄우며, 서버 요청과 이동은 모달의 Primary 버튼에서 일어난다.
  모달 표시 여부는 각 UiState의 `isConfirmPopupVisible`이다
  ([a005](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[s102](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md) 스펙).
  🔁 **#261 전에는 참여 갈래의 모달이 A-004에 있었다** — 중간 화면이 게이트를 쥐면서 이후 화면 이탈이
  참여를 되돌리지 못했기 때문에 마지막 화면으로 내려왔다.
  📌 **모달은 게이트로 남고 진행·실패는 밖으로 나왔다(2026-08-27, PR #393·#394)** — 두 화면 다 요청을
  보내기 **직전에** 모달을 닫는다. 요청이 도는 동안 화면에 남는 것은 `YGScaffoldV2` 로딩 오버레이이고,
  실패는 토스트다. 그래서 "모달의 Primary에서 서버 요청과 이동이 일어난다"는 여전히 맞지만,
  요청 중에 모달이 떠 있지는 않다(OQ-P-137 ④·OQ-P-204).
  같은 라운드에서 **A-005의 닉네임 필드가 열렸다** — `NavKeyGroupCreate(nickName)`으로 넘어오는 값이
  읽기 전용 표시값이 아니라 초기값이 됐고, 확인은 그룹명과 닉네임을 함께 검사한다.
  📌 **참여 갈래도 같은 모양이 됐다(2026-09-07, PR #461)** — `NavKeyGroupNickName`에
  세 번째 인자 `nickName`이 붙고 A-004가 계정 SSoT(`GetMyAccountFlowUseCase`)를 구독해 그 값을 실어
  보낸다. 그전까지 **참여 갈래만 빈 입력칸으로 시작했다** — 생성 갈래는 G-001이 같은 구독으로 값을
  넘기고 있었는데(#312) 참여 쪽에 그 경로가 없었다. 구독 자리는 두 갈래 모두 **목록·입력 화면의 `init`**
  이라 진입 시점의 값이 아니라 스트림을 따라간다.
  ⚠️ **닉네임이 아직 없을 때의 답은 두 갈래가 반대다** — 참여 갈래는 **막지 않고 빈 값으로 넘어가고**
  (초대코드 조회를 이미 마친 뒤라 여기서 되돌리면 사용자에게는 아무 반응 없는 실패로 보인다), 생성
  갈래는 `handleClickCreateNewGroup`이 **이동 자체를 접고** 로그만 남긴다(다시 누르면 열린다). 어느
  쪽이 이 앱의 답인지 정한 적이 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-377.
- 📌 **이동이 안내 하나를 거쳐 간다(2026-09-05, PR #450)** — 두 갈래의 마지막 이펙트
  (`NavigateToNext`)가 곧바로 `replaceAll` + `goTo`를 부르지 않고, **목적지를 들고 대기**한다.
  그 사이에 `NotificationPermissionGate`가 알림 권한 안내를 띄우고, 허용·거부·"나중에" 어느
  갈래로 끝나든 `onFinished`가 불려 원래 이동이 이어진다(이미 허용돼 있으면 안내 없이 곧장 통과).
  **이 저장소에서 이펙트가 화면 상태를 거쳐 지연되는 첫 자리**다.
  - 대기하는 값은 `rememberSaveable`이다. `remember`로 두면 구성 변경으로 Activity가 다시 설 때
    값이 유실되는데, 이펙트가 `Channel`이라 **다시 오지 않는다** — 서버에서는 그룹 생성·참여가 끝났는데
    사용자만 이전 화면에 갇힌다. 두 Route가 각자 `listSaver`(`NavigateToNextSaver`)를 두고 그 왕복을
    유닛 테스트로 잠근다. **프로세스 사망은 이 구조가 막지 못한다** — `Navigator` 백스택이
    `@ActivityRetainedScoped`의 순수 `mutableStateListOf`라 스플래시로 초기화된다.
  - ⚠️ **배선이 두 Route에 복제됐다** — 이펙트 타입이 달라 공용화하려면 제네릭이나 공통 인터페이스가
    필요하고, 지금은 이득이 얇아 두었다 → [open-questions](../synthesis/open-questions.md) OQ-P-372.
  - 📌 **정원 1로 만든 그룹은 그 안내를 건너뛴다**(2026-09-10, PR #482) — 토핑 알림은 서버가 작성자를
    빼고 보내 혼자인 그룹에서는 영영 오지 않으므로, **한 번뿐인 런타임 권한 요청**을 실익이 적은
    자리에서 소진하지 않는다. `NavigateToNext`가 `memberLimit`를 함께 나르고 `GroupCreateRoute`가
    `memberLimit > 1`일 때만 게이트를 세운다. `NavigateToNextSaver`도 그 필드를 저장·복원한다 —
    빠뜨리면 복원 뒤에 정원 1 그룹에도 모달이 뜬다. **A-004 참여는 그대로다**(참여하는 그룹은
    정의상 혼자가 아니다)
    → [canvas-feedback-fixes 스펙](../superpowers/specs/archive/2026-09-10-canvas-feedback-fixes.md).
  - **기기 토큰 등록은 이 게이트에 매달려 있지 않다** — 등록은 세션 축이 맡고 권한과 독립이다
    ([data-layer](data-layer.md) 「기기 토큰 등록」 ·
    [스펙](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md) 결정 1).
- 의존은 규약대로 `:api`만: `feature/groups/enter/impl` → `feature/groups/list/api`(#224에서 추가).
- ✅ **위키 정본과 목적지가 맞았다(2026-09-01, PR #411)** — [[기능정의서-v6]]이 중간 화면 G-002를
  삭제하며 A-004(참여)·A-005(생성)의 다음 단계로 적어 둔 **C-001(메인 캔버스) 직접 진입**이 코드에
  들어왔다(OQ-P-135 해소). 정본과 다른 점은 목록을 백스택 **아래에 깔아 둔다**는 것뿐이고, 그것은
  뒤로가기가 앱을 끄지 않게 하려는 구현 사정이다.
- 📌 **캔버스가 진입 사유를 인자로 받는다** — `NavKeyCanvasMain`에 `welcomeGroupName`·
  `welcomeInviteCode`가 붙어, 생성 직후면 초대코드까지 실린 배너를, 참여 직후면 그룹명만 실린 배너를
  캔버스가 1회 띄운다(평범한 진입은 둘 다 `null`). **목적지 키가 "어디로"만이 아니라 "왜 왔는가"를
  나르기 시작한 첫 자리**이고, 1회성은 화면 상태가 아니라 **ViewModel 생성 시점**에 기댄다 —
  백스택 재진입에서는 ViewModel이 새로 만들어지지 않아 배너가 다시 뜨지 않는다(OQ-P-339).

## 토핑 생성 플로우 (2026-08-14, PR #221)

촬영·갤러리에서 시작해 캔버스 배치 직전까지가 이어졌다. 상세는
[c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md).

```
NavKeyCameraCustom ─┐
                    ├─▶ NavKeyPictureConfirm(uri, source) ══▶ NavKeySegmentation(sourceImageUri)
NavKeyGalleryPicker ┘        (goToAndPopCurrent — 확인 화면은 걷힌다)          │
                                                                              ▼
                          NavKeySegmentationConfirm(sourceImageUri?, subjectImagePath?, trimmedSubjectImagePath)
                                     ▲              │  ▲ ToppingEditResult(ResultEventBus)  │
   갤러리 "최근"의 알맹이 ────────────┘              ▼  │                                    ▼
   (앞의 둘이 null — 테두리만 편집)   NavKeyToppingEdit(source, segmentation, borderLayers)   NavKeyCanvasToppingPlace
                                                                                             │ popUpTo<NavKeyCanvasMain>()
                                                                                             ▼
                                                                                        C-001 캔버스
```

- 확인 화면(C-101-confirm) → C-103은 **`goToAndPopCurrent`**다. 확인 화면이 걷히므로 세그멘테이션에서
  뒤로 가면 촬영/갤러리로 바로 돌아간다.
- C-103 안의 두 화면(`Segmentation` → `SegmentationConfirm`)은 평범한 `goTo`다 — 뒤로 가면 인식이
  끝난 화면으로 돌아온다(재추출하지 않는다).
- **편집 결과는 `ResultEventBus` 왕복이다** — `NavKeyToppingEdit`는 `@Serializable` NavKey라
  *들어갈 때의 인자*만 담고, 나올 때는 `sendResult(TOPPING_EDIT_RESULT_KEY, ToppingEditResult)` +
  `onBack()`으로 돌려준다. 확인 화면이 `ResultEffect<ToppingEditResult>`로 받아 `rememberSaveable`에 쌓는다.
  **데코레이터 존치 여부를 묻던 항목이 실사용 소비처를 되찾았다** →
  [open-questions](../synthesis/open-questions.md) [2026-08-10].
- 재편집을 위해 확인 화면이 **최종본과 "테두리 전 알맹이"를 따로** 들고 있다가 알맹이 쪽을 마스크로
  넘긴다. 최종본을 넘기면 테두리 색이 원본 픽셀로 덮여 사라진다.
- 📌 **결과가 원본 사진의 긴 변도 나른다**(2026-09-09, PR #480) — `ToppingEditResult.sourceLongSide`가
  업로드 축소 배율의 분모다. **값 클래스가 아니라 벌거벗은 `Int?`**인 이유는 `feature/segmentation/api`가
  `:domain`을 의존하지 않기 때문이고(같은 이유로 `ToppingBorderLayer`가 색을 ARGB 정수로 내린다),
  감싸는 곳은 소비처인 `SegmentationConfirmViewModel`이다. `borderOnly` 진입은 `null`을 싣는다 —
  그 진입의 `cutout`은 사진이 아니라 되살린 알맹이라 분모가 못 된다
  → [topping-upload-source-scaled 스펙](../superpowers/specs/archive/2026-09-09-topping-upload-source-scaled.md).
- 📌 **실패 화면에서도 편집 화면으로 간다**(2026-09-10, PR #487). `C-103-Error`의 「직접 편집」이 원본을
  `saveBitmap`으로 한 번 떨구고 `SegmentationEffect.GoToEdit`을 내면, Route가 떨군 원본을 마스크 자리에 실어
  `NavKeyToppingEdit(sourceImageUri, segmentationImageUri)`로 `goTo`한다. 그래서 편집 화면은 사진 전체가 남은
  마스크로 열린다. 결과는 **세그멘테이션 Route도 `ResultEffect`로** 같은 `TOPPING_EDIT_RESULT_KEY`에서 받아
  (`SegmentationIntent.OnEditResult`) 초안을 기록한 뒤 `GoToConfirm`으로 확인 화면에 간다. C-103 안에
  `Segmentation` → C-104 → `Segmentation` → `SegmentationConfirm` 순서의 갈래가 하나 생긴 셈이다. 결과를 초안에
  적는 변환은 확인 화면과 함께 쓰는 `RecordToppingDraftUseCase.recordEditResult` 확장이다. 종전 「편집 없이 사용」
  (초안을 바로 기록하고 확인 화면으로 가던 동작)은 사라졌다
  → [c103-error-use-original 스펙](../superpowers/specs/archive/2026-09-05-c103-error-use-original.md).
- ✅ **플로우를 나가는 경로가 생겼다(2026-08-20, PR #309)** — 세 화면 + C-101-confirm의 `onClickClose`가
  전부 빈 람다이던 것이 `popUpTo<NavKeyCanvasMain>()`으로 결선됐다(OQ-P-152 해소). 세그멘테이션 쪽은
  로딩·에러·본문 세 화면이 콜백 하나를 공유해 **한 자리를 채우자 셋이 함께 출구를 얻었다.**
- 엔트리 3개는 Route를 부르기만 하고 **스캐폴드(`YGScaffoldV2`)는 Route가 소유한다**(2026-08-20,
  PR #309 이관). 빌더 하나(`featureSegmentationEntryBuilder`)가 세 entry를 등록하는 것은 그대로다.
- 📌 **C-103-select를 별도 목적지로 만들지 않았다**(2026-08-24, PR #342). 위키 정책이 다중 검출
  분기를 별도 화면 ID로 가르지만 **`NavKeySegmentation` 하나가 후보 수에 따라 점선 박스를 1개
  또는 N개 그린다** — 두 상태의 UI가 같은 형태라 목적지를 쪼개면 NavKey·Route·EntryBuilder·
  ViewModel이 한 벌 늘고 거의 같은 코드가 복제된다. 실패 화면(`C-103-Error`)도 같은 목적지 안에서
  **상태(`SegmentationState.isError`)로 갈린다**(엔트리 수 불변).
  같은 라운드에서 **다음 화면으로 가는 시점이 Route의 직접 호출에서 이펙트 수신으로 옮겨 갔다** —
  저장이 탭 시점으로 내려오면서 이동이 비동기 결과에 걸렸기 때문이다(`GoToConfirm`이 저장된 경로
  둘을 싣는다). 그 순서가 곧 계약인 이유는 [state-management](state-management.md)와
  [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md).

> 📌 **마지막 목적지가 실물로 바뀌었다(2026-08-19, PR #290)** — 확인 화면의 "다음"이 자리채움이던
> `NavKeyCanvasMove`를 버리고 **`NavKeyCanvasToppingPlace`**로 간다(당시엔 `imageUri` 인자를 실었고, PR #334가 그것을 초안으로 옮기며 인자를 걷었다). 넘기는 값도 파일
> 경로가 아니라 `File(...).toUri()`로 감싼 `file` 스킴 uri이고(배치 화면이 Coil로 읽는다), 그 대상은
> 여백을 걷어낸 **트리밍본**이다(확인 화면이 `key.trimmedSubjectImagePath`로 초기화한다) →
> [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md).
>
> ✅ **되돌아가는 방식이 고쳐졌다(2026-08-20, PR #309)** — 배치 확정 이펙트가
> `goTo(NavKeyCanvasMain(groupId = 0L))`에서 **`popUpTo<NavKeyCanvasMain>()`**으로 바뀌었다. 이미 있는
> 엔트리로 되감으므로 그룹 id를 실어 나를 필요가 없어져 하드코딩 `0L`도 사라졌고, 촬영·세그멘테이션·
> 편집 화면이 백스택에 쌓인 채 남지도 않는다. **이 되감기가 세그멘테이션 캐시 정리의 안전 근거이기도
> 하다** — 진입 시 캐시를 통째로 비우는데, 이전 흐름 화면이 살아 있으면 그 화면들이 가리키던 PNG가
> 지워진다(OQ-P-003 ③).
>
> ✅ **끝이 흐름을 닫는다(2026-08-22, PR #334)** — 배치 확정이 발급 → S3 PUT → confirm → 배치
> 네 단계를 태우고 성공해야 되감는다(OQ-P-238 ②③ 해소). 함께 **NavKey가 인자를 잃었다** —
> `NavKeyCanvasToppingPlace`는 `data object`가 됐고, 배치할 토핑·캔버스 식별값·테두리는 DataStore
> 초안이 나른다([ADR-0026](../adr/0026-topping-draft-datastore-ssot.md)). `camera`·`segmentation`
> 모듈이 캔버스 개념을 떠안지 않는 것이 이 배치의 실익이다.
>
> ✅ **흐름에 두 번째 입구가 생겼다(2026-08-22, PR #334)** — 배치에 성공한 알맹이가 갤러리 "최근"에
> 남고, 그것을 고르면 촬영·세그멘테이션을 건너뛰어 **확인 화면으로 직행**한다. 그래서
> `NavKeySegmentationConfirm`의 `sourceImageUri`·`subjectImagePath`가 nullable로 넓어졌고, 둘이 없는
> 진입에서는 "사진 편집"이 잠겼다(🔁 2026-08-31 뒤집힘, 아래 항목). 확인 화면은 초안이 이번 알맹이를 가리키지 않으면 **스스로 초안을
> 먼저 적은 뒤** 구독을 연다 — 순서를 뒤집으면 첫 방출의 `null`이 없는 실패를 알린다.
> 노출은 `returnResultOnly = false`인 토핑 만들기 진입에서만이라 배경 선택(C-301)에는 안 섞인다 →
> [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) 「누끼 알맹이 재사용 (PR6)」.
>
> 📌 **가르는 축이 바뀌었다(2026-08-31, PR #408)** — 최근 줄에 무엇을 싣는지가 더는
> `returnResultOnly`에서 나오지 않는다. 신설 `RecentImagePick`(`SOURCE`·`CUTOUT`)을
> `NavKeyCustomGalleryPicker`가 받아 화면이 그대로 쓴다. 두 플래그가 우연히 같은 방향을 가리키던
> 것을 부르는 쪽이 직접 고르게 갈랐고, **배경 편집은 `SOURCE`·토핑 만들기는 `CUTOUT`**이다.
> 겸해 최근 목록의 정원도 종류별로 갈렸다([data-layer](data-layer.md) 「예: 최근 이미지」, OQ-P-258).
>
> 🔁 **재사용 진입의 "사진 편집"이 열렸다(2026-08-31 브랜치 작업 → **2026-09-01 develop 머지, 이슈 #424 ·
> PR #425 `6a1da1b0`**)** — 잠그는 대신 **`NavKeyToppingEdit(borderOnly = true)`로 테두리 편집만** 연다. 되살릴
> 원본이 없다는 사실은 그대로이므로 영역(잘라내기) 탭은 열리지 않는다. 원본 자리에는 알맹이를 같이
> 넣는다 — 원본과 누끼가 같은 그림이면 `buildCutoutBitmap`의 SRC_IN 결과가 알맹이 그대로다.
> 캔버스에 놓인 토핑을 다시 손보는 C-306이 쓰던 경로를 그대로 태운 것이라 편집 화면은 안 바뀌었다.
> 함께 **재사용 진입의 초안 재기록 가드가 `SavedStateHandle` 표시로 옮겼다** — 편집을 열면서
> "초안의 알맹이는 진입 인자에서 벗어나지 않는다"는 전제가 깨졌고, 그대로 두면 프로세스 사망 복원이
> 진입 인자로 편집 결과와 테두리를 덮어쓴다.
>
> ✅ **호출자를 잃고 남았던 잔해가 지워졌다(2026-09-20, PR #514)** — `NavKeyCanvasMove`·
> `CanvasMoveRoute`·`CanvasMoveScreen`과 엔트리 등록이 삭제됐다. 같은 라운드가
> `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect` 계열도 함께 걷어 **도달 불가 캔버스 화면은 0개**가
> 됐다 → OQ-P-239 해소.

## 캔버스 배경 편집 플로우 (2026-08-15, PR #231)

C-001 캔버스 메인의 편집 버튼이 C-301 배경 편집(`NavKeyCanvasBGEdit`)으로 결선되고, 그 화면이
**촬영·갤러리·확인 세 화면을 토핑 생성 플로우와 공유**한다. 상세는
[c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md).

```
NavKeyCanvasMain ─▶ NavKeyCanvasBGEdit(groupId, parfaitId) ─┬─▶ NavKeyCameraCustom(showGuideToast=false, returnResultOnly=true) ─┐
                                            └─▶ NavKeyCustomGalleryPicker(recentImagePick=SOURCE, 나머지 동일) ──┤
                                                                                                                 ▼
                                                        NavKeyPictureConfirm(uri, source, returnResultOnly=true)
                                                             │ sendResult(PictureConfirmResult) + popUpTo<NavKeyCanvasBGEdit>()
                                                             ▼
                                                        NavKeyCanvasBGEdit (ResultEffect<PictureConfirmResult>)
```

- **분기의 주체가 NavKey 인자다** — 같은 확인 화면이 `returnResultOnly`가 false면 종전대로
  `goToAndPopCurrent(NavKeySegmentation)`으로 전진하고, true면 결과를 돌려주고 물러난다.
  `showGuideToast`도 같은 부류로, 카메라·갤러리 가이드 토스트를 토핑 생성 경로에서만 띄운다.
  즉 **화면이 그릴 값이 아니라 호출자가 고르는 동작 플래그가 백스택 키에 실린 첫 사례**다.
- ~~**복귀가 `onBack()` 2회 하드코딩**이다~~ → ✅ **깊이 대신 타입이 됐다(2026-08-20, PR #309)**.
  확인·닫기 두 콜백 모두 `popUpTo<NavKeyCanvasBGEdit>()`라 사이에 화면이 몇 장 끼든 부른 화면으로
  되감는다. 목적지를 타입으로 특정할 수 있는 근거는 `returnResultOnly = true`를 주는 곳이
  `CanvasBGEditRoute` 하나뿐이라는 것이고, **대가는 `feature:camera:impl`이 자기를 부른 화면을 이름으로
  안다는 결합**이다(닫기 결선 때문에 이미 `NavKeyCanvasMain`을 알고 있어 방향이 새로 생기지는 않았다).
- 카메라 실패·취소가 결과를 **보내지 않게 됐다**(2026-08-20, PR #309) — `CustomCameraEffect.ReturnResult(uri: String?)`가
  인자 없는 `Cancel`로 좁혀지면서 한 플로우에 반환 타입이 둘이던 상태가 없어졌다. 다만 배경 편집
  화면이 **실패를 아는 수단은 여전히 없다** — 이제는 아무 결과도 오지 않는다
  → [open-questions](../synthesis/open-questions.md) OQ-P-178 ③.
- ~~⚠️ **플로우 전체가 도달 불가다**~~ → ✅ **닫혔다(2026-08-17, PR #268)**. G-001 그룹 카드의 토핑
  클릭이 `goTo(NavKeyCanvasMain(groupId))`로 이어져 진입 화면 C-001에 호출자가 생겼고, 이 플로우
  전체가 함께 도달 가능해졌다
  → [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md).
- **목적지가 인자를 얻었다**(2026-08-22, PR #329) — `NavKeyCanvasBGEdit`가 `data object` →
  `data class(groupId, parfaitId)`다. **인자 출처는 C-001이 이미 받아 둔 오늘 캔버스**이고,
  못 받았으면 편집을 아예 열지 않는다(로그만 남기고 버튼이 조용히 안 먹는다). 편집 화면이 스스로
  오늘 조회를 부르면 캔버스가 없는 날에는 서버가 캔버스를 새로 만들기 때문에, "화면 상태가 그대로
  다음 목적지의 인자"(그룹 설정과 같은 형태)를 여기서도 택했다.
- **선택 상태를 실어 보내는 인자가 하나 더 붙었다**(2026-08-27, PR #400) —
  `NavKeyCanvasBGEdit(groupId, parfaitId, initialToppingId: Long? = null)`이다. C-001에서 **본인
  토핑을 탭하면** 그 id가 실려 오고, 편집 화면이 토핑 탭을 편 채 그 토핑을 선택한 상태로 열린다
  (`CanvasBGEditViewModel`의 `withCanvas`가 첫 조회 결과에 이 값을 얹는다). ⚠️ **정책이 말하는
  C-305 편집 화면이 새로 생긴 것이 아니라 기존 목적지가 그 역할을 받은 것**이고, 앞선 `groupId`·
  `parfaitId`와 달리 이 인자는 **그릴 값도 동작 플래그도 아닌 초기 선택 상태**다. 기본값이 `null`
  이라 편집 버튼으로 들어오는 기존 경로는 그대로다
  → [open-questions](../synthesis/open-questions.md) OQ-P-250.
- **entry에서 `YGScaffold` 껍질이 걷혔다**(2026-08-22, PR #329) — 이 화면은 저장 실패를 토스트로
  알려야 해서 Route가 `YGScaffoldV2`를 직접 든다. 두 겹으로 씌우면 **인셋 패딩이 두 번 먹으므로**
  entry는 `Modifier.fillMaxSize()`만 넘긴다. 아래 [체크리스트](#신규-목적지-등록-체크리스트) 2번의
  기본형에 대한 예외이고, 세그멘테이션 계열이 먼저 같은 이유로 예외가 됐다.

### 토핑 테두리 재편집 왕복 (2026-08-16, PR #264)

같은 화면의 **토핑 탭**이 편집 화면을 한 번 더 재사용한다. 상세는
[c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md).

```
NavKeyCanvasBGEdit ─(선택된 토핑의 편집 버튼)─▶ NavKeyToppingEdit(source, segmentation, borderLayers, borderOnly = true)
        ▲                                                          │ sendResult(TOPPING_EDIT_RESULT_KEY, ToppingEditResult) + onBack
        └──────────────────────────────────────────────────────────┘
```

- **`NavKeyToppingEdit`의 세 번째 호출자**이자, 그 목적지를 **모드로 가른 첫 사례**다 —
  `borderOnly = true`면 영역 탭 없이 테두리 편집만 열린다. **그 플래그의 뜻이 넓어졌다**(2026-09-01,
  PR #425) — "이미 캔버스에 놓인 토핑"이 아니라 **"되살릴 원본이 없는 진입"**이 조건이고, 누끼 확인
  화면의 재사용 진입이 같은 값으로 들어오는 네 번째 호출자다. `returnResultOnly`(#231)와 같은 부류의
  동작 플래그가 백스택 키에 하나 더 늘었다 → [open-questions](../synthesis/open-questions.md) [2026-08-15].
- 결과는 종전대로 `ResultEventBus` 왕복이지만, **받는 쪽이 어느 토핑인지 알아야 한다.**
  그 id를 ViewModel이 아니라 Route의 `rememberSaveable`(`editingToppingId`)이 들고 있다가
  인텐트에 실어 준다 → [open-questions](../synthesis/open-questions.md) [2026-08-16].
- 복귀는 편집 화면의 `onBack()` 1회다(배경 이미지 왕복의 `onBack()` 2회와 달리 스택 깊이 가정이 없다).

## 그룹 설정 진입·이탈 (2026-08-17, PR #285·#287)

상세는 [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md).

```
NavKeyCanvasMain(groupId) ─(상단 메뉴)─▶ NavKeyGroupSetting(groupId)
                                             │ 나가기·신고 성공
                                             ▼
                                        replaceAll(NavKeyGroupList)
```

- **도달 불가가 닫혔다** — `NavKeyGroupSetting`이 `data object` → `data class(groupId)`가 되고
  C-001이 호출자가 됐다(OQ-P-138). 인자 출처는 C-001이 Assisted로 들고 있던 `groupId`라
  **화면 상태가 그대로 다음 목적지의 인자**다. 체크리스트 6번(호출자 없이 머지된 목적지) 사례가
  또 하나 닫혔고, 도달 불가 기간은 약 4일이었다.
- 나가기·신고의 이탈은 `onBack()`이 아니라 `replaceAll`이다 — 위 "백스택 리셋 관용구" 참고.
- `feature/groups/canvas/impl` → `feature/groups/setting/api`, `feature/groups/setting/impl` →
  `feature/groups/list/api`. 둘 다 규약대로 `:api`만 본다.

## 캔버스 저장 미리보기 왕복 (2026-09-05, PR #445 · 2026-09-07 캡처 홀더 PR #463)

갤러리 저장이 한 번에 끝나던 것이 **화면 하나를 거쳐** 돈다.

```
C-001 캔버스 메인
  OnClickSaveToGallery ─▶ [VM] RequestCanvasCaptureForPreview
                      ─▶ [Route] graphicsLayer.toImageBitmap()
                                 writeToCanvasCaptureCache(Dispatchers.IO)
                                   성공 ─▶ CanvasCaptureHolder.put(bitmap)
                                           goTo(NavKeyCanvasImageSave(imagePath, date))
                                   실패 ─▶ 토스트(canvas_main_capture_failure)
                                              │
                              C-001 저장 미리보기(CanvasImageSaveRoute, ViewModel 없음)
                                   remember { CanvasCaptureHolder.peek()?.asImageBitmap() }
                                     비트맵 있음 ─▶ Image(bitmap)
                                     비트맵 없음 ─▶ AsyncImage(fallbackImagePath)
                                   닫기 ─▶ onBack()
                                   저장 ─▶ sendResult(CANVAS_IMAGE_SAVE_RESULT_KEY,
                                              CanvasImageSaveResult(imagePath)) + onBack()
                                              │
  [Route] ResultEffect<CanvasImageSaveResult> ─▶ readCanvasCaptureCache(Dispatchers.IO)
                                   성공 ─▶ 권한 있음: SaveCapturedCanvas(bitmap)
                                           권한 없음: 런처 → 승인 시 같은 자리로 합류
                                   실패 ─▶ 토스트(canvas_main_gallery_save_failure)
                      ─▶ [VM] ShowGallerySaveResult(isSuccess, date) ─▶ [Route] 토스트
```

- **미리보기는 저장하지 않는다.** 결과 토스트가 뜨는 자리는 캔버스 메인이고, 미리보기가 저장까지
  맡으면 알림만 남기고 사라지는 화면이 되어 실패했을 때 알릴 곳이 없다. 그래서 돌려주는 결과는
  받은 경로를 그대로 되돌리는 `CanvasImageSaveResult` 하나뿐이다.
- **ViewModel이 없는 두 번째 화면이다**(`NavKeyWebView` 이후) — navKey 인자와 전역 CanvasCaptureHolder만 읽어 그리면 되고 부를 API가 없다.
  엔트리 빌더가 `navKey`를 Route에 그대로 넘기고 Route가 `LocalResultEventBus`만 잡는다.
- **돌아온 뒤 캔버스를 다시 캡처하지 않는다.** 사용자가 보고 확정한 그림과 갤러리에 남는 그림이
  같아야 하므로 미리보기에 들어가기 전에 구운 파일을 다시 읽는다. 권한 승인 뒤의 길과 미리보기에서 돌아온 길이
  `saveWithPermission` 한 자리로 모이는 것도 같은 이유다.
- **정상 경로는 미리보기가 파일을 다시 열지 않는다**(2026-09-07, PR #463) — 캔버스 메인이 `goTo` 직전에
  `CanvasCaptureHolder.put(bitmap)`으로 방금 캡처한 비트맵을 넘기고, 미리보기가
  `remember { CanvasCaptureHolder.peek()?.asImageBitmap() }`로 받아 `Image`로 그린다. 파일을 열어
  전체 해상도 PNG를 디코드하고 크로스페이드하던 자리가 사라진다. **`NavKeyCanvasImageSave`는 그대로다** —
  키가 나르는 것은 여전히 경로와 날짜이고 홀더는 비트맵만 담는다(표시할 날짜의 출처를 하나로 두려는
  것이다). 홀더는 `internal object`라 프로세스 전역이고 **읽으면서 비우지 않는다** — 미리보기는 화면에서
  사라지지 않고도 다시 컴포즈되므로(Activity 재생성·백스택 하강 후 복귀) 읽으며 비우면 그때마다 보고
  있던 그림을 잃는다. **비우는 자리는 미리보기의 `onDispose` 하나**이고, 거기서 자기 키가
  `Navigator.backStack`에 아직 있는지 보아 없을 때만 비운다 — 잠깐 내려간 것과 화면이 끝난 것을
  가르는 조건이 그것이고, 닫기·저장 확정·시스템 백이 전부 이 하나로 모인다(`NavDisplay`가
  `onBack = navigator::onBack`으로 시스템 백을 직접 받아, 화면의 닫기 콜백만으로는 그 경로가 안 걸린다).
  근거는
  [캔버스 저장 미리보기 캡처 전달 스펙](../superpowers/specs/archive/2026-09-07-canvas-save-preview-capture-holder.md).
- **캐시 파일명이 고정이다**(`canvas_capture/canvas_preview.png`) — 저장을 그만둔 캡처가 쌓이지 않게
  한 것이고, 대신 같은 경로를 반복해 그리므로 미리보기가 Coil 요청에
  `addLastModifiedToFileCacheKey`를 걸어 이전 캡처가 다시 뜨는 것을 막는다. 지우는 자리는 없다
  → [open-questions](../synthesis/open-questions.md) OQ-P-365.
  ⚠️ **정상 경로는 더 이상 그 파일을 그리지 않는다** — 홀더가 들어오면서 `addLastModifiedToFileCacheKey`는
  홀더가 비었을 때의 **방어 분기(`AsyncImage(fallbackImagePath)`)에서만** 의미를 갖는다. 파일 자체는
  여전히 필요하다 — 저장을 확정하면 `readCanvasCaptureCache`가 그것을 읽는다.
- **프로세스가 죽으면 이 왕복도 복원되지 않는다** — 백스택이 저장되지 않기 때문이다(위
  [구성 요소](#구성-요소)). 그래서 "프로세스 사망 뒤 경로만 살아 돌아온다"를 전제한 서술은 이
  흐름에도 해당되지 않는다 → [open-questions](../synthesis/open-questions.md) OQ-P-364 ① 정정.

## 신규 목적지 등록 체크리스트
1. `feature/xxx/api`에 `@Serializable NavKeyXxx : NavKey` 추가.
2. `feature/xxx/impl`에 `featureXxxEntryBuilder()` 작성: `entry<NavKeyXxx> { XxxRoute(navigator = navigator, modifier = Modifier.fillMaxSize()) }`.
   > 🔁 **정본 변경 (2026-08-16, PR #267 develop 머지)** — **엔트리는 더 이상 스캐폴드를 감싸지 않는다.** 스캐폴드는
   > `YGScaffoldV2`이고 **Route가 소유**한다 — `hiltViewModel()`이 Route 안에 있어 EntryBuilder는
   > `isLoading`도 실패 이펙트도 볼 수 없기 때문이다. 아래 인셋 관용구 논의는 구 형태(엔트리
   > `YGScaffold`) 기준의 역사이고, 인셋은 이제 Route의 `YGScaffoldV2(contentWindowInsets = …)`가 정한다.
   > 규약 본문 → [design-system](design-system.md) "화면 컨테이너", 설계 →
   > [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md).

   (구 형태) `entry<NavKeyXxx> { YGScaffold { innerPadding -> XxxRoute(modifier = Modifier.padding(innerPadding)) } }`. 화면 최외곽 컨테이너 `YGScreen`과의 역할 분리는 그대로 → [design-system](design-system.md) "화면 컨테이너".
   ✅ **공존이 끝났다(2026-09-20, PR #513·#514)** — 구 형태 호출이 **0건**이다. A-004 초대 코드가
   Route의 `YGScaffoldV2`로 옮겼고(#513), 캔버스 EntryBuilder에 남아 있던 구 형태 엔트리 셋은 도달
   불가 화면과 함께 삭제됐다(#514). `YGScaffold`(V1) 파일 자체는 `@Deprecated(WARNING)`으로 남아
   있다. 수치의 정본은 [design-system](design-system.md) "화면 컨테이너"이고, V1 삭제 시점은
   → [open-questions](../synthesis/open-questions.md) [2026-08-17] OQ-P-204 ②.
3. 빌더를 Hilt 모듈(`NavigationModule`, ActivityRetainedComponent)의 `Set<...>` 멀티바인딩에 `@IntoSet`으로 제공.
   > 📌 **화면 ID 매핑도 함께 더한다**(2026-09-09, PR #478) — `:app`의 `NavKey.toAnalyticsScreenOrNull()`이
   > `NavKey`를 기획 화면 ID(`C-001`·`G-001` 등)로 바꿔 `screen_view` 로 보낸다. ⚠️ **컴파일러가 누락을
   > 잡지 못한다** — `NavKey`는 sealed 가 아니라 `when` 이 빠짐없음을 강제하지 못하고, 잊으면 런타임
   > 경고 한 줄로만 드러난다. 인자로 화면이 갈리는 키(`returnResultOnly`·`source`·`borderOnly`·
   > `initialToppingId`)는 그 인자까지 보고 가른다
   > → [release-analytics-screen-tracking 스펙](../superpowers/specs/archive/2026-09-09-release-analytics-screen-tracking.md).
4. 이동 원하는 feature는 대상의 `:api`에 의존 추가(`settings.gradle.kts`/build 파일).
5. 결과가 필요하면 `ResultEventBus` 데코레이터 경로 사용.
   > ⚠️ **반환 경로를 없앨 땐 호출자의 `ResultEffect`도 같이 본다(2026-08-04, PR #191)** — 커스텀
   > 갤러리가 `sendResult` 대신 확인 화면으로 `goTo` 하도록 바뀌었는데, 호출 화면
   > `CanvasMainRoute`의 `ResultEffect<String>`는 그대로 남아 아무것도 받지 못한다
   > → [open-questions](../synthesis/open-questions.md) [2026-08-04].
   > 📌 **반대로 되살아난 사례(2026-08-14, PR #221)** — 토핑 편집 화면이 `sendResult` +
   > `ResultEffect` 왕복으로 결과를 돌려준다. NavKey가 담지 못하는 **나올 때의 값**을 넘겨야 해서
   > 이 관용구를 다시 골랐다. 즉 "전진하며 `goTo`"와 "결과 반환"이 develop에 공존한다.
   > 📌 **같은 화면이 둘 다 하는 사례(2026-08-15, PR #231)** — C-101-confirm이 `NavKeyPictureConfirm`
   > 인자 `returnResultOnly`로 갈린다: false면 전진(`goToAndPopCurrent`), true면
   > `sendResult(PictureConfirmResult)` + 물러남. 반환 타입이 `String?`(카메라 실패 경로)과
   > `PictureConfirmResult`(확인 성공)로 **한 플로우 안에서 둘**이라, 받는 쪽이 타입을 하나만 구독하면
   > 나머지는 조용히 버려진다.
6. **`goTo` 호출자를 같은 PR에 넣는다** — entry만 등록하고 진입 경로가 없으면 도달 불가 화면이 된다
   (선례: `NavKeyGroupCreate` — 등록 후 **약 2주 뒤**인 #222에서야 G-001 그룹 추가 오버레이가 호출자가 됐다,
   [open-questions](../synthesis/open-questions.md) [2026-07-29]).
   > 📌 **사례가 하나 더(2026-08-11, PR #199)** — C-001 캔버스 메인이 실물화됐지만 `NavKeyCanvasMain`를
   > `goTo` 하는 호출자가 develop에 0건이다. 유일한 후보인 G-001 `GroupListSideEffect.NavigateToCanvas`
   > 분기는 여전히 `// Todo`다. 화면을 채우는 PR과 진입을 여는 PR이 갈리면 **완성된 화면이 도달 불가로
   > 머지**된다 → [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) ·
   > [open-questions](../synthesis/open-questions.md) [2026-08-12].
   > ✅ **그 사례가 닫혔다(2026-08-17, PR #268) — 벌어진 기간은 6일이다.** 진입을 여는 데 필요한 것이
   > 클릭 배선만이 아니었다: 캔버스는 `groupId`가 있어야 조회되므로 `NavKeyCanvasMain`를
   > `data object` → `data class(groupId)`로 바꾸는 것이 선행이었다. **도달 불가 기간이 길어지는 이유가
   > 대개 이것**이다 — 호출자가 없는 화면은 자기가 무엇을 인자로 받아야 하는지도 모른 채 머지된다.
7. **닫기 경로를 빈 람다로 비워 두지 않는다** — 진입 경로가 있어도 나가는 경로가 없으면 화면이
   막다른 길이 된다(선례: C-101-confirm 이후 세그멘테이션 3화면이 전부 `onClickClose = {}` TODO로
   머지됐다가 벌어진 기간 동안 [OQ-P-055](../synthesis/open-questions.md)로 남았다). 닫기가 되돌아갈
   대상 화면의 인자를 모르면(`groupId` 같은) `goToSingleClearTop` 대신 `popUpTo<T>()`를 쓴다 — 위
   `popUpTo` 항목 참고. 배경 편집처럼 캔버스까지 튀면 안 되는 진입 경로가 섞여 있으면 그 경로만
   `onBack`으로 분기한다(`returnResultOnly` 선례).
8. **전환은 기본을 쓰고, 다를 이유가 있을 때만 `metadata`를 단다**(2026-08-22, PR #326) — 근거는
   대개 공유 요소이거나 앞뒤 관계가 없는 경계다. 붙이는 대상은 **위에 놓이는 화면**이라는 점에 주의
   → 위 [화면 전환](#화면-전환-2026-08-22-pr-326).

> ⚠️ **이탈 사례(2026-08-01, PR #173)** — G-001 `featureGroupListEntryBuilder`는 엔트리 컨테이너를
> `YGScaffold`가 아니라 `Box`(전면 배경 이미지)로 두고 `YGScaffold`를 Route 안으로 내렸으며, 그룹 추가
> 오버레이를 **두 번째 `YGScaffold`**(Dim 배경)로 겹친다. 화면 최외곽 `YGScreen`도 쓰지 않는다.
> 배경 이미지·오버레이가 붙는 화면에서 위 2번 관용구가 부족했다는 신호다 →
> [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) · [open-questions](../synthesis/open-questions.md) [2026-08-01].

> ⚠️ **인셋 소유가 컴포넌트로 내려간 사례(2026-08-07, PR #194)** — G-001은 `YGTopBarEmpty`가
> `windowInsets`(기본 `WindowInsets.statusBars`)를 자기 패딩으로 흡수하고, 그만큼 엔트리
> `YGScaffold`가 `contentWindowInsets = systemBars.only(Horizontal + Bottom)`으로 상단을 뺀다.
> 둘 다 상단을 주면 인셋이 이중 적용된다. 이로써 develop에 인셋 관용구가 **3형태**(엔트리 `YGScaffold`
> 기본 / 화면이 직접 `windowInsetsPadding`(C-101) / 컴포넌트 흡수(G-001)) 공존한다 →
> [open-questions](../synthesis/open-questions.md) [2026-08-07].
> 📌 **형태 선택이 화면 정책을 깎은 사례(2026-08-11, PR #199)** — C-001은 ①(엔트리 `YGScaffold` 기본)을
> 골랐는데, 화면 배경 점 격자를 `innerPadding` 안쪽 `Column`에 걸어 위키 [[캔버스-반응형-레이아웃]]이
> 요구하는 "상단바·하단바 포함 화면 전체 뒤"를 못 지킨다. `YGTopBarCanvas`에는 `windowInsets`가 없어
> G-001 관용구를 그대로 쓸 수도 없다 → [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md).
> 📌 **4형태째 — 엔트리가 인셋을 소비하는 관용구(2026-08-13, PR #223)** — S-101 그룹 설정은 ①(엔트리
> `YGScaffold` 기본)을 쓰되 `padding(innerPadding)` **뒤에 `consumeWindowInsets(innerPadding)`을 얹는다**.
> 화면 하단 확인 버튼이 `imePadding()`을 쓰는데, 소비하지 않으면 그 `imePadding()`이 창 바닥 기준
> IME 인셋(키보드 + 내비게이션 바)을 통째로 다시 적용해 버튼이 내비게이션 바 높이만큼 떠오른다
> (실기기에서 드러났다). **엔트리가 `innerPadding`을 주고 하위가 `imePadding()`·`navigationBarsPadding()`
> 계열을 쓰는 화면은 소비가 필수**라는 뜻이고, 저장소에서 이 규약을 지키는 곳은 아직 여기뿐이다 —
> `feature/groups/enter/impl`의 세 entry는 `contentWindowInsets = WindowInsets(0.dp)`로 인셋을 끄고
> `statusBarsPadding()` + `navigationBarsAndImePadding()`을 직접 붙이는 **또 다른 형태**이며, 그중
> `GroupInviteCodeRoute`는 거기에 `imePadding()`을 한 번 더 얹어 IME가 이중 적용된다
> → [open-questions](../synthesis/open-questions.md) [2026-08-07]·[2026-08-13].
>
> 📌 **이중 적용은 걷혔다(2026-08-14, PR #237)** — `GroupInviteCodeRoute`의 `imePadding()`이 제거되고
> entry 단독으로 정리됐다. 같은 PR이 매니페스트에 **`android:windowSoftInputMode="adjustResize"`**를
> 붙였는데, `MainActivity` 단일 액티비티라 **앱 전 화면에 걸리는 변경**이다(창이 줄어드는 방식이 바뀌므로
> 다른 입력 화면의 인셋 체감도 함께 달라진다 — 실기기 확인 기록은 없다).
>
> ⚠️ **이중 적용이 다시, 다른 화면에서 드러났다(2026-08-25, PR #350)** — 갤러리 권한 거부 화면이
> Route의 `YGScaffoldV2`가 준 `innerPadding` 위에 `windowInsetsPadding(systemBars)`을 한 번 더 걸어
> 닫기 버튼이 상태바 높이만큼 내려앉아 있었다(이슈 #345). **같은 화면의 목록 갈래는 멀쩡했다** —
> 인셋 소유가 화면 단위가 아니라 **갈래 단위로 갈릴 수 있다**는 뜻이고, 스펙에 "이중 적용이
> 사라졌다"고 적힌 뒤에도 안 걷힌 갈래가 세 주를 살아남았다. `Scaffold`가 `innerPadding`을 넘겨줄
> 뿐 소비하지 않는다는 성질이 [2026-08-13] 건과 같은 뿌리다 →
> [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) ·
> [open-questions](../synthesis/open-questions.md) [2026-08-07].

> **의도적 예외(2026-08-01, PR #182)** — C-101 카메라 entry는 `YGScaffold`를 쓰되 **`innerPadding`을
> 화면에 먹이지 않는다**. 카메라 피드가 시스템 바 아래까지 덮어야 하고 인셋은 컨트롤 영역이
> `windowInsetsPadding`으로 직접 처리하기 때문이다(코드 주석에 근거 명시). 전체화면 카메라·미디어
> 화면의 관용구로 볼지는 위 이탈 사례와 함께 정리 대상 → [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md).
> 📌 **예외 안에서 무는 주체가 갈래마다 다르다(2026-08-25, PR #350)** — 피드 갈래는 컨트롤 `Column`이
> 물고, 권한 거부 갈래는 컨트롤이 없어 `CameraPermissionRequestComponent` 최외곽이 문다. 이번
> 라운드가 그 자리를 닫기 `Row`에서 바깥 `Box`로 올렸다 — 닫기 줄에 물리면 하단 인셋이 버튼 아래
> 빈칸이 되어 세로 가운데 정렬 블록이 밀린다. **"화면이 직접 문다"는 형태는 무는 주체까지 정해야
> 재현 가능한 규약이 된다**.

## 인자 있는 목적지 (`data class NavKey`)

목적지가 값을 받으면 `data object`가 아니라 `@Serializable data class NavKeyXxx(val …)`로 정의한다
(`NavKeySegmentation`·`NavKeyGroupCreate`·
`NavKeyPictureConfirm`·`NavKeyTermAgree`·`NavKeyGroupNickName`·
`NavKeyCameraCustom`·`NavKeyCustomGalleryPicker`(뒤 둘은 #231에서 `data object` → `data class` 승격)·
`NavKeyCanvasMain`(#268 승격 — `groupId`, **#411에서 `welcomeGroupName`·`welcomeInviteCode` 추가**)·`NavKeyGroupSetting`(#285 승격 — `groupId`)·
`NavKeyWebView`(#296 신설 — `title`·`url`)·`NavKeyCanvasToppingPlace`(#290 신설 — `imageUri`, **#334에서 인자를 잃고 `data object`로 되돌아갔다**)·
`NavKeyCanvasBGEdit`(#329 승격 — `groupId`·`parfaitId`, **#400에서 `initialToppingId` 추가**)·
`NavKeyCanvasImageSave`(#445 신설 — `imagePath`·`date`)).
**#514에서 이 목록이 둘 줄었다** — `NavKeyCanvasEdit(imageUri)`·`NavKeyCanvasMove(imageUri)`가
도달 불가 잔해로 삭제됐다(OQ-P-239 해소).
**목적지 둘이 인자 하나로 합쳐진 첫 사례가 #296이다** — `NavKeyServiceTerms`·`NavKeyPrivacyPolicy`
두 `data object`가 삭제되고 `NavKeyWebView(title, url)` 하나가 됐다. 두 화면은 상단바 제목과 여는
주소만 달랐고 그 둘이 이제 서버 응답 값이라(`GET /api/v1/policies`의 `title`·`url`,
[api/policy.md](../api/policy.md)) **화면을 종류별로 나눌 근거가 사라졌다.** 앞선 사례들이
"같은 화면을 재사용하려고 인자를 붙인" 것이라면 이쪽은 **인자가 생겨서 화면이 하나로 줄어든** 방향이다.
동반해 Route/Screen/ViewModel 2벌이 1벌로 줄고 **ViewModel은 아예 사라졌다**(상태가 인자뿐이고 부를
API가 없다) → [s004-terms-privacy-webview 스펙](../superpowers/specs/archive/2026-07-20-s004-terms-privacy-webview.md).
**인자가 표시 값이 아니라 동작 플래그인 형태가 #231에서 처음 나왔다** — `showGuideToast`·
`returnResultOnly`는 화면이 그릴 데이터가 아니라 호출자가 고르는 분기이고, 기본값이 있어 기존
호출부는 `NavKeyCameraCustom()`처럼 생성자 호출만 바꾸면 됐다. 재사용 화면의 동작 차이를 NavKey에
싣는 방식이 관용구인지는 미결이다 → [open-questions](../synthesis/open-questions.md) [2026-08-15].
**인자가 데이터도 동작 플래그도 아니라 "진입 사유"인 형태가 나왔다**(2026-09-01, PR #411) —
`NavKeyCanvasMain`의 `welcomeGroupName`·`welcomeInviteCode`는 캔버스가 그릴 데이터가 아니고
(그룹명은 화면이 어차피 조회한다) 동작을 가르는 플래그도 아니다. **직전 화면에서 무슨 일이
있었는지**를 나르며, 값이 있으면 배너 1회, 없으면 아무 일도 없다. 둘의 조합이 배너 갈래까지 정한다
— 그룹명만 있으면 참여, 초대코드까지 있으면 생성이다. 그래서 이 인자는 **소비되고 나면 뜻이
없어지는데** 키에는 그대로 남는다(직렬화·복원되면 다시 실려 온다) → OQ-P-339.
**기본값 없는 동작 인자가 처음 나왔다**(2026-08-31, PR #408) — `NavKeyCustomGalleryPicker`의
`recentImagePick: RecentImagePick`은 앞의 둘과 달리 기본값이 없어 **호출부 둘이 값을 반드시
고른다.** 앞선 둘이 "안 주면 종전대로"였다면 이쪽은 잊고 안 주면 컴파일이 깨지는 쪽이고, 그것이
빠뜨림을 막으려는 의도다(둘 중 무엇이 기본인지 정할 근거가 없다). 불린 두 개가 우연히 만들던
분기를 열거 하나로 옮긴 형태이기도 하다.
**인자가 값이 아니라 캐시 파일의 자리인 형태가 나왔다**(2026-09-05, PR #445) —
`NavKeyCanvasImageSave.imagePath`는 미리보기가 그릴 **비트맵 자체를 실을 수 없어서** 생겼다(NavKey는
`@Serializable`이라 직렬화돼 오간다). 캔버스 메인이 캡처를 캐시에 PNG로 굽고 그 절대경로만 넘긴다.
그래서 이 인자는 **키가 복원되는 시점에 가리키는 파일이 남아 있으리라는 보장이 없다** — 앞선 인자들이
"뜻이 없어져도 값은 유효한" 형태였다면 이쪽은 값 자체가 썩을 수 있는 쪽이다. 같은 키의 `date`는
`LocalDate.toString()`(ISO-8601) 문자열이다 — canvas `:api` 모듈이 kotlinx-datetime을 쓰지 않아
도메인 타입을 못 싣고, **받는 쪽이 `LocalDate.parse`로 되돌린다**(앞의 "원시 타입으로 넘기고 받는
쪽에서 감싼다"와 같은 형태이되 이유가 직렬화가 아니라 모듈 의존이다) → OQ-P-364.
**서버 응답 값이 다음 화면의 인자가 되는 형태가 develop에 자리 잡았다**(2026-08-15) — 로그인 응답의
가입 토큰이 `NavKeyTermAgree(registrationToken)`으로, 참여 응답의 그룹 ID가 `NavKeyGroupNickName(groupId)`로
간다. 두 값 다 원시 타입으로 넘기고 **받는 쪽에서 value class로 감싼다**(`RegistrationToken`·`GroupId`) —
NavKey는 `@Serializable`이라 도메인 타입을 직접 싣지 않는다.
🔁 **뒤엣것은 그 뒤로 두 번 바뀌었다** — #261이 합류를 S-102로 내리면서 인자가 **`groupId`에서
초대코드·그룹명으로 교체**됐고(참여가 아직 일어나지 않은 시점이라 그룹 ID가 없다), #461이 세 번째
인자 `nickName`을 더했다. 앞의 예가 **서버 응답**이 인자가 되는 형태라면 뒤엣것은 **로컬 SSoT 값**이
인자가 되는 형태다 — `GetMyAccountFlowUseCase` 구독값을 A-004가 실어 보내고 S-102가 입력칸 초기값으로
쓴다. 받는 쪽이 감싸는 것은 같다(`InviteCode`). `NavKeyGroupCreate(nickName)`이 같은 형태의 먼저 난 예다.
**ViewModel이 없는 화면이면 엔트리 빌더가 `navKey.…` 값을 Route 파라미터로 그냥 넘긴다**
(`NavKeyPictureConfirm(uri, source)` → `PictureConfirmRoute(uri = …, source = …)`, #182·#191).
**여러 진입점이 한 화면을 공유하면 출처를 NavKey 인자(`@Serializable` enum)로 넘긴다** — 확인 화면은
카메라·갤러리 공용이고 `PictureConfirmSource`로 문구만 가른다(#191). 이때 호출하는 feature는 대상
feature의 `:api`만 참조한다(`feature/gallery/impl` → `feature/camera/api`).
**반대로 출처를 안 넘기는 사례가 #296이다** — `NavKeyWebView`는 설정(S-001)과 온보딩 약관 동의
(TermAgree) 양쪽에서 열리는데 화면이 출처에 따라 달라질 것이 없어(제목·주소가 이미 인자다) 출처
인자를 두지 않았다. 즉 출처 인자는 "공유 화면이면 붙인다"가 아니라 **그려야 할 것이 갈릴 때만**
붙는다. 여기서도 의존은 `:api`뿐이다(`feature/intro/impl` → `feature/common/terms/api`, #296 신설).
**인자 값의 출처는 호출 화면의 상태다** — G-001이 `goTo(NavKeyGroupCreate(nickName = uiState.nickName))`로
A-005를 연다(#222). 현재 그 `nickName`은 `GroupListUiState` 기본값 mock이라, 인자 결선과 값의 진위는
별개 문제로 남아 있다 — **#243부터는 그 값이 실서버 그룹 생성 요청으로 나간다**
→ [open-questions](../synthesis/open-questions.md) [2026-08-07]·[2026-07-29].
그 값을 ViewModel 초기 상태로 넘길 때는 **Assisted 주입**을 쓴다 — `@HiltViewModel(assistedFactory = …)` + `@AssistedInject` +
`@Assisted` 파라미터, 엔트리 빌더에서 `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(navKey.…) })`로 생성해 Route에 넘긴다
(`GroupCreateViewModel`·`SegmentationViewModel`·`GroupNickNameViewModel`(#244)·`TermAgreeViewModel`(#242)·
`CanvasMainViewModel`(#268)·`GroupSettingViewModel`(#285)·`CanvasBGEditViewModel`(#329)).
**Assisted 파라미터가 여럿이면 이름표를 붙인다**(#411) — `CanvasMainViewModel`이 `welcomeGroupName`·
`welcomeInviteCode` 둘을 함께 받으면서 `@Assisted(ASSISTED_WELCOME_GROUP_NAME)`처럼 **문자열 키로
구분**한다(둘 다 `String?`이라 타입만으로는 못 가른다). 팩토리 인터페이스와 생성자 양쪽에 같은 키를
적어야 하고, 어긋나면 컴파일이 아니라 Hilt 생성 코드에서 깨진다.
**생성 위치는 두 형태가 공존한다** — 엔트리 빌더에서 만들어 Route 파라미터로 넘기거나(`GroupNickName`),
Route의 기본 인자에서 만들거나(`TermAgree`·`CanvasMain`). 후자는 Route가 인자 값을 받아 팩토리에 넘긴다.
**인자 출처가 "목록에서 누른 항목"인 첫 사례가 #268이다** — G-001이 `ClickTopping(groupId)`으로 누른
카드의 식별자를 인텐트에 실어 이펙트까지 나른다(첫 그룹으로 고정하면 두 번째 그룹의 캔버스에 들어갈
방법이 없다). 클릭은 디자인시스템 컴포넌트(`YGToppingGroup`, `onClick` 없음)가 아니라 **호출부가
`modifier`로** 붙인다.
