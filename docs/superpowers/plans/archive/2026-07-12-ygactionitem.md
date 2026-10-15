---
id: ygactionitem
title: YGActionItem Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: ygactionitem
related_code: YGActionItem
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGActionItem Implementation Plan

> **상태: 구현 완료** — 머지된 구현을 사후 기록(reverse-doc). 체크박스는 완료 표시.

**Goal:** `core:designsystem`에 텍스트 액션 항목 `YGActionItem`(좌측 텍스트 + 탭 콜백 + pressed 색)을 구현한다. 보조/디스트럭티브 액션(예: "그룹 나가기") 텍스트 버튼.

**Architecture:** 단일 `Box`(clickable + `semantics { role = Button }` + padding) 안에 `Text`. pressed는 `interactionSource.collectIsPressedAsState()`로 파생, 색을 pressed(Gray700)/기본(Gray500)으로 분기. 배경은 호출부 `modifier` 지정.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `Box`/`clickable`/`interaction`, material3 `Text`, `semantics`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/archive/2026-07-12-ygactionitem.md](../../specs/archive/2026-07-12-ygactionitem.md)

## Global Constraints
- 대상 repo: `TJYG-Android`.
- 패키지: `com.teamyg.parfait.core.designsystem.component.ygactionitem`.
- 색은 `YGAtomicColors.Gray.{Gray500,Gray700}` 직접 참조(과도기 → [open-questions](../../../../wiki/open-questions.md)). 타이포 `YGTheme.typography.body.b02R`, 패딩 `layout.padding.{padding5,padding6}`.
- 검증: compile + ktlint + `@Preview` 육안.

---

### Task 1: YGActionItem 본체 + Preview
- [x] `Box(clickable(onClick, interactionSource), semantics{role=Button}, padding(vertical=padding5, horizontal=padding6))` + `Text(b02R, color = if(isPressed) Gray700 else Gray500)`.
- [x] `isPressed = interactionSource.collectIsPressedAsState()`.
- [x] `@Preview`를 `YGCustomTheme { }`로 감싸 "그룹 나가기" 렌더(`fillMaxWidth().background(White)`).

### Task 2: 검증
- [x] `compileReleaseKotlin` + `ktlintMainSourceSetCheck` 통과.
- [x] 프리뷰 육안(기본/pressed 색).

## 열린 질문
- disabled 상태 미지원(enabled/pressed만) — 필요 시 후속.
- 프리뷰 컨벤션 분기(`@Preview`+`YGCustomTheme`) → [design-system](../../../architecture/design-system.md) 정리 대상.
- 원자 색 직접 참조 → 시맨틱 정리([open-questions](../../../../wiki/open-questions.md)).
