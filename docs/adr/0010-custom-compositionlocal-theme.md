---
id: ADR-0010
title: 자체 CompositionLocal 디자인시스템 테마 (Material3 MaterialTheme·dynamic color 대체)
status: accepted
date: 2026-07-10
deciders: Parfait 팀
supersedes: ADR-0007
superseded_by:
related_adr:
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0010: 자체 CompositionLocal 디자인시스템 테마 (Material3 MaterialTheme·dynamic color 대체)

- 대체: [ADR-0007](0007-compose-material3-design-tokens.md)의 테마·토큰 메커니즘

## 맥락
[ADR-0007](0007-compose-material3-design-tokens.md)은 UI를 100% Jetpack Compose로 하고 디자인 값을 토큰으로 중앙화하기로 했으나, 테마 구현을 Material3 `MaterialTheme`(+ Android 12 dynamic color)와 플랫 토큰 object(`GapTokens`·`PaddingTokens`·`ShapeTokens`)로 상정했다.

실제 구현에서 두 지점이 어긋났다.
- **dynamic color 부적합** — 브랜드 색(Cherry/Melon/Pudding)이 기기 배경화면 기반 dynamic color와 충돌. YG 브랜드 아이덴티티를 기기가 흔들면 안 된다.
- **Material3 색·타이포 슬롯 부적합** — 디자인 스펙의 시맨틱 축(primary/secondary/tertiary + danger/warning/success/info + grayScale/transparency)과 타이포 스케일(title/body/caption)이 Material3의 `ColorScheme`/`Typography` 슬롯 구조와 1:1로 안 맞는다.

100% Compose·토큰 중앙화(0007)는 유지하되, 테마 전달 메커니즘을 교체하기로 했다.

## 결정
`MaterialTheme` 대신 **자체 `staticCompositionLocalOf` 기반 테마 시스템**을 쓴다. UI는 계속 100% Compose(XML 레이아웃 없음), Compose 활성은 `JetpackComposeConventionPlugin`.

- **진입점**: `YGTheme.kt`의 `YGCustomTheme(darkTheme, content)` 컴포저블이 4개 `CompositionLocalProvider`를 심는다 — `LocalYGColorScheme`·`LocalYGTypography`·`LocalYGShapes`·`LocalYGLayout`(전부 `internal`, 미초기화 시 `error`).
- **접근자**: `object YGTheme`가 `@ReadOnlyComposable` 프로퍼티 `colorScheme`/`typography`/`shapes`/`layout`으로 노출. 사용처는 `YGTheme.typography.body.b01SB`처럼 읽는다.
- **토큰은 `@Immutable data class` 홀더**로 구조화(플랫 object 폐기):
  - `YGColorScheme` — 시맨틱 색(`primary/secondary/tertiary/danger/warning/success/info` + `grayScale: YGColorGrayScale` + `transparency: YGColorTransparency`).
  - `YGTypography` — `title: YGTypographyTitle` / `body: YGTypographyBody` / `caption: YGTypographyCaption` (각 그룹이 웨이트·크기 변형 `TextStyle` 보유, 예 `b01B/b01SB/b01R/b02...`). 폰트는 `YGFontFamily`의 SUIT(regular/medium/semi_bold/bold).
  - `YGShapes` — `radius: YGShapeRadius`(`xSmall`~`round` 명명 스케일).
  - `YGLayout` — `gap: YGLayoutGap` + `padding: YGLayoutPadding`(`gap1..`, `padding1..` 명명 스케일).
- **색 2계층**: 원자 색 `YGAtomicColors`(팔레트: `Cherry/Melon/Pudding/Soda/Gray/Transparency`, **public** — #158 이후) → 시맨틱 `YGColorScheme`. 원자→시맨틱 매핑은 `YGSemanticColorDefaults`(`YGLightColorScheme`, `YGDarkColorScheme`)가 담당. 원래 "컴포넌트는 시맨틱을 읽는다"가 원칙이었으나 —
  > ⚠️ **원칙 실질 이탈 머지됨(#158, develop `ce4e9b8`, 2026-07-19)** — 디자인이 GUI에서 시맨틱 대신 원자 색을 직접 쓰는 현실을 반영해 `YGAtomicColors`를 `internal`→public으로 개방. "컴포넌트는 시맨틱을 읽는다" 강제 메커니즘 소멸, 원자 색이 실질 SoT. 이 ADR 본문 갱신 또는 방향 전환 신규 ADR **아직 미작성(잔존)** → [open-questions](../synthesis/open-questions.md).
- **기본값 제공 object**: `YGSemanticColorDefaults`·`YGTypographyDefaults`·`YGShapesDefaults`·`YGLayoutDefaults`가 각 홀더의 `YGDefault*` 인스턴스를 만들어 `YGCustomTheme`에 주입.
- **크기 토큰만 별도**: `SizeTokens`(object) + `SizeToken`(`@JvmInline value class`, `.getDp()`)는 테마 홀더 밖에 유지 — 컴포넌트가 `SizeTokens.Size24.getDp()`로 직접 참조.
- **다크 모드**: 스캐폴딩만(`YGDarkColorScheme = YGLightColorScheme`, 코드 `TODO`). 실제 다크 팔레트는 후속.

## 대안
- **Material3 `MaterialTheme` + dynamic color (ADR-0007 원안)** — 표준·친숙, 시스템 통합 무료. 그러나 브랜드 색을 기기가 흔들고, 시맨틱 축이 Material 슬롯과 안 맞아 매핑 오버헤드·의미 왜곡.
  **→ 기각:** 브랜드 아이덴티티 우선, 디자인 스펙 축을 그대로 표현.
- **플랫 토큰 object 유지(`GapTokens` 등 전역 상수)** — 단순. 그러나 테마 스코프·교체(다크 등) 불가, 프리뷰별 오버라이드 불가.
  **→ 기각:** CompositionLocal 홀더로 스코프·교체 가능성 확보.
- **`SizeTokens`도 테마 홀더로 흡수** — 일관성. 그러나 크기는 테마별로 안 바뀌는 물리 상수 성격.
  **→ 보류:** 값 클래스 object로 홀더 밖 유지.

## 영향

**긍정**
- 디자인 스펙의 시맨틱 축·타이포 스케일을 홀더 형태로 그대로 표현 → 매핑 왜곡 없음.
- 브랜드 색 고정(dynamic color 배제)으로 아이덴티티 일관.
- `CompositionLocalProvider` 스코프라 테마 교체(다크)·프리뷰 오버라이드 경로 확보.
- 원자→시맨틱 2계층으로 팔레트 변경이 시맨틱 매핑 한 곳에 격리.

**트레이드오프**
- Material3 표준 테마를 벗어나 자체 규약 학습 필요(신규 컴포넌트는 `YGTheme.*`를 읽어야 함).
- 미초기화 시 `error(...)` — `YGCustomTheme` 밖에서 `YGTheme.*` 접근하면 런타임 크래시(프리뷰 포함 래핑 필수).
- 다크 스킴 미구현(라이트와 동일) — 다크 대응 시 팔레트 작업 부채.

**위험·방어**
- 컴포넌트가 `YGAtomicColors`(원자)를 직접 읽으면 시맨틱 계층 우회 → 리뷰에서 차단, 시맨틱(`YGTheme.colorScheme`) 우선.
- 하드코딩 색·치수 유입 방지는 0007과 동일하게 리뷰 규칙으로.

## 관련
- 구현 가이드·컴포넌트 작성 규약: [architecture/design-system.md](../architecture/design-system.md)
- 전신: [ADR-0007](0007-compose-material3-design-tokens.md)(superseded)
