---
id: yglistitem
title: YGListItem
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-16
related_code: core:designsystem component/etc/ YGListItem, YGListItemImpl
related_adr: ADR-0010
related_spec: yglistitem-trailingicon-iconbutton
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGListItem

- 대상: `core:designsystem` — `component/etc/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · 이슈 #136

## 목표
피그마 List-Item 스펙의 리스트 행을 `core:designsystem` 컴포넌트로 만든다. 좌측 메인 텍스트 + 우측에 **sub 텍스트(값) 또는 trailing 아이콘 버튼** 중 하나. 메뉴·설정 리스트 항목용.

> **현행 API**(refactor `list item 구현 형식 변경`, #136 브랜치). 초기 설계(단일 함수 + nullable `subText`/`trailingIcon`)에서 **오버로드 2개 + 공통 `YGListItemImpl` 슬롯**으로 재설계됨. trailing→YGIconButton 교체는 [[2026-07-12-yglistitem-trailingicon-iconbutton]] 참고.

## 범위
- **포함**: 메인 텍스트 + (a) sub 텍스트 값 **또는** (b) trailing 아이콘 버튼 — **상호배타**(오버로드 분리). trailing 아이콘 탭 콜백.
- **제외**: sub·trailing **동시** 표시(오버로드로 배제), "둘 다 없음" 케이스(오버로드로 배제), 행 전체 clickable(clickable은 trailing 아이콘만), leading 아이콘, 하단 divider 결합([[2026-07-12-yghorizontaldivider|YGHorizontalDivider]] 별도 조합), `Colors`/`Defaults` 색 홀더(직접 파라미터로 대체).

## API / 인터페이스
```kotlin
// (a) trailing 아이콘 버전
@Composable
fun YGListItem(
    text: String,
    @DrawableRes trailingIcon: Int,
    onClickTrailingIcon: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = YGAtomicColors.Gray.Gray800,
)

// (b) subText 버전
@Composable
fun YGListItem(
    text: String,
    subText: String,
    modifier: Modifier = Modifier,
    textColor: Color = YGAtomicColors.Gray.Gray800,
    subTextColor: Color = YGAtomicColors.Gray.Gray400,
)

// 공통 셸(private)
@Composable
private fun YGListItemImpl(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
)
```
- **오버로드 분리**: sub 텍스트 행과 trailing 아이콘 행이 별도 함수. 호출측이 시그니처로 배타 선택(nullable 분기 제거).
- `text`: 메인 텍스트(필수, 양쪽 공통).
- `trailingIcon`(a): `@DrawableRes`, **필수·non-null**(예: `R.drawable.ic_caret_right`). `onClickTrailingIcon`: trailing 아이콘 버튼 탭 콜백(**필수**, 구 `onClick`에서 개명).
- `subText`(b): 메인 우측 값 텍스트(**필수·non-null**).
- `textColor`(공통) / `subTextColor`(b): 색 오버라이드. 기본은 테마 원자색. `trailingIconColor`는 없음(tint를 `YGIconButton` 내부 상태 tint로 이관 — [[2026-07-12-yglistitem-trailingicon-iconbutton]]).

## 동작 / 상태
- 런타임 상태 없음(정적 표시 + 아이콘 탭 콜백). 상태별 색 분기 없음.
- **구조**: 공통 `YGListItemImpl`이 루트 `Row`(`fillMaxWidth`, `verticalAlignment = CenterVertically`, `horizontalArrangement = spacedBy(layout.gap.gap2)`) + 메인 `Text`(`weight 1f`) + `trailing` 슬롯을 그린다. 두 public 오버로드가 `trailing`에 각각 sub `Text`·`YGIconButton`을 넣는다.
  1. 메인 `Text`(`weight 1f`) — 남는 폭 차지, trailing 슬롯을 우측으로 밀어냄.
  2. `trailing` 슬롯: (a) `YGIconButton(SIZE_44)` 또는 (b) sub `Text`.

| 요소 | 타이포 | 색 | 비고 |
|---|---|---|---|
| 메인 텍스트 | `typography.body.b02R` | `textColor`(기본 `Gray.Gray800`) | 양쪽 공통 |
| sub 텍스트 | `typography.body.b02SB` | `subTextColor`(기본 `Gray.Gray400`) | 우측 값 텍스트, 세미볼드 (b) |
| trailing 아이콘 | — | `YGIconButton` 내부 상태 tint(기본 `Gray.Gray300`) | 임의 `@DrawableRes` (a) |

- **패딩**: `YGListItemImpl`의 `Row`에 `horizontal = layout.padding.padding7`, `vertical = layout.padding.padding2`.
- **아이콘 배치**: `YGIconButton(size = SIZE_44)`(컨테이너 44 / 아이콘 24, 상태 tint 내재). → [[2026-07-12-yglistitem-trailingicon-iconbutton]].
- **높이**: 피그마 Hug. trailing 아이콘 버전은 `YGIconButton`(Size44) + 상하 `padding2`가 높이 지배. sub 버전은 텍스트 높이로 Hug.

## 표시·제어 규칙
- **sub vs trailing**: 오버로드 시그니처로 결정(둘을 함께·둘 다 없이 호출 불가).
- 입력 제어 없음(정적).

## 파일 구성 (`component/etc/`)
- `YGListItem.kt` — public `YGListItem` 오버로드 2개(trailing / subText) + private `YGListItemImpl`(공통 Row + `trailing: @Composable` 슬롯) + `@YGPreview`/`PreviewBox` 프리뷰(trailingIcon·sub 2종).

## 주의 / 열린 질문
- **API 재설계 이력**: 초기 단일 함수(nullable `subText`/`trailingIcon` + `onClick`)에서 오버로드 2개 + 슬롯으로 변경(#136 브랜치 refactor). sub·trailing 동시/무 케이스가 타입 레벨로 배제됨. `onClick`→`onClickTrailingIcon` 개명.
- **미머지**: #136(`feature/#136-etc-component`) develop 미머지 — [open-questions 2026-07-13](../../../synthesis/open-questions.md). 머지 시 최종 시그니처 재확인.
- **sub 텍스트 스타일**: 우측 값 텍스트로 `body.b02SB` + `Gray.Gray400` 채택. 피그마 확정본과 어긋나면 갱신.
- **과도기**: gray 음영·아이콘 tint가 시맨틱 슬롯 없어 `YGAtomicColors` 직접 참조 — YGButton·YGTextField 선례. → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../synthesis/open-questions.md).
