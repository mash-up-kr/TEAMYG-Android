---
id: yglistitem-trailingicon-iconbutton
title: YGListItem trailing 아이콘 → YGIconButton 교체
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-16
related_code: core:designsystem component/etc/ YGListItem
related_adr: ADR-0010
related_spec: ygiconbutton, yglistitem, ygtextfield-clear-iconbutton
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGListItem trailing 아이콘 → YGIconButton 교체

- 대상: `core:designsystem` — `component/etc/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · [[2026-07-12-ygiconbutton|YGIconButton]](공통 아이콘 버튼) · [[2026-07-12-yglistitem|YGListItem]] · [[2026-07-12-ygtextfield-clear-iconbutton]](동일 패턴 선행)

## 목표
`YGListItem`의 trailing 아이콘을 지금의 인라인 `Box`+`Image`(`// TODO IconButton 컴포넌트`)에서 공통 컴포넌트 [[2026-07-12-ygiconbutton|YGIconButton]]으로 교체한다. YGIconButton 도입 시 예고된 인라인 아이콘 버튼 통일 후속 과제의 두 번째 실행(첫 번째 [[2026-07-12-ygtextfield-clear-iconbutton|YGTextField clear 교체]]).

## 범위
- **포함**: `YGListItem`의 trailing 블록을 `YGIconButton` 호출로 치환, 잉여 인라인 코드·미사용 import 제거, `trailingIconColor` 파라미터 제거.
- **제외**: `YGIconButton` 자체 변경(색 override 파라미터 추가 안 함 — 고정 상태 tint 수용), `YGListItem`의 다른 동작(메인/sub 텍스트·패딩·`trailingIcon != null` 노출 규칙·onClick 대상) 변경, 아이콘·문자열 리소스화.

## API / 인터페이스
public 시그니처 변화: `trailingIconColor: Color` 파라미터 **제거**(tint를 `YGIconButton` 내부 상태 tint로 이관).

> **후속 변경(#136 브랜치 refactor, 2026-07-16)**: 이 교체 시점엔 단일 함수(nullable `subText`/`trailingIcon` + `onClick`)였으나, 이후 `YGListItem`이 **오버로드 2개 + `YGListItemImpl` 슬롯**으로 재설계됨. 그 결과 `onClick`→`onClickTrailingIcon` 개명, `trailingIcon`은 non-null 필수. 이 교체(인라인→YGIconButton)의 무손실 매핑 근거 자체는 유효(trailing 슬롯이 여전히 `YGIconButton(SIZE_44)`). 현행 시그니처는 [[2026-07-12-yglistitem]] 참고.

교체 전(`trailingIcon?.let` 블록):
```kotlin
trailingIcon?.let {
    // TODO IconButton 컴포넌트
    Box(
        modifier = Modifier
            .clickable(role = Role.Button) { onClick() }
            .size(SizeTokens.Size44.getDp()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = trailingIcon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(trailingIconColor),
            modifier = Modifier.size(SizeTokens.Size24.getDp()),
        )
    }
}
```

교체 후:
```kotlin
trailingIcon?.let {
    YGIconButton(
        iconResource = trailingIcon,
        size = YGIconButtonSize.SIZE_44,
        contentDescription = null,
        onClick = onClick,
    )
}
```

## 동작 / 상태
- **정확 매핑**(무손실 치환 근거):

| 항목 | 기존(인라인) | YGIconButton |
|---|---|---|
| 컨테이너/아이콘 크기 | `SizeTokens.Size44` / `Size24` | `YGIconButtonSize.SIZE_44`(44/24) — 동일 |
| 기본 tint | `trailingIconColor`(기본 `Gray.Gray300`) | 기본 상태 tint `Gray.Gray300` — 동일 |
| 아이콘 | `trailingIcon`(`@DrawableRes`) | 동일 전달 |
| contentDescription | `null` | `null` — 동일 |
| onClick | `onClick` | `onClick` — 동일 |
| role | `Role.Button` | YGIconButton `clickable` 내재 |

- **동작 변화(의도된 개선)**: YGIconButton 상태 tint로 **pressed 시 `Gray.Gray400` 피드백이 새로 생긴다**(기존 인라인은 정적). 평상시 외형은 동일(`Gray.Gray300`).
- **disabled 무관**: YGListItem은 disabled 개념이 없어 YGIconButton `isEnabled` 기본 `true` 사용(disabled tint 경로 미도달).

## 표시·제어 규칙
- trailing 노출 조건 불변: `trailingIcon != null`.
- 메인/sub 텍스트·패딩·gap 배치 불변.

## 파일 구성 (`component/etc/`)
- `YGListItem.kt` — trailing 블록을 `YGIconButton` 호출로 교체. `trailingIconColor` 파라미터 제거. 미사용 import 제거: `Image`·`clickable`·`Box`·`size`(layout)·`ColorFilter`·`painterResource`·`Role`·`SizeTokens`(모두 인라인 아이콘 전용). `YGIconButton`·`YGIconButtonSize` import 추가. `@DrawableRes`(trailingIcon)·`Alignment`(Row `verticalAlignment`)·`Color`(textColor/subTextColor)·`R`(프리뷰 `ic_caret_right`)는 유지.

## 주의 / 열린 질문
- **`trailingIconColor` 색 커스터마이즈 소멸**: 호출부가 trailing tint를 개별 지정하던 경로 제거(기본 Gray300 외 사용처 없음 전제). 향후 색 분기 필요 시 YGIconButton에 색 파라미터 도입 재검토.
- **contentDescription `null` 유지**: 아이콘을 장식 취급. clickable 라벨 부재는 후속 a11y 개선 대상([[2026-07-12-yglistitem|YGListItem]]과 동일 이슈).
- **과도기 색**: YGIconButton이 `YGAtomicColors.Gray.*`를 직접 참조(시맨틱 슬롯 없음) — 이 교체로 trailing의 원자색 참조도 YGIconButton 내부로 이관. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
