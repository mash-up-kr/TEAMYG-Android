---
id: ygtextfield
title: YGTextField Implementation Plan
status: done
type: work-order
created: 2026-07-10
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: ygtextfield
related_code: YGTextField, YGTextFieldColors, YGTextFieldDefaults
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGTextField Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `core:designsystem`에 디자인 스펙의 텍스트필드 폼을 `YGTextField` 컴포넌트로 구현한다(단일 폼, 런타임 상태 idle/focused/error/disabled).

**Architecture:** `BasicTextField`(foundation) 위에 자체 CompositionLocal 테마(`YGTheme.*`)를 읽는 얇은 래퍼. 상태별 색은 `@Immutable` `YGTextFieldColors` data class로 묶고 `YGTextFieldDefaults.colors()`가 테마 기반 기본값을 제공. YGButton 컴포넌트 작성 컨벤션(component/<name>/ 파일 분리, Colors data class + resolver, Defaults object)을 그대로 따른다.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `BasicTextField`, material3 `Text`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/2026-07-10-ygtextfield.md](../../specs/archive/2026-07-10-ygtextfield.md)

## Global Constraints
- 대상 repo: `TJYG-Android`, 브랜치 `feature/#134-text-field-form`.
- 패키지: `com.teamyg.parfait.core.designsystem.component.textfield`.
- 테마 값은 `YGTheme.*`로만 접근. 크기는 `SizeTokens.*.getDp()`.
- 배경·danger는 시맨틱(`colorScheme.transparency.white75`·`colorScheme.danger`) 채택. 나머지 회색 음영·연핑크는 시맨틱 슬롯에 없어 `YGAtomicColors` 직접 참조(과도기, YGButton 선례 — [open-questions](../../../../wiki/open-questions.md)).
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`compileReleaseKotlin`) + `ktlintMainSourceSetCheck` + `@Preview` 육안**으로 대체.
- ktlint 엄격(단일 표현식 함수는 한 줄). 커밋 전 `ktlintMainSourceSetFormat`.

---

### Task 1: YGTextFieldColors — 상태별 색 홀더

**Files:**
- Create: `core/designsystem/.../component/textfield/YGTextFieldColors.kt`

**Interfaces:**
- Produces: `data class YGTextFieldColors(...)` + resolver:
  - `fun backgroundColor(isEnabled: Boolean): Color`
  - `fun borderColor(isEnabled: Boolean, isFocused: Boolean, isError: Boolean): Color`
  - `fun textColor(isEnabled: Boolean): Color`
  - `fun counterColor(isError: Boolean): Color`

- [x] **Step 1: 파일 작성** — `@Immutable data class`에 색 슬롯(background/disabledBackground, border/focusedBorder/errorBorder, text/disabledText, placeholder, cursor, counter/errorCounter, clearIconTint) + resolver 함수. border 우선순위: `!enabled` → idle, `isError` → error, `isFocused` → focused, else idle.
- [x] **Step 2: 컴파일** — `./gradlew :core:designsystem:compileReleaseKotlin --offline` → BUILD SUCCESSFUL.

### Task 2: YGTextFieldDefaults — 테마 기반 기본 색

**Files:**
- Create: `core/designsystem/.../component/textfield/YGTextFieldDefaults.kt`

**Interfaces:**
- Consumes: `YGTextFieldColors`(Task 1), `YGTheme.colorScheme.{transparency.white75,danger}`, `YGAtomicColors`.
- Produces: `object YGTextFieldDefaults { @Composable @ReadOnlyComposable fun colors(...): YGTextFieldColors }` — 각 색 슬롯이 기본값 채워진 named 파라미터(오버라이드 가능).

- [x] **Step 1: 파일 작성** — `colors()`가 background=`colorScheme.transparency.white75`, disabledBackground=`background`(동일), border=`Gray.Gray100`, focusedBorder=`Cherry.Cherry200`, errorBorder=`colorScheme.danger`, text=`Gray.Gray900`, disabledText=`text`(동일), placeholder=`Gray.Gray300`, cursor=`Gray.Gray900`, counter=`Gray.Gray400`, errorCounter=`colorScheme.danger`, clearIconTint=`Gray.Gray300` 반환.
- [x] **Step 2: 컴파일** — BUILD SUCCESSFUL.

