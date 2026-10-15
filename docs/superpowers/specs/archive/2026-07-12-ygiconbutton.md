---
id: ygiconbutton
title: YGIconButton
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-12
related_code: core:designsystem component/ygiconbutton/ YGIconButton, YGIconButtonSize
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGIconButton

- 대상: `core:designsystem` — `component/ygiconbutton/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · 이슈 #126(button-icon)

## 목표
재사용 가능한 아이콘 전용 버튼을 `core:designsystem` 컴포넌트로 만든다. 정사각 컨테이너 안 중앙 아이콘 + 탭 콜백 + enabled/pressed 상태별 tint. 지금까지 인라인으로 중복 구현하던 아이콘 버튼(예: [[2026-07-12-yglistitem|YGListItem]]의 trailing caret Box, YGTextField clear 박스)을 대체하는 **공통 icon-button 컴포넌트**.

## 범위
- **포함**: 아이콘 리소스 표시, 크기 프리셋(container/icon 쌍) 선택, 탭 콜백, `isEnabled` 토글, pressed/disabled 상태별 tint, `contentDescription`(a11y) 파라미터.
- **제외**: 텍스트/라벨(아이콘 전용), 배경/보더(투명), 커스텀 tint 파라미터(상태별 gray 음영 하드코딩), ripple 커스터마이즈(기본 `clickable`).

## API / 인터페이스
```kotlin
@Composable
fun YGIconButton(
    @DrawableRes iconResource: Int,
    size: YGIconButtonSize,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isEnabled: Boolean = true,
)

@Immutable
enum class YGIconButtonSize(val containerSize: Dp, val iconSize: Dp) {
    SIZE_44(containerSize = 44.dp, iconSize = 24.dp),
    SIZE_48(containerSize = 48.dp, iconSize = 28.dp),
}
```
- `iconResource`: 표시할 `@DrawableRes` 아이콘(필수).
- `size`: `YGIconButtonSize` 프리셋. 컨테이너/아이콘 크기 쌍을 enum이 보유(SIZE_44=44/24, SIZE_48=48/28).
- `contentDescription`: 스크린리더 라벨(nullable). 장식이면 `null`.
- `onClick`: 탭 콜백(필수).
- `interactionSource`: pressed 등 상호작용 관찰용. 기본 `remember { MutableInteractionSource() }`.
- `isEnabled`: 비활성 시 tint를 흐리게(클릭 자체는 `clickable` 유지 — 아래 주의 참고). 기본 `true`.

## 동작 / 상태
- **런타임 상태**: `interactionSource.collectIsPressedAsState()`로 pressed 파생. `isEnabled`는 prop.
- **구조**: `Box`(`size(containerSize)`, `clickable(onClick, interactionSource)`, `contentAlignment = Center`) 안에 `Image`(`painterResource`, `size(iconSize)`, `ColorFilter.tint(...)`).
- **상태 → tint 매핑**:

| 상태 | tint(원자색) |
|---|---|
| `isEnabled == false` | `Gray.Gray200` |
| `isPressed` | `Gray.Gray400` |
| 그 외(기본) | `Gray.Gray300` |

- 우선순위: disabled > pressed > default(코드 `when` 순서).

## 표시·제어 규칙
- 아이콘은 항상 표시(옵션 없음). 상태는 tint로만 표현(크기·위치 불변).
- 색 오버라이드 파라미터 없음 — 상태별 gray 음영 고정.

## 파일 구성 (`component/ygiconbutton/`)
- `YGIconButton.kt` — public 컴포저블 본체 + `@Preview`.
- `YGIconButtonSize.kt` — `@Immutable enum YGIconButtonSize`(container/icon Dp 쌍).
- `YGIconButtonPreviewData.kt` — `YGIconButtonPreviewData` data class + `YGIconButtonPreviewParameterProvider`(SIZE_44/48 × enabled/disabled 4종).

## 주의 / 열린 질문
- **원자 색 직접 참조(과도기)**: 상태별 `YGAtomicColors.Gray.*`를 직접 참조. 시맨틱 슬롯 없음 — YGButton·YGTextField 선례와 동일. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
- **`isEnabled`와 클릭 분리**: `isEnabled = false`여도 `clickable`은 그대로라 tint만 흐려지고 **탭은 여전히 동작**. 실제 비활성 동작 차단은 호출부 책임 또는 후속 개선 대상.
- **프리뷰 컨벤션 분기**: 이 컴포넌트는 `@Preview` + `YGCustomTheme` + `PreviewParameterProvider`를 쓴다. etc 계열([[2026-07-12-yglistitem|YGListItem]]·[[2026-07-12-yghorizontaldivider|YGHorizontalDivider]])의 `@YGPreview`/`PreviewBox`와 다른 방식이 공존. → [design-system](../../../architecture/design-system.md) 프리뷰 규약 정리 대상.
- **인라인 아이콘 버튼 교체**: YGListItem caret·YGTextField clear의 인라인 `Box`+`Image`(`// TODO IconButton 컴포넌트`)를 이 컴포넌트로 치환 가능. 후속 리팩터 과제.
