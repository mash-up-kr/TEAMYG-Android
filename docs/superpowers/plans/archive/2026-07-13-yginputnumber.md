---
id: yginputnumber
title: YGInputNumber Implementation Plan (사후 기록)
status: done
type: work-order
created: 2026-07-13
updated: 2026-07-13
platforms: android
owner:
related_adr: ADR-0010
related_spec: yginputnumber
related_code: YGInputNumber#YGInputNumber
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGInputNumber Implementation Plan (사후 기록)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `core:designsystem`에 숫자 표시 선택형 셀 `YGInputNumber`(50×50, isSelected 반전)를 구현한다.

**Architecture:** `Box`(50.dp 고정, `shapes.radius.xSmall`, 1dp border, `clickable`, `Role.Button` semantics) 중앙에 숫자 `Text`(`body.b01R`). `isSelected` prop 하나로 배경/텍스트/테두리 색을 반전(`YGAtomicColors` 직접). 크기 프리셋 없이 리터럴 50 고정.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `Box`/`background`/`border`/`clickable`, material3 `Text`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/archive/2026-07-13-yginputnumber.md](../../specs/archive/2026-07-13-yginputnumber.md)

> **사후 기록**: 이 컴포넌트는 타 작업자 PR(develop `#129`/이슈 `#125`)로 이미 머지됨. 본 계획은 현행 코드를 문서화한 것으로, Task 1은 완료 상태다.

## Global Constraints
- 대상 repo: `TJYG-Android`(develop 머지 완료).
- 패키지: `com.teamyg.parfait.core.designsystem.component.yginputnumber`.
- 테마 값은 `YGTheme.*`. 색은 `YGAtomicColors.*` 직접(과도기). 치수는 리터럴 `50.dp`/`1.dp`(디자인가이드 50 고정).
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile + `ktlintMainSourceSetCheck` + `@Preview` 육안**으로 대체.

---

### Task 1: YGInputNumber + PreviewData (완료)

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yginputnumber/YGInputNumber.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/yginputnumber/YGInputNumberPreviewData.kt`

**Interfaces:**
- Consumes: `YGTheme.{shapes.radius.xSmall, typography.body.b01R}`, `YGAtomicColors.Gray.{White, Gray900, Gray100}`, `YGCustomTheme`·`PreviewParameterProvider`.
- Produces: public `@Composable fun YGInputNumber(number: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)`, `data class YGInputNumberPreviewData(isSelected: Boolean)`.

- [x] **Step 1: YGInputNumberPreviewData.kt** — `data class YGInputNumberPreviewData(val isSelected: Boolean)` + `class YGInputNumberPreviewParameterProvider : PreviewParameterProvider<YGInputNumberPreviewData>` (values: false, true).

- [x] **Step 2: YGInputNumber.kt 본체** —
  - `Box(contentAlignment = Center, modifier = modifier.size(50.dp).background(color = if isSelected Gray900 else White, shape = radius.xSmall).clip(radius.xSmall).clickable(onClick).border(1.dp, if isSelected Gray900 else Gray100, radius.xSmall).semantics { role = Role.Button })`.
  - 내부 `Text(number.toString(), style = body.b01R, color = if isSelected White else Gray900)`.

- [x] **Step 3: Preview** — `@Preview` + `YGCustomTheme { Box(fillMaxWidth) { YGInputNumber(number = 3, isSelected = data.isSelected, onClick = {}) } }`, `@PreviewParameter(YGInputNumberPreviewParameterProvider::class)`.

- [x] **Step 4: 컴파일 + ktlint** — develop 머지 시점 통과(현행 유지).

- [x] **Step 5: 커밋/머지** — PR #129로 develop 반영 완료.

---

## Self-Review
- **Spec coverage**: 목표(50×50 선택 셀)·API(number/isSelected/onClick/modifier)·색 반전 표(White↔Gray900, 테두리 Gray100↔Gray900, 텍스트 Gray900↔White)·b01R·Role.Button·파일(YGInputNumber + PreviewData)·과도기(리터럴 치수·원자색·프리뷰 방식) — Task 1에 대응.
- **Placeholder**: 없음(현행 코드 기록).
- **Type consistency**: `YGTheme.shapes.radius.xSmall`·`typography.body.b01R`·`YGAtomicColors.Gray.{White,Gray900,Gray100}`·`Role.Button` 코드 심볼과 일치.

## 열린 질문
- 리터럴 `50.dp`/`1.dp`(SizeTokens 미사용), 원자색 직접, 프리뷰 방식 혼재 — [spec](../../specs/archive/2026-07-13-yginputnumber.md) 및 [open-questions](../../../../wiki/open-questions.md) 참고.
