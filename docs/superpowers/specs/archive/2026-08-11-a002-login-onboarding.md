---
id: a002-login-onboarding
title: A-002 로그인 화면 (온보딩 페이저 + 카카오 로그인)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-21
related_code: LoginRoute, LoginScreen, LoginViewModel, OnboardingPager, OnboardingPage, OnboardingPagesPreviewParameterProvider, KakaoSignInButton, KakaoLoginHelper, NavKeyLogin, KakaoDesignGuideColors
related_adr: ADR-0002, ADR-0005, ADR-0006, ADR-0010
related_spec: intro-term-agree, g001-group-list
related_architecture: navigation-flow, module-structure, design-system
supersedes:
superseded_by:
tags: [spec, parfait, login, a002]
---

# Spec: A-002 로그인 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #218(`feature/login-a002`)이 develop에 머지됐다
> (2026-08-11). 화면 골격(`LoginRoute`·`LoginScreen`·`OnboardingPager`·`KakaoSignInButton`)은 그
> 전부터 있었고 온보딩 자리는 회색 placeholder 박스 + "[ 일러스트 ]" 문구였다. 이번 PR이 **일러스트
> 3장·문구·토큰화·리소스화**를 채워 화면을 실물로 만들었다. 아래는 머지 코드를 역기록한 것이며 설계
> 대조가 아니라 **규약(parfait)·정책(위키) 대조**로 드리프트를 표기한다.

## 목표
앱 첫 진입에서 서비스를 3장으로 소개하고 **카카오 로그인 하나**로 온보딩 체인에 태운다.
다음 목적지는 약관 동의(`NavKeyTermAgree`)다 → [navigation-flow](../../../architecture/navigation-flow.md) "앱 진입 체인".

## 범위
- **포함**:
  - 온보딩 페이저 — 인디케이터(상) + `HorizontalPager`(하) 세로 배치, 페이지당 일러스트 + 설명 1줄.
  - 일러스트 3종(`image_onboarding_1`~`_3`) 신규 에셋 — **`core:designsystem` `res/drawable*`**에 밀도별 PNG.
  - 문구 리소스화 — `feature/login/impl` `res/values/strings.xml` 신설(온보딩 설명 3 + 카카오 버튼/접근성 2 + 애플 2).
  - 카카오 버튼 토큰화 — shape·패딩·타이포를 `YGTheme.*`로 교체, 라벨·`contentDescription`을 `stringResource`로.
  - `OnboardingPage` 모델 축소 — `title` 삭제, `painterResourceId`가 nullable에서 non-null로.
- **제외**(이번 라운드에서 안 함):
  - 서버 인증·토큰 저장·신규/기존 회원 분기 — 카카오 SDK 토큰은 여전히 `LoginState.token`에만 담긴다
    → [open-questions](../../../synthesis/open-questions.md) [2026-08-10].
  - 애플 로그인 — 버튼을 만들었다가 같은 브랜치에서 **되돌렸다**(잔여 심볼은 아래 "드리프트" 참고).
  - 페이지 자동 넘김·스킵·인디케이터 탭 이동.

## 동작 / 구조

### 화면 구성
- 엔트리는 규약대로 `entry<NavKeyLogin> { YGScaffold { innerPadding -> LoginRoute(...) } }`.
- `LoginScreen`은 세로 `Column`이고 위에서부터 **온보딩 페이저(`weight(1f)`) → 카카오 버튼**이다.
  화면 최외곽 컨테이너 `YGScreen`은 쓰지 않는다(규약 이탈, 이번 PR 이전부터 그렇다).
- `OnboardingPager`는 **인디케이터를 페이저 위**에 둔다(이전에는 페이저 아래였다). 페이지 내용은
  일러스트(`weight(1f)`, `ContentScale.Fit`) + 설명 텍스트 한 덩어리다.
- 페이지 목록은 ViewModel이 아니라 **Route가 `remember`로 만든다**(변수명도 `tempPages` 그대로).
  `stringResource` 3개를 `remember` 키로 잡아 로케일 변경 시 다시 만든다.

### 카카오 로그인
- 버튼 클릭 → `LoginIntent.LoginWithKakao` → `LoginSideEffect.RequestLoginWithKakao` →
  Route가 `KakaoLoginHelper.login(activity)` 호출 → 결과를 Success/Failure/Cancel 인텐트로 되돌린다.
- 성공 시 `LoginState.token`에 담고 `NavigateToNext` → `navigator.goTo(NavKeyTermAgree)`.
  실패·취소는 **로그만 남기고 화면 표현이 없다**(토스트·에러 문구 없음).
- 버튼 자체는 디자인시스템 컴포넌트가 아니라 feature 로컬 Material3 `Button`이다. 카카오 디자인
  가이드 색(`KakaoDesignGuideColors`)을 `ButtonDefaults.buttonColors`로 주입하고, shape는
  `YGTheme.shapes.radius.none`(각짐), 패딩은 `YGTheme.layout.padding.*`, 라벨 타이포는
  `YGTheme.typography.body.b01SB`를 쓴다.

