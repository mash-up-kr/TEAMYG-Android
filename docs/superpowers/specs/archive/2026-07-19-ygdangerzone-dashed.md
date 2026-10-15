---
id: ygdangerzone-dashed
title: YGDangerZone 점선 재설계 + 점선 프리미티브 (dashedBorder · YGHorizontalDashedDivider)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-22
related_code: YGDangerZone.kt#YGDangerZone, DashedBorder.kt#dashedBorder, YGHorizontalDashedDivider.kt#YGHorizontalDashedDivider
related_adr: ADR-0010
related_spec: ygdangerzone, yghorizontaldivider, ygactionitem
related_architecture: design-system
supersedes: ygdangerzone
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGDangerZone 점선 재설계 + 점선 프리미티브

- 대상: `core:designsystem` — `component/ygdangerzone/`, `border/`, `component/etc/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · [ygdangerzone(구 solid baseline, superseded)](2026-07-18-ygdangerzone.md) · [yghorizontaldivider(solid 원본)](2026-07-12-yghorizontaldivider.md)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
> ✅ **develop 머지 완료**(PR #159 `feature/sync-design-system-260719`, 2026-07-22). 코드=설계 일치. 이 스펙이 구 solid 채움 baseline [2026-07-18-ygdangerzone.md](2026-07-18-ygdangerzone.md)를 supersede.

## 목표
피그마 Danger-Zone 갱신 반영 — 반투명 **채움 박스 → 점선 테두리 박스**, **solid 구분선 → 점선 구분선**으로 시각 재설계. 재설계 과정에서 재사용 점선 프리미티브 2종을 신설한다.

## 범위
- 포함:
  - `YGDangerZone` 루트: 배경 fill 제거 → 점선 테두리(`dashedBorder()`), 세로 패딩 추가.
  - 신규 `dashedBorder()` Modifier 확장(`border/DashedBorder.kt`) — 점선 사각형 테두리.
  - 신규 `YGHorizontalDashedDivider`(`component/etc/`) — 점선 수평 구분선. 기존 solid `YGHorizontalDivider` 대체 사용(DangerZone 한정).
- 제외:
  - 슬롯 내용·개수 가변·액션 동작 — 기존 [ygdangerzone](2026-07-18-ygdangerzone.md) 스펙과 동일(상/하 2슬롯 고정, 호출자 주입).
  - solid `YGHorizontalDivider` 제거 — 유지(타 사용처 보존).

## API / 인터페이스

### dashedBorder Modifier (`border/DashedBorder.kt`)
```kotlin
fun Modifier.dashedBorder(
    color: Color = YGAtomicColors.Gray.Gray100,
    stroke: Dp = SizeTokens.Size1.getDp(),
    dash: Dp = SizeTokens.Size4.getDp(),
): Modifier
```
- `drawBehind` + `drawRoundRect(style = Stroke(pathEffect = dashPathEffect(dash, dash)))` — 사각형 4변 점선 테두리.
- stroke의 절반만큼 안쪽으로 inset(`topLeft = stroke/2`, `size = 크기 - stroke`)해 테두리 클리핑 방지.
- 기본값: gray-100(`#ECECEE`), 1dp, dash 4,4 (피그마 `Border 1px Dashed / Dashes 4,4 / gray-100`).

### YGHorizontalDashedDivider (`component/etc/`)
```kotlin
@Composable
fun YGHorizontalDashedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = SizeTokens.Size1.getDp(),
    dash: Dp = SizeTokens.Size4.getDp(),
    color: Color = YGAtomicColors.Gray.Gray100,
)
```
- `Canvas` + `drawLine(pathEffect = dashPathEffect(dash, dash))` — 가로 **선 하나**. `fillMaxWidth().height(thickness)`.
- ⚠️ `dashedBorder()`(사각형)로 구현하지 않는다: 높이 1dp에 rect stroke를 씌우면 위·아래 변이 겹쳐 두꺼워짐. 구분선은 단일 `drawLine`이 정답(피그마 `Divider Height 0px` = 선 하나).

### YGDangerZone 루트 (변경분)
```kotlin
Column(
    modifier = modifier
        .width(IntrinsicSize.Max)
        .dashedBorder()
        .padding(vertical = YGTheme.layout.padding.padding2),
) { topZone(); YGHorizontalDashedDivider(...); bottomZone() }
```

## 레이아웃 / 토큰 매핑 (심볼명)
| 요소 | 토큰 / 값 | 이전(baseline) |
|------|-----------|----------------|
| 루트 배경 | **없음(fill 제거)** | `YGAtomicColors.Transparency.Black5` |
| 루트 테두리 | `dashedBorder()` = gray-100·1dp·dash 4,4 | (없음, radius medium1 채움만) |
| 루트 세로 패딩 | `YGTheme.layout.padding.padding2` | (없음) |
| 루트 폭 | `IntrinsicSize.Max` | `IntrinsicSize.Max`(동일) |
| 구분선 | `YGHorizontalDashedDivider`(gray-100 점선) | `YGHorizontalDivider(White25)` |
| 구분선 좌우 패딩 | `YGTheme.layout.padding.padding6` | `padding6`(동일) |

## modifier 체이닝 규칙 (재설계 핵심)
- `dashedBorder()`는 **draw modifier**(`drawBehind`) — 체인 상 그 위치의 layout 크기에 그린다.
- 순서 **`dashedBorder().padding()`**: 테두리는 바깥 bounds, 패딩이 내용물을 안으로 inset → 피그마 구조(테두리 최외곽 → 안쪽 패딩).
- 뒤집으면(`padding().dashedBorder()`) 테두리가 패딩 안쪽에 그려져 상하로 밀림 → 잘못.

## 파일 구성
- `core/designsystem/.../border/DashedBorder.kt` — public `Modifier.dashedBorder`.
- `core/designsystem/.../component/etc/YGHorizontalDashedDivider.kt` — public `YGHorizontalDashedDivider` + `@YGPreview`/`PreviewBox`.
- `core/designsystem/.../component/ygdangerzone/YGDangerZone.kt` — 루트 modifier 체인 교체 + `YGHorizontalDashedDivider` 사용.

## 주의 / 열린 질문
- **피그마 대비 미반영 델타(머지 후 잔존)**: 코드는 `vertical = padding2` + `IntrinsicSize.Max`로 머지됨. 피그마는 상하 padding-2 **+ 좌우 gap-5**, 폭 `Fixed 335px`. 좌우 gap-5·고정 폭 미반영 상태로 머지 → [open-questions](../../../synthesis/open-questions.md) [2026-07-22] 등록.
- **원자 색 직접 참조(과도기)**: `gray-100`을 `YGAtomicColors.Gray.Gray100` 직접 참조(시맨틱 슬롯 미경유). YGButton·YGHorizontalDivider 선례와 동일 → [design-system](../../../architecture/design-system.md) · [open-questions](../../../synthesis/open-questions.md).
- **프리뷰 방식**: `YGDangerZone`·`YGHorizontalDashedDivider` 모두 `@YGPreview`+`PreviewBox`로 통일 머지(#158 프리뷰 표준 정합).
- **점선 프리미티브 재사용 범위**: 현재 DangerZone 전용. 타 컴포넌트 확산 시 `dash` 토큰화·시맨틱 색 정리 재검토.
- **supersede**: 이 스펙이 구 solid 채움 [2026-07-18-ygdangerzone.md](2026-07-18-ygdangerzone.md)를 대체(frontmatter `supersedes: ygdangerzone`).
