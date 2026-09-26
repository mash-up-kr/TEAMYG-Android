---
id: design-system
title: Design System — 테마·토큰·컴포넌트 작성 가이드
category: architecture
status: living
platforms: android
verified: 2026-09-21
related_spec: c103-multi-subject-selection, c001-canvas-gallery-save, c202-canvas-spotlight, segmentation-pipeline-hardening, designsystem-ygscreen-scaffold, designsystem-button-component-sync, designsystem-button-missing-components, designsystem-canvas-components, designsystem-grouptag-topping-components, designsystem-bar-listdate-components, c101-camera-picture-confirm, a002-login-onboarding, c001-canvas-main, ygmodalpopup, a004-group-invite-code, c301-canvas-background-edit, c201-canvas-calendar, session-token-refresh-infra, c301-topping-edit-tab, ygscaffold-v2-common-loading-error, s101-group-setting-api
related_adr: ADR-0007, ADR-0010, ADR-0018, ADR-0025
related_architecture:
related_code: core:designsystem, YGTheme, ParfaitImageLoader.kt#newParfaitImageLoader, YGDimOverlay.kt#YGDimOverlay, YGSkeleton.kt#YGSkeleton, YGLoadingLottie.kt#YGLoadingArt
tags: [architecture, parfait]
---
# Design System — 테마·토큰·컴포넌트 작성 가이드

`core:designsystem` 모듈의 테마 홀더·토큰 계층·컴포넌트 작성 규약. "왜"는 [ADR-0010](../adr/0010-custom-compositionlocal-theme.md)(테마 메커니즘), [ADR-0007](../adr/0007-compose-material3-design-tokens.md)(100% Compose·중앙화 원칙, superseded).

> 근거는 파일명 + 심볼명으로 표기. 라인번호·색 hex값·개수는 적지 않는다(코드에서 직접 확인). 값이 필요하면 `theme/colors/YGAtomicColors.kt` 등 소스를 본다.

## 전체 구조

```
core/designsystem/.../theme/
  YGTheme.kt              ← 진입점 YGCustomTheme() + 접근자 object YGTheme + Local* CompositionLocal
  colors/                 ← 색 2계층 (원자 → 시맨틱)
    YGAtomicColors        원자 팔레트 (Cherry/Melon/Pudding/Soda/Gray/Transparency) — **public** (#158로 internal→public 머지, 2026-07-19)
    YGColorScheme         시맨틱 홀더 (primary/secondary/tertiary/danger/warning/success/info/grayScale/transparency)
    YGColorGrayScale, YGColorTransparency   서브 홀더
    YGSemanticColorDefaults    원자→시맨틱 매핑 (YGLightColorScheme / YGDarkColorScheme)
    KakaoDesignGuideColors     외부 가이드 색 참조(A-002 카카오 버튼)
                               (`AppleDesignGuideColors`는 #218 신설 후 사용처 0으로 남았다가 #514에서 삭제됐다)
  typography/             ← YGTypography(title/body/caption) + YGFontFamily(SUIT) + *Defaults
  shapes/                 ← YGShapes(radius: YGShapeRadius) + YGShapesDefaults
  layout/                 ← YGLayout(gap: YGLayoutGap, padding: YGLayoutPadding) + YGLayoutDefaults
  size/SizeTokens.kt      ← SizeTokens(object) + SizeToken(value class .getDp()) — 홀더 밖 별도
component/
  ygbutton/               ← YGButton (첫 컴포넌트, 작성 패턴 레퍼런스)
  ygalert/                ← YGAlert 배너 + YGAlertPolicy/Host (노출 정책 패턴)
  ygtoast/                ← YGToast(+YGToastType) + YGToastPolicy/Host (노출 정책 패턴) + showError 확장 (#267) + 태그 교체 replaceTag (#405)
  ygloading/              ← YGLoadingOverlay (Dim 판 + 인디케이터) + YGLoadingLottie(+YGLoadingArt) (#267 · 인디케이터 로띠 교체 #305 · Dim·터치 삼킴을 ygdimoverlay 로 분리하고 YGLoadingTone → YGLoadingArt 개명 #440)
  ygdimoverlay/           ← YGDimOverlay (Dim 판 + 터치 삼킴 + 선택적 병합 시맨틱, #440 develop 머지 — YGLoadingOverlay 에서 뽑아낸 공용 판)
  ygskeleton/             ← YGSkeleton (원격 이미지 대기 자리의 시머 회색면, #440 develop 머지)
  ygcanvas/               ← YGCanvas + YGCanvasBackground + YGCanvasBackgroundState (C-001 캔버스 합성 컨테이너, 반응형 배치 #199 · 배경 로딩 상태 보고 #440) (#185 develop 머지)
  ygbackgrounddotgrid/    ← ygBackgroundDotGrid() Modifier (C-001 배경 점 격자, drawBehind) (#199 develop 머지)
  yglistdate/             ← YGListDate (C-201 날짜 셀 = YGDateButton + YGChipColorIndicator) (#188 develop 머지)
  ygfloatingbar/          ← YGFloatingBar* 5변형 + private 컨테이너·버튼 2종 (4변형 #188 · Title #406, 둘 다 develop 머지)
  ygtopbar/               ← YGTopBar* 4변형 + YGTopBarDefaults(배경 블러 반경 #188, 상단 인셋 #194)
  ygtutorial/             ← YGTutorialOverlay + YGTutorialBox + YGTutorialProgress + YGTutorialBoxPlacement (화면 첫 진입 튜토리얼, #449 develop 머지)
border/
  DashedBorder.kt         ← dashedBorder() Modifier (점선 사각형 테두리, drawBehind+dashPathEffect) (#159 develop 머지)
shape/
  CanvasCutCornerShape.kt ← canvasCutCornerShape() 좌상단 컷 Shape (Outline.Generic) (#185 develop 머지)
component/etc/
  YGHorizontalDashedDivider.kt  ← 점선 수평 구분선 (Canvas+drawLine+dashPathEffect) (#159 develop 머지)
screen/                   ← 화면 루트 컨테이너 (아래 "화면 컨테이너")
  YGScreen.kt             Surface 래퍼 + YGScreenScope 리시버 (화면 최외곽)
  YGScaffold.kt           Material3 Scaffold 래퍼 (구판, @Deprecated(WARNING) #267 — **호출부 0건**, #513·#514 이후)
  YGScaffoldV2.kt         Scaffold + 로딩 오버레이 + 토스트 호스트 3층 (Route 소유, #267 develop 머지 · 덮개가 `loadingOverlay` 슬롯이 됨 #440)
image/                    ← ParfaitImageLoader.kt: newParfaitImageLoader(전역 Coil 로더 팩토리) + rememberReloadableImageRequest (#440 develop 머지)
  YGScreenScope.kt        YGScreenScope + OnBack(@Composable, BackHandler 래핑)
res/font/                 ← suit_regular/medium/semi_bold/bold.ttf (SUIT **수정본**, #366 — 아래 참고)
res/values/strings.xml    ← 모듈 최초의 문자열(#267) — 로딩 오버레이 접근성 contentDescription 1건 + 튜토리얼 카드의 진행 표시·버튼 라벨 3건(#449)
res/raw/                  ← 로딩 로띠 3종(밝은 바탕용·어두운 바탕용 #305, 캔버스 토핑용 `loading_topping` #440) — 모듈 최초의 raw 리소스
res/drawable*/            ← ic_* 아이콘 + 밀도별 PNG 세트(#218로 A-002 온보딩 일러스트 `image_onboarding_*` 추가, #264로 `ic_edit`·`ic_scale` 추가)
(모듈 루트)
  OFL.txt                 ← 번들 폰트의 SIL Open Font License 1.1 사본 + 수정 내역 고지 (#366 신설)
```

