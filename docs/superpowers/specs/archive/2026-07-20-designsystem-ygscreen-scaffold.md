---
id: designsystem-ygscreen-scaffold
title: 화면 컨테이너 컴포넌트 (YGScreen · YGScaffold · YGScreenScope)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-22
related_code:
  - YGScreen.kt#YGScreen
  - YGScreenScope.kt#YGScreenScope
  - YGScaffold.kt#YGScaffold
related_adr: ADR-0010
related_spec: clearfocusontap-modifier
related_architecture: design-system, navigation-flow
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, screen]
---

# Spec: 화면 컨테이너 컴포넌트 (YGScreen · YGScaffold · YGScreenScope)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표
개별 화면(feature)이 매번 `Surface`/`Scaffold` + 뒤로가기 배선을 직접 작성하지 않도록,
디자인시스템 레벨의 화면 루트 컨테이너 2종을 제공한다. 컨테이너 색·모양 기본값을
디자인 토큰으로 고정하고, 뒤로가기 처리를 `YGScreenScope` 리시버로 노출한다.

## 범위
- 포함:
  - `YGScreen` — `Surface` 기반 단순 화면 루트. `YGScreenScope` 리시버 content.
  - `YGScaffold` — Material3 `Scaffold` 얇은 래퍼. content가 `PaddingValues`를 받음(TopBar/BottomBar/inset 필요 화면용).
  - `YGScreenScope` — content 리시버. `OnBack(enabled, handler)` @Composable 제공.
- 제외:
  - TopBar/BottomBar 슬롯 프리셋(호출부가 `YGScaffold` content 안에서 `YGTopBar` 등 직접 배치).
  - `YGScreen`과 `YGScaffold`의 통합(현재 별개 컨테이너 — 아래 [주의] 참고).
  - 스크롤·insets 자동 처리.

## API / 인터페이스
```kotlin
@Composable
fun YGScreen(
    modifier: Modifier = Modifier,
    content: @Composable YGScreenScope.() -> Unit,
)   // shape/color 파라미터 미노출. 내부에서 Surface color = YGAtomicColors.Gray.Transparent 고정(배경 안 칠함), shape는 Surface 기본.

@Composable
fun YGScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = YGAtomicColors.Gray.White,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
)

@Stable
class YGScreenScope {
    @Composable
    fun OnBack(                 // @Composable + node-emit(BackHandler 래핑) → PascalCase
        enabled: Boolean = true,
        handler: () -> Unit,
    )   // 내부에서 BackHandler(enabled, onBack = handler) emit
}
```
- `YGScreen` — `shape`/`color` 파라미터 미노출(초기엔 있었으나 제거). **`color`는 내부에서 `YGAtomicColors.Gray.Transparent`(=`Color.Transparent`) 고정** — `Surface`는 `color`를 area에 항상 칠하므로(기본 `MaterialTheme.colorScheme.surface`, 불투명) 배경을 강제 칠하지 않으려면 Transparent가 필요. 배경은 상위 컨테이너(nav의 `YGScaffold` containerColor)가 담당. `shape`는 `Surface` 기본. 커스터마이즈 필요 시 파라미터 재도입 검토.
- `YGScaffold.containerColor` — `Scaffold`에 위임. 기본 흰 배경.
- `YGScaffold.contentWindowInsets` — `Scaffold`에 위임. 기본 `ScaffoldDefaults.contentWindowInsets`. inset 무시가 필요한 화면은 `WindowInsets(0.dp)` 주입(예: `groups/enter`).
- `OnBack.enabled` — false면 뒤로가기 가로채지 않음(시스템 back 통과). 기본 true.
- `OnBack.handler` — back 이벤트 콜백.

