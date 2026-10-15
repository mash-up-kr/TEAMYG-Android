---
id: login-debug-mode
title: 로그인 화면 디버그 모드 (Debug Mode)
status: draft
category: behavior-spec
platforms: android
verified: 2026-08-28
related_code:
  - LoginRoute.kt#LoginRoute
  - LoginScreen.kt#LoginScreen
  - LoginViewModel.kt#LoginViewModel
  - KakaoLoginHelper.kt#login
  - DataStoreModule.kt#provideParfaitPreferencesDataStore
  - LocalDataSourceModule.kt
  - RepositoryModule.kt
  - ToppingDraftLocalDataSourceImpl.kt#draft
related_adr:
related_spec:
related_architecture:
  - data-layer.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, login, debug]
---

# Spec: 로그인 화면 디버그 모드

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> 📌 **구현이 release 계보에만 있다(2026-08-30 확인)** — `origin/feature/debug-mode` 가
> `release/version-0.1.0-4` 에 머지됐고 `develop` 에는 `DebugMode*` 심볼이 0건이다. 아래 설계와
> 실제 코드의 대조는 그 브랜치가 `develop` 에 들어오는 회차로 미룬다 →
> [open-questions](../../synthesis/open-questions.md) OQ-P-311 ③.

## 목표

로그인 화면의 숨은 제스처로 디버그 모드를 켜고 끈다. 디버그 모드가 켜져 있으면 카카오
로그인이 카카오톡 앱을 거치지 않고 **항상 카카오계정 웹 로그인**으로 들어간다. 카카오톡이
설치된 기기에서 웹 로그인 경로를 검증하려면 지금은 카카오톡을 지우는 것 말고 방법이 없다.
그 우회를 없애는 것이 목적이다.

플래그는 DataStore에 저장해 앱을 다시 켜도 유지된다.

## 범위

- 포함:
  - `debug_mode` 불린 하나를 읽고 쓰는 데이터·도메인 슬라이스.
  - 로그인 화면 빈 영역의 더블탭 7회 + 롱프레스 제스처로 켜기.
  - 로그인 화면 우측 상단 "디버그 모드" 배지 표시, 배지 탭으로 끄기.
  - 디버그 모드가 켜져 있을 때 카카오 로그인의 웹 로그인 강제.
- 제외:
  - **로그인 화면 밖의 배지 노출.** 배지는 `LoginRoute`에만 그린다. 다른 화면으로 넓힐 일이
    생기면 배지 컴포저블을 `core/designsystem`으로 옮기는 별도 라운드로 다룬다.
  - **디버그 모드가 여는 다른 기능**(서버 URL 전환·로그 뷰어·기능 플래그 등). 이 라운드의
    소비처는 카카오 로그인 분기 하나뿐이다.
  - **빌드 타입 게이트.** 릴리즈 빌드에서도 동작한다(아래 "주의" 참고).
  - 제스처 카운트·활성화 이력의 서버 전송이나 분석 로깅.

## API / 인터페이스

### domain

```kotlin
// domain/repository/debug/DebugModeRepository.kt
interface DebugModeRepository {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
```

UseCase는 만들지 않는다. 이 저장소는 값 하나를 읽고 쓰는 것이 전부라 감쌀 규칙이 없고,
`ToppingDraftRepository`를 ViewModel이 직접 주입받는 선례가 이미 있다.

### data

```kotlin
// data/source/debug/local/DebugModeLocalDataSource.kt
interface DebugModeLocalDataSource {
    val isEnabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}
```

구현은 기존 `parfait_preferences` DataStore를 주입받아 `booleanPreferencesKey("debug_mode")`
하나를 다룬다. 값이 없으면 `false`다. `DebugModeRepositoryImpl`은 데이터 소스로 위임만 한다.

### feature/login

```kotlin
// KakaoLoginHelper
suspend fun login(
    activity: Activity,
    forceAccountLogin: Boolean = false,
): KakaoLoginResult
```

`forceAccountLogin`이 참이면 `isKakaoTalkLoginAvailable` 검사를 건너뛰고 곧장
`loginWithKakaoAccount`를 호출한다. nonce 생성과 결과 매핑은 그대로다. 기본값 `false`는
디버그 모드를 모르는 호출부의 동작을 지금과 같게 유지한다.

## 동작 / 상태

`LoginState`에 `isDebugMode: Boolean = false`를 더한다. `LoginViewModel`은 `init`에서
`DebugModeRepository.isEnabled`를 수집해 이 값에 반영한다. **저장소가 단일 진실이고 화면
상태는 그 투영이다** — 화면이 자기 상태를 먼저 뒤집고 저장을 나중에 하지 않는다.