### 리소스
- 일러스트는 `core:designsystem` 소유라 feature가 `com.teamyg.parfait.core.designsystem.R as DesignSystemR`로
  참조한다. 반면 카카오·애플 로고 벡터는 `feature/login/impl` `res/drawable/`에 있다.
- 문자열은 규약대로 화면 소유 모듈(`feature/login/impl`) `strings.xml`이다
  → [module-structure](../../../architecture/module-structure.md) "규칙".

## 드리프트 / 잔존

1. **애플 로그인 잔여 심볼 3종** — 브랜치 중간 커밋이 애플 버튼을 넣었다가 지웠는데
   `AppleDesignGuideColors`(`core:designsystem` `theme/colors/`)·`icon_logo_apple.xml`
   (`feature/login/impl`)·`strings.xml`의 애플 라벨/접근성 2건이 **사용처 0으로 develop에 남았다.**
   Android가 애플 로그인을 쓰지 않기로 한 결정(2026-08-11, OQ-P-117 ②)과 정면으로 어긋나는
   잔여물이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-11].
   📌 **셋 중 하나만 걷혔다(2026-09-20, PR #514)** — `AppleDesignGuideColors.kt` 가 삭제됐다.
   `icon_logo_apple.xml` 과 `strings.xml` 의 애플 라벨·접근성 2건은 그대로 남는다(OQ-P-123).
   **코틀린 심볼만 가고 리소스는 남는 것이 두 번째**다 — 자동 검사가 소스 심볼 쪽만 잡는다.
2. **치수 리터럴 4종** — 상단 `45.dp`, 페이저 좌/우 `35.dp`/`34.dp`, 일러스트↔설명·페이저↔버튼
   간격 `30.dp` 두 곳. 코드 주석이 **"30.dp가 gap 없음"**이라고 공백을 자인한다(`YGLayoutGap`·
   `YGLayoutPadding` 스케일에 해당 값이 없다). 좌우도 값이 1 차이로 갈려 토큰화 대상이 아니다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-11].
3. **에셋 소유가 갈린다** — 이 화면 전용 일러스트는 `core:designsystem`에, 같은 화면의 로고 아이콘은
   `feature/login/impl`에 있다. 밀도 버킷도 이미지마다 다르게 채워졌다(`image_onboarding_1`은
   xhdpi 버킷이 없고 대신 밀도 없는 기본 `drawable/`에 하나 더 있다)
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-11].
4. **프리뷰가 실화면과 다르다** — `OnboardingPagesPreviewParameterProvider`의 3번째 페이지가
   `image_onboarding_3`이 아니라 `_1`을 가리키고, 설명 문구도 `strings.xml` 값과 줄바꿈이 다르다.
   프리뷰가 실화면 대신 볼 수 있는 유일한 그물인데 세 번째 장에서 갈린다.
5. **인디케이터 색이 리터럴** — `PagerIndicator`의 활성/비활성 색이 `YGAtomicColors`도
   `YGTheme.colorScheme`도 아닌 `Color(...)` 리터럴 기본 파라미터다(이번 PR 이전부터 그렇다).
6. **`contentColor`가 死** — 카카오 버튼이 `ButtonColors.contentColor`로 가이드 라벨색을 주지만
   내부 `Text`가 색을 명시해 덮는다. 이 화면에서는 결과가 같지만 두 자리가 어긋나면 조용히 갈린다.
7. **`painterResourceId: Int`에 `@DrawableRes`가 없다** — 아무 `Int`나 들어가도 컴파일된다.

## 정책 대조

| 위키 정책 | 코드 | 판정 |
|---|---|---|
| [[화면-ID-체계]] — `A-002 로그인` | `NavKeyLogin` + `LoginRoute`, 진입은 `NavKeySplash` | 일치 |
| 온보딩 3장 구성·문구·일러스트 | 정책 소스 없음, 코드가 먼저 확정 | **대조 대상 부재** |
| 로그인 수단 | 카카오 단독(애플은 Android 미사용 확정) | 일치 |

위키에는 A-002의 온보딩 슬라이드 정책이 없다(수집된 것은 "프로필 이미지 안 넣음" 한 줄뿐).
G-001 에러 문구와 같은 성격 — **코드가 정본이 된 문구**다
→ [open-questions](../../../synthesis/open-questions.md) [2026-08-11].

## 파일 구성

```
feature/login/impl/
  route/LoginRoute.kt                    Route(페이지 목록 remember·side effect 수집)
  screen/LoginScreen.kt                  세로 배치 + 프리뷰
  component/OnboardingPager.kt           페이저 + OnboardingPageContent + PagerIndicator
  component/KakaoSignInButton.kt         Material3 Button + 카카오 가이드 색
  model/OnboardingPage.kt                description + painterResourceId
  model/OnboardingPagesPreviewParameterProvider.kt
  viewmodel/LoginViewModel.kt            State/Intent/SideEffect
  util/KakaoLoginHelper.kt
  navigation/EntryBuilder.kt, NavigationModule.kt
  res/drawable/icon_logo_kakao.xml, icon_logo_apple.xml(사용처 0)
  res/values/strings.xml                 신설
core/designsystem/
  theme/colors/AppleDesignGuideColors.kt 신설(사용처 0 → #514에서 삭제)
  res/drawable*/image_onboarding_1~3.png 신설
```
