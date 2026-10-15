---
id: ygchipbutton
title: YGChipButton
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-16
related_code: core:designsystem component/ygchipbutton/ YGChipButton, YGChipButtonColors, YGChipButtonColorsDefaults
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGChipButton

- 대상: `core:designsystem` — `component/ygchipbutton/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · PR #141(`feature/ygchipbutton`)

## 목표
텍스트 + 선택 아이콘(앞/뒤)을 담는 pill(완전 둥근) 형태의 칩 버튼. 색 묶음(`YGChipButtonColors`)을 주입받아 프리셋별로 배경/전경/테두리를 바꾼다. pressed에서 색이 바뀐다.

## 범위
- **포함**: `text` 표시, 선택 `startIconResource`/`endIconResource`(tint), `onClick`, pressed 파생 상태, `YGChipButtonColors` 주입, `YGChipButtonColorsDefaults` 프리셋 2종, `Role.Button` 시맨틱.
- **제외**: selected/toggle 상태(→ [YGToggleButton](2026-07-16-ygtogglebutton.md)), disabled 상태, 크기 variant, 아이콘-only 변형.

## API / 인터페이스
```kotlin
@Composable
fun YGChipButton(
    text: String,
    colors: YGChipButtonColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes startIconResource: Int? = null,
    @DrawableRes endIconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```
- `text`: 칩 라벨.
- `colors`: 상태별 색 묶음(아래 `YGChipButtonColors`). 호출측이 프리셋 선택·주입.
- `startIconResource`/`endIconResource`: 선택 아이콘. `null`이면 미표시 + 해당 방향 패딩이 넓어짐(아래 표시 규칙).
- `interactionSource`: pressed 파생용. 기본 `remember`.

## 동작 / 상태
- pressed는 `interactionSource.collectIsPressedAsState()`로 런타임 파생. `YGChipButtonColors`가 `foregroundColor/backgroundColor/borderColor(isPressed)`로 분기.
- `YGChipButtonColors`(@Immutable data class): `default*`/`pressed*` × `foreground/background/border` 6필드.

### 프리셋 (`YGChipButtonColorsDefaults`)
| 프리셋 | 전경(default→pressed) | 배경(default→pressed) | 테두리(default→pressed) |
|---|---|---|---|
| `CherryBorderPressed` | `Gray.Gray600`→`Gray.Gray700` | `Cherry.Cherry50`(고정) | `Gray.Transparent`→`Cherry.Cherry100` |
| `CherryBackgroundPressed` | `Gray.Gray950`(고정) | `Cherry.Cherry100`→`Cherry.Cherry200` | `Gray.Transparent`(고정) |

- **타이포**: `typography.body.b02R`.
- **모양**: `shapes.radius.round`(background·clip·border 공통), 테두리 두께 `1.dp`.
- **아이콘 크기**: `SizeTokens.Size16`, tint = `foregroundColor(isPressed)`.
- **간격**: 요소 간 `layout.gap.gap2`.
- **상호작용**: `clickable(indication = null)` + `semantics { role = Role.Button }`.

## 표시·제어 규칙
- **비대칭 패딩**: 상하 `padding3` 고정. 좌/우는 아이콘 유무로 분기 — 아이콘 있으면 `padding3`, 없으면 `padding5`(라벨만 있는 쪽을 더 벌림).

## 파일 구성 (`component/ygchipbutton/`)
- `YGChipButton.kt` — public `YGChipButton` 컴포저블 + `@Preview`(`YGCustomTheme` + `@PreviewParameter`).
- `YGChipButtonColors.kt` — `@Immutable data class YGChipButtonColors` + `foreground/background/borderColor(isPressed)` 헬퍼.
- `YGChipButtonColorsDefaults.kt` — `object` 프리셋 2종(`CherryBorderPressed`·`CherryBackgroundPressed`).
- `YGChipButtonPreviewData.kt` — `data class YGChipButtonPreviewData(startIconResource, endIconResource, colors)` + `YGChipButtonPreviewParameterProvider`(3종: 아이콘 없음·start 아이콘·end 아이콘).

## 주의 / 열린 질문
- **과도기 색**: 프리셋이 `YGAtomicColors.{Gray,Cherry,Transparency}`를 직접 참조(시맨틱 아님) — YGButton·YGInputNumber 선례. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../synthesis/open-questions.md).
- **YGButton 패턴 준수**: Colors data class + Defaults object 분리로 규약을 잘 따름(cf. YGToggleButton은 미분리).
- **프리뷰 방식**: `@Preview` + `YGCustomTheme` + `PreviewParameterProvider`(YGIconButton·YGActionItem 계열). etc 계열과 혼재 — 표준화 미확정([open-questions 2026-07-12 컨벤션 분기](../../../synthesis/open-questions.md)).
- **사후 문서**: 코드가 PR #141로 develop에 이미 머지(2026-07-16). 본 스펙은 현행 코드 기록(설계 선행 아님).