더블탭 카운터는 `LoginState`에 넣지 않는다. 화면을 벗어나면 사라져도 되는 값이고, 상태에
넣으면 탭마다 리컴포지션이 돈다. `LoginViewModel`의 `private var`로 둔다.

`LoginIntent`에 세 갈래를 더한다.

| Intent | 조건 | 동작 |
|--------|------|------|
| `DebugDoubleTap` | 항상 | 카운터를 1 올린다 |
| `DebugLongPress` | 카운터 == 7 | `setEnabled(true)` 호출 후 카운터를 0으로 되돌린다 |
| `DebugLongPress` | 카운터 != 7 | 카운터만 0으로 되돌린다 |
| `DisableDebugMode` | 항상 | `setEnabled(false)` 호출 |

**7은 "이상"이 아니라 "정확히"다.** 8회 더블탭 뒤 롱프레스는 켜지지 않는다. 롱프레스는 성공
여부와 무관하게 카운터를 되돌리므로, 실패한 시도가 다음 시도에 누적되지 않는다.

`LoginSideEffect.RequestLoginWithKakao`는 `data object`에서
`data class RequestLoginWithKakao(val forceAccountLogin: Boolean)`으로 바뀐다.
`requestSdkLogin`이 `state.value.isDebugMode`를 그 값으로 실어 보내고, `LoginRoute`는 받은
값을 그대로 `KakaoLoginHelper.login`에 넘긴다. 기존 로딩 중복 가드와 `CancellationException`
처리 경로는 건드리지 않는다.

## 표시·제어 규칙

### 제스처를 다는 자리

제스처는 `LoginRoute`가 `LoginScreen`을 감싸는 `Box`의 `Modifier.pointerInput`에 단다.
`detectTapGestures`의 `onDoubleTap`과 `onLongPress`를 각각 Intent로 연결한다.

⚠️ **콘텐츠 위에 `fillMaxSize` 오버레이를 덮으면 안 된다.** 그렇게 하면 그 오버레이가
`KakaoSignInButton` 탭까지 먹어 로그인이 아예 안 된다. 부모에 다는 방식은 Compose가 자식부터
히트 테스트하는 성질을 쓴다. 버튼이 소비한 탭은 부모까지 오지 않고, 온보딩 영역과 여백처럼
소비자가 없는 곳의 탭만 카운터로 들어간다. 사용자가 말한 "빈 화면"이 이 영역이다.

### 배지

`state.isDebugMode`가 참일 때만 같은 `Box`의 `Alignment.TopEnd`에 "디버그 모드" 텍스트를
그린다. 문구는 `feature/login/impl`의 `strings.xml`에 `login_debug_mode_badge`로 넣고
`stringResource`로 읽는다. 배지를 탭하면 `DisableDebugMode`가 나간다.

⚠️ **배지의 탭 영역은 최소 터치 타깃(48dp)에 닿아야 한다.** 12sp 글자에 여백을 조금만 붙이면
20dp 안팎이 되는데, 이 배지가 디버그 모드를 끄는 유일한 경로라 안 눌리는 것이 곧 갇히는 것이다.
`clickable`을 `padding`보다 앞에 두어 여백까지 탭 영역에 넣고, 접근성 라벨을 함께 단다.

`Box`는 `YGScaffoldV2`가 준 `innerPadding` 안에 놓이므로 배지가 시스템 상태바와 겹치지 않는다.
토큰은 같은 모듈의 `OnboardingPager`가 쓰는 것을 따른다 — `YGTheme.typography.caption.c01R`과
`YGAtomicColors.Gray.Gray300`이고, 여백은 터치 타깃을 위해 `YGTheme.layout.padding.padding6`이다.

## 파일 구성

**새로 만드는 파일**

- `domain/repository/debug/DebugModeRepository.kt` — 도메인 인터페이스.
- `data/source/debug/local/DebugModeLocalDataSource.kt` — 로컬 소스 인터페이스.
- `data/source/debug/local/DebugModeLocalDataSourceImpl.kt` — DataStore 구현.
- `data/repository/debug/DebugModeRepositoryImpl.kt` — 위임 구현.

**고치는 파일**

- `data/di/LocalDataSourceModule.kt`, `data/di/RepositoryModule.kt` — `@Binds` 한 줄씩.
- `LoginViewModel.kt` — 상태·Intent·SideEffect·저장소 주입.
- `LoginRoute.kt` — `Box` 감싸기, 제스처, 배지, `forceAccountLogin` 전달.
- `KakaoLoginHelper.kt` — `forceAccountLogin` 파라미터.
- `feature/login/impl`의 `strings.xml` — 배지 문구.

**테스트**