> **번들 폰트는 원본이 아니라 수정본이다**(#366 develop 머지, 2026-08-26). SUIT 원본은 **빈
> 글리프(zero-contour)로 향하는 cmap 매핑**을 갖고 있어 그 문자들이 화면에 **투명하게** 찍혔다.
> 수정본은 그 매핑만 걷어내 플랫폼이 **시스템 폰트로 fallback** 하게 한다 — 글리프 아웃라인과
> 폰트 버전은 그대로이고 파일명도 유지해 `R.font` 참조와 `YGFontFamily`는 손대지 않았다.
> 두 가지가 이 교체에 딸려 온다. ① fallback 문자는 **기기 시스템 폰트로 그려지므로 기기마다
> 글자체가 섞인다**(투명보다 낫다는 판단이지 같은 그림이라는 뜻이 아니다). ② OFL 1.1이 수정본
> 배포 시 라이선스 사본 동봉을 요구해 모듈 루트에 `OFL.txt`가 생겼는데, **그 파일은 APK에 실리지
> 않고 앱 안에 오픈소스 고지 화면도 없다** → [open-questions](../synthesis/open-questions.md)
> OQ-P-306·OQ-P-307.

> **에셋 소유가 갈린다** — 화면 전용 이미지가 여기 들어오는 경우(A-002 온보딩 일러스트)와 해당 feature
> 모듈 `res/`에 두는 경우(같은 화면의 카카오·애플 로고 벡터)가 공존한다. 기준이 문서에 없다
> → [open-questions](../synthesis/open-questions.md) [2026-08-11].

## 테마 접근 규약

- 테마 값은 **항상 `YGTheme.*`로 읽는다**: `YGTheme.colorScheme` / `.typography` / `.shapes` / `.layout`. 전부 `@Composable @ReadOnlyComposable`.
  - 예: `YGTheme.typography.body.b01SB`, `YGTheme.layout.padding.padding4`, `YGTheme.shapes.radius.round`.
- **크기만 예외**: `SizeTokens.Size24.getDp()`로 직접(`SizeToken`은 `@JvmInline value class`, 홀더 밖).
- `Local*` CompositionLocal은 `internal` + 미초기화 시 `error(...)`. → **모든 UI·프리뷰는 `YGCustomTheme { }`로 감싸야** 크래시 안 남.
- **원자 색 직접 참조 — develop 실질 허용(#158 이후)** — 컴포넌트 대부분(`YGButton`·`YGActionItem`·`YGIconButton`·`YGInputNumber`·`YGChipButton`·`YGModalPopup`·`YGInviteCard`·`YGNametagChip`·`YGTopBar`·`YGDateButton`·`YGDate`·`YGLabel`·`YGDangerZone`·`YGAlert`·`YGToast`, #183·#185·#186 신설 12종 포함)이 시맨틱(`YGTheme.colorScheme`) 대신 `YGAtomicColors`를 직접 참조. 원래 규칙은 "시맨틱만 읽고 `YGAtomicColors`는 `internal`+시맨틱 매핑에서만 소비"였으나 —
  > ✅ **방향 전환 머지됨(#158, develop `ce4e9b8`, 2026-07-19)** — `YGAtomicColors` **`internal`→public**. "원자 직접 참조 금지"의 강제 메커니즘(외부 모듈 접근 차단)이 사라지고 원자 색이 실질 SoT가 됨. [ADR-0010](../adr/0010-custom-compositionlocal-theme.md) "시맨틱 우선" 원칙 재검토/신규 ADR 필요(잔존) → [open-questions](../synthesis/open-questions.md).

## 토큰 계층

| 축 | 홀더 | 스케일(심볼) | 기본값 제공 |
|---|---|---|---|
| 색 | `YGColorScheme` | primary/secondary/tertiary + danger/warning/success/info + grayScale/transparency | `YGSemanticColorDefaults` |
| 타이포 | `YGTypography` | title/body/caption 그룹, 각 웨이트·크기 변형(`b01B/b01SB/b01R/b02...`) | `YGTypographyDefaults` |
| 모양 | `YGShapes.radius`(`YGShapeRadius`) | none/xSmall/small/medium1/medium2/large/xLarge1/xLarge2/round (`none`=RectangleShape 각짐, #159 develop 머지) | `YGShapesDefaults` |
| 레이아웃 | `YGLayout.gap`/`.padding` | gap1.. / padding1.. (명명 스케일) | `YGLayoutDefaults` |
| 크기 | `SizeTokens`(홀더 밖) | Size1/2/4/6/…/44/48/64/80 + Size18·Size28(#183)·Size96·Size160(#186) (`SizeToken`) | — |

색 2계층: `YGAtomicColors`(브랜드 팔레트 — Cherry가 primary 계열, Melon=secondary, Pudding=tertiary, Soda=info 등) → `YGSemanticColorDefaults`가 라이트/다크 스킴으로 매핑. **다크는 현재 라이트와 동일**(`YGDarkColorScheme = YGLightColorScheme`, 코드 `TODO`).

## 신규 토큰 값 추가 체크리스트

1. **원자 색 추가** → `YGAtomicColors`에 팔레트 항목 추가. 시맨틱에 노출하려면 `YGColorScheme` 필드 + `YGSemanticColorDefaults` 매핑까지.
2. **타이포/모양/레이아웃 스케일 추가** → 해당 홀더 data class(`YGTypography*`/`YGShapeRadius`/`YGLayoutGap`·`YGLayoutPadding`)에 필드 추가 + 대응 `*Defaults`에 값 채움.
3. **크기 추가** → `SizeTokens`에 `SizeN` 상수.
4. 홀더 필드를 늘리면 `*Defaults`가 컴파일로 강제되므로 누락 시 빌드 실패(가드).

## 컴포넌트 작성 규약 (레퍼런스: `component/ygbutton/`)

`YGButton`이 첫 컴포넌트이자 패턴 기준.

- **패키지**: `component/<컴포넌트명 소문자>/`. 한 컴포넌트당 파일 분리:
  - `YGButton.kt` — 컴포저블 본체(`clickable`·semantic·`enabled`·`isPressed` 내재화).
  - `YGButtonType.kt` — `sealed interface`로 변형(variant) 정의. 각 변형이 자기 토큰(패딩·textStyle·iconSize·gap·colors)을 `@get:Composable`로 노출. 현재 변형: `SmallSquare`/`Medium.{Primary,Secondary,Transparency}`/`Large`. (fix/ygbutton **#140 develop 머지**로 `XSmall`/`Small` 제거. `SmallSquare` radius는 **#159로 `radius.none`(각짐)** 적용, **#183으로 `Medium.*`·`Large`도 `radius.none`** + `iconSize` `Size20` 교정 — 그전까지 `iconSize`는 렌더에 쓰이지 않는 死필드였다.)
    > ⚠️ **radius 속성 삭제(#182 develop 머지, 2026-08-01, 카메라 화면 PR)** — `YGButtonType.radius`가 통째로 제거되고 `YGButton`의 `background`·`border` `shape` 인자와 `clip`도 빠졌다. 전 변형이 `radius.none`이던 터라 렌더 결과는 같지만, **각짐이 토큰 경유에서 "shape 미지정 기본값"으로 되돌아갔다** — #159·#183이 세운 원칙과 어긋나고 변형별 곡률이 다시 필요해지면 속성을 되살려야 한다 → [open-questions](../synthesis/open-questions.md) [2026-08-01].
  - `YGButtonColors.kt` — 상태별 색 묶음 data class(enabled/disabled/pressed × foreground/background/**border**). (#140에서 `borderColor` 제거·`iconColor`→`foregroundColor` 통합 → **#183으로 테두리 3필드(기본 투명) + `borderColor()` 복원**, `Medium.Secondary`만 값을 채운다. `iconColor` 통합은 유지.)
  - `YGButtonPreviewData.kt` — 프리뷰용 데이터.
- **토큰 참조**: 변형 내부에서 `YGTheme.layout.padding.*`, `YGTheme.shapes.radius.*`, `YGTheme.typography.body.*`, `SizeTokens.*.getDp()`로 읽는다.
- **프리뷰**: `YGCustomTheme { }`로 감싼다(Local 미초기화 크래시 방지). Coil 프리뷰는 `YGCustomTheme`이 `LocalAsyncImagePreviewHandler`를 이미 심음.

> **Assumption / 과도기** — `YGButtonType`의 각 변형 `colors`가 시맨틱(`YGTheme.colorScheme`) 대신 `YGAtomicColors`를 직접 참조하고, 값이 잠정(mock)이다. 코드 주석("Design Token 규칙이 조금 이상… 컴포넌트 완성 시점에 문의 예정")대로 **확정 전 상태**. 이 원자 직접 참조는 `YGButton`에 국한되지 않고 이후 대부분 컴포넌트(`YGActionItem`·`YGIconButton`·`YGInputNumber`·`YGChipButton`·`YGModalPopup`·`YGInviteCard`·`YGNametagChip`·`YGTopBar`·`YGDateButton`·`YGDate`·`YGLabel`·`YGDangerZone`·`YGAlert`·`YGToast`, #183 신설 버튼 5종·#185 캔버스 5종·#186 칩/토핑 2종, 대체로 `YGAtomicColors.Gray.*`·`Cherry.*`·`Melon.*`·`Pudding.*`·`Transparency.*`)로 확산됨. 확정 시 시맨틱으로 정리 권장. → [open-questions](../synthesis/open-questions.md) 후보.

## 화면 컨테이너 (`screen/`)

화면 루트에 쓰는 컨테이너 2종 + 뒤로가기 스코프. 설계 상세 → [designsystem-ygscreen-scaffold 스펙](../superpowers/specs/archive/2026-07-20-designsystem-ygscreen-scaffold.md).

> 🔁 **정본 변경 (2026-08-16) — 스캐폴드는 이제 `YGScaffoldV2`이고, 그 자리는 nav가 아니라 Route다.**
> 아래 `YGScaffold` 항목은 **역사**이고, **새 코드는 예외 없이 `YGScaffoldV2`를 Route 안에서 쓴다.**
> 설계 → [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md).
>
> ```kotlin
> // EntryBuilder — Route 를 부르기만 한다
> entry<NavKeyXxx> { XxxRoute(navigator = navigator, modifier = Modifier.fillMaxSize()) }
>
> // Route — 스캐폴드를 소유하고 로딩·실패를 배선한다
> YGScaffoldV2(modifier = modifier, isLoading = state.isLoading, toastPolicy = toastPolicy) { innerPadding ->
>     XxxScreen(..., modifier = Modifier.fillMaxSize().padding(innerPadding))
> }
> ```
>
> **왜 Route인가**: `hiltViewModel()`이 Route 안에서 호출되므로 `EntryBuilder`는 `state.isLoading`을
> 읽을 수도, 실패 이펙트를 받을 수도 없다. 스캐폴드가 nav 레벨에 있으면 공통 로딩·에러를 채울 방법이
> 원리적으로 없다.
>
> 지켜야 할 것 셋:
> - **`modifier`는 스캐폴드가 받고, Screen에는 `Modifier.fillMaxSize().padding(innerPadding)`을 새로 만들어 준다.**
>   `fillMaxSize`를 빼면 `weight(1f)`을 쓰는 화면이 접힌다.
> - **에러 토스트가 필요한 화면만** `rememberYGToastPolicy()`를 만들어 넘기고 이펙트에서 `showError(문구)`를 부른다.
>   문구는 화면 소유이고(`String` 계약), 사유→문구 매핑은 `GlobalNicknameError`·`LoginError`처럼
>   enum + `@Composable toStringResource()`로 둔다(ADR-0016).
> - **재시도 동선이 필요한 실패는 V2가 다루지 않는다.** 그건 화면이 자기 UI로 표현한다
>   (`GroupListErrorScreen` 같은 전면 에러, 입력 자리 인라인 등).
>
> ✅ **이관이 끝났다(2026-09-20, PR #513·#514)** — develop에 `YGScaffold`(V1) 호출부가 **0건**이고
> 스캐폴드를 쥔 Route는 **23파일**이다(`GroupListRoute`만 호출 둘 — 본문 + 그룹 추가 오버레이).
> 아래 문단은 그 과정의 역사다.
>
> 이관은 화면별로 진행했다 — **한때 develop 기준 17화면 이관(A-002 로그인 · S-003 앱 설정 · S-002 계정
> 정보 · S-101 그룹 설정(#285) · G-001 그룹 목록(#297) · 스플래시(#305) · 약관 웹뷰(#296) ·
> C-106 토핑 배치(#290) · 카메라 3 · 갤러리 2 · 세그멘테이션 3(#309) · 온보딩 약관 동의(#315)),
> V1 잔여 2파일**(PR #267 · #285 · #290 · #296 · #297 · #305 · #309 · #315).
> **여덟째는 이관이 아니라 신규 화면이다** — #290의 `CanvasToppingPlaceRoute`가 처음부터 Route에서
> V2를 소유해 규약을 지켰다(`isLoading`도 토스트도 안 쓴다 — 이 화면은 부를 API가 없다). 잔여 파일
> 수가 안 준 것도 같은 이유다: `feature/groups/canvas/impl` EntryBuilder에는 옛 엔트리들이 그대로
> `YGScaffold`를 쓰고 있고 새 엔트리만 스캐폴드 없이 등록됐다.
> 뒤의 둘은 **로딩·실패를 채우려고 옮긴 것이 아니다** — 스플래시는 로띠를 시스템바 밑까지 그리려고
> `contentWindowInsets = WindowInsets(0)`으로 V2를 쓰고, 약관 웹뷰는 그전까지 머티리얼 `Scaffold`를
> 직접 부르던 자리(V1·V2 어느 쪽도 아닌 규약 이탈)를 메운 것이다. 둘 다 `isLoading`을 넘기지 않는다.
> 잔여 파일 수가 그때 그대로였던 이유는 `feature/intro/impl` EntryBuilder에 **약관 동의 화면 엔트리가
> 남아 있어서**다 — 스플래시만 빠지고 파일은 목록에 남았다(그 엔트리는 #315에 걷혔다).
> S-101은 **API 결선 라운드에 이관이 딸려 온 첫 사례**다 — 로딩 오버레이와 실패 토스트를 채울 것이
> 그때 생겼기 때문이고, OQ-P-204 ①("결선 라운드에 붙일지 이관 전용 라운드를 돌릴지")에 사례로 답한
> 셈이다. G-001은 그 답을 넓힌다 — API 결선이 아니라 **재조회 라운드**였는데, 새로고침 실패를
> 토스트로 알리기로 하면서 호스트가 필요해졌다(→ [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md)).
> 즉 이관을 끌어오는 것은 결선 자체가 아니라 **채울 것(로딩·실패)이 생기는 시점**이다. `YGScaffold`는 `@Deprecated(WARNING)`이고 삭제 시점은 **모든 화면이
> Route에서 스캐폴드를 소유하고 로딩·실패를 배선한 뒤**다 →
> [open-questions](../synthesis/open-questions.md) [2026-08-17] OQ-P-204.
>
> **로딩 오버레이를 켜는 기준은 "네트워크 왕복인가"**다(세 사례에서 귀납한 것이고 디자인이 확정한
> 규칙은 아니다 → OQ-P-205). 버튼 비활성은 "지금 눌러도 소용없다"만 말할 뿐 언제 끝날지 모르는 대기를
> 표현하지 못하고, 그동안 입력 필드가 살아 있어 요청이 나간 뒤에도 값을 고칠 수 있다.
>
> 📌 **S-001이 토스트 호스트를 얻었다(2026-08-19, PR #306)** — 회원 탈퇴 결선으로 실패를 말할 자리가
> 처음 필요해졌고, 이미 V2를 쓰던 Route가 `rememberYGToastPolicy()`를 만들어 넘기는 것으로 끝났다.
> **이관이 끝난 화면은 채울 것이 생겨도 컨테이너를 손대지 않는다**는 것이 이 라운드가 보여 준 것이고,
> 그래서 그때는 이관 수치(8화면·V1 잔여 6파일)도 그대로였다.
>
> 📌 **한 라운드가 8개 엔트리를 한꺼번에 옮겼다(2026-08-20, PR #309 develop 머지)** — 어차피
> 세 모듈(`camera`·`gallery`·`segmentation`) 파일을 다 여는 라운드라 스캐폴드 이관을 같이 태웠다.
> `camera`(`NavKeyCameraCustom`·`NavKeyCameraSystem`·`NavKeyPictureConfirm`) · `gallery`
> (`NavKeyCustomGalleryPicker`·`NavKeySystemGalleryPicker`) · `segmentation`
> (`NavKeySegmentation`·`NavKeySegmentationConfirm`·`NavKeyToppingEdit`) 8개 엔트리가 이번에 V2로
> 옮겨 **이관 화면이 16개**가 됐다. `CustomCameraScreen`·
> `CustomGalleryPickerScreen`이 직접 꽂고 있던 `YGToastHost`·`toastPolicy` 파라미터를 걷어 스캐폴드로
> 옮겼고, 세 모듈 모두 `isLoading`은 쓰지 않는다(로딩이 전부 화면 고유 표현이라 V2가 흡수하지 않는
> 갈래). 카메라 촬영 실패는 이번에 `showError` 토스트가 붙었다(전에는 조용히 뒤로 갔다). **V1
> `YGScaffold` 잔여가 처음으로 줄어 3파일이 됐다** — 전부 EntryBuilder(`feature/intro/impl`·
> `feature/groups/enter/impl`·`feature/groups/canvas/impl`)이고 셋 다 그 모듈의 진입 화면을 만드는
> 자리다 → [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).
>
> 📌 **이관만 하는 라운드가 처음 돌았다(2026-08-20, PR #315 develop 머지)** — 온보딩 약관 동의가
> 옮겨 오며 **V1 잔여가 2파일**(`feature/groups/enter/impl` 3곳 · `feature/groups/canvas/impl` 5곳,
> 둘 다 EntryBuilder)이 됐다. OQ-P-204 ①이 묻던 "결선 라운드에 붙일지, 이관 전용 라운드를 돌릴지"에
> **후자의 첫 사례**이고, 앞선 사례들과 갈린 이유는 **채울 것이 이미 있었기** 때문이다 — 이 화면은
> 서버 조회·가입 요청이 진작 결선돼 있었고 실패 표현만 `TODO`로 비어 있었다. 그래서 이관이
> 컨테이너 교체로 끝나지 않고 **`TermAgreeError` 2갈래 + 공통 토스트 결선**을 같이 데려왔다
> (→ [state-management](state-management.md) "서버 실패 갈래는 feature 로컬 enum",
> [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md) "실패 표현").
> 같은 라운드가 `isLoading`에 **두 플래그를 함께** 넘기는 첫 사례도 만들었다
> (`state.isLoading || state.isSigningUp` — 조회와 가입이 같은 오버레이를 쓴다).
>
> 🔁 **"화면 고유 로딩 화면은 V2가 흡수하지 않는 갈래"가 뒤집혔다(2026-08-22, PR #311 develop 머지)** —
> 세그멘테이션이 `SegmentationLoadingScreen`·`SegmentationErrorScreen` 둘을 **삭제하고** 로딩을
> `YGScaffoldV2(isLoading = …)`에, 실패를 공통 토스트에 넘겼다. 바로 위 #309 문단이 "세 모듈 모두
> `isLoading`은 쓰지 않는다(로딩이 전부 화면 고유 표현이라 V2가 흡수하지 않는 갈래)"고 적은 그
> 세 모듈 중 하나이고, **이관 두 달 만에 그 판정만 다시 봤다**. 이관 화면 수는 안 변한다(17개,
> 세그멘테이션은 #309에 이미 옮겨 왔다) — 바뀐 것은 옮겨 온 스캐폴드에 **채울 것이 있었다**는 판정이다.
> 되짚은 근거는 제외를 세웠던 두 조건이 실제로는 값이 없었다는 것이다 — 로딩 문구는 안내문 두 줄이었고,
> 닫기 버튼은 **오버레이가 터치를 삼키므로 로딩 중에는 눌리지도 않던 것**이다. 대가는 명시한다:
> **로딩 중 닫기가 도달 불가**가 됐고(시스템 뒤로가기는 그대로), 안내 문구 두 줄이 사라졌다.
>
> 🔁 **#309의 토스트 절반이 되돌려졌다(2026-08-26, PR #371 develop 머지)** — 카메라·갤러리 두 화면이
> `YGToastHost`를 **다시 자기 레이아웃 안**으로 가져갔다. 방식은 #309 이전과 같다: Route가
> `rememberYGToastPolicy()`를 만들어 **Screen에 넘기고**, Screen이 자기 프레임 `Box`(카메라는 뷰파인더
> 자리, 갤러리는 사진 그리드 자리) 안에 `TopCenter`로 호스트를 심는다.
> 되돌린 이유는 **위치**다. 스캐폴드 호스트는 상태바 인셋 바로 아래에 떠서 두 화면이 자기 위에
> 그리는 헤더 행(날짜·닫기 버튼)을 **덮었다**. 즉 #309 문단이 갤러리에 대해 "보이는 위치는 사실상
> 그대로다"라고 적어 둔 것은 **틀렸고**, 카메라에 대해 "위키 Toast 공통 정책에는 이쪽이 맞는다"고
> 적은 판단도 이 화면들에는 안 맞았다 — 위키 [[toast]]는 노출 **방향**만 정하고 **어느 프레임의
> 위인지**는 정하지 않는다.
> ⚠️ **고치는 방법이 브랜치 안에서 한 번 갈렸다.** 첫 커밋은 `YGScaffoldV2`에 `toastTopPadding`
> 파라미터를 더하고 Route가 헤더 높이를 **dp로 다시 계산**해 넘겼다(닫기 버튼 지름·간격 상수를
> Route에 복제). 두 번째 커밋이 그것을 **통째로 되돌리고** 호스트를 프레임 안에 심는 쪽으로 갔다 —
> 코드 주석이 근거를 적는다: **위치 계산 없이** 프레임 윗변에 뜬다. 그래서 `YGScaffoldV2`의
> 시그니처는 이 라운드에서 한 줄도 안 바뀌었다.
> ⚠️ **그 대가로 두 화면에는 호스트가 둘이 됐다** — Route가 `toastPolicy`를 넘기지 않으므로
> 스캐폴드는 **자기 기본 정책으로 호스트를 하나 더 만들고**, 그 호스트에는 아무도 발행할 수 없다.
> 스캐폴드에 호스트를 끄는 수단이 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-312.
> 이로써 이 저장소의 화면 고유 로딩 화면은 **0개**다(OQ-P-205 ①③ 해소). **`isLoading`을 켜는 기준의
> 반례이기도 하다** — 세그멘테이션은 온디바이스 추론이라 네트워크 왕복이 아닌데 오버레이를 켠다.
> 아래 "네트워크 왕복인가"는 그래서 **"사용자가 기다려야 하는 비동기 작업인가"**로 읽는 편이 맞고,
> 규약 승격 여부는 여전히 OQ-P-205 ②다.
>
> 🔁 **둘 중 실패 화면만 이틀 만에 되돌아왔다(2026-08-24, PR #342 develop 머지)** —
> `SegmentationErrorScreen`이 다시 생겼다. 뒤집힌 근거는 판단이 아니라 **디자인이 나온 것**이다
> (Figma `C-103-Error`: 닫기만 있는 상단 바 + 경고 아이콘 + 문구 두 줄). #311이 삭제할 때의
> 이유가 "그 화면이 위키가 정의한 실패 처리를 담은 적이 없다"였는데, 담을 것이 생겼다.
> **토스트가 통째로 대체되지는 않았다** — 실패를 둘로 가른다: 대상을 아예 못 얻어 화면에서 할 수
> 있는 것이 없으면 **화면**(`SegmentationState.isError`, 1회성 효과가 아니라 상태여야 재구성에서
> 살아남는다), 고른 뒤의 저장 실패는 후보 목록이 살아 있으므로 **토스트**다.
> **로딩 화면 0개는 그대로다**(되살아난 것은 실패 화면뿐이고 `isLoading`은 계속 `YGScaffoldV2`가
> 받는다). ⚠️ 재시도·원본 사용 버튼은 디자인에 없어 넣지 않았고, 위키 [[누끼-따기]]
> (link) 정책과
> 갈리는 그 자리는 OQ-P-153 ④로 남았다 →
> [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md).
>
> 📌 **오버레이가 조회 화면으로 넘어왔다(2026-08-30, PR #407 develop 머지)** — 그때까지 `isLoading`을
> 켜던 자리는 사용자가 **누른** 작업(로그인·가입·세그멘테이션·그룹 생성·참여)이었는데, G-001 그룹
> 목록과 C-001 오늘 캔버스가 **화면에 들어오자마자 나가는 첫 조회**에 같은 오버레이를 켠다. 두 화면
> 다 이미 `YGScaffoldV2`를 쥐고 있으면서 `isLoading`만 안 넘기던 자리라 시그니처는 안 바뀌었고,
> 각 `UiState`에 `isInitialLoading`이 하나씩 생겼다.
> **조건이 "조회 중"이 아니라 "아직 한 번도 못 받은 조회"다** — 두 화면 모두 재진입마다 조회가
> 나가므로(`Enter` + `LifecycleResumeEffect`) 조건을 안 걸면 다른 화면에서 돌아올 때마다 이미 그려진
> 목록·캔버스 위로 덮개가 번쩍인다. 그룹 목록은 **당겨서 새로고침도 제외**한다(그쪽은 자기
> 인디케이터가 돈다 — `isRefreshing`과 합치지 않은 이유가 이것이고, 그리는 자리도 다르다).
> 켜고 끄는 것을 둘 다 `launch` 블록 **안**에서 하는 것도 규칙이다 — 코루틴 키 가드에 막히면 블록이
> 아예 안 돌아 밖에서 켠 것을 내려 줄 `finally`가 따라오지 않는다.
> ⚠️ 그 판정이 지금은 **화면마다 복제돼 있다**(`todayCanvas == null` / `groupList == null && 당김 아님`)
> → OQ-P-205 ②·OQ-P-330. 캔버스 쪽은 실패 갈래를 좁혀 **오늘 캔버스 조회만** 덮는다(연도 목록·달력
> 기록은 실패해도 노출하지 않는 값이라 화면을 붙들 이유가 없다).
> ⚠️ **캔버스 쪽은 `launch` 규칙이 곧 성립하지 않는다**(브랜치 `feature/canvas-polling`, develop
> 미머지) — 폴링이 얹히면 오늘 캔버스 조회가 화면의 `launch` 에서 저장소 구독으로 옮겨 가
> `finally` 를 둘 자리가 없어진다. 그 자리의 규칙은 **구독이 열릴 때 캔버스가 없으면 켜고, 캔버스가
> 실리거나 갱신 실패 신호가 오면 내린다**로 바뀐다. 그룹 목록은 그대로다
> → [canvas-today-ssot-polling 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md) 「실패 표현」.
>
> 🔁 **덮개가 슬롯이 됐다(2026-09-04, PR #440 develop 머지)** — `loadingOverlay: @Composable () -> Unit`이
> 세 번째 파라미터로 붙고 기본값이 종전 동작(`YGLoadingOverlay()`)이라 호출부 전부가 그대로다.
> 첫 소비는 C-001이다 — 같은 `isLoading` 하나로 로딩 덮개와 **실패 덮개**(다시 시도 버튼 포함)를
> 갈아 끼운다. **터치를 삼키는 것이 슬롯의 몫으로 내려간 것**이 이 변경이 옮긴 책임이다: 스캐폴드는
> 어디에 겹칠지만 정하므로, 그냥 그리기만 하는 것을 넘기면 아래가 그대로 눌린다(그래서 화면이
> `YGDimOverlay`를 쓴다). ⚠️ **함께 접근성 차단 수단이 정정됐다** — `isLoading`일 때 content에 걸던
> `semantics { hideFromAccessibility() }`가 `clearAndSetSemantics { }`로 바뀌었다. 앞의 것은 **그 노드
> 하나만** 감추고 자식이 붙인 시맨틱은 트리에 남아, 덮개 아래 버튼이 TalkBack에 그대로 노출됐다
> ([ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md) as-built ①이
> 세운 규칙의 수단이 틀렸던 것이고, 규칙 자체는 그대로다). 계측 5 → **7건**.
>
> 📌 **덮개에 최소 노출 500ms 가 생겼다(2026-09-10, PR #482 develop 머지)** — `isLoading` 이 켜진
> 순간부터 `YG_LOADING_MINIMUM_VISIBLE_MILLIS` 가 지나기 전에는 꺼도 유지한다. **덮개가 깜빡이기만
> 하고 사라지면 무엇을 기다렸는지 알 수 없어서**이고, 붙드는 대상은 덮개와 접근성 차단 **둘 다**다
> (하나만 붙들면 터치는 막히는데 TalkBack 은 통과하는 비대칭이 생긴다). **값을 화면이 아니라
> 디자인시스템이 소유한다** — 처음에는 C-001 안에 있었고, 같은 문제가 스캐폴드를 쓰는 모든 화면의
> 것이라 여기로 올라왔다. 그래서 이 성질은 `YGScaffoldV2` 를 쓰는 **모든 화면의 계약**이다. 시각은
> `TimeSource.Monotonic` 으로 재 시각 변경에 흔들리지 않는다. ⚠️ G-001 당겨서 새로고침 인디케이터도
> 같은 값 500 을 쓰지만 그리는 자리가 달라(`isRefreshing`) **상수가 둘로 갈렸다**
> ([open-questions](../synthesis/open-questions.md) OQ-P-394). 계측 7 → **9건**
> → [canvas-feedback-fixes 스펙](../superpowers/specs/archive/2026-09-10-canvas-feedback-fixes.md).
>
> ✅ **마지막 한 화면이 옮겨 V1 호출이 0이 됐다(2026-09-20, PR #513·#514)** — A-004 초대 코드가
> `feature/groups/enter/impl` EntryBuilder의 `YGScaffold(contentWindowInsets = WindowInsets(0.dp))`를
> 벗고 Route가 `YGScaffoldV2`를 쥔다(#513). 인셋 관용구는 그대로 옮겨 왔다 —
> `statusBarsPadding()` + `navigationBarsAndImePadding()`을 Screen `modifier`가 계속 직접 문다.
> **옮기면서 `isLoading = uiState.isSubmitting`이 붙어 코드 제출 중 공통 로딩 덮개가 처음 떴다**
> (전에는 다음 버튼 비활성만 있었다). 이것도 "채울 것이 생기는 시점"이 아니라 **이관이 채울 것을
> 데려온** 방향이다. 같은 날 #514가 캔버스 EntryBuilder에 남아 있던 구 형태 엔트리 셋
> (캔버스 편집·이미지 선택·`NavKeyCanvasMove`)을 **화면째 삭제**해 나머지 잔여도 사라졌다.
> ⚠️ **V1 파일은 아직 지우지 않았다** — `@Deprecated(WARNING)` 그대로이고 `ERROR` 승급·삭제 시점은
> OQ-P-204 ②다. 호출부 0이라는 조건은 이제 충족됐다.

- **역할 분리 (구 컨벤션 — `YGScaffold` 시절)**:
  - **`YGScaffold` = nav 레벨(EntryBuilder)** — `entry<NavKeyXxx> { YGScaffold { innerPadding -> XxxRoute(...) } }`. Material3 `Scaffold` 얇은 래퍼(기본 배경 흰색, `contentWindowInsets` 노출). TopBar/BottomBar/inset이 필요한 엔트리 컨테이너. → [navigation-flow](navigation-flow.md) 체크리스트.
  - **`YGScreen` = 화면 최외곽(Screen 컴포저블)** — `internal fun XxxScreen(...) { YGScreen(modifier = modifier) { ... } }`. `Surface` 래퍼(`modifier` + `content`만) + `YGScreenScope` 리시버. `Surface`는 `color`를 항상 칠하므로(기본 Material surface 불투명) 내부 `color = YGAtomicColors.Gray.Transparent` 고정 → 배경 미페인트, 실제 배경은 nav의 `YGScaffold` containerColor가 담당(레이어 분리). 화면 `modifier`는 `YGScreen`에 전달(관례).
- **뒤로가기**: `YGScreen`의 content는 `YGScreenScope` 리시버라 `OnBack(enabled, handler) { }`(@Composable, 내부 `BackHandler` emit)로 처리. 호출한 화면만 back 가로챔 — 안 쓰면 안 부르면 됨(강제 리턴 없음). `OnBack`은 @Composable node-emit이라 PascalCase(`BackHandler` 동일 규칙).
- **배경 탭 포커스 해제는 컨테이너 책임이 아니다 (🔁 2026-08-03)**: `YGScreen`은 포커스 관심사를 갖지 않는다. 텍스트 입력이 있는 화면이 `YGScreen(modifier = modifier.clearFocusOnTap())`처럼 **직접 opt-in**한다(`core:util:android` `focus/`, 상세 → [clearfocusontap-modifier 스펙](../superpowers/specs/archive/2026-08-03-clearfocusontap-modifier.md), PR #192 develop 머지). `YGScreen`에 `clickableYGNoRipple { clearFocus() }`를 상시 결선했다가 철회한 이유는 두 가지 — ① `Modifier.clickable`은 `role = null`이어도 semantics에 `onClick` action과 focus target을 추가해 **컨테이너를 쓰는 모든 화면의 배경 전체**가 접근성 서비스에 인터랙티브 요소로 노출된다, ② 컨테이너 선택과 "입력이 있는가"는 직교하는 축이라 컨테이너에 묶으면 입력 없는 화면이 비용만 지고 `YGScreen`을 안 쓰는 입력 화면은 혜택을 못 받는다. **동작만 있고 시각 표현이 없는 관심사는 DS 컨테이너가 아니라 `core:util:android` 유틸로 둔다**가 일반 규칙.
  결선 도입·철회는 둘 다 S-002 브랜치 안에서 일어나 develop의 `YGScreen`은 한 번도 결선을 가진 적이 없다. 철회 잔여물인
  `clickableYGNoRipple`은 한동안 사용처 0으로 남았다가 **#284 이관으로 프로젝트 표준 클릭 유틸이 됐다**(아래 clickable 규약).
- **주의**: 현재 `YGScreen`↔`YGScaffold` 미통합(`YGScaffold`는 `YGScreenScope`/OnBack 없음). 통합·역할 정리는 [open-questions](../synthesis/open-questions.md) 미결(머지 후 ADR 예정).

## 컴포넌트 인벤토리

구현된 `component/*` 컴포넌트와 상세 설계(스펙). 심볼명 기준(개수·라인 미기재).

| 컴포넌트 | 패키지 | 스펙 |
|---|---|---|
| `YGButton` | `component/ygbutton/` | (레퍼런스, 스펙 이전) |
| `YGTextField` / `YGTextFormField` | `component/textfield/` | [ygtextfield](../superpowers/specs/archive/2026-07-10-ygtextfield.md) · [ygtextformfield](../superpowers/specs/archive/2026-07-10-ygtextformfield.md) |
| `YGHorizontalDivider` / `YGHorizontalDashedDivider` / `YGListItem` | `component/etc/` | [yghorizontaldivider](../superpowers/specs/archive/2026-07-12-yghorizontaldivider.md) · [ygdangerzone-dashed](../superpowers/specs/archive/2026-07-19-ygdangerzone-dashed.md)(점선 구분선) · [yglistitem](../superpowers/specs/archive/2026-07-12-yglistitem.md) |
| `dashedBorder()` Modifier | `border/` | [ygdangerzone-dashed](../superpowers/specs/archive/2026-07-19-ygdangerzone-dashed.md) |
| `YGIconButton`(+`YGIconButtonSize`) | `component/ygiconbutton/` | [ygiconbutton](../superpowers/specs/archive/2026-07-12-ygiconbutton.md) |
| `YGActionItem`(#183로 `iconResource` 선두 아이콘 변형 신설, #260로 `enabled`) | `component/ygactionitem/` | [ygactionitem](../superpowers/specs/archive/2026-07-12-ygactionitem.md) |
| `YGInputNumber`(+`YGInputNumberPreviewData`) | `component/yginputnumber/` | [yginputnumber](../superpowers/specs/archive/2026-07-13-yginputnumber.md) |
| `YGChipButton`(+`YGChipButtonColors`·`YGChipButtonColorsDefaults`) | `component/ygchipbutton/` | [ygchipbutton](../superpowers/specs/archive/2026-07-16-ygchipbutton.md) |
| `YGInviteCard`(+`YGInviteCardStatus`) | `component/card/` | [yginvitecard](../superpowers/specs/archive/2026-07-14-yginvitecard.md) |
| `YGModalPopup` | `component/modal/` | [ygmodalpopup](../superpowers/specs/archive/2026-07-15-ygmodalpopup.md) |
| `YGNametagChip`(+`YGNametagChipStyle`·`YGColorChipType`·`YGNametagChipPreviewData`) / `YGUserChip`(+`YGUserNameStyle`) / `YGChipColorIndicator` | `component/ygcolorchip/` | [ygcolorchip](../superpowers/specs/archive/2026-07-18-ygcolorchip.md) |
| `YGDate` / `YGLabel` | `component/ygtext/` | [ygtext-date-label](../superpowers/specs/archive/2026-07-18-ygtext-date-label.md) |
| `YGAlert`(+`YGAlertPolicy`·`YGAlertHost`·`YGAlertItem`·`rememberYGAlertPolicy`) | `component/ygalert/` | [ygalert](../superpowers/specs/archive/2026-07-23-ygalert.md) |
| `YGToast`(+`YGToastType`·`YGToastPolicy`·`YGToastHost`·`YGToastItem`(+`tag` #405)·`rememberYGToastPolicy`·`showError` #267) | `component/ygtoast/` | [ygtoast](../superpowers/specs/archive/2026-07-23-ygtoast.md) · [ygscaffold-v2](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)(`showError`) |
| `YGLoadingOverlay`(+`YG_LOADING_OVERLAY_TEST_TAG`) / `YGLoadingLottie`(+`YGLoadingArt`·`YG_LOADING_LOTTIE_TEST_TAG`) — 인디케이터는 디자인 로띠로 확정(#305), Dim 농도는 `YGDimOverlay` 소관으로 넘어갔다(#440) | `component/ygloading/` | [ygscaffold-v2](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md) |
| `YGDimOverlay` — Dim 판 + 터치 삼킴 + 선택적 병합 시맨틱, `ColumnScope` 슬롯(#440 신설) | `component/ygdimoverlay/` | (스펙 없음 — 아래 "로딩·덮개") |
| `YGSkeleton`(+`YG_SKELETON_TEST_TAG`) — 원격 이미지 대기 자리의 시머 회색면, 크기는 호출자 몫(#440 신설) | `component/ygskeleton/` | (스펙 없음 — 아래 "로딩·덮개") |
| `YGTopBar`(Back/Detail/Empty/Canvas 변형 + private `YGTopBarContent`·`ygTopBarBackdrop` + `YGTopBarDefaults`) | `component/ygtopbar/` | [ygtopbar](../superpowers/specs/archive/2026-07-18-ygtopbar.md)(최초 계약) · [designsystem-bar-listdate-components](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md)(Canvas·날짜·블러) |
| `YGListDate` | `component/yglistdate/` | [designsystem-bar-listdate-components](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md) |
| `YGFloatingBar{BackClose,Close,Edit,EditTab,Title}`(`Title`은 #406 develop 머지) | `component/ygfloatingbar/` | [designsystem-bar-listdate-components](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md) |
| `YGDateButton` | `component/ygdatebutton/` | [ygdatebutton](../superpowers/specs/archive/2026-07-18-ygdatebutton.md) |
| `YGDangerZone` | `component/ygdangerzone/` | [ygdangerzone-dashed](../superpowers/specs/archive/2026-07-19-ygdangerzone-dashed.md)(현행 점선, #159) · [ygdangerzone](../superpowers/specs/archive/2026-07-18-ygdangerzone.md)(구 solid, superseded) |
| `YGCircleButton`(+`YGCircleButtonType`) | `component/ygcirclebutton/` | [designsystem-button-missing-components](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) |
| `YGEditButton` | `component/ygeditbutton/` | [designsystem-button-missing-components](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) |
| `YGEditTabButton` | `component/ygedittabbutton/` | [designsystem-button-missing-components](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) |
| `YGEditActionButton` | `component/ygeditactionbutton/` | [designsystem-button-missing-components](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) |
| `YGCameraShutter` | `component/ygcamerashutter/` | [designsystem-button-missing-components](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) |
| `YGCanvas`(+`YGCanvasBackground`) | `component/ygcanvas/` | [designsystem-canvas-components](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md) |
| `YGCanvasMenu`(+`YGCanvasMenuAction`·`YGCanvasMenuItem`) | `component/ygcanvasmenu/` | [designsystem-canvas-components](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md) |
| `YGCanvasDateSelectButton` | `component/ygcanvasdateselect/` | [designsystem-canvas-components](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md) |
| `YGStrokeButton`(#259로 `borderWidth`) / `YGMenuItem` | `component/ygstrokebutton/` · `component/ygmenuitem/` | [designsystem-canvas-components](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md) · [c201-canvas-calendar](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md) |
| `canvasCutCornerShape()` | `shape/` | [designsystem-canvas-components](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md) |
| `ygBackgroundDotGrid()`(Modifier) | `component/ygbackgrounddotgrid/` | [c001-canvas-main](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) |
| `YGGrouptagChip`(+`YGGrouptagChipType`) | `component/yggrouptagchip/` | [designsystem-grouptag-topping-components](../superpowers/specs/archive/2026-07-31-designsystem-grouptag-topping-components.md) |
| `YGToppingGroup`(+`YGToppingGroupType`·`YGToppingImage`·`YGToppingTemplate`) | `component/ygtoppinggroup/` | [designsystem-grouptag-topping-components](../superpowers/specs/archive/2026-07-31-designsystem-grouptag-topping-components.md) |
| `YGToppingCutoutImage` — 위 `YGToppingImage`와 **다른 물건**(아래 항목), #334 develop 머지 | `component/ygtoppingcutout/` | [ADR-0025](../adr/0025-topping-border-as-server-field.md) · [c106-topping-place-api](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) |
| `YGTutorialOverlay` / `YGTutorialBox`(+`YGTutorialProgress`·`YGTutorialBoxPlacement`), #449 develop 머지 | `component/ygtutorial/` | (선작성 스펙 없음 — 아래 항목) |
| `YGScreen` / `YGScaffold`(+`YGScreenScope`·`OnBack`) | `screen/` | [designsystem-ygscreen-scaffold](../superpowers/specs/archive/2026-07-20-designsystem-ygscreen-scaffold.md) (위 "화면 컨테이너") |

- **`YGIconButton` = 공통 아이콘 버튼**: 정사각 컨테이너 + 중앙 아이콘 + enabled/pressed tint, 크기 프리셋 enum(`YGIconButtonSize` — `SIZE_48` 아이콘 크기는 #183에서 교정). `YGTextField`의 clear 아이콘은 이미 인라인 `Box`+`Image`에서 `YGIconButton(size = YGIconButtonSize.SIZE_44)`로 치환됨(`YGTextFieldImpl.kt`). `YGListItem` trailing caret도 `YGIconButton`으로 치환(#136 develop 머지 #148).
  **줄 높이는 컴포넌트가 맞춘다(#295)** — trailing 아이콘 오버로드는 `YGIconButton`이 높이를 정하고
  `subText` 오버로드는 텍스트 한 줄뿐이라 같은 목록 안에서 줄마다 높이가 달랐다. 공용 `YGListItemImpl`이
  아이콘 쪽 높이(프리셋 컨테이너 + 세로 패딩 2배)를 계산해 `heightIn(min = …)`으로 깐다 — 리터럴이
  아니라 토큰 계산이라 프리셋이 바뀌면 따라온다. 브랜치 안에서는 호출 화면(S-001)이 먼저 높이를
  맞췄다가 컴포넌트로 내려보냈다 — develop에 남은 것은 컴포넌트 쪽 하나뿐이다.
- **`YGInputNumber`**: 숫자 셀. 컨테이너 크기·보더는 토큰 대신 고정 dp로 하드코딩(코드 주석: 디자인가이드 고정 크기)이라 토큰화 예외 사례. **각짐 sync(#183)** — 배경·`clip`·테두리 3곳 모두 `radius.none`. shape·typography는 `YGTheme.*` 사용, 색은 `YGAtomicColors.Gray.*` 직접 참조.
- **`YGChipButton`**: pill(`shapes.radius.round`) 칩 버튼. text + 선택 start/end 아이콘, 아이콘 유무로 좌/우 패딩 비대칭. **Colors 패턴 준수** — `YGChipButtonColors`(@Immutable, default/pressed×fg/bg/border) 주입 + `YGChipButtonColorsDefaults` 프리셋(**#183으로 `CherrySubtle`·`CherrySolid` 재명명**, **#188로 `CherrySubtle`→`GrayOutline` 교체·개명**(Figma `Button-Chip-Left`가 Cherry 계열 → 흰 배경 + 회색 테두리로 바뀌어 값과 이름을 함께 갈았다. 프리셋 하나를 고치자 소비처 6곳이 따라오며 G-001 칩 드리프트도 닫혔다) — Figma `Button-Chip-Left`/`Right`를 KDoc으로 병기. 세로 패딩도 #183에서 `padding2`로 내려 `YGAlert`·`YGTopBar` 높이에 전파). pressed 분기(아래 관용구). 프리셋 색은 `YGAtomicColors` 직접 참조(과도기).
- **`YGToggleButton` 삭제(#183 develop 머지, 2026-08-01)**: 대응 Figma 원본이 없고 실화면 사용처가 0건이라 제거했다(대체물은 신설 `YGEditButton`). `component/ygtogglebutton/` 2파일 + `:app-preview` 잔재까지 함께 지웠고, 이로써 [2026-07-16 규약 이탈 항목](../synthesis/open-questions.md)이 해소됐다.
- **화면 적용(#182 develop 머지, 2026-08-01)**: C-101 카메라·C-101-confirm·갤러리 화면이 `YGCameraShutter`·`YGCircleButton`(플래시·전환·닫기)·`YGButton`·`YGDate`·`YGToast` 호스트를 쓰면서 feature 로컬 임시 셔터·flip 구현이 삭제됐다 — **셔터 2구현 공존 해소**([2026-07-30 항목](../synthesis/open-questions.md)). `YGToastPolicy`/`YGToastHost`의 첫 실사용처이기도 하다(촬영 가이드 토스트) → [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md).
- **화면 적용(#218 develop 머지, 2026-08-11)**: A-002 로그인이 실물화되면서 `KakaoSignInButton`이 `RoundedCornerShape` 리터럴 → `YGTheme.shapes.radius.none`, 리터럴 dp 패딩 → `YGTheme.layout.padding.*`, 라벨 타이포 → `YGTheme.typography.body.b01SB`로 토큰화됐다. 다만 **버튼 자체는 DS 컴포넌트가 아니라 feature 로컬 Material3 `Button`**이고(외부 로그인 가이드 색을 따라야 해서 `YGButton` 변형에 안 맞는다), 주입한 `ButtonColors.contentColor`는 내부 `Text`가 색을 명시해 死필드다. 같은 화면 `PagerIndicator`의 활성/비활성 색은 여전히 리터럴이다 → [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md).
- **버튼 신설 5종**(#183, `ygcirclebutton`·`ygeditbutton`·`ygedittabbutton`·`ygeditactionbutton`·`ygcamerashutter`): `YGCircleButton`만 변형 타입(`YGCircleButtonType`)이 색·아이콘 크기·tint·`paintsOuterCircle`을 들고(단 `@Immutable` + 평범한 `val`이라 `YGButtonType`의 `@get:Composable` 패턴과 갈린다), 나머지 4종은 컴포저블 본문 상태 분기다. Colors data class는 5종 모두 미분리 — 규약과 갈리는 판단(→ [open-questions](../synthesis/open-questions.md)). 선택형(`YGEditButton`·`YGEditTabButton`)은 `selectable`(`Role.Button`/`Role.Tab`), 나머지는 `clickable(indication = null)` + `role = Role.Button`. 밑줄 폭은 `width(IntrinsicSize.Max)`로 텍스트에 묶는다.
  - 📌 **첫 실화면 소비처(2026-08-14, PR #221)** — C-104/C-105 편집 화면이 `YGEditButton` 2개(모드 전환)·`YGEditActionButton` 2개(되돌리기/다시실행)를 쓰고, `YGEditTabButton`은 `YGFloatingBarEditTab`을 통해 간접 소비된다. `YGCircleButton`·`YGCameraShutter`(#182)에 이어 신설 5종이 전부 실화면에 닿았다.
  - 📌 **`YGCircleButton`이 "버튼 아닌 손잡이"로도 쓰이기 시작했다(#264 develop 머지, 2026-08-16)** — C-301 토핑 탭의 모서리 4개 중 둘(크기조절·회전)이 `onClick = {}` 빈 람다에 `Modifier.dragBy`를 덧대 **드래그 핸들**로 쓴다. 컴포넌트는 여전히 `role = Role.Button`이라 스크린리더에는 눌리는 버튼으로 읽히고 눌러도 아무 일이 없다 — 조작 종류(탭 vs 드래그)를 표현할 API가 없다는 뜻이다 → [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md). 나머지 둘(삭제·편집)은 평범한 소비이고 `ic_edit`·`ic_scale` 아이콘 2종이 이때 신설됐다.
    - 📌 **핸들 관용구가 화면 밖으로 나왔다(#290 develop 머지, 2026-08-19)** — 같은 조합을 감싼
      `ToppingDragHandleButton`이 `feature/groups/canvas/impl`의 `component/` 패키지로 올라가
      C-301 편집 탭과 C-106 배치 화면이 공유한다(제스처 추적 키가 `toppingId: Long` → `key: Any?`로
      일반화돼, 대상이 하나뿐인 화면은 `Unit`을 넘긴다). **디자인시스템이 아니라 feature 로컬 공유**라
      "조작 종류를 표현할 API가 없다"는 문제는 그대로이고, 이제 **두 화면**에 퍼졌다 →
      [open-questions](../synthesis/open-questions.md) OQ-P-202 ③.
- **캔버스 5종 + 컷 도형**(#185, `ygcanvas`·`ygcanvasmenu`·`ygcanvasdateselect`·`ygstrokebutton`·`ygmenuitem` + `shape/`): `YGCanvas`가 배경(`YGCanvasBackground` sealed — `Solid`/`Image`+Coil)·토핑 `BoxScope` 슬롯·날짜바·메뉴·Dim을 합성한다. Figma 5상태를 단일 enum이 아니라 **직교 불리언 플래그**(`isDimmed`·`isMenuExpanded`·`isEmpty`·`isCalendarVisible`)로 표현하고, 값 파라미터는 내용만 든다(모순 조합 방지는 호출자 책임). Dim은 항상 최상단에서 아래 레이어 터치를 **소비**하고 확장 메뉴·캘린더만 그 위로 승격하며, 승격 시 `Spacer(Size44)`로 총높이를 유지한다. 좌상단 컷 실루엣은 배경·날짜바·Dim이 공유하므로 `shape/canvasCutCornerShape()`로 분리했다(`border/`와 같은 "컴포넌트 아닌 그리기 프리미티브" 층위). 높이 44는 패딩 도출 대신 `SizeTokens.Size44` 고정. **첫 소비 화면(#199 develop 머지, 2026-08-11)** — C-001이 임시 `Button` 2개를 걷어내고 이 5종을 쓰면서 세 가지가 바뀌었다. ① `YGCanvas`가 **반응형 배치를 흡수**했다(`BoxWithConstraints` + private `calculateCanvasLayoutMetrics` — 좌우 패딩·상하 최소 gap·세로 중앙·세로 부족 시 축소가 컴포넌트 안으로 들어와 전제가 `fillMaxWidth`에서 `fillMaxSize`로 바뀌었다. 위키 [[캔버스-반응형-레이아웃]]의 크기·위치 우선순위를 컴포넌트가 구현한다). ② Dim 탭 닫기가 **컴포넌트 API로** 열렸다(`onDimClick`, 구현은 소비 전용 `pointerInput` → `clickable(indication = null)`이라 터치 소비는 유지) → [2026-08-01 항목](../synthesis/open-questions.md) ① 해소. ③ 인접 테두리를 `spacedBy(-1.dp)`로 겹쳐 접합선이 2dp에서 1dp가 됐다(스펙이 "그대로 둔다"고 적었던 것을 뒤집음). 화면 배경 점 격자는 `YGCanvas` 밖 `Modifier.ygBackgroundDotGrid()`로 신설됐다 → [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md). **두 번째 캔버스 화면은 컴포넌트를 안 썼다(#231 develop 머지, 2026-08-15)** — C-301 배경 편집의 미리보기는 `YGCanvas`가 아니라 화면 로컬 `Box` + `aspectRatio` + `border`다. 그래서 컷 도형·Dot Grid·메뉴가 빠지고 좌우 여백이 `padding7`(20)이 아닌 21dp 리터럴이며, 캔버스 비율도 컴포넌트 private 상수가 아니라 `domain`에 새로 만든 `CANVAS_ASPECT_RATIO`를 쓴다. 🔁 **비율 상수는 하나로 모였다**(#334 develop 머지, 2026-08-22) — `domain` 쪽을 지우고 `YGCanvas`의 상수를 public으로 올려 이 미리보기도 그것을 참조한다. 미리보기가 `YGCanvas`를 안 쓰는 것 자체는 그대로다(OQ-P-174). `YGCanvasBackground`(`Solid`/`Image`)는 그 화면의 **이펙트 payload로만** 소비되고 실제 `YGCanvas`에는 여전히 안 넘어간다 → [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md). **`calendarContent` 슬롯이 처음 채워졌다(#259 develop 머지, 2026-08-16)** — C-001이 `isCalendarVisible`을 켜고 화면 로컬 `CustomCalendar`를 넣는다. 이로써 `YGCanvas`의 직교 플래그 넷이 전부 실사용되고, Dim이 **메뉴와 캘린더 양쪽의 스크림을 겸한다**(`isDimmed = isMenuExpanded || isCalendarVisible`, `onDimClick`이 둘 다 닫는다). `YGCanvasBackground.Image` 렌더만 여전히 미검증이다 → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md). **`background`·토핑 `content` 슬롯이 처음 채워졌다(#268 develop 머지, 2026-08-17)** — C-001이 캔버스 응답의 배경을 `YGCanvasBackground`로 옮겨 넘기고(`Solid`/`Image` 둘 다 실경로가 생겼다), `content`에 화면 로컬 `CanvasToppingLayer`를 넣는다. `isEmpty`도 상수에서 토핑 목록 파생이 됐다. 🔁 **컴포넌트가 배경 폴백을 갖지 않는다는 사실이 여기서 드러났다** — 미설정·미지 type·색 파싱 실패 셋을 화면이 각각 판정해 `Solid(Gray100)`(= 컴포넌트 기본값과 같은 값)으로 떨어뜨리므로 **기본값이 화면 쪽에 복제됐다**(이 복제는 #351이 걷었다, 아래 참고). 토핑 레이어는 자기 상자를 `fillMaxSize`로 정하지 않고 **호출자에게서 받는다**(배치가 받은 상자에 대한 비율이라 상자를 컴포넌트가 정하면 안 된다) — `YGCanvas`가 배치를 흡수한 것과 반대 방향의 선택이다 → [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md). **다섯 번째 슬롯 `overlayContent`가 생겼다(#298 develop 머지, 2026-08-20)** — 캔버스 본체 `Box`의 **형제**라 Dim·확장 메뉴·달력보다 위에 그려지고, 상단은 캔버스 위 여백에 맞추되 **폭은 캔버스가 아니라 화면 기준**이다. C-001이 여기에 `YGToastHost`를 꽂아 Spotlight 작성자 토스트를 띄우고, 그 결정이 이 화면의 다른 토스트 자리까지 정했다(스캐폴드가 아니라 이 호스트, OQ-P-167) → [c202-canvas-spotlight 스펙](../superpowers/specs/archive/2026-08-20-c202-canvas-spotlight.md). **달력 슬롯은 탭을 스스로 삼킨다(#319, 2026-08-20)** — 달력은 Dim 위에 겹쳐 있을 뿐이라 항목 사이 여백을 누르면 뒤의 Dim이 받아 달력이 닫혔다. 막는 자리를 슬롯 주입자가 아니라 **컴포넌트 쪽 `pointerInput`**에 둬서 `calendarContent`에 무엇이 들어와도 같은 규칙이 걸린다. **배경 폴백이 컴포넌트로 내려왔다(#351 develop 머지, 2026-08-25)** — `background`가 **nullable**이 되고 기본값이 `Solid(Gray100)`에서 `null`로 바뀌었다. `null`이면 컴포넌트가 **흰 바탕**을 깔고, 화면 쪽 `DEFAULT_CANVAS_BACKGROUND` 복제는 사라져 C-001은 미설정·미지 type·색 파싱 실패 셋을 전부 `null`로 넘긴다. 함께 **빈 안내판의 조건이 좁아졌다** — `isEmpty`만이 아니라 `isEmpty && background == null`일 때만 뜨고, 그때는 문구만 얹는 대신 `Gray100` **판**이 배경 자리를 통째로 덮는다(배경을 고른 캔버스는 토핑이 0개여도 고른 배경이 그대로 보인다). 직교 플래그 하나가 다른 파라미터와 얽힌 첫 사례라 "값 파라미터는 내용만 든다"는 이 컴포넌트의 원칙에서 한 칸 벗어나 있고, 노출 조건 자체는 정책 근거가 없다 → OQ-P-304. 이 규칙을 잠그는 계측 테스트 2건(`YGCanvasTest`)이 함께 들어왔으나 **CI는 계측을 컴파일만 한다**(OQ-P-102 ②).
  - 📌 **배경이 자기 로딩 상태를 밖으로 알린다(#440 develop 머지, 2026-09-04)** — `YGCanvas`에
    `reloadKey`·`onBackgroundStateChange`가 붙고 새 enum `YGCanvasBackgroundState`(`Loading`/`Loaded`/
    `Failed`)가 그 값이다. **이미지 배경이 아니면 기다릴 것이 없어 곧바로 `Loaded`**이고, 배경이나
    `reloadKey`가 바뀌면 판정이 다시 선다. 대기 중에는 `YGSkeleton`이 배경 자리를 덮는데 그리는 자리가
    **캡처 `Box`의 형제**다 — 안에 두면 갤러리에 저장한 이미지에 시머 회색면이 그대로 박힌다(같은
    컴포넌트가 저장 대상이기도 하다는 것이 이 배치의 근거다, [c001-canvas-gallery-save](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)).
    이 라운드의 계측 테스트가 2 → **8건**이 됐으나 **CI는 여전히 계측을 컴파일만 한다**(OQ-P-102 ②).
  - 📌 **메뉴 액션이 활성 여부를 싣는다(#334 develop 머지, 2026-08-22)** — `YGCanvasMenuAction`에
    `isEnabled`가 붙어 `YGCanvasMenu`가 비활성 표현과 클릭 차단을 함께 맡는다. 첫 소비는 C-001의
    토핑 추가 버튼 가드다(오늘 캔버스를 아직 못 받았으면 흐름에 못 들어간다) →
    [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) 「표시·제어 규칙」.
  - 📌 **메뉴 버튼 한 칸이 비면 나머지가 전폭을 갖는다(#414 develop 머지, 2026-09-01)** —
    `YGCanvasMenu.addAction`이 nullable이 되어, `null`이면 `weight(1f)` 두 칸을 만들지 않고
    `editAction` 하나가 `fillMaxWidth`로 선다. 지난 캔버스에서 "갤러리에 저장"이 날짜바로 옮겨 가며
    왼쪽 칸에 넣을 것이 없어진 자리다. **직교 플래그로 상태를 표현하던 이 컴포넌트가 "파라미터가
    없음"으로 배치를 가른 첫 자리**이고, 두 칸을 겹쳐 그리던 `spacedBy(-1.dp)` 행 자체가 사라지므로
    접합선 규칙도 그때만 성립하지 않는다.
  - 📌 **`YGMenuItem`이 아이콘과 비활성 표현을 갖췄다(#414, 2026-09-01)** — `Box` + 중앙 텍스트이던
    것이 `Row`가 되어 `iconResource`(선택, 텍스트 오른쪽 `Size20`)와 `isEnabled`를 받는다. 비활성이면
    테두리 `Gray200`·내용 `Gray300`으로 내려가고 눌림 배경과 클릭이 함께 잠긴다. 즉 활성 여부를
    액션 데이터(`YGCanvasMenuAction.isEnabled`)로 싣던 규칙이 **확장 메뉴 항목에도 같은 모양으로**
    생겼다 — 다만 이쪽은 데이터가 아니라 컴포저블 파라미터다.
  - 📌 **날짜바가 캘린더 열기와 갤러리 저장 둘을 든다(#413, 2026-09-01)** —
    `YGCanvasDateSelectButton`의 오른쪽 `YGIconButton`(`ic_calender`)이 걷히고, **캘린더 그림이 왼쪽
    텍스트 앞으로** 옮겨 가 날짜·요일과 함께 하나의 클릭 영역이 됐다(그림은 `YGIconButton`이 아니라
    `Image`이고 눌림 색만 부모에게서 받는다 — 터치 영역은 부모가 갖는다). 오른쪽 자리는
    `isSaveVisible`이 참일 때만 `ic_save` 버튼이 채우고, 없을 때는 텍스트가 잘린 모서리에 붙지 않게
    끝 패딩을 대신 넣는다. **이 컴포넌트가 동작 둘을 갖게 된 첫 라운드**이고, 저장을 보일지는
    컴포넌트가 아니라 호출자(C-001)가 캔버스 내용으로 정한다.
  - 📌 **캔버스가 자기 그림을 밖으로 내보낼 수 있게 됐다(#324 develop 머지, 2026-08-23)** —
    `captureGraphicsLayer: GraphicsLayer?`를 넘기면 `CanvasArea`가 그리면서 그 레이어에도 함께
    기록하고, 호출부가 나중에 `toImageBitmap()`으로 받아 간다. **레이어를 거는 자리가 이 API의
    전부다** — 바깥 `Box`가 아니라 **배경·토핑만 담는 안쪽 `Box`**에 걸어 테두리·컷 도형·빈 캔버스
    문구·날짜 버튼(화면 크롬)을 캡처에서 뺀다. 컴포넌트가 "무엇이 그림이고 무엇이 프레임인가"를
    처음으로 갈라 놓은 자리이고, 그 경계가 곧 갤러리에 저장되는 이미지의 경계다. 넘기지 않으면
    분기 자체가 없어 기존 호출부는 영향이 없다 →
    [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md).
  - 📌 **`overlayContent`에 호스트가 둘이 됐다(#324, 2026-08-23)** — C-001이 `YGToastHost` 아래
    `YGAlertHost`를 세로로 병치했다. 슬롯은 여전히 하나이고 겹침 규칙은 컴포넌트가 아니라 주입자
    몫인데, ~~**얼럿을 띄우는 코드가 아직 없어** 그 규칙이 검증된 적이 없다~~
    → 🔁 **첫 소비처가 생겼다(#411, 2026-09-01)** — C-001이 그룹 생성·참여 직후 진입에서 환영 배너를
    1회 띄운다. 다만 그것이 원래 이 호스트를 세운 사유(전일 캔버스 마감 알림)는 아니라서 문자열 셋과
    TODO 주석은 그대로 남았고, **토스트와 겹칠 때의 규칙은 여전히 검증된 적이 없다**(배너를 띄우는
    진입에서 토스트가 함께 뜰 경로가 없다) → [open-questions](../synthesis/open-questions.md) OQ-P-273.
- **`YGGrouptagChip` / `YGToppingGroup`**(#186, G-001 그룹 목록용): 칩은 이름+구분점+상대시간 pill이고 `YGGrouptagChipType`이 **타임스탬프 색만** 결정한다(Nametag용 `YGColorChipType`과 매핑이 별개라 타입도 분리). 🔁 **6종 + `DEFAULT`가 됐다**(#308 develop 머지, 2026-08-20) — Nametag 12종을 둘씩 묶은 6종에 **가리킬 사람이 없는 경우**(마지막 토퍼가 나갔거나 아직 아무도 안 올렸다)를 위한 중립 변형이 붙었다. `YGToppingGroup`은 160dp 프레임에 96dp 토핑을 회전·오프셋으로 얹고 칩을 겹치며 **클리핑·`onClick`이 없다**(오버플로우 허용, 터치 범위는 호출자가 `clickableYG`로 감쌈). 대체 그래픽 정책은 갖지 않고 `YGToppingImage` 3상태(`Remote`/`Template`/`Error`)를 주입받아 렌더만 한다. 고정폭 프레임이 칩에 **측정** 제약을 내리므로 칩에 `wrapContentWidth(unbounded = true)`, 비정사각 원격 이미지 때문에 `rotate` **안쪽**에 `clip(RectangleShape)`가 필요하다(둘 다 실기기 검증에서 드러난 조건). **첫 소비처(#194 develop 머지, 2026-08-07)** — G-001이 `YGToppingGroup`을 그리기 시작했다. 다만 ① 호출부가 `clickableYG`로 감싸지 않아 토핑 클릭 경로가 없고, ② `YGToppingImage`는 `Remote`만 쓰여 템플릿·에러 분기가 화면 선택으로 들어오지 않았으며(에러는 `AsyncImage(error = …)` 폴백으로만 그려짐), ③ `chipType`이 전 항목 동일 값 고정이라 위키 [[S-101-프로필-닉네임-컬러-규칙-v0.3]] 매핑이 미구현이다 → [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) · [open-questions](../synthesis/open-questions.md) [2026-08-07]. ✅ **③이 닫혔다(#308·#310 develop 머지, 2026-08-20)** — 서버가 목록에 `lastPlacedByNameTagChip`을 실어 주고 `list/impl/util/GrouptagChipType.kt`가 그것을 6종으로 접는다. **색이 목록 순서가 아니라 사람에 걸린 것이 이 변경의 뜻이다** — 그룹이 하나 빠져도 남은 카드의 색이 밀리지 않는다. ✅ **①이 닫혔다(#268 develop 머지, 2026-08-17)** — 호출부가 카드 `modifier`에 `clickableYGScaleRipple`을 걸어 **토핑에 첫 클릭 경로**가 생겼고 누른 그룹의 캔버스(C-001)로 간다. 컴포넌트는 그대로다 — `onClick`을 열지 않고 `modifier` 위임으로 해결한 첫 사례다 → [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md). 배치(지그재그·인셋·저개수 규칙)는 컴포넌트가 아니라 화면의 `ToppingLayout`이 쥔다. ✅ **원격 이미지가 `ContentScale.Crop`에서 `Fit`으로 바뀌었다**(#396 develop 머지, 2026-08-27) — `Crop`은 짧은 변을 96dp 프레임에 맞추느라 긴 변을 잘라 내서 비정사각 누끼의 피사체가 사라졌다. `clip(RectangleShape)`는 그대로 두되 **역할이 바뀌었다** — 비정사각 이미지를 프레임에 가두는 수단이 아니라, 이미지가 프레임을 넘어 인접 셀을 덮는 것을 막는 방어선이다(`rotate` 안쪽이어야 하는 조건은 그대로다. 바깥이면 회전 오버행까지 잘려 배치 변형이 깨진다). 템플릿·에러 갈래의 스케일은 손대지 않았다. 이 변경은 [open-questions](../synthesis/open-questions.md) OQ-P-316이 ②로 적어 둔 것인데, 선행이라던 ①(서버가 목록 응답에 테두리 필드를 주는 것)보다 **먼저 일어났다**. ✅ **테두리를 그리기 시작했다**(#497 develop 머지, 2026-09-16) — `YGToppingImage.Remote`에 `border: YGToppingBorder?`(색·dp·거리판)가 붙고, `Remote` 갈래가 `AsyncImage`를 버리고 **크기를 명시한 Coil painter**로 바꿔 `YGToppingCutoutImage`에 넘긴다. ⚠️ **요청에 `size`를 넣는 것이 이 갈래의 조건이다** — `AsyncImage`는 레이아웃 제약으로 크기를 정해 주지만 `rememberAsyncImagePainter`에 `model = url`만 넘기면 원본 해상도로 디코딩한다(테두리 없는 카드도 이 경로를 타므로 영향이 목록 전체다). 띠가 알맹이 상자 밖으로 굵기만큼 나가는데 `clip(RectangleShape)`은 그대로 둬야 하므로, **토핑을 굵기만큼 안쪽으로 줄여**(`clip` **아래**의 `padding(borderWidth)`) 테두리까지 96dp 프레임에 넣는다 — 바깥 틀과 배치를 한 치도 바꾸지 않는 쪽을 골랐고, 대가로 굵기 상한 30dp에서는 알맹이 긴 변이 36dp까지 줄어든다. 축소는 painter 상태가 아니라 **데이터**로 정해 거리판·이미지 도착 시점에 크기가 튀지 않게 하고, 에러 갈래에서만 푼다(에러 그래픽엔 테두리가 없다). 색은 `Success`에서만 넘긴다 — 알맹이보다 먼저 깔면 실루엣 모양 색 덩어리만 보인다. 거리판은 컴포넌트가 아니라 **feature가 불러와 넘긴다**(`rememberToppingOutlines`가 `:core:ui`에 있고 디자인시스템은 그 모듈에 의존하지 않는다 — 캔버스 `CanvasToppingLayer`와 같은 분업이다). `Template`·`Error` 갈래와 `YGToppingImage` 3상태 계약은 그대로다 → [g001-group-list-topping-border 스펙](../superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md)
- **`YGToppingCutoutImage` = 누끼 + 실루엣을 따르는 테두리**(#334 develop 머지, 2026-08-22): 누끼(잘라 낸 배경) 위에 사각 테두리를 그대로 두르면 잘라 낸 배경이 다시 드러나므로, 실루엣을 따르는 테두리를 먼저 깔고 그 위에 원본을 얹는다. develop은 이 테두리를 같은 그림을 테두리 색으로 물들여 **여덟 방향으로 밀어 찍는** 방식으로 그린다. ⚠️ **바로 위 `YGToppingImage`(G-001 파르페 토핑)와는 이름만 닮은 다른 물건이다** — 그쪽은 목록 카드가 쓰는 3상태(`Remote`/`Template`/`Error`) 렌더러라 테두리 개념이 없다. 한쪽을 고치면서 다른 쪽까지 같은 것으로 보면 안 된다. 이 컴포넌트가 `:core:designsystem`에 있는 이유는 소비 화면 셋(누끼 확인·배치·캔버스)이 **모듈 둘에 걸쳐** 있어서다([module-structure](module-structure.md) 컴포저블 소유 규칙). `borderWidth`는 **화면 dp**라 토핑을 키워도 굵기가 그대로다 — 편집 화면이 원본 픽셀 좌표계로 그리는 것과 어긋나는 자리이고 그 판정은 OQ-P-245다. 그림이 아직 뜨지 않은 painter로 찍으면 플레이스홀더 실루엣이 테두리로 보이므로, 준비되기 전에는 호출부가 `borderColor`에 `null`을 넘기는 것이 계약이다. **방향 수 상수가 공개됐다(2026-08-27, PR #388)** — `private const OUTLINE_STAMP_COUNT`가 `TOPPING_OUTLINE_STAMP_COUNT`로 개명하며 최상위 `const`로 열렸다. 캔버스의 [토핑 알파 판정](../superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md)이 **같은 방향으로 되민 점의 알파**를 읽어 판정 모양을 테두리까지 맞추므로, 그리는 쪽과 판정하는 쪽이 같은 값을 봐야 한다. 양쪽이 각자 상수를 들면 언젠가 갈린다 — 그래서 정본은 그리는 쪽이 갖고 판정이 그것을 읽는다. **소비처가 하나 늘었다** — 배경 편집(`CanvasBGEditScreen#CanvasToppingImage`)이 맨 `Image`에서 이 컴포넌트로 갈아탔다(같은 라운드, OQ-P-254 해소).
  ⚠️ **렌더러가 거리장 하나로 바뀐다(PR #464 develop 머지, [ADR-0030](../adr/0030-topping-outline-distance-field.md))** — 여덟 방향 스탬프는 원판과의 민코프스키 합이 아니라 여덟 개 평행이동의 합집합이라, 인접 스탬프 사이가 굵기의 약 0.765배만큼 벌어진다. 그보다 가는 부위(빨대 등)는 사본이 서로 닿지 않아 갈라져 보이고 볼록한 외곽은 부채꼴로 울퉁불퉁해진다. 이 결함을 없애려고 스탬프 여덟 장을 **거리판 띠 한 장 + 원본 한 장**으로 바꾼다 — 토핑당 프레임 그리기가 9회에서 2회로 준다. 새 파라미터 `outline: ToppingOutline?`이 그 띠를 만들 거리판을 받는다(**기본값 없음** — 넘기는 것을 잊으면 컴파일이 막는다. `null`은 `borderColor = null`과 같은 뜻으로, 그림이 아직 준비되지 않았을 때는 테두리를 안 그리는 기존 계약을 그대로 잇는다). `TOPPING_OUTLINE_STAMP_COUNT`는 이 브랜치에서 사라지고, 캔버스 판정도 같은 거리판을 읽어 "여덟 방향으로 되민 점" 근사가 없어진다 — 판정 모양이 근사가 아니라 정의상 그리는 모양과 일치한다.
  ⚠️ **띠 판은 알맹이 상자 밖으로 `borderWidth`만큼 나간다 — 이것이 계약이다.** 부르는 쪽이 클리핑 레이어나 `alpha < 1`을 씌우면 오프스크린 버퍼가 상자 밖을 잘라 띠가 그만큼 잘리므로, 그런 자리는 상자를 굵기만큼 키우고 안쪽으로 덜어내야 한다. `CanvasBGEditScreen`이 `outlineInset`으로 상자를 키우고 안쪽으로 덜어내는 이 우회를 이미 쓰고 있고, 이번 변경으로 그 우회가 **네 화면 공통 규칙으로 승격**된다. 🔁 **다섯이 됐다**(#497 develop 머지, 2026-09-16) — G-001 목록 카드가 다섯 번째이고, 상자를 키우는 대신 **알맹이를 줄이는** 반대 방향을 골랐다(96dp 프레임과 지그재그 배치를 고정하려고).
  ✅ **실기기 확인을 마쳤다(2026-09-07)** — 육안 확인 6항목이 모두 통과했고 `ALPHA_8` 비트맵에 `ColorFilter.tint`가 하드웨어 가속 캔버스에서 의도대로 먹었다. `ARGB_8888` 폴백은 쓰지 않으므로 색은 캐시 열쇠에 안 들어간다. 다만 그 확인이 깜빡임 하나를 새로 드러냈다 — **띠 판이 컴포저블 `remember` 수명이라 화면 전환·Spotlight 전환마다 사라지고, 다시 만드는 동안 테두리가 없었다.** `ToppingBorderPlateCache.kt`(같은 패키지, LRU 32칸)가 판을 컴포지션 밖에 남겨 그 자리를 없앤다(수정 뒤 실기기 재확인 완료). 열쇠는 **거리판 인스턴스 + 굵기px + 알맹이 비율**이고 표시 크기는 안 들어간다. 다만 굵기가 판에 구워져 있어 크기가 다른 판을 늘려 그리면 화면상 굵기가 그 배율만큼 틀어지므로, **1.25배 넘게 어긋난 판은 안 그리고 새 판을 기다린다.** 자세한 내용은 [topping-border-distance-field 스펙](../superpowers/specs/archive/2026-09-07-topping-border-distance-field.md) 「띠 비트맵을 언제 다시 만드는가」. 🔁 **열쇠당 판이 한 장에서 선반(크기별 최대 3장)이 됐다**(#497 develop 머지, 2026-09-16) — 목록 토핑과 캔버스 토핑은 같은 URL이라 거리판 인스턴스도 굵기도 같아 **열쇠가 겹치는데** 긴 변은 1.25배를 넘게 어긋난다. 한 장만 두면 한쪽이 만든 판이 다른 쪽 판을 덮어써 카드를 눌러 캔버스로 들어갈 때마다 테두리가 깜빡였다. 열쇠에 크기를 넣는 대신 선반을 둔 것은 "핀치 한 번에 항목이 쏟아진다"는 [ADR-0030](../adr/0030-topping-outline-distance-field.md)의 근거를 **열쇠당 장수 상한**으로 대신 막을 수 있고, 크기를 모르는 첫 컴포지션에서도 판을 꺼낼 수 있어서다. `get`에 `subjectLongSide`(선택)가 붙었고, `put`은 `fitsSubject`로 비슷한 크기 판을 걷어 낸 뒤 맨 앞에 넣는다. 그리기 단계도 바뀌었다 — 상태의 판이 지금 크기에 안 맞으면 **캐시에서 맞는 판을 한 번 더 찾고**, 거기에도 없을 때만 새 판을 기다린다. 열쇠 상한 32칸은 그대로라 **총 장수 상한이 세 배**가 된다(OQ-P-317).
- **이미지 로딩**: Coil 3. `coil-compose`에 더해 **`coil-network-okhttp`가 `build-logic` `ComposeConfig`에 추가됨(#186)** — 그전까지 네트워크 페처가 없어 원격 URL이 아예 로드되지 않았다(로컬 MediaStore URI만 쓰던 탓에 드러나지 않았다). `YGToppingGroup.Remote`·`YGCanvasBackground.Image`가 이 의존에 걸린다. **로더 설정은 #440부터
`image/ParfaitImageLoader.kt`가 한 곳에서 쥔다**(아래 "이미지 로더").
- **`YGModalPopup`**: Compose `Dialog` 위 중앙 팝업. 아이콘+제목+본문 + 2버튼(`YGButton.Medium.Secondary` 좌/`Primary` 우, `weight(1f)` 균등). 버튼 confirm/cancel 의미 미규정(타입만 노출), 단일 `isEnabledButton`. 프리뷰 `@YGPreview`/`PreviewBox`. **첫 실화면 소비처(#224 develop 머지, 2026-08-12)** — 그때까지 `:app-preview` 갤러리에서만 그려지던 컴포넌트를 A-005 그룹 생성·A-004 초대코드 두 화면이 확인 모달로 쓴다(🔁 **#261, 2026-08-16 — A-004의 호출이 S-102 그룹 닉네임으로 옮겨갔다.** 문구·좌우 배치는 그대로라 소비처 수도 좌우 진영도 변함없다). 표시 여부는 규약대로 호출자가 쥔다(각 UiState `isConfirmPopupVisible`). 두 화면이 **취소=좌 Secondary / 실행=우 Primary**로 배치했는데, Danger Zone 팝업 스펙은 피그마 근거로 **파괴적 액션=좌 Secondary / 취소=우 Primary**라 좌우 의미가 갈린다. **두 배치가 develop에 공존 확정(#225 머지, 2026-08-13)** — 확인 팝업 3종(서비스 탈퇴·그룹 나가기·그룹 신고)이 반대 배치로 들어와 호출자가 6곳이 됐고 정확히 반으로 갈렸다. 네 인자가 전부 같은 타입이고 `Dialog`가 프리뷰에 안 떠서 뒤바꿈을 잡는 자동 검증이 0건이다. **7번째 소비처(#231 머지, 2026-08-15)** — C-301 배경 편집의 그만두기 확인이 파괴적 액션=좌 Secondary(`그만두기`) / 취소=우 Primary(`계속 편집하기`) 배치를 골라 그쪽이 하나 앞선다. 또 A-005가 `isEnabledButton = isCreating.not()`을 쓰면서 "요청 중엔 확인만 비활성" 불가라는 단일 플래그 제약을 develop 코드가 처음 만났다 → [open-questions](../synthesis/open-questions.md) [2026-08-12] · [a005 스펙](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md). ✅ **그 제약을 만난 유일한 호출자가 사라졌다**(#393·#394 develop 머지, 2026-08-27) — A-005 그룹 생성과 S-102 그룹 참여가 둘 다 **요청 직전에 팝업을 닫는** 쪽으로 옮겼다. 그래서 A-005의 `isEnabledButton` 전달이 없어지고 진행 중 dismiss 가드(`isCreating`·`isEntering` 중 닫기 차단)도 함께 걷혔다 — 팝업이 이미 닫혀 있어 막을 것이 없다. 진행은 `YGScaffoldV2` 로딩 오버레이가, 실패는 토스트가 말한다. **Danger Zone 3종이 PR #287에서 고른 관용구와 같아졌고**(OQ-P-141 ②의 "순서는 뒤집지 않았다"), 이로써 `isEnabledButton`을 명시적으로 주는 호출자는 develop에 **0곳**이다 — [ygmodalpopup 스펙](../superpowers/specs/archive/2026-07-15-ygmodalpopup.md)의 "개별 비활성 불가" 미결은 소비처가 사라진 채로 남는다.
- **`YGInviteCard`**(+`YGInviteCardStatus` enum): 그룹 초대 코드 카드. Active/Invalid 상태로 border·subText·코드박스 배경·복사 버튼 활성 분기. 복사 버튼은 `YGButton.SmallSquare` 재사용. **각짐 sync(#159)** — 테두리 `shape`·`.clip`·`InviteCodeBox` clip 모두 `radius.none`. 프리뷰 `@YGPreview`/`PreviewBox`.
- **`YGTextField` / `YGTextFormField`**(`component/textfield/`): 단일 폼 + errorDescription 확장. **각짐/배경 sync(#159)** — 공통 `commonShape` = `radius.none`(각짐), 배경 = `grayScale.white`(불투명, 구 `transparency.white75`에서 변경). clear 아이콘은 `YGIconButton` 재사용. 🔁 **S-101 라운드 확장 2건(#223 develop 머지, 2026-08-13)** — ① `YGTextFormField`·`YGTextFieldImpl`에 `keyboardOptions`·`keyboardActions`를 기본값(`KeyboardOptions.Default`·`KeyboardActions.Default`)과 함께 노출해 `BasicTextField`로 전달한다(키보드 엔터로 확정을 받으려면 통로가 필요했다. 기존 호출부는 전부 named argument라 무영향). ② `YGTextFieldImpl`에 `defaultMinSize(minHeight = SizeTokens.Size48.getDp())`를 체인 맨 앞에 걸어 **최소 높이 48 고정** — `showClear`일 때 상하 패딩이 `padding5`→`padding1`로 줄고 44dp 아이콘 버튼이 들어오는 구조라 클리어 버튼 등장·소멸마다 행 높이가 재계산돼 필드가 들썩였다.
- **`YGNametagChip`**(+`YGNametagChipStyle`·`YGColorChipType`): 원형 네임태그 컬러칩. `YGColorChipType`이 fill/stroke/text 색을, `YGNametagChipStyle`(`Style28`/`Style40`)가 지름·테두리·타이포를 고정. 위키 정책 [[nametag-chip]] 구현체. **개명·정리(#165 develop 머지, 2026-07-31)** — 구 `YGColorChip`/`YGColorChipStyle`/`text` 파라미터 → `YGNametagChip`/`YGNametagChipStyle`/`userFirstName`, **패키지↔폴더 불일치 해소**(전 파일 `…component.ygcolorchip`). 🔁 **타입 개수 정정(#223 develop 머지, 2026-08-13)** — 구 14종(`NametagChip1~13`+`Plus`)에서 **12종 + `Plus`** 로 정렬했다. `NametagChip11`이 `NametagChip3`과 완전 중복이라 뒤가 한 칸씩 밀려 있었고 `NametagChip9`의 글자색이 테두리색과 같았다(Figma는 `Pudding500`). Figma 컴포넌트셋 `144:5415`가 정본이고 위키 정책 12종이 맞았다 → [open-questions](../synthesis/open-questions.md) [2026-07-18] 해소. **첫 화면 소비처도 같은 PR**이다 — S-101 그룹 설정의 `GroupMemberList`가 그룹원마다 칩을 그린다. 다만 타입 부여 주체가 미정이라(서버 `ParfaitGroupMemberResponse`에 타입 필드 없음) **목록 인덱스 순환 mock**이었고, 위키 [[nametag-chip]]의 "타입은 유저별 고정" 규칙은 미구현이었다.
**두 번째 화면도 같은 형태로 붙었다(#268, 2026-08-17)** — C-001 상단 바 멤버 칩이 mock 7명을 버리고
캔버스 응답의 `groupMembers`를 그리지만, 서버가 칩 색을 주지 않아 **목록 순서로 팔레트 7종을 돌려 썼다**.
🔁 **mock 순환이 둘 다 사라졌다(#308·#310 develop 머지, 2026-08-20)** — 부여 주체가 **서버**로 정해져
(그룹 안 활동 멤버 사이 유일·나가면 반납·재추첨 없음) S-101은 `members[].nameTagChip`, C-001은
`groupMembers[].nameTagChip`을 각각 자기 모듈 `util/ColorChipType.kt`로 옮겨 그린다. `NAMETAG_CHIP_PALETTE`가
사라졌고 **같은 사람이 두 화면에서 같은 색**이다(서버가 같은 행에서 두 값을 준다). 위키 [[nametag-chip]]의
"유저별 고정"은 이로써 구현됐으나 **유일성 범위가 정책과 다르다** — 계정 공통이 아니라 그룹 안에서만
성립한다. 글자는 닉네임 `take(1)` → [open-questions](../synthesis/open-questions.md).
🔁 **변형이 12종 + `Plus` + `Default`가 됐다**(#308 develop 머지, 2026-08-20) — 가리킬 사람이 없으면
아무 색이나 돌리지 않고 중립으로 간다(색이 "그룹 안의 이 사람"을 가리키는 신호라 거짓 신호를 만들면
안 된다). **매퍼가 앱이 모르는 문자열도 여기로 접으므로**(#310, [ADR-0024](../adr/0024-nametag-chip-unknown-fold.md))
이 변형이 그려졌다고 반드시 "나간 사람"인 것은 아니다. `Default`의 색 구분·대비는 여전히 디자인 미결
→ [open-questions](../synthesis/open-questions.md).
- **`YGUserChip`**(+`YGUserNameStyle`) / **`YGChipColorIndicator`**(#165 신설, 같은 패키지): `YGUserChip` = `YGNametagChip` + 이름 텍스트 `Row`(`gap3`, 수직 중앙), 이름 프리셋 `StyleMedium`/`StyleBold`가 타이포+Gray 색을 고정. `YGChipColorIndicator` = `isChecked`로 Cherry ↔ 투명 분기하는 작은 원(꺼짐에도 자리 유지). 둘 다 `:app-preview` 갤러리 미등록·feature 참조 0건이고, 인디케이터는 대응 위키 정책 문서가 없다(C-201 Chip-Indicator와 별개) → [open-questions](../synthesis/open-questions.md). 상세 [ygcolorchip](../superpowers/specs/archive/2026-07-18-ygcolorchip.md).
- **`YGDate` / `YGLabel`**(`component/ygtext/`): 타이포+색 프리셋 텍스트 래퍼. **`YGDate` 재설계(#149 develop 머지)** — `YGDate(text)`→`YGDate(date, day)` 2텍스트 `Row`(테두리 `border(0.75dp, Gray800)`)로 변경, 패딩은 하드코딩→`YGTheme.layout.padding.*` 토큰화(구 토큰 예외 해소). ⚠️ **#182(카메라 PR)에서 `background`가 `border` 뒤에 한 번 더 추가**돼 modifier 그리기 순서상 테두리를 덮는다(체인: 배경→테두리→배경). C-101 상단 날짜 라벨이 첫 실사용처다 → [open-questions](../synthesis/open-questions.md) [2026-08-01]. 상세 [ygtext-date-label](../superpowers/specs/archive/2026-07-18-ygtext-date-label.md).
- **`YGAlert` / `YGToast`**(노출 정책 패턴, #149 develop 머지): 상단 배너/토스트. **표시 컴포저블 + `*Policy`(상태 홀더) + `*Host`(자동 소멸·위로 스와이프 닫기·슬라이드 애니메이션) 분리**가 공통 관용구. 화면은 `remember*Policy()` 후 `show()`만, Host가 렌더/소멸 담당. `YGAlert`=단일 슬롯(새 show가 대체. **#411로 `buttonIconResource`가 열려** 칩 버튼 끝 아이콘이
`ic_caret_right` 고정에서 호출자 선택이 됐다 — 첫 소비가 초대코드 복사의 `ic_copy`이고, 복사한 뒤
같은 내용을 문구만 바꿔 다시 `show`하는 방식이라 **단일 슬롯이 곧 "상태 갱신 수단"으로 쓰인다**), `YGToast`=다중 스택(`add(0,…)` 최신 위로). **스택이 기본이되 태그를 주면 교체가 된다(#405, 2026-08-30)** — `show(type, replaceTag)`가 같은 `tag`를 단 토스트를 먼저 걷어내고 새것만 남긴다(`replaceTag = null`이면 지금까지처럼 쌓인다). 가르는 축을 `YGToastType`으로 두지 않은 이유는 **같은 타입이면서 쌓여야 하는 토스트**가 있어서다. 태그 문자열은 정책이 아니라 호출 화면이 갖는다 → OQ-P-329. `YGToast`는 `YGToastType`(InviteCode/Edit/Record/Fail) sealed로 색/구성 분기, 노출 동작은 위키 [[Toast-공통-정책]] 일치. **`Record`는 닉네임 색을 호출자에게서 받는다(#298, 2026-08-20)** — 디자인시스템이 들고 있던 `Pudding500` 고정이 `userNameColor` 파라미터가 됐다(작성자 칩 타입에 따라 6색으로 갈리므로 컴포넌트가 정할 수 없다). 같은 라운드에 `time`의 계약이 "숫자+단위"에서 **"조사까지 포함한 완성된 구절"**로 바뀌었다(`오래전`처럼 "전"이 안 붙는 갈래가 있어서) — 문장 조립이 `님이 {time}에 쌓았어요`다. 상세 [ygalert](../superpowers/specs/archive/2026-07-23-ygalert.md)·[ygtoast](../superpowers/specs/archive/2026-07-23-ygtoast.md).
- **`YGTopBar`**: 상단 바 4변형(Back/Detail/Empty/Canvas) 공유 레이아웃 private `YGTopBarContent`. 좌측 `YGIconButton.SIZE_44` + 안쪽 `weight(1f)` 타이틀 슬롯 + (Canvas만) 바깥 `trailingContent`. **변형 통합(#173 develop 머지, 2026-08-01)** — `YGTopBarDefault`(로고 + "새 그룹" 칩 하드결선)가 삭제되고 `YGTopBarEmpty`가 `rightContent` 슬롯을 받아 흡수했다. 칩 색·문구는 호출 화면이 정한다 → [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md). **Canvas 변형·날짜·배경 블러(#188 develop 머지, 2026-08-04)** — ① `YGTopBarCanvas` 신설(사방 `padding3` + 멤버 슬롯 + 우측 메뉴, List-Member는 컴포넌트가 아니라 호출자 조립), ② `YGTopBarContent`에 `contentPadding`·`trailingContent` 추가(기본값이 기존 동작과 같아 나머지 변형 무영향, `trailingContent`는 `weight(1f)` Row **바깥** 형제라 `Empty`의 `rightContent`(안쪽 형제)와 측정 의미가 다르다 → [open-questions](../synthesis/open-questions.md)), ③ `YGTopBarEmpty`의 로고 `ic_plus` placeholder가 **날짜 2텍스트**(`date`·`day`)로 교체돼 placeholder todo가 닫혔다, ④ 반투명 `White75` + **배경 블러**가 private `Modifier.ygTopBarBackdrop(hazeState)`로 들어갔다(`null`이면 틴트만, 반경은 `YGTopBarDefaults.BackdropBlurRadius`) → [ADR-0018](../adr/0018-backdrop-blur-haze.md). 프리뷰 칩 색 드리프트도 프리셋 교체(`GrayOutline`)로 함께 해소됐다. 상세 [designsystem-bar-listdate-components](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md). **상단 인셋 흡수(#194 develop 머지, 2026-08-07)** — `YGTopBarEmpty`가 `windowInsets: WindowInsets = YGTopBarDefaults.windowInsets`(= `WindowInsets.statusBars`, `@Composable` getter)를 받아 `windowInsetsPadding`으로 직접 흡수한다. 배경(`ygTopBarBackdrop`)이 인셋 패딩보다 **바깥** 체인이라 틴트·블러가 상태바 영역까지 덮는다. 호출 화면은 `edgeToEdge` 대응을 안 하는 대신 `YGScaffold`에서 상단을 빼야 하고(G-001은 `systemBars.only(Horizontal + Bottom)`), 인셋이 필요 없으면 `WindowInsets(0)`을 주입한다(`:app-preview` 3곳). **4변형 중 `Empty`만** 이 파라미터를 갖고, 화면 쪽 인셋 관용구가 3형태로 갈렸다 → [open-questions](../synthesis/open-questions.md) [2026-08-07]. **`Canvas` 변형 첫 소비처(#199 develop 머지, 2026-08-11)** — C-001이 그룹명 + `memberContent` 슬롯(멤버 칩 최대 5 + `NametagChipPlus` `+N`, 겹침은 화면이 정한 리터럴) + 햄버거로 조립한다. `windowInsets`가 없는 변형이라 엔트리 `YGScaffold` 기본 인셋을 쓰고, 그 탓에 같은 화면의 배경 점 격자가 상태바·내비게이션 바 영역을 못 덮는다 → [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md).
- **`YGActionItem`의 `enabled`**(#260 develop 머지, 2026-08-15): `clickable(enabled = …)`만 가른다 —
  **색은 바뀌지 않는다.** 비활성 색이 디자인시스템에 정의돼 있지 않아 컴포넌트가 임의로 정하지 않았고,
  KDoc이 그 사실을 적는다. 첫 소비처는 S-001 앱 설정의 로그아웃 항목(요청 중 비활성)이라 **눌리지
  않는 이유가 사용자에게 보이지 않는 상태**다 → [open-questions](../synthesis/open-questions.md) [2026-08-16].
- **`YGListDate` / `YGFloatingBar`**(#188 신설): `YGListDate` = `YGDateButton`(`Size44`) + `YGChipColorIndicator`를 `gap1`로 쌓은 C-201 날짜 셀. 업로드 점은 미체크 시 투명이라 **자리를 유지한 채 비노출**(셀 높이 불변)이고, "Button-Date가 Disabled면 인디케이터 항상 False"라는 위키 [[캘린더-컴포넌트]] 예외를 **컴포넌트가 내부에서 강제**한다(`isEnabled && isUploaded`) — 정책 예외를 호출부에 맡기지 않는 선례. **첫 실화면 소비처(#259 develop 머지, 2026-08-16)** — C-201 캘린더의 날짜 그리드가 셀마다 이걸 그린다. 다만 **패널·머리글·드롭다운은 디자인시스템에 없다** — `feature/groups/canvas/impl`의 화면 로컬 `CustomCalendar`·`CalendarDropdown`이고, C-201 컴포넌트 중 DS가 가진 것은 셀 하나뿐이다 → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md). `YGFloatingBar*`는 변형별 공개 함수 4종이 private `YGFloatingBarContent`(가로 `padding7`·상 `padding6`)를 공유하고, 자식이 하나뿐인 `Close`만 `Arrangement.End`다(`SpaceBetween`이면 좌측에 붙는다). 반복되는 닫기·확인 버튼은 파일 안 private 컴포저블로 묶었다. 폭은 컴포넌트가 정하지 않고 호출자 `modifier` 몫이라 **화면 배치 책임이 밖에 있다**. **`Title` 변형(#406 develop 머지, 2026-08-30)만 공용 `Row` 컨테이너를 쓰지 않는다** — 버튼이 우측 하나뿐이라 `SpaceBetween`으로는 제목이 중앙에 오지 않아 전용 `Box`로 조립하고, 제목 좌우를 버튼 지름(`Size44`)만큼 비워 겹침을 막는다. 첫 소비처는 C-102 갤러리이고, 그 화면이 사진 유무로 `Title`과 `Close`를 갈아 끼운다(빈 상태에는 제목이 없다 — 근거는 작업자 지시뿐이라 OQ-P-331). **`YGFloatingBarEditTab` 두 번째 실화면 소비처(#231 머지, 2026-08-15)** — C-301 배경 편집이 배경/토핑 2탭으로 쓴다(C-104/C-105 편집 화면에 이어). 탭 라벨은 화면 `strings.xml`이 갖고, 선택 인덱스는 화면 enum(`CanvasEditTab`) 순서에 묶인다. **`YGFloatingBarEdit`(제목형)도 첫 실화면 소비처를 얻었다(#264 머지, 2026-08-16)** — C-301에서 열린 토핑 테두리 재편집은 영역 탭이 없어야 하므로, 같은 편집 화면이 `borderOnly`면 `YGFloatingBarEditTab` 자리에 이걸 그린다. **한 화면이 상황에 따라 변형 둘을 갈아 끼운 첫 사례**다(변형별 공개 함수 분리가 호출부 분기로 드러난 자리) → [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md). **두 번째 화면은 C-106 토핑 배치(#290 머지, 2026-08-19)** — 다룰 대상이 하나뿐이라 탭 전환이 필요 없고 하단 바 가운데가 고정 문구다. 즉 `Edit`(제목형)를 고르는 조건이 "재편집이라 탭이 없다"에서 **"대상이 하나라 탭이 없다"**로 넓어졌다 → [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md).
  - 📌 **첫 실화면 소비처가 생겼다(2026-08-14, PR #221)** — C-103~C-105 세그멘테이션 4화면이 `BackClose`(추출·확인)·`Close`(로딩·에러)·`EditTab`(편집)을 쓴다. 배치는 전부 **세로 `Column`의 맨 위/맨 아래에 `fillMaxWidth()`로 붙이는 형태**이고 오버레이가 아니다. `EditTab`의 탭 문자열("영역"/"테두리")은 **화면 소유**로 확정됐다(`ToppingEditTab`의 `@StringRes label`을 화면이 `stringResource`로 풀어 넘긴다) — 컴포넌트 기본값이 아니다. `Edit` 변형만 여전히 소비처 0건이라 중앙 문구의 정체는 미결로 남는다 → [open-questions](../synthesis/open-questions.md) [2026-08-04].
- **로띠 의존 `com.airbnb.android:lottie-compose`**(#305): `gradle/libs.versions.toml`에 버전이 올라가고
  **`core:designsystem`이 모듈 `dependencies`에 직접** 건다(haze·coil처럼 `build-logic` `ComposeConfig`로
  전역에 깔지 않는다). 그래서 로띠를 쓰려는 다른 모듈은 각자 의존을 다시 달아야 하고, 실제로
  `feature/intro/impl`이 스플래시 때문에 같은 줄을 한 번 더 적었다 → [open-questions](../synthesis/open-questions.md).
  디자인시스템 쪽 소비 표면은 `YGLoadingLottie`뿐이다 — `progress`를 넘기지 않으면 스스로 무한 반복하고,
  넘기면(당겨서 새로고침처럼 손가락을 따라가야 할 때) 시계를 하나만 돌린다. 색은 화면 테마가 아니라
  **애니메이션이 얹히는 바탕**으로 고른다(`YGLoadingArt.Light`/`Dark`) — 그래서 Dim 위의
  `YGLoadingOverlay`는 `Light` 고정이고, 흰 바탕인 G-001 당겨서 새로고침은 `Dark`다.
  - 🔁 **축이 "색"에서 "애셋"으로 넓어졌다(#440 develop 머지, 2026-09-04)** — `YGLoadingTone`이
    `YGLoadingArt`로 개명되고 **그림 자체가 다른 세 번째 값 `Topping`**이 붙었다(C-001 캔버스 전용
    로띠 `res/raw/loading_topping`). 색만 가르던 enum에 크기가 다른 애셋이 들어오면서 각 값이
    `intrinsicSize`를 함께 들고, `YGLoadingLottie`가 그 크기를 **기본 `Modifier.size`로 깔아 준다** —
    호출부가 크기를 안 정해도 원본 배율로 그려지고, 종전처럼 `modifier`로 덮어쓸 수도 있다.
    호출부 세 곳(`YGLoadingOverlay`·G-001 새로고침 인디케이터·C-001 로딩 덮개)이 모두 그 규칙을 따른다.
- **로딩·덮개 (#440 develop 머지, 2026-09-04)**: `YGLoadingOverlay`가 쥐고 있던 **Dim 판·터치 삼킴·
  선택적 병합 시맨틱**이 `YGDimOverlay`로 빠지고, 오버레이는 그 판에 로띠 하나를 얹는 얇은 조립으로
  남았다. 뽑아낸 이유는 소비처가 둘이 됐기 때문이다 — C-001이 같은 판 위에 **로띠 + 안내 문구 2줄**
  (로딩)과 **경고 아이콘 + 문구 + 다시 시도 버튼**(실패)을 그린다. 터치 차단을 `clickable`이 아니라
  `pointerInput`으로 두는 근거(TalkBack이 버튼으로 읽는다)는 그대로 옮겨 왔고, `contentDescription`은
  **누를 것이 없을 때만** 넘기는 파라미터로 노출됐다(넘기면 자식을 병합해 버려 버튼이 개별 액션을 잃는다).
  `YGSkeleton`은 같은 라운드의 다른 축이다 — 덮개가 아니라 **한 이미지의 자리**를 시머 회색면으로
  채우고, 크기를 스스로 정하지 않으므로 자리를 아는 곳에서만 쓴다(현재 소비처는 `YGCanvas` 배경 하나).
  흐르는 값은 `drawBehind` 안에서 읽어 프레임마다 재구성이 돌지 않게 한다.
- **이미지 로더 (#440 develop 머지, 2026-09-04)**: `image/ParfaitImageLoader.kt`가 전역 Coil 로더를
  만드는 **유일한 자리**다(`newParfaitImageLoader` — 크로스페이드 on). `app`·`app-preview` 두
  `BaseApplication`이 `SingletonImageLoader.Factory`를 구현해 같은 함수를 물어 주므로, 두 앱의 이미지
  동작이 갈리지 않는다. 같은 파일의 `rememberReloadableImageRequest(url, reloadKey)`는 **다시 시도의
  전제**다 — `reloadKey`가 0보다 크면 메모리·디스크 캐시를 끄고 다시 받아 온다(같은 url로 다시 그리기만
  하면 실패한 요청과 디스크에 앉은 깨진 바이트가 그대로 재사용돼 몇 번을 눌러도 같은 실패다).
- **배경 블러 의존 `dev.chrisbanes.haze`**(#188): `gradle/libs.versions.toml` + `build-logic` `ComposeConfig`에 배선돼 Compose 모듈 전역에 깔린다(`coil-compose`와 같은 자리). 소스가 `Modifier.hazeSource(state)`, 소비 표면이 `Modifier.hazeEffect(state)`이고 **`HazeState`는 호출 화면이 소유**한다. `RenderEffect` 기반이라 **API 31 미만에서는 블러 없이 틴트만** 남는다(`minSdk` 26). 자체 `GraphicsLayer` 구현이 기각된 경위와 실측은 [ADR-0018](../adr/0018-backdrop-blur-haze.md) — 프로젝트에 블러 관용구가 둘(배경=Haze / 자기 자식=C-101 자체 구현) 존재한다 → [open-questions](../synthesis/open-questions.md).
- **`YGDangerZone`**: 상/하 2슬롯 + 사이 구분선 컨테이너, `IntrinsicSize.Max`. **점선 재설계(#159 develop 머지)** — 반투명 채움 → `dashedBorder()`(gray-100 점선 테두리) + 세로 패딩, 구분선은 solid `YGHorizontalDivider` → `YGHorizontalDashedDivider`(gray-100 점선). modifier 체이닝 `dashedBorder().padding()` 순서 규칙(테두리 최외곽 → 안쪽 패딩). 슬롯에 대개 `YGActionItem` 주입(로그아웃/탈퇴 묶음). 상세 [ygdangerzone-dashed](../superpowers/specs/archive/2026-07-19-ygdangerzone-dashed.md).
- **pressed 상태 관용구**: 상호작용형 컴포넌트(YGButton·YGIconButton·YGActionItem·YGChipButton)는 `MutableInteractionSource` + `collectIsPressedAsState()`로 pressed를 파생해 색/tint를 분기한다. (예외: 선택형(`YGEditButton`·`YGEditTabButton`·`YGStrokeButton`)은 pressed와 함께 `selectable`의 selected를 쓰고, `YGGrouptagChip`·`YGToppingGroup`은 상호작용 자체가 없다. `YGDateButton`은 상태(selected/today/enabled) prop `when` 분기만 하고 pressed를 안 쓴다 — ~~표준 `clickable(indication=null)`을 써 스로틀 규약을 벗어나 있었으나~~ **#284로 `clickableYGNoRipple`로 이관돼 해소됐다**.)
  이 관용구가 `clickableYGNoRipple`에 `interactionSource` 파라미터가 필요한 이유다 — 컴포넌트가 pressed를 직접 그리므로 hoist한 `MutableInteractionSource`를 클릭 유틸에 그대로 넘겨야 한다.
- **clickable 유틸(`clickableYG`·`ygDimRipple`·`ygScaleRipple`)은 `core:designsystem`이 아니라 [`core:util:android`](module-structure.md)의 `clickable/` 패키지에 있다**(2026-07-14 이동, develop 머지 #143). `@Composable Modifier.clickableYG`(중복 클릭 leading-throttle) + 변형 3종(Dim/Scale/Merge) + 리플 없는 `clickableYGNoRipple`이 표준 `Modifier.clickable` 위에 throttle을 얹어 focus/키/hover/시맨틱을 확보. 테마 비의존이라 ripple 색은 리터럴(`YGDimRippleColor`). 상세 → [clickableyg-throttle](../superpowers/specs/archive/2026-07-12-clickableyg-throttle.md) · [ygripple](../superpowers/specs/archive/2026-07-13-ygripple.md) · [clickableyg-ripple-variants](../superpowers/specs/archive/2026-07-13-clickableyg-ripple-variants.md).
  - **무리플이 기본이다 (#284, 2026-08-17)**: 프로덕션의 Foundation `Modifier.clickable` 호출 **28곳을 전량 `clickableYGNoRipple`로 이관**했고, 규약 적용 범위가 디자인시스템 컴포넌트에서 **feature 화면 클릭까지** 넓어졌다. 남은 `clickable`은 `androidTest` 픽스처 2건뿐이다.
    - 방향을 이렇게 잡은 이유: 컴포넌트 대부분이 `collectIsPressedAsState()`로 눌림을 직접 그려 Material 리플이 필요 없는데, 호출 지점마다 `indication = null`을 손으로 적거나 안 적어 기본 리플이 도는 상태가 섞여 있었다 — 어느 쪽이 의도인지 코드로 구분되지 않았다. **무리플을 기본에 두고 리플이 필요한 지점을 찾아 `clickableYG`로 올리는 편이 반대보다 빠르다.**
    - `clickableYGNoRipple`에 `interactionSource: MutableInteractionSource? = null`이 첫 파라미터로 추가됐다(다른 네 변형과 같은 자리). 없으면 hoisted `interactionSource`를 넘기던 컴포넌트 9종의 눌림 표현이 끊긴다.
    - **300ms 스로틀이 함께 딸려온다.** 게이트는 `remember`라 **Modifier 노드마다 하나**여서 다른 요소로 옮겨 누르는 것은 막지 않고 같은 요소 연타만 막는다. 이관 지점은 전부 단일 선택이거나 멱등이고, 셔터·재시도·`goTo` 유발 클릭은 오히려 중복 실행이 막힌다.
    - **리플이 유일한 피드백이었다가 사라진 곳은 `clickableYG`(Dim) 승격 후보다** — `NotionWebView` 재시도, `TermAgreeScreen` 재시도·약관 링크 caret, `InviteCodePasteBar`, `GalleryImageGridComponent` 셀 → [open-questions](../synthesis/open-questions.md) [2026-08-17]. (여섯째였던 `CanvasImageSelectScreen` 이미지는 #514에서 화면이 삭제돼 후보에서 빠졌다.)
- **튜토리얼 4종**(#449 develop 머지, 2026-09-05, `ygtutorial/`): 화면 첫 진입에서 한 번 도는 안내를
  `YGTutorialOverlay`(화면을 덮는 한 장) + `YGTutorialBox`(설명 카드) + `YGTutorialProgress`(몇 번째 장인가)
  + `YGTutorialBoxPlacement`(카드가 위·아래 중 어디에 붙는가)로 나눠 담는다. **강조할 자리만 뚫은
  오버레이가 아니다** — `imageResource`가 딤까지 구워진 **알파 없는 풀스크린 목업**이고 그 한 장이 실제
  화면을 통째로 덮는다. 그래서 목업은 시스템바를 뺀 자리에만 그리고(`windowInsetsPadding(systemBars)`)
  딤 자체는 인셋을 받지 않아 화면 끝까지 이어진다. 딤은 `clickableYGNoRipple`로 **클릭을 삼킨다** —
  안내가 떠 있는 동안 아래 화면이 눌리면 사용자가 무엇을 건드렸는지 모른 채 다른 화면에 가 있게 된다.
  - **버튼 라벨을 호출부가 정하지 않는다** — 마지막 장의 버튼은 "다음"이 아니라 "시작하기"인데, 그
    판단이 `YGTutorialProgress.isLast` 하나로 끝나므로 진행 표시(`2/3`)와 라벨이 **같은 사실 하나**에서
    나온다. 화면마다 다시 쓰면 `3/3`인데 "다음"인 조합이 언제든 만들어진다. `progress`를 넘기지 않으면
    한 장짜리라는 뜻이고, 라벨은 "시작하기"가 되며 **진행 표시가 비운 윗줄로 제목이 올라온다**(칩만
    남겨 두면 카드가 위아래로 벌어진다). 문자열 3건은 이 컴포넌트가 모듈 `res/`에 갖는다.
  - **겹치는 자리는 스캐폴드 밖이다** — 소비 화면 셋이 모두 `Box` 안에서 `YGScaffoldV2`와 **형제**로
    놓는다. 안에 넣으면 컨텐츠 인셋을 받아 딤이 상태바 밑에서 끊기고 시스템바만 안 덮인 화면이 된다.
    카드만 인셋을 받아 글자가 시스템바를 피한다.
  - **첫 소비처가 셋이다**(같은 PR) — C-001 캔버스(3장, `CanvasTutorialStep` enum이 순서·목업·문구·
    카드 위치를 든다), 갤러리 업로드(1장), 누끼 확인(1장). **여러 장짜리를 감싸는 자리는 디자인시스템이
    아니라 feature 로컬**이다(`canvas/impl`의 `CanvasTutorialOverlay`) — 한 장짜리 둘은 화면이
    `YGTutorialOverlay`를 직접 부른다. `boxPlacement`를 고르는 기준은 취향이 아니라 **그 장이 강조하는
    UI가 어디 있는가**이고, 카드는 그 반대편에 붙는다(달력을 여는 장만 `Bottom`이다).
  - ⚠️ **목업 이미지가 실제 화면과 어긋날 길이 열려 있다** — 안내가 가리키는 버튼이 옮겨 가거나 문구가
    바뀌어도 PNG는 따라오지 않고, 통짜 스크린샷이라 기기 폭·글자 크기·테마에 맞춰 다시 그려지지도
    않는다(`ContentScale.Crop`으로 잘린다). 정책 근거도 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-363.

> **과도기 — 컨벤션 분기(정리 대상)**
> - **`YGStrokeButton.borderWidth`**(#259): `Dp.Hairline`이면 테두리를 **안 그린다**(`Modifier.border`는 `Dp.Hairline`을 1px 선으로 그리므로 `then`으로 직접 걸러낸다). 배경·높이·눌림 색만 재사용하고 테두리는 감싸는 쪽이 한 번만 긋는 사용법이고, C-201 캘린더 드롭다운이 그 첫 소비처다 — 버튼마다 두르고 겹쳐 지우는 방식은 겹침이 빠지는 순간 두께가 두 배가 된다.
> - **그리기 프리미티브 소유**: 점선 테두리(`border/dashedBorder()`)·컷 도형(`shape/canvasCutCornerShape()`)은 `core:designsystem`에 두는데, G-001 툴팁 꼬리(`Modifier.drawTooltipCornerTop`, #176 develop 머지)는 [`core:util:android`](module-structure.md)의 `extension/`에 들어갔다. 같은 층위(테마 비의존 그리기 확장)인데 소유 모듈이 갈린다 → [open-questions](../synthesis/open-questions.md) [2026-08-01]. **#199로 자리가 하나 더 늘었다** — 배경 점 격자 `ygBackgroundDotGrid()`는 `border/`·`shape/`가 아니라 `component/ygbackgrounddotgrid/`에 들어갔다(컴포저블이 아닌 `Modifier` 확장인데 컴포넌트 폴더 규약을 따랐고, 기본값으로 `YGAtomicColors`·`SizeTokens`를 읽어 테마 비의존도 아니다). **#259로 둘 더**(2026-08-16) — 스크롤바 `Modifier.verticalScrollbar`는 `core:util:android` `extension/`, 달력 컨테이너의 3변 테두리 `Modifier.sideBorder`는 **feature 파일 안 private**다. 같은 층위가 이제 네 곳에 흩어져 있다. **#264는 그 위에 재사용 실패를 하나 더 얹었다**(2026-08-16) — C-301 토핑 선택 스트로크가 `border/dashedBorder()`를 두고 화면에서 `drawBehind` + `dashPathEffect`를 직접 그린다(회전을 `graphicsLayer`로 얹어야 해서 형태가 달랐다). 반면 같은 라운드의 배치·제스처 확장 2종(`Modifier.centeredAt`·`Modifier.dragBy`)은 규약대로 `core:util:android` `extension/`으로 올라갔다 — **그리기만 계속 예외로 남는다.**
> - **패키지 네이밍**: 컴포넌트별 폴더(`ygbutton/`·`ygiconbutton/`·`ygactionitem/`·`ygcolorchip/`·`ygtopbar/`·`ygdatebutton/`·`ygdangerzone/`·`ygtext/`)와 그룹 폴더(`textfield/`·`etc/`·`card/`·`modal/`)가 혼재. 규약(위 "컴포넌트 작성 규약")은 컴포넌트별 폴더 기준. (`ygcolorchip/`의 패키지 선언 불일치는 #165로 해소됨 — 폴더 혼재만 잔존) → [open-questions](../synthesis/open-questions.md).
> - **프리뷰 방식**: `@YGPreview`+`PreviewBox` 표준(#158 develop 머지, 2026-07-19, [designsystem-preview-migration 스펙](../superpowers/specs/archive/2026-07-18-designsystem-preview-migration.md)). ⚠️ **부분 회귀(#149·#165)** — 신규 `YGAlert`·`YGToast`(#149)와 `YGUserChip`(#165)이 표준을 안 따르고 `@Preview`+`YGCustomTheme` 사용, `YGDate`는 `@YGPreview`이나 `PreviewBox` 대신 `YGCustomTheme` 직접 래핑. 즉 "전 컴포넌트 통일"은 더 이상 참이 아님 → [open-questions](../synthesis/open-questions.md) [2026-07-23]. 반면 #183·#185·#186 신설 12종과 #188 신설 2종(`YGListDate`·`YGFloatingBar`)은 전부 `@YGPreview`+`PreviewBox`(프리뷰 함수 `private`) 표준을 지킨다 — 회귀는 `YGAlert`·`YGToast`·`YGUserChip`·`YGDate`에 국한된다.

## 관련 ADR
- [ADR-0010](../adr/0010-custom-compositionlocal-theme.md) — 자체 CompositionLocal 테마(why).
- [ADR-0007](../adr/0007-compose-material3-design-tokens.md) — 100% Compose·중앙화 원칙(superseded).
