---
id: ygdatebutton
title: 날짜 버튼 (YGDateButton)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-18
related_code: YGDateButton.kt#YGDateButton
related_adr: ADR-0010
related_spec: clickableyg-throttle
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: 날짜 버튼 (YGDateButton)

- 대상: `core:designsystem` — `component/ygdatebutton/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · [clickableyg-throttle](2026-07-12-clickableyg-throttle.md) · PR #147(`feature/#146-date-button`)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표
캘린더 그리드의 단일 날짜 셀. 선택(selected)·오늘(today)·비활성(disabled)·기본 4상태를 배경/테두리/글자색/타이포로 구분한다. 캘린더 컴포넌트(위키 [[캘린더-컴포넌트]] / C-201) 안에서 쓰인다.

## 범위
- 포함: 4상태 시각 분기, 클릭 콜백, 토큰 매핑, 프리뷰.
- 제외:
  - 날짜 계산·selected/today 판정 — 호출자(캘린더) 소유. 컴포넌트는 boolean 플래그만 받는다.
  - 그리드 배치·요일 헤더 — 상위 캘린더 소관.

## API / 인터페이스
```kotlin
@Composable
fun YGDateButton(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```
- `text`: 날짜 숫자 문자열. 호출자 주입.
- `isSelected` / `isToday` / `isEnabled`: 상태 플래그(외부 주입). 우선순위 `!isEnabled` > `isSelected` > `isToday` > 기본.
- `onClick`: 클릭 콜백(`isEnabled=false`면 비활성).
- `modifier`: 크기 지정 등(프리뷰 `size(44.dp)`).

## 동작 / 상태
- Stateless presentational. 상태는 3개 boolean prop 조합, `when` 분기로 색/타이포 결정.
- 원형(`YGTheme.shapes.radius.round`) `Box` 중앙 정렬 `Text`.

### 상태 → 토큰 매핑 (심볼명)
| 상태 | 배경 | 테두리 | 텍스트 색 | 타이포 |
|------|------|--------|-----------|--------|
| disabled(`!isEnabled`) | `Gray.Transparent` | `Gray.Transparent` | `Gray.Gray400` | `body.b02R` |
| selected | `Gray.Gray900` | `Gray.Transparent` | `Gray.White` | `body.b02SB` |
| today | `Gray.Transparent` | `Gray.Gray850` | `Gray.Gray950` | `body.b02R` |
| 기본 | `Gray.Transparent` | `Gray.Transparent` | `Gray.Gray800` | `body.b02R` |

(색은 `YGAtomicColors.*`, 타이포는 `YGTheme.typography.*`)

### 고정 토큰
| 요소 | 토큰 / 값 |
|------|-----------|
| 셀 패딩 | `SizeTokens.Size6.getDp()` |
| radius | `YGTheme.shapes.radius.round` |
| 테두리 두께 | `1.dp` |

## 파일 구성
- `component/ygdatebutton/YGDateButton.kt` — public `YGDateButton`.
- 프리뷰: `@Preview` + `YGCustomTheme`(기본/선택/오늘/비활성 4종).

## 주의 / 열린 질문
- **⚠️ `clickableYG` 미사용(규약 이탈)**: 클릭을 표준 `Modifier.clickable(indication = null, ...)` + `semantics { role = Role.Button }`로 직접 구현 — `core:util:android`의 중복 클릭 leading-throttle 유틸(`clickableYG`) 미적용. 다른 상호작용형 컴포넌트(YGButton·YGIconButton·YGActionItem·YGChipButton)와 스로틀 규약 불일치, 빠른 연타 방어 없음. → [open-questions](../../../synthesis/open-questions.md).
- **원자 색 직접 참조(과도기)**: 전 상태 색이 `YGAtomicColors.Gray.*` 직접 참조.
- **today 배경/테두리 미세**: today는 배경 투명 + `Gray.Gray850` 테두리로만 구분(선택과 대비). Figma 육안 확인 대상.
