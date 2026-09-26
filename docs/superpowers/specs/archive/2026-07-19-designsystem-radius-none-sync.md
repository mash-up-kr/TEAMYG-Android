---
id: designsystem-radius-none-sync
title: radius none(0) 토큰 추가 + 컴포넌트 각진 corner 동기화
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-22
related_code: YGShapeRadius.kt#none, YGShapesDefaults.kt#radius0, YGTextFieldDefaults.kt#colors, YGTextFieldImpl.kt#commonShape, YGInviteCard.kt#YGInviteCard, YGButtonType.kt#SmallSquare
related_adr: ADR-0010
related_spec: ygtextfield, yginvitecard, ygdangerzone-dashed
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: radius none(0) 토큰 추가 + 컴포넌트 각진 corner 동기화

- 대상: `core:designsystem` — `theme/shapes/`, `component/textfield`, `component/card`, `component/ygbutton`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md) · [design-system](../../../architecture/design-system.md)(radius 토큰 목록) · [ygtextfield](2026-07-10-ygtextfield.md) · [yginvitecard](2026-07-14-yginvitecard.md) · [ygdangerzone-dashed](2026-07-19-ygdangerzone-dashed.md)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처.
> ✅ **develop 머지 완료**(PR #159 `feature/sync-design-system-260719`, 2026-07-22). `radius.none`(=RectangleShape) 토큰 신설 + YGTextField·YGInviteCard·YGButtonType SmallSquare 각짐 sync 모두 코드 반영. 아래 "이전(baseline)" 열은 머지 전 값.

## 목표
피그마 갱신 sync — radius 스케일에 **`none`(0, 각진 corner)** 토큰을 신설하고, 각진 처리가 필요한 컴포넌트를 이 토큰으로 통일. 함께 YGTextField 배경색을 반투명→불투명 흰색으로 정정.

## 범위 (커밋 단위)
| 커밋 | 대상 | 변경 | 이전(baseline) |
|------|------|------|----------------|
| `feat: radius 0 추가` | `YGShapeRadius` / `YGShapesDefaults` | `none: Shape` 필드 추가, `radius0 = RectangleShape` 매핑 | (토큰 없음) |
| `refactor: sync YGTextField` | `YGTextFieldDefaults.colors` | `backgroundColor` = `colorScheme.grayScale.white` | `colorScheme.transparency.white75` |
| ″ | `YGTextFieldImpl` | `commonShape` = `radius.none` | `radius.small` |
| `refactor: sync YGInviteCard` | `YGInviteCard` | 테두리 `shape` = `radius.none`, 초기엔 `.clip(radius.medium1)` 유지 | 테두리 `shape` = `radius.medium1` |
| `refactor: YGInviteCard clip shape 수정` | `YGInviteCard` | `.clip` = `radius.none` (테두리와 통일, 비대칭 해소) | (직전 `.clip(radius.medium1)`) |
| `refactor: sync YGInviteCard` | `InviteCodeBox` | `clip` = `radius.none` | `clip` = `radius.small` |
| `refactor: sync YGButtonType SmallSquare` | `YGButtonType.SmallSquare` | `radius` = `radius.none` | `radius.small` |

## 토큰 정의 (`theme/shapes/`)
```kotlin
// YGShapeRadius: 필드 추가 (스케일 최소단)
data class YGShapeRadius(val none: Shape, val xSmall: Shape, /* ... */)
// YGShapesDefaults
private val radius0: Shape = RectangleShape   // 각진 corner
// 매핑: none = radius0
```
- `radius.none` = `RectangleShape`(반경 0). 스케일: **none** → xSmall(4) → small(8) → medium1(12) → … → round(Circle).

## 주의 / 열린 질문
- ~~**YGInviteCard 테두리/클립 비대칭**: 테두리 `radius.none` vs clip `radius.medium1`.~~ → **해소**(커밋 `refactor: YGInviteCard clip shape 수정`): `.clip`도 `radius.none`으로 통일, 카드 전체 각짐.
- **`none` 시맨틱 vs 하드코딩 `RectangleShape`**: 각진 처리를 토큰(`radius.none`)으로 승격 → 컴포넌트 내 `RectangleShape` 직접 참조 대신 테마 경유. 신규 컴포넌트도 각짐 필요 시 `radius.none` 사용 권장.
- **YGButton 전용 스펙 없음**: SmallSquare radius 변경은 [design-system](../../../architecture/design-system.md)(`YGButtonType` 변형 서술)이 유일 문서 지점. 값 변동은 소스(`YGButtonType.kt`)가 단일 출처.
- **아카이브 스펙 마커**: baseline 스펙([ygtextfield](2026-07-10-ygtextfield.md)·[yginvitecard](2026-07-14-yginvitecard.md))의 "진행 중" 마커는 본 스펙 머지 반영으로 "머지됨(#159)"으로 갱신함(before→after는 위 범위 표가 단일 출처).