### Task 3: YGTextField — 컴포저블 본체 + Preview

**Files:**
- Create: `core/designsystem/.../component/textfield/YGTextField.kt` (public `YGTextField` + 프리뷰)
- Create: `core/designsystem/.../component/textfield/YGTextFieldImpl.kt` (`internal YGTextFieldImpl` 본체)

**Interfaces:**
- Consumes: `YGTextFieldColors`, `YGTextFieldDefaults.colors()`, `YGTheme.{shapes,typography,layout}`, `SizeTokens.{Size1,Size24,Size44}`, `R.drawable.ic_close_round`, `PreviewBox`·`@YGPreview`(utils.preview).
- Produces: public `@Composable fun YGTextField(value, onValueChange, modifier, placeholder, enabled, isError, maxLength: Int? = null, colors = YGTextFieldDefaults.colors())` → 별도 파일 `YGTextFieldImpl.kt`의 `internal YGTextFieldImpl(..., interactionSource)`.

- [x] **Step 1: 본체 작성** —
  - public `YGTextField`는 `internal YGTextFieldImpl`(테스트/프리뷰용 `interactionSource: MutableInteractionSource = remember { ... }` 주입)에 위임.
  - focus: `interactionSource.collectIsFocusedAsState()`, `BasicTextField(interactionSource=...)`에 전달.
  - `showCounter = maxLength != null && value.isNotEmpty()`; `showClear = enabled && value.isNotEmpty()`.
  - Row(spacedBy `layout.gap.gap5`, CenterVertically): background+border+clip `shapes.radius.small`(`commonShape`), 테두리 두께 `SizeTokens.Size1.getDp()`, padding 분기(clear 있음 → start=`padding6`, end=`padding2`, vertical=`padding1`; 없음 → start/end=`padding6`, vertical=`padding5`).
  - 텍스트 영역 Box(weight 1f): 빈값이면 placeholder `Text`(`typography.body.b01R`), `BasicTextField`(singleLine, `textStyle=body.b01R.copy(color=textColor)`, `cursorBrush=SolidColor(cursorColor)`), `onValueChange`에서 `maxLength` 초과 무시.
  - 트레일링: `showCounter || showClear`이면 내부 `Row`(CenterVertically)로 묶어 우측 배치.
    - `showCounter` → `Text("${value.length}/$maxLength", style=if(isError) body.b02SB else body.b02R, counterColor(isError))`.
    - `showClear` → Box(`size(SizeTokens.Size44.getDp())`, clickable Role.Button → `onValueChange("")`, contentAlignment Center) { `Image(ic_close_round, cd="clear", tint=clearIconTint, size=SizeTokens.Size24.getDp())` }. `// TODO Change IconButton`.
- [x] **Step 2: Preview 작성** — `@YGPreview` + `PreviewBox { Column { idle/filled/error/disabled 4개 스택 } }`(focus 테두리는 런타임 전용, 프리뷰 제외).
- [x] **Step 3: 컴파일** — BUILD SUCCESSFUL.
- [x] **Step 4: ktlint** — `ktlintMainSourceSetCheck` → BUILD SUCCESSFUL.
- [ ] **Step 5: 커밋** — 사용자 승인 후 `feature/#134-text-field-form`에 commit.

> **참고**: 초기 작성본을 사용자가 수동 조정(배경 semantic white75, idle 테두리 Gray100, radius small, 테두리 Size1, clear 고정 Size44 박스, error 카운터 b02SB, colors() 파라미터화, `YGTextFieldImpl`을 별도 `YGTextFieldImpl.kt`로 분리, PreviewBox, 트레일링 gap gap5, 카운터+clear 내부 Row 그룹핑). 위 서술은 조정 후 현행 기준. spec도 동기화됨.

---

## 검증 결과 (2026-07-10)
- `:core:designsystem:compileReleaseKotlin` — BUILD SUCCESSFUL.
- `:core:designsystem:ktlintMainSourceSetCheck` — BUILD SUCCESSFUL.
- 육안 프리뷰(idle/filled/error/disabled) — 스펙 부합. focus 핑크 테두리는 기기 확인 필요.

## 열린 질문
- YGButton과 동일하게 원자 색 직접 참조 → 시맨틱 정리 대상([open-questions](../../../../wiki/open-questions.md)).
