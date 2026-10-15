---
id: designsystem-preview-migration
title: designsystem 프리뷰 관용구 통일 (@YGPreview + PreviewBox)
status: implemented
category: refactor-spec
platforms: android
verified: 2026-07-19
related_code: YGPreview.kt#YGPreview, PreviewComponent.kt#PreviewBox
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, refactor, preview]
---

# Spec: designsystem 프리뷰 관용구 통일 (@YGPreview + PreviewBox)

- 대상: `core:designsystem` — `component/*` 프리뷰
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)
- SoT: TJYG-Android `develop` — **PR #158(`refactor/design-system-preview`) 머지 완료**(2026-07-19, `ce4e9b8`).

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
> ✅ [2026-07-19] develop 머지(#158). 함께 `YGAtomicColors` internal→public 전환 반영 → [design-system](../../../architecture/design-system.md)·[open-questions](../../../synthesis/open-questions.md).

## 목표
`core:designsystem` 컴포넌트 프리뷰에 혼재된 두 관용구 —
(A) `@Preview` + 본문 `YGCustomTheme { }`, (B) `@YGPreview` + `PreviewBox { }` —
를 **(B) `@YGPreview` + `PreviewBox { }` 하나로 통일**한다. 프리뷰 커버리지(6-config: phone/night/foldable 변형)와 테마 래핑을 전 컴포넌트가 동일하게 갖는다.

## 배경
- `@YGPreview`(`utils/preview/YGPreview.kt`) = `@Preview` 6개(phone·phone-night·foldable·foldable-night·foldable-fold·foldable-fold-night) 묶은 다중 프리뷰 애노테이션.
- `PreviewBox`(`utils/preview/PreviewComponent.kt`) = `YGCustomTheme { Box(fillMaxSize) { content } }` 래퍼(Local 미초기화 크래시 방지 + 프리뷰 배치 컨테이너).
- design-system.md·[open-questions](../../../synthesis/open-questions.md) "[2026-07-12] 컨벤션 분기"에서 미확정으로 추적하던 프리뷰 방식 표준화를 (B)로 확정.

## 범위
- **포함**: 아래 12개 컴포넌트 파일의 프리뷰를 (B) 관용구로 변환.
- **제외**:
  - 공용 유틸 `YGPreview.kt`·`PreviewComponent.kt` 변경 — **불변**(시그니처·config 유지).
  - 이미 (B) 준수하는 6파일 — **변경 없음**.
  - 프리뷰 외 컴포넌트 본체 로직·API — 손대지 않는다(프리뷰 애노테이션·래퍼만).
  - `PreviewBox`에 배경 파라미터 추가 등 유틸 확장 — 안 함(배경은 content 내부 처리).

### 대상 12파일 (현재 A 관용구)
`YGButton` · `YGChipButton` · `YGToggleButton` · `YGIconButton` · `YGInputNumber` · `YGActionItem` · `YGColorChip` · `YGDate` · `YGLabel` · `YGTopBar` · `YGDateButton` · `YGDangerZone`

### 유지 6파일 (이미 B 관용구, 변경 없음)
`YGTextField` · `YGTextFormField` · `YGListItem` · `YGHorizontalDivider` · `YGInviteCard` · `YGModalPopup`

## 변환 규칙 (파일별)
1. **애노테이션**: 프리뷰 함수의 `@Preview`(옵션 포함) → `@YGPreview`.
2. **테마 래퍼**: 본문 `YGCustomTheme { … }` → `PreviewBox { … }`. 내부 content(Column/Row/컴포넌트 호출)는 그대로 이관.
3. **import 정리**: 프리뷰에서만 쓰이던 `androidx.compose.ui.tooling.preview.Preview`·`theme.YGCustomTheme` import 제거, `utils.preview.YGPreview`·`utils.preview.PreviewBox` import 추가. (컴포넌트 본체가 `@Preview`/`YGCustomTheme`를 별도로 쓰지 않는 것 전제 — 프리뷰 전용이면 제거.)
4. **배경 보존**: 명시 배경을 쓰던 프리뷰는 content 람다 내부 `Modifier.background(...)`로 유지. **PreviewBox 자체엔 배경 미주입.**
   - `YGTopBar` = White, `YGDateButton` = White, `YGDangerZone` = **Black**(밝은 반투명 컨테이너라 가독성 필수).
5. **@PreviewParameter 유지**: 6파일(`YGButton`·`YGChipButton`·`YGIconButton`·`YGInputNumber`·`YGColorChip`·`YGToggleButton`)은 파라미터 provider를 그대로 두고 애노테이션만 교체:
   ```kotlin
   @YGPreview
   @Composable
   private fun XPreview(
       @PreviewParameter(XPreviewParameterProvider::class) data: T,
   ) = PreviewBox { /* data 사용 */ }
   ```
   → 6 config × N 파라미터값 프리뷰 생성(폭증 수용).

## 동작 / 상태
- 런타임 동작 변화 없음(프리뷰 전용 애노테이션·래퍼 교체). 컴포넌트 public API·상태 로직 불변.
- 프리뷰 커버리지: 전 대상이 6-config로 확대(기존 A는 단일 `@Preview`).

## 표시·제어 규칙
- `PreviewBox`는 `fillMaxSize` `Box`(기본 top-start 정렬). 기존 `YGCustomTheme { Column {...} }`의 내부 정렬·`spacedBy`는 content 이관으로 유지되나, Box 채움 특성상 중앙성/여백이 미세 변할 수 있음 — IDE 육안 확인 대상.
- `@YGPreview` config별 `showBackground`가 상이(phone=미설정, foldable=true)하므로 배경이 필요한 프리뷰는 규칙 4로 자체 배경을 그린다.

## 파일 구성
- 변경: 위 대상 12파일(각 프리뷰 함수만).
- 불변: `utils/preview/YGPreview.kt`, `utils/preview/PreviewComponent.kt`, 유지 6파일, 전 컴포넌트 본체.

## 검증
- 프리뷰 애노테이션 리팩터라 **런타임 동작 테스트 없음**(TDD 부적합).
- 검증선: `core:designsystem` **컴파일**(`:core:designsystem:compileDebugKotlin`) + **ktlint** 통과.
- 프리뷰 렌더는 Android Studio 육안(수동) — 특히 `@YGPreview`+`@PreviewParameter` 첫 조합, 배경 보존 3파일.

## 주의 / 열린 질문
- **`@YGPreview` + `@PreviewParameter` 첫 사례**: 기존 (B) 준수 6파일엔 PreviewParameter가 없음. 조합이 IDE 프리뷰에서 정상 렌더되는지(각 config × 각 값) 확인 필요. 문제 시 해당 파일만 파라미터 합침(본문 나열) 대안 검토.
- **프리뷰 수 폭증**: `YGColorChip`(14타입 × 6 = 84) 등. IDE 렌더 부하만 있고 빌드 산출물 영향 없음. 수용.
- 완료 시 [open-questions](../../../synthesis/open-questions.md) "[2026-07-12] 컨벤션 분기"의 프리뷰 방식 항목 해소 + design-system.md 프리뷰 방식 노트 갱신(혼재 → @YGPreview/PreviewBox 표준).
