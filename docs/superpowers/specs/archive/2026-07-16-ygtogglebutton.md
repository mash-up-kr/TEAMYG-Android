---
id: ygtogglebutton
title: YGToggleButton
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-16
related_code: core:designsystem component/ygtogglebutton/ YGToggleButton
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGToggleButton

- 대상: `core:designsystem` — `component/ygtogglebutton/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · PR #142(`feature/ygtogglebutton`)

## 목표
텍스트 + 선택 아이콘을 담는 pill 형태의 **선택형(토글) 버튼**. `isSelected`(prop) 하나로 배경/전경/타이포가 반전된다. 세그먼트/탭류 선택 UI의 단위.

## 범위
- **포함**: `text` 표시, 선택 `iconResource`(tint), `isSelected` 반전, `onClick`, `selectable`(`Role.Button`) 시맨틱.
- **제외**: pressed 피드백(없음), disabled 상태, 색 주입(프리셋/Colors 없음 — 색 하드 결선), 크기 variant, 다중 선택 컨테이너(호출측 조립).

## API / 인터페이스
```kotlin
@Composable
fun YGToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```
- `text`: 라벨.
- `isSelected`: 선택 상태 → 색·타이포 반전.
- `onClick`: 탭 콜백(선택 토글은 호출측 상태 관리).
- `iconResource`: 선택 아이콘. `null`이면 미표시 + start 패딩이 넓어짐.
- `interactionSource`: `selectable`에 연결(indication 없음).

## 동작 / 상태
- 런타임 파생 상태 없음. `isSelected`(prop)로 색·타이포 분기(pressed 미사용).

| 요소 | unselected | selected |
|---|---|---|
| 배경 | `Gray.Transparent` | `Gray.White` |
| 전경(텍스트·아이콘 tint) | `Transparency.Black50` | `Gray.Gray900` |
| 타이포 | `typography.body.b01R` | `typography.body.b01SB` |

- **모양**: `shapes.radius.round`(background·clip 공통). 테두리 없음.
- **아이콘 크기**: `24.dp` 리터럴 하드코딩(`SizeTokens` 미사용).
- **간격**: 요소 간 `layout.gap.gap2`.
- **상호작용**: `selectable(selected, role = Role.Button, indication = null, onClick)`.

## 표시·제어 규칙
- **비대칭 패딩**: 상하 `padding3`, 우 `padding5` 고정. 좌는 아이콘 있으면 `padding3`, 없으면 `padding5`.

## 파일 구성 (`component/ygtogglebutton/`)
- `YGToggleButton.kt` — public `YGToggleButton` 컴포저블 + `@Preview`(`YGCustomTheme` + `@PreviewParameter`, 배경 `Cherry.Cherry50`).
- `YGToggleButtonPreviewData.kt` — `data class YGToggleButtonPreviewData(isSelected, iconResource)` + `YGToggleButtonPreviewParameterProvider`(4종: selected×아이콘 유무).

## 주의 / 열린 질문
- **규약 이탈**: 다른 상호작용 컴포넌트(YGButton·YGChipButton 등)와 달리 **Colors data class를 분리하지 않고** `YGAtomicColors.{Gray,Transparency}`를 컴포저블 본문에서 인라인 조건 분기. 색 커스터마이즈 불가·과도기 색 직접 참조. → [open-questions 2026-07-16 YGToggleButton 규약 이탈](../../../synthesis/open-questions.md).
- **하드코딩 아이콘 크기**: `24.dp` 리터럴(`SizeTokens` 미사용) — YGInputNumber `50.dp` 선례와 유사. 토큰화 시 정리 대상.
- **`selectable` 관용구**: pressed 파생(`clickable`+`collectIsPressedAsState`) 대신 `selectable`(selected 시맨틱)을 씀 — 기존 컴포넌트와 다른 신규 상호작용 관용구.
- **프리뷰 방식**: `@Preview` + `YGCustomTheme` + `PreviewParameterProvider` 계열. etc 계열과 혼재 — 표준화 미확정([open-questions 2026-07-12 컨벤션 분기](../../../synthesis/open-questions.md)).
- **사후 문서**: 코드가 PR #142로 develop에 이미 머지(2026-07-16). 본 스펙은 현행 코드 기록(설계 선행 아님).
