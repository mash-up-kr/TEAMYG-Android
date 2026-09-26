---
id: ygactionitem
title: YGActionItem
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-12
related_code: core:designsystem component/ygactionitem/ YGActionItem
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGActionItem

> ⚠️ **as-built 이탈(2026-08-15, 세션 인프라 라운드)** — 아래 "제외"가 `disabled 상태`를 명시적으로
> 빼 뒀으나, 로그아웃 요청 중 같은 동작이 다시 실행되는 것을 막으려고 **`enabled: Boolean = true`
> 파라미터가 추가됐다.** 기본값이라 기존 호출부는 영향받지 않는다.
>
> **색은 바뀌지 않는다.** `enabled = false`는 `clickable(enabled = ...)`로 클릭만 차단하고
> `contentColor`는 `isPressed` 두 갈래 그대로다. 처음 구현에서 비활성 색을 `Gray300`으로 정했다가
> 되돌렸다 — **비활성 색이 디자인시스템에 정의돼 있지 않아 컴포넌트가 임의로 결정할 값이
> 아니다**(아래 "주의"의 `disabled 미지원` 항목이 가리키던 바로 그 후속 확장 지점이다).
>
> 결과적으로 **요청 중 항목이 시각적으로 변하지 않는다** — 클릭은 안 먹지만 사용자는 그 이유를
> 알 수 없다. 비활성 색이 디자인에서 확정되면 그때 채운다.

- 대상: `core:designsystem` — `component/ygactionitem/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약)

## 목표
텍스트 한 줄짜리 액션 항목을 `core:designsystem` 컴포넌트로 만든다. 좌측 정렬 텍스트 + 탭 콜백 + pressed 상태별 색. 보조/디스트럭티브 액션(예: "그룹 나가기")용 텍스트 버튼.

## 범위
- **포함**: 텍스트 표시, 탭 콜백, pressed 상태별 텍스트 색, 버튼 접근성 role.
- **제외**: 아이콘, 배경/보더(투명 — 배경은 호출부 `modifier`로 지정), disabled 상태, 색·타이포 오버라이드 파라미터(고정).

## API / 인터페이스
```kotlin
@Composable
fun YGActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```
- `text`: 액션 라벨(필수).
- `onClick`: 탭 콜백(필수).
- `modifier`: 폭·배경 등 호출부 지정(프리뷰는 `fillMaxWidth().background(White)`).
- `interactionSource`: pressed 관찰용. 기본 `remember { MutableInteractionSource() }`.

## 동작 / 상태
- **런타임 상태**: `interactionSource.collectIsPressedAsState()`로 pressed 파생.
- **구조**: 단일 `Box`(`clickable(onClick, interactionSource)`, `semantics { role = Role.Button }`, `padding(vertical = layout.padding.padding5, horizontal = layout.padding.padding6)`) 안에 `Text`.

| 요소 | 타이포 | 색 |
|---|---|---|
| 텍스트(기본) | `typography.body.b02R` | `YGAtomicColors.Gray.Gray500` |
| 텍스트(pressed) | `typography.body.b02R` | `YGAtomicColors.Gray.Gray700` |

- **패딩**: `vertical = padding.padding5`, `horizontal = padding.padding6`.

## 표시·제어 규칙
- 텍스트 항상 표시. 상태는 색으로만 표현.
- 입력/조건부 노출 없음.

## 파일 구성 (`component/ygactionitem/`)
- `YGActionItem.kt` — public 컴포저블 본체 + `@Preview`(public `YGActionItemPreview`).

## 주의 / 열린 질문
- **원자 색 직접 참조(과도기)**: `YGAtomicColors.Gray.{Gray500,Gray700}` 직접 참조. 시맨틱 슬롯 없음 — YGButton·YGTextField 선례. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
- **disabled 미지원**: enabled/pressed만. 비활성 상태 필요 시 후속 확장.
- **프리뷰 컨벤션 분기**: `@Preview` + `YGCustomTheme` 사용(etc 계열의 `@YGPreview`/`PreviewBox`와 다른 방식 공존). → [design-system](../../../architecture/design-system.md) 프리뷰 규약 정리 대상.
