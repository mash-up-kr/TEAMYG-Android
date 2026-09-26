---
id: ygtextfield
title: YGTextField
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-10
related_code: core:designsystem component/textfield/ YGTextField
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGTextField

> 🔁 [2026-07-22] 이 스펙은 초기 baseline. `commonShape` `radius.small→none`(각짐)·배경 `transparency.white75→grayScale.white` 변경이 PR #159로 **develop 머지 완료** → [2026-07-19-designsystem-radius-none-sync](2026-07-19-designsystem-radius-none-sync.md)(현행 값의 단일 출처).

- 대상: `core:designsystem` — `component/textfield/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · 이슈 #134

## 목표
디자인 스펙의 텍스트필드 폼을 `core:designsystem` 컴포넌트로 만든다. 한 폼의 **런타임 상태 4종**(idle / focused / error / disabled)을 하나의 컴포저블로 표현. YGButton 컴포넌트 작성 컨벤션을 따른다.

## 범위
- **포함**: 입력 텍스트·placeholder·커서, 글자수 카운터(`현재/최대`), clear(X) 아이콘 버튼, 상태별 테두리/색, `maxLength` 초과 입력 차단.
- **제외**: 하단 description/헬퍼 텍스트(디자인상 존재하나 이번 구현 제외), 크기 variant(현재 폼 1종만), 다크 모드 별도 팔레트(테마가 라이트=다크).

## API / 인터페이스
```kotlin
@Composable
fun YGTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
    colors: YGTextFieldColors = YGTextFieldDefaults.colors(),
)
```
- `maxLength`: `null`이면 무제한·카운터 없음. 지정 시 카운터 `${value.length}/${maxLength}` 표시 + 초과 입력 무시(자르기 아님).
- `colors`: 상태별 색 묶음. 기본값 `YGTextFieldDefaults.colors()`(테마 기반). `colors()`는 **각 색 슬롯을 파라미터로 오버라이드 가능**(기본값 채워진 named 파라미터).
- `BasicTextField`(foundation) 기반, `cursorBrush = SolidColor(커서색)`.
- **구조**: public `YGTextField`는 별도 파일 `YGTextFieldImpl.kt`의 `internal YGTextFieldImpl`(테스트/프리뷰용 `interactionSource` 주입 파라미터 보유)에 위임.

## 동작 / 상태
상태는 prop + 런타임 focus 조합으로 파생(별도 variant 타입 없음).
- **focused**: `interactionSource`의 `collectIsFocusedAsState()`.
- **enabled / isError**: prop.

| 요소 | idle | focused | error | disabled |
|---|---|---|---|---|
| 배경 | `colorScheme.transparency.white75` | 동일 | 동일 | 동일(=배경) |
| 테두리 | `Gray.Gray100` | `Cherry.Cherry200` | `colorScheme.danger` | `Gray.Gray100`(=idle) |
| 입력 텍스트 | `Gray.Gray900` | `Gray.Gray900` | `Gray.Gray900` | `Gray.Gray900`(=텍스트) |
| placeholder | `Gray.Gray300` | — | — | — |
| 커서 | `Gray.Gray900` | — | — | — |
| 카운터 | (숨김) | `Gray.Gray400` | `colorScheme.danger` | `Gray.Gray400` |
| clear(X) tint | (숨김) | `Gray.Gray300` | `Gray.Gray300` | **숨김** |

- 토큰: radius `shapes.radius.small`, 테두리 두께 `SizeTokens.Size1.getDp()`, 입력 텍스트 `typography.body.b01R`. clear 아이콘 `R.drawable.ic_close_round`.
- **카운터 스타일**: error 시 `typography.body.b02SB`(강조), 그 외 `typography.body.b02R`.
- **트레일링 배치**: 카운터·clear는 `showCounter || showClear`일 때 하나의 내부 `Row`로 묶여 우측에 배치. 텍스트 영역(weight 1f)과 트레일링 그룹 사이 간격 `layout.gap.gap5`.
- 배경은 시맨틱 `YGTheme.colorScheme.transparency.white75`, error/카운터 danger도 시맨틱 `colorScheme.danger`. 나머지 회색 음영(Gray100/300/400/900)·연핑크(Cherry200)는 시맨틱에 없어 `YGAtomicColors` 직접 참조.
- disabled는 배경·테두리·입력 텍스트가 enabled와 동일(현재 별도 dim 처리 없음). clear만 숨김.

## 표시·제어 규칙
- **카운터**: `maxLength != null` **AND** `value.isNotEmpty()`.
- **clear(X)**: `enabled` **AND** `value.isNotEmpty()` **AND** (`isFocused` **OR** `isError`). 탭 시 `onValueChange("")`. `contentDescription = "clear"`. 🔁 **2026-07-23 정정**: 초안은 `enabled && value.isNotEmpty()`(값만 있으면 항상)였으나, 디자인 컴포넌트 상태(node 144-7104)상 **default(비포커스·유효) 상태엔 clear 없음** → 포커스∨에러일 때만 노출로 게이팅. 위 tint 표 idle 컬럼 `(숨김)`과 정합. (IconButton 교체는 [[2026-07-12-ygtextfield-clear-iconbutton]]에서 완료.)
  - ✅ **2026-08-04 develop 머지(PR #192)**: 이 게이팅은 정정 시점엔 문서 선반영이었고, `YGTextFieldImpl`의 `showClear` 조건 변경이 S-002 브랜치를 타고 이제야 develop에 들어왔다. `YGTextFieldImpl` 공용이라 `YGTextField`·`YGTextFormField` **양쪽 전 사용처에 동시 적용**된다(`AccountInfoScreen`·`GroupCreateScreen`·`GroupNickNameScreen`). 기존 두 화면의 회귀 확인 기록은 없다.
- **입력 제어**: `onValueChange` 진입 시 `maxLength` 초과분은 반영하지 않음(콜백에서 게이트).

## 컨테이너 패딩
clear 노출 유무로 분기(clear 박스(`Size44`)가 end·vertical 공간을 흡수).

| | start | end | top | bottom |
|---|---|---|---|---|
| clear 없음 | `padding.padding6`(16) | `padding.padding6`(16) | `padding.padding5`(12) | `padding.padding5`(12) |
| clear 있음 | `padding.padding6`(16) | `padding.padding2`(4) | `padding.padding1`(2) | `padding.padding1`(2) |

## 파일 구성 (`component/textfield/`)
- `YGTextField.kt` — public `YGTextField`(→ `YGTextFieldImpl`에 위임) + `@YGPreview` 프리뷰(`PreviewBox`로 래핑, idle/filled/error/disabled 스택).
- `YGTextFieldImpl.kt` — `internal YGTextFieldImpl`(interactionSource 주입, Row/Box/BasicTextField·카운터·clear 등 필드 본체 로직). YGTextFormField가 재사용하는 실제 구현체.
- `YGTextFieldColors.kt` — `@Immutable data class`, 상태별 색 슬롯 + resolver 함수(`backgroundColor`/`borderColor`/`textColor`/`counterColor`).
- `YGTextFieldDefaults.kt` — `@Composable @ReadOnlyComposable fun colors(...)`가 테마 기반 기본값(오버라이드 가능 named 파라미터) 제공(theme `*Defaults` 네이밍 일치).

## 주의 / 열린 질문
- **과도기**: 컴포넌트가 시맨틱 grayScale 대신 `YGAtomicColors` 직접 참조 — YGButton 선례와 동일. 디자인 토큰 규칙 확정 시 시맨틱으로 정리 대상. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
- focused 테두리(pink)는 실제 focus에서만 보임 → 프리뷰는 idle/filled/error/disabled 중심, focus는 기기 확인.
