---
id: ygtextfield-clear-iconbutton
title: YGTextField clear 버튼 → YGIconButton 교체
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-12
related_code: core:designsystem component/textfield/ YGTextFieldImpl
related_adr: ADR-0010
related_spec: ygiconbutton, ygtextfield
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGTextField clear 버튼 → YGIconButton 교체

- 대상: `core:designsystem` — `component/textfield/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · [[2026-07-12-ygiconbutton|YGIconButton]](공통 아이콘 버튼 컴포넌트) · [[2026-07-10-ygtextfield|YGTextField]]

## 목표
`YGTextFieldImpl`의 clear 버튼을 지금의 인라인 `Box`+`Image`(`// TODO Change IconButton`)에서 공통 컴포넌트 [[2026-07-12-ygiconbutton|YGIconButton]]으로 교체한다. YGIconButton 도입 시 예고된 인라인 아이콘 버튼 통일 후속 과제의 실행.

## 범위
- **포함**: `YGTextFieldImpl`의 clear 블록을 `YGIconButton` 호출로 치환, 잉여 인라인 코드·미사용 import 제거, `YGTextFieldColors`/`YGTextFieldDefaults`에서 `clearIconTint` 제거.
- **제외**: `YGIconButton` 자체 변경(색 override 파라미터 추가 안 함 — 고정 상태 tint 수용), `YGListItem` trailing 아이콘 교체(현재 브랜치 미포함, 별도 후속), clear 아이콘 리소스·문자열 리소스화, disabled clear 동작 신설.
- **무편집(반사 반영)**: [[2026-07-10-ygtextformfield|YGTextFormField]] — clear 버튼을 자체 구현하지 않고 `YGTextFieldImpl`에 위임하므로 Impl 교체만으로 자동 적용. FormField 파일 편집 없음.

## API / 인터페이스
호출부 교체(내부 구현). `YGTextField`/`YGTextFormField`의 public 시그니처는 **불변**.

교체 전(`YGTextFieldImpl`, `showClear` 블록):
```kotlin
if (showClear) {
    // TODO Change IconButton
    Box(
        modifier = Modifier
            .clickable(role = Role.Button) { onValueChange("") }
            .size(SizeTokens.Size44.getDp()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_close_round),
            contentDescription = "clear",
            colorFilter = ColorFilter.tint(colors.clearIconTint),
            modifier = Modifier.size(SizeTokens.Size24.getDp()),
        )
    }
}
```

교체 후:
```kotlin
if (showClear) {
    YGIconButton(
        iconResource = R.drawable.ic_close_round,
        size = YGIconButtonSize.SIZE_44,
        contentDescription = "clear",
        onClick = { onValueChange("") },
    )
}
```

## 동작 / 상태
- **정확 매핑**(무손실 치환 근거):

| 항목 | 기존(인라인) | YGIconButton |
|---|---|---|
| 컨테이너/아이콘 크기 | `SizeTokens.Size44` / `Size24` | `YGIconButtonSize.SIZE_44`(44/24) — 동일 |
| 기본 tint | `colors.clearIconTint`(기본 `Gray.Gray300`) | 기본 상태 tint `Gray.Gray300` — 동일 |
| 아이콘 | `R.drawable.ic_close_round` | 동일 |
| contentDescription | `"clear"` | `"clear"` — 동일 |
| onClick | `onValueChange("")` | `onValueChange("")` — 동일 |
| role | `Role.Button` | YGIconButton `clickable` 내재 |

- **동작 변화(의도된 개선)**: YGIconButton 상태 tint로 **pressed 시 `Gray.Gray400` 피드백이 새로 생긴다**(기존 clear는 정적). 평상시 외형은 동일.
- **disabled 무관**: `showClear`가 `enabled`를 포함하므로 disabled(`enabled=false`)면 clear 자체가 미표시. YGIconButton `isEnabled`는 기본 `true`로 두면 됨(disabled tint 경로 미도달).

## 표시·제어 규칙
- clear 노출 조건(당시): `showClear = enabled && value.isNotEmpty()`. 🔁 **2026-07-23 정정**: 이후 `&& (isFocused || isError)` 추가로 default(비포커스·유효)엔 미노출(근거 [[2026-07-10-ygtextfield]], **develop 머지는 2026-08-04 PR #192**). 이 교체(IconButton) 자체는 노출 조건 무관.
- counter/clear 배치·패딩(`showClear` 분기) 불변.

## 파일 구성 (`component/textfield/`)
- `YGTextFieldImpl.kt` — clear 블록을 `YGIconButton` 호출로 교체. 미사용 import 제거: `Image`·`ColorFilter`·`painterResource`·`Role`(clear 전용). `Box`·`Alignment`는 타 사용처 확인 후 정리(counter/레이아웃에서 쓰면 유지). `YGIconButton`·`YGIconButtonSize` import 추가.
- `YGTextFieldColors.kt` — `clearIconTint: Color` 필드 제거.
- `YGTextFieldDefaults.kt` — `colors()`의 `clearIconTint` 파라미터 + 생성자 할당 제거. `YGAtomicColors` import는 타 기본값이 계속 사용하므로 유지.

## 주의 / 열린 질문
- **`clearIconTint` 색 커스터마이즈 소멸**: 호출부가 clear tint를 개별 지정하던 경로 제거(현재 기본값 Gray300 외 사용처 없음 전제). 향후 색 분기 필요 시 YGIconButton에 색 파라미터 도입 재검토([design-system](../../../architecture/design-system.md) 원자색 정리 이슈와 함께).
- **YGListItem 미포함**: 동일 인라인 패턴(trailing 아이콘)은 현재 브랜치에 없어 이 스펙 범위 밖. YGIconButton 통일 완결을 위해 별도 후속 스펙 필요.
- **과도기 색**: YGIconButton이 `YGAtomicColors.Gray.*`를 직접 참조(시맨틱 슬롯 없음) — 이 교체로 clear의 원자색 참조도 YGIconButton 내부로 이관. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