## 동작 / 상태
- **뒤로가기(YGScreenScope.OnBack)**: content 안에서 `OnBack { ... }`을 **호출한 화면만** 뒤로가기를 가로챈다. 호출하지 않으면 `BackHandler` 자체가 emit되지 않아 시스템 기본 동작. → 처리 안 하는 화면에 강제 리턴/no-op 불필요.
- **배경 탭 → 포커스 해제**: ~~2026-07-23 `YGScreen`의 `Surface`에 `clickableYGNoRipple { focusManager.clearFocus() }` 결선~~ →
  🔁 **2026-08-03 철회. `YGScreen`은 포커스 관심사를 갖지 않는다.** 대신 opt-in Modifier
  [`Modifier.clearFocusOnTap()`](2026-08-03-clearfocusontap-modifier.md)(`core:util:android` `focus/`)를 입력이 있는 화면이 직접 붙인다.
  > 📌 **2026-08-04 정정** — 이 결선은 **develop에 도달한 적이 없다.** 도입도 철회도 브랜치
  > `feature/#86-app-setting-account-info-screen` 안에서 일어나 PR #192로 함께 머지됐고, 그 PR의 변경 파일에
  > `YGScreen.kt`는 없다. develop의 `YGScreen`은 처음부터 지금까지 순수 `Surface` 래퍼다.
  > 아래 철회 사유 ①②는 그대로 유효하다(브랜치에서 실제로 겪은 문제이고, 재도입을 막는 근거이므로 보존).
  - **철회 사유 ①(접근성)**: `clickableYGNoRipple`은 내부적으로 `Modifier.clickable`이라, `role = null`이어도
    semantics에 `onClick` action과 focus target을 추가한다. → `YGScreen`을 쓰는 **모든 화면의 배경 전체**가
    TalkBack에는 단일 인터랙티브 요소로, 키보드/D-pad 탐색에는 포커스 스톱으로 노출된다.
    `onClickLabel`도 `null`이라 라벨 없이 "두 번 탭하여 활성화"만 읽힌다.
  - **철회 사유 ②(적용 범위 역전)**: 당시 `YGScreen` 사용처는 `AppSettingScreen`(입력 없음)·`AccountInfoScreen`뿐이었고,
    정작 텍스트 입력이 있는 `GroupNickNameScreen`·`GroupCreateScreen`·`InviteCodeInputFieldElement`는
    `YGScreen`을 쓰지 않아 혜택을 못 받았다. 공용 컨테이너에 두면 **필요 없는 화면이 접근성 비용만 지고
    필요한 화면은 못 받는** 배치가 된다.
  - 이 철회로 `clickableYGNoRipple`은 사용처 0이 됐다(정의만 잔존). 철회 후에도 API는 되돌리지 않아
    **PR #192로 develop에 신설**됐다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-03].
- **scope 인스턴스**: `YGScreen` 내부에서 `remember { YGScreenScope() }`로 1회 생성(recomposition마다 재할당 안 함). stateless지만 `@Stable`.
- **컨테이너 색·모양**: 런타임 상태 없음. 전부 prop 기본값(토큰).

| 심볼 | 토큰/기본값 | 위임 대상 |
|------|-------------|-----------|
| `YGScreen` color | `YGAtomicColors.Gray.Transparent`(내부 고정, 미노출) | `Surface.color` |
| `YGScreen` shape | `Surface` 기본값(미노출) | `Surface.shape` |
| `YGScaffold.containerColor` | `YGAtomicColors.Gray.White` | `Scaffold.containerColor` |
| `YGScaffold.contentWindowInsets` | `ScaffoldDefaults.contentWindowInsets` | `Scaffold.contentWindowInsets` |

## 표시·제어 규칙
- `YGScreen` vs `YGScaffold` 선택 기준:
  - TopBar/BottomBar/시스템 inset 패딩 필요 → `YGScaffold`(content가 `PaddingValues` 수신).
  - 단순 전체 화면 배경 + 뒤로가기만 → `YGScreen`.
- `OnBack`은 content 리시버(`YGScreenScope`) 안에서만 호출 가능(스코프 밖 노출 안 함).

## 파일 구성
- `screen/YGScreen.kt` — `Surface` 래퍼 + `YGScreenScope` 생성/`content` 실행.
- `screen/YGScaffold.kt` — Material3 `Scaffold` 래퍼.
- `screen/YGScreenScope.kt` — content 리시버 + `OnBack` @Composable.

