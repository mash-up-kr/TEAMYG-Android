---
id: ygiconbutton
title: YGIconButton Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: ygiconbutton
related_code: YGIconButton
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGIconButton Implementation Plan

> **상태: 구현 완료** — 이 계획은 머지된 구현을 사후 기록한 것이다(reverse-doc). 체크박스는 완료 표시.

**Goal:** `core:designsystem`에 재사용 아이콘 전용 버튼 `YGIconButton`(정사각 컨테이너 + 중앙 아이콘 + enabled/pressed tint)을 구현한다. 인라인 아이콘 버튼(YGListItem caret·YGTextField clear) 대체용 공통 컴포넌트.

**Architecture:** `Box`(size=containerSize, clickable) 안에 `Image`(size=iconSize, `ColorFilter.tint`). 상태는 `interactionSource.collectIsPressedAsState()` + `isEnabled` prop으로 파생, `when`으로 tint 분기(disabled>pressed>default). 크기 쌍은 `@Immutable enum YGIconButtonSize`(container/icon Dp)로 캡슐화. 프리뷰는 `PreviewParameterProvider`로 크기×enabled 조합.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `Box`/`clickable`/`Image`/`interaction`, `@Immutable` enum), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/archive/2026-07-12-ygiconbutton.md](../../specs/archive/2026-07-12-ygiconbutton.md)

## Global Constraints
- 대상 repo: `TJYG-Android`, 브랜치 `feature/#126-buton-icon`.
- 패키지: `com.teamyg.parfait.core.designsystem.component.ygiconbutton`.
- 색은 상태별 `YGAtomicColors.Gray.*` 직접 참조(시맨틱 슬롯 없음 — YGButton·YGTextField 선례, 과도기 → [open-questions](../../../../wiki/open-questions.md)).
- 검증: compile + ktlint + `@Preview` 육안.

---

### Task 1: YGIconButtonSize enum
- [x] `@Immutable enum YGIconButtonSize(containerSize, iconSize)` — SIZE_44(44/24), SIZE_48(48/28).

### Task 2: YGIconButton 본체
- [x] `Box(size(containerSize), clickable(onClick, interactionSource), contentAlignment=Center)` + `Image(painterResource(iconResource), tint=when{disabled→Gray200; pressed→Gray400; else→Gray300}, contentDescription, size(iconSize))`.
- [x] `isPressed = interactionSource.collectIsPressedAsState()`.

### Task 3: Preview
- [x] `YGIconButtonPreviewData(buttonIconSize, isEnabled)` + `YGIconButtonPreviewParameterProvider`(SIZE_44/48 × enabled/disabled 4종).
- [x] `@Preview` 컴포저블을 `YGCustomTheme { }`로 감싸 `@PreviewParameter`로 주입.

### Task 4: 검증
- [x] `compileReleaseKotlin` + `ktlintMainSourceSetCheck` 통과.
- [x] 프리뷰 4종 육안(SIZE_44/48 × enabled/disabled tint 차이).

## 열린 질문
- `isEnabled = false`여도 `clickable` 유지 → tint만 흐려지고 탭은 동작. 비활성 차단은 후속/호출부 책임.
- 프리뷰 컨벤션(`@Preview`+`PreviewParameterProvider`)이 etc 계열(`@YGPreview`/`PreviewBox`)과 분기 → [design-system](../../../architecture/design-system.md) 정리 대상.
- 원자 색 직접 참조 → 시맨틱 정리([open-questions](../../../../wiki/open-questions.md)).
