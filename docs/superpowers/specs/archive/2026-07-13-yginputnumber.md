---
id: yginputnumber
title: YGInputNumber
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-13
related_code: core:designsystem component/yginputnumber/ YGInputNumber
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGInputNumber

- 대상: `core:designsystem` — `component/yginputnumber/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · 이슈 #125

## 목표
숫자 하나를 표시하는 50×50 선택형 정사각 셀. `isSelected`에 따라 색이 반전된다. 숫자 입력(핀/코드 등) 그리드의 셀 단위 컴포넌트.

## 범위
- **포함**: `number` 표시, `isSelected` 반전(배경/텍스트/테두리), `onClick`, `Role.Button` 시맨틱.
- **제외**: 그리드·시퀀스 컨테이너(호출측 조립), 입력 검증·상태 관리, 크기 variant(50 고정), disabled 상태, pressed 피드백.

## API / 인터페이스
```kotlin
@Composable
fun YGInputNumber(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- `number`: 표시 숫자(`number.toString()`).
- `isSelected`: 선택 상태 → 색 반전.
- `onClick`: 셀 탭 콜백.

## 동작 / 상태
- 런타임 파생 상태 없음. `isSelected`(prop) 하나로 색 분기.

| 요소 | unselected | selected |
|---|---|---|
| 배경 | `YGAtomicColors.Gray.White` | `YGAtomicColors.Gray.Gray900` |
| 테두리 | `Gray.Gray100` | `Gray.Gray900` |
| 텍스트 색 | `Gray.Gray900` | `Gray.White` |

- **텍스트**: `typography.body.b01R`, `number.toString()`, 중앙 정렬(`Box` `contentAlignment = Center`).
- **크기**: `50.dp` 고정(코드 주석 "디자인가이드상 50x50 고정" — `SizeTokens` 미사용).
- **모양**: `shapes.radius.xSmall`(background·clip·border 공통), 테두리 두께 `1.dp`.
- **상호작용**: `clickable(onClick = onClick)` + `semantics { role = Role.Button }`.

## 파일 구성 (`component/yginputnumber/`)
- `YGInputNumber.kt` — public `YGInputNumber` 컴포저블 + `@Preview`(`YGCustomTheme` + `@PreviewParameter`) 프리뷰(선택/비선택 2종).
- `YGInputNumberPreviewData.kt` — `data class YGInputNumberPreviewData(isSelected)` + `YGInputNumberPreviewParameterProvider`(false/true).

## 주의 / 열린 질문
- **하드코딩 치수**: `50.dp`·`1.dp`가 `SizeTokens`가 아닌 리터럴(디자인가이드 50 고정). `SizeTokens`에 50 프리셋 없음(Size48/Size64 존재). 토큰화 시 정리 대상.
- **과도기 색**: `YGAtomicColors`(White/Gray900/Gray100) 직접 참조 — YGButton·YGIconButton 선례. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
- **프리뷰 방식**: `@Preview` + `YGCustomTheme` + `PreviewParameterProvider`(YGIconButton·YGActionItem 계열). etc 계열의 `@YGPreview`/`PreviewBox`와 혼재 — 표준화 미확정([design-system](../../../architecture/design-system.md) 컨벤션 분기 노트).
- **사후 문서**: 코드가 타 작업자 PR(#129)로 develop에 이미 머지됨. 본 스펙은 현행 코드를 기록한 것(설계 선행 아님).