## 설계 결정 (리팩터 근거)
- **초안**: `onBack`(camelCase, 값 반환)이 `OnBackResult`(enabled/handler 보유)를 반환하고 `content` 시그니처가
  `-> OnBackResult`, `YGScreen`이 반환값으로 `BackHandler`를 배선. → 모든 화면 content가
  `OnBackResult` 반환을 **강제**당함(뒤로가기 안 쓰는 화면도 `onBack(enabled = false){}` 필요).
- **변경(채택)**: `OnBack`을 @Composable로 바꿔 내부에서 `BackHandler`를 직접 emit.
  `content` 반환형 `Unit`. `OnBackResult` 삭제.
  - 리턴 강제 제거 → 뒤로가기 불필요 화면은 아무것도 안 함.
  - "content 마지막 줄이 곧 반환값" 어색함 제거.
  - 여러 back 조건도 `OnBack`을 여러 번 호출해 자연스럽게 표현 가능.

## 적용 이력
- **feature :impl EntryBuilder Scaffold → YGScaffold 마이그레이션** (2026-07-20): `feature/*/impl`의
  `navigation/EntryBuilder.kt` 11개 파일, Material3 `Scaffold` 사용처 19곳을 `YGScaffold`로 교체.
  - 순수 `Scaffold { innerPadding -> }` → `YGScaffold { ... }`(기본값 동일).
  - `app/setting`의 `containerColor = YGAtomicColors.Gray.White` 명시 → YGScaffold 기본과 중복이라 제거(미사용 `YGAtomicColors` import 정리).
  - `groups/enter`의 `contentWindowInsets = WindowInsets(0.dp)` → YGScaffold에 동명 파라미터 신설 후 그대로 위임(이 마이그레이션이 파라미터 추가 근거).
  - `:core:designsystem`은 `ModuleFeatureImplConventionPlugin`이 모든 feature :impl에 전이 제공 → 별도 의존 추가 불필요.
  - 검증: 대상 12개 모듈 `compileDebugKotlin` + `ktlintMainSourceSetCheck` 통과.
- **YGScreenScope.onBack → OnBack 개명** (2026-07-20): @Composable + node-emit이라 Compose 관례상 PascalCase(`BackHandler`와 동일). API 시그니처·동작 동일.
- **YGScreen 첫 실사용** (2026-07-20): `AppSettingScreen`이 `YGScreen(modifier = modifier) { ... OnBack { onClickBack() } }`로 적용, A안 사용성 검증. 화면 `modifier`는 최외곽 `YGScreen`에 전달(관례 준수), 내부 최상위 `Column`은 `fillMaxSize()`.
- **YGScreen `shape`·`color` 파라미터 제거 → color Transparent 고정** (2026-07-21): 초기 시그니처의 `shape: Shape = RectangleShape`·`color: Color = YGAtomicColors.Gray.White` 두 파라미터 삭제 → `modifier` + `content`만. 단 `Surface`는 `color`를 항상 area에 칠하므로(파라미터 없이 두면 Material surface 색으로 강제 페인트) 내부 `color = YGAtomicColors.Gray.Transparent` 고정으로 배경 미페인트 처리. 실제 배경은 nav의 `YGScaffold` containerColor가 담당(레이어 분리).
- **EntryBuilder YGScaffold import 그룹 정렬** (2026-07-21): 마이그레이션이 import-ordering disabled 전제로 in-place 교체해 `androidx` 블록에 남았던 `YGScaffold` import를 9파일에서 `com.teamyg` 블록으로 이동(코스메틱, 런타임 무관).

## 주의 / 열린 질문
- **YGScreen ↔ YGScaffold 관계 미통합**: 둘 다 흰 배경 컨테이너지만 서로 조합/합성하지 않는다.
  `YGScreen`은 `YGScreenScope`(OnBack)를 주지만 `YGScaffold`는 안 준다 →
  `YGScaffold` 화면에서 뒤로가기를 스코프로 처리하려면 별도 배선 필요. 통합/역할 정리는 후속 결정.
- 실사용 화면 축적 후 API 사용성 재검토(스크롤·inset·TopBar 슬롯 프리셋 필요 여부).
