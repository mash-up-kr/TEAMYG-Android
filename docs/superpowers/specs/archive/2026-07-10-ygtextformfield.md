---
id: ygtextformfield
title: YGTextFormField
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-10
related_code: core:designsystem component/textfield/ YGTextFormField
related_adr: ADR-0010
related_spec: ygtextfield
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGTextFormField

- 대상: `core:designsystem` — `component/textfield/`
- 관련: [YGTextField 스펙](2026-07-10-ygtextfield.md) · [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md) · [design-system](../../../architecture/design-system.md) · 이슈 #134

## 목표
[YGTextField](2026-07-10-ygtextfield.md) 아래에 **errorDescription(에러/안내 텍스트)** 을 붙인 폼 필드. 필드 본체는 기존 `internal YGTextFieldImpl`을 **그대로 재사용**하고, 이 컴포넌트는 errorDescription 표시만 얹는 얇은 래퍼.

## 범위
- **포함**: YGTextField의 모든 동작(상태·카운터·clear·입력 제어) + 필드 하단 errorDescription.
- **제외**: 필드 자체 로직(YGTextFieldImpl에 이미 있음 — 재구현 금지). 별도 label(상단 제목)·필수표시(*)·일반(비에러) 헬퍼 색 등.

## API / 인터페이스
```kotlin
@Composable
fun YGTextFormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
    errorDescription: String? = null,
    colors: YGTextFormFieldColors = YGTextFormFieldDefaults.colors(),
)
```
- `value`~`maxLength`: [YGTextField](2026-07-10-ygtextfield.md)와 동일 의미. `YGTextFieldImpl`에 위임.
- `errorDescription`: `null`이면 미표시. non-null이면 필드 하단에 표시(항상 error/danger 색).
- `colors`: **전용 `YGTextFormFieldColors`**(필드 색 + errorDescription 색 묶음). 필드 색은 `colors.textFieldColors`로 `YGTextFieldImpl`에 전달.
- 호출측은 error/입력 제한 안내 메시지를 표시할 때 `errorDescription`을 넘기고, 없을 때 `null` 전달.

## 동작 / 구성
- 루트 `Column`(`modifier`, `verticalArrangement = spacedBy(layout.gap.gap2)`):
  1. `YGTextFieldImpl(..., colors = colors.textFieldColors)` — 필드 본체.
  2. `errorDescription != null`이면 `Text(errorDescription)`. Column `spacedBy(gap2)`가 필드와의 간격을 담당(errorDescription 없으면 자식 1개라 여백 없음).
- **errorDescription 색**: 항상 `colors.errorDescriptionColor`. 기본값 `colorScheme.danger`(빨강). (일반/에러 분기 슬롯 없음 — error 색 단일.)
- **errorDescription 타이포**: `typography.caption.c01R`.
- focus/enabled/isError 상태 파생·카운터·clear·패딩은 전부 `YGTextFieldImpl` 소관(이 컴포넌트는 관여 안 함).

## 색 (`YGTextFormFieldColors`)
```kotlin
@Immutable
data class YGTextFormFieldColors(
    val textFieldColors: YGTextFieldColors,   // 필드 본체로 위임
    val errorDescriptionColor: Color,
)
```
- `YGTextFormFieldDefaults.colors(...)`가 기본값 제공(오버라이드 가능 named 파라미터): `textFieldColors = YGTextFieldDefaults.colors()`, `errorDescriptionColor = colorScheme.danger`.

## 파일 구성 (`component/textfield/`)
- `YGTextFormField.kt` — 신설. `YGTextFormField` 컴포저블 + `@YGPreview`/`PreviewBox` 프리뷰(errorDescription 있음/없음·error 조합).
- `YGTextFormFieldColors.kt` — 신설. `@Immutable data class`(textFieldColors + `errorDescriptionColor`). resolver 없음.
- `YGTextFormFieldDefaults.kt` — 신설. `@Composable @ReadOnlyComposable fun colors(...)` 기본값 제공.
- **무변경**: `YGTextFieldImpl.kt`(`YGTextFieldImpl` 재사용), `YGTextFieldColors.kt`, `YGTextFieldDefaults.kt`.

## 주의 / 열린 질문
- description 색: 최종 구현은 **error 색 단일**(`errorDescriptionColor`, 항상 danger). 초기 안의 일반 `descriptionColor` 슬롯·`descriptionColor(isError)` resolver는 도입하지 않음(에러/입력제한 안내 용도로만 쓰이므로). 향후 일반 헬퍼 텍스트가 필요하면 슬롯 추가 검토.
- `YGTextFieldImpl`이 `internal`이라 재사용은 같은 모듈(`core:designsystem`) 한정. 외부 모듈에서 폼 필드가 필요하면 public 진입점은 `YGTextFormField`(또는 `YGTextField`).