- `DebugModeLocalDataSourceImplTest` — 기존 `FakePreferencesDataStore`를 쓴다. 기본값 `false`,
  저장 후 방출, 그리고 **같은 DataStore 파일의 다른 키가 바뀔 때 재방출하지 않는 것**까지
  덮는다. 마지막 항목이 `distinctUntilChanged`의 존재 이유다
  (`ToppingDraftLocalDataSourceImpl`이 같은 이유로 같은 연산자를 단다).
- `DebugModeRepositoryImpl`은 위임만 하므로 단독 테스트를 만들지 않는다.
- `LoginViewModelTest` — 더블탭 7회 + 롱프레스가 `setEnabled(true)`를 부른다 / 6회와 8회는
  부르지 않는다 / 롱프레스 뒤 카운터가 0이라 이어지는 탭이 누적되지 않는다 /
  `isDebugMode`가 참이면 `RequestLoginWithKakao(forceAccountLogin = true)`가 나간다 /
  `DisableDebugMode`가 `setEnabled(false)`를 부른다.
- `KakaoLoginHelperTest` — 신설. `forceAccountLogin = true`면 `isKakaoTalkLoginAvailable`이
  호출되지 않고 `loginWithKakaoAccount`만 불린다. `UserApiClient`가 이미 생성자 주입이라
  MockK로 세울 수 있다. 다만 `Activity`를 목으로 만드는 것은 이 저장소에 선례가 없다 —
  목 생성이 실패하면 이 테스트를 포기하고 수동 검증에 맡긴다. 프로덕션 코드를 테스트에
  맞춰 비틀지 않는다.

## 주의 / 열린 질문

- ⚠️ **릴리즈 빌드에서도 켜진다.** `BuildConfig.DEBUG` 게이트를 두지 않기로 확정했다. QA와
  운영자가 스토어 빌드에서도 웹 로그인 경로를 확인할 수 있게 하려는 의도다. 대신 한 번 켜지면
  DataStore에 남아 앱을 지우기 전까지 카카오톡 로그인이 웹 로그인으로 유지된다. 회복 경로는
  배지 탭 하나뿐이므로 배지는 디버그 모드가 켜진 동안 **항상 보여야 한다** — 배지를 조건부로
  숨기는 변경은 이 계약을 깬다.
- ⚠️ **끄는 경로는 로그인 화면에 있는 동안에만 열려 있다.** 로그인에 성공해 화면을 떠나면
  배지가 사라지므로, 다시 끄려면 로그아웃해 로그인 화면으로 돌아와야 한다. 배지를 다른
  화면으로 넓히는 것은 이 라운드의 범위 밖이므로 제약으로 남긴다.
  배지 탭이 일시적으로 삼켜지는 구간이 둘 더 있다. 로딩 오버레이가 떠 있는 동안에는 그
  오버레이가 터치를 가져가고, **에러 토스트가 떠 있는 약 2.3초 동안에도** 마찬가지다 —
  `YGScaffoldV2`의 토스트 호스트가 배지와 같은 자리를 위에서 덮고 그 토스트에 스와이프
  해제용 드래그가 달려 있어 탭이 조용히 사라진다. 디버그 모드에서 웹 로그인이 실패하면
  바로 이 조합이 된다. 둘 다 스스로 풀리는 구간이라 코드로 막지 않는다.
- **플래그는 계정이 아니라 기기 단위다.** 로그아웃과 탈퇴는 토큰과 계정 정보만 지우고
  DataStore를 통째로 비우지 않으므로 디버그 모드는 그대로 남는다. 한 기기를 두 사람이
  나눠 쓰면 앞사람이 켜 둔 설정을 뒷사람이 물려받는다(그 사람의 로그인 화면에도 배지가
  보이므로 끌 수는 있다). 이것은 "회복은 로그아웃 후 배지 탭"이라는 계약이 성립하기 위한
  전제이기도 하다 — **세션 정리 경로에 `preferences.clear()`를 넣으면 이 계약이 조용히
  바뀐다.**
- **제스처와 배지에 자동 테스트가 없다.** `feature/login/impl`에는 `androidTest` 소스셋이 없고
  이 라운드는 하니스를 신설하지 않는다. 히트 테스트 순서에 기대는 설계라 회귀를 수동 절차가
  막는다.
- 제스처 자체에는 잠금이 없다. 실사용자가 빈 영역에서 더블탭 7회와 롱프레스를 연달아 밟을
  확률이 사실상 없다는 판단에 기댄다.
- 더블탭 7회는 쌍 사이에 간격이 필요하다. `detectTapGestures`가 탭 둘을 한 쌍으로 묶고 쌍
  사이에 더블탭 타임아웃 만료를 기다리므로, 14회를 균등하게 빠르게 치면 쌍 경계가 어긋난다.
  롱프레스도 손가락이 터치 슬롭을 넘으면 페이저가 이동을 소비해 취소된다.
