---
id: ygdangerzone
title: 위험 구역 컨테이너 (YGDangerZone)
status: superseded
category: ui-spec
platforms: android
verified: 2026-07-18
related_code: YGDangerZone.kt#YGDangerZone, YGHorizontalDivider.kt#YGHorizontalDivider, YGActionItem.kt#YGActionItem
related_adr: ADR-0010
related_spec: yghorizontaldivider, ygactionitem
related_architecture: design-system
supersedes:
superseded_by: ygdangerzone-dashed
tags: [spec, parfait, designsystem]
---

# Spec: 위험 구역 컨테이너 (YGDangerZone)

- 대상: `core:designsystem` — `component/ygdangerzone/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · [yghorizontaldivider](2026-07-12-yghorizontaldivider.md) · [ygactionitem](2026-07-12-ygactionitem.md) · PR #148(`feature/#136-etc-component`)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
> 🔁 [2026-07-22] **superseded** — 점선 재설계 [2026-07-19-ygdangerzone-dashed](2026-07-19-ygdangerzone-dashed.md)가 PR #159로 develop 머지되며 이 solid 채움 baseline을 대체. 현행 YGDangerZone(점선 테두리)은 dashed 스펙 참조.

## 목표
두 개의 슬롯(위/아래)을 구분선으로 나눠 담는 반투명 컨테이너. 설정 화면의 "로그아웃 / 서비스 탈퇴" 같은 위험 액션 묶음이 대표 유스케이스. 자식 컴포넌트는 호출자가 주입(대개 `YGActionItem`).

## 범위
- 포함: 상/하 슬롯 배치 + 사이 구분선(`YGHorizontalDivider`), 배경·radius 토큰 매핑, `IntrinsicSize.Max` 폭.
- 제외:
  - 슬롯 내용 — 호출자 소유. 컴포넌트는 `@Composable` 람다 두 개만 받는다(내용·개수·의미 미규정).
  - 액션 동작(로그아웃·탈퇴 등) — 슬롯에 넣는 컴포넌트(예: `YGActionItem`)의 `onClick` 소관.
  - 슬롯 개수 가변 — **상/하 2슬롯 고정**(N슬롯 미지원).

## API / 인터페이스
```kotlin
@Composable
fun YGDangerZone(
    topZone: @Composable () -> Unit,
    bottomZone: @Composable () -> Unit,
    modifier: Modifier = Modifier,
)
```
- `topZone` / `bottomZone`: 구분선 위/아래 슬롯. 호출자 주입.
- `modifier`: 루트 배치용. 기본 `Modifier`.

## 동작 / 상태
- Stateless presentational. 내부 상태 없음.
- 세로 `Column`, **폭은 `IntrinsicSize.Max`** — 슬롯 중 넓은 쪽에 맞춰 컨테이너 폭 결정.
- 슬롯 사이에만 구분선 1개(상단/하단 테두리 없음).

## 레이아웃 / 토큰 매핑 (심볼명)
| 요소 | 토큰 / 값 |
|------|-----------|
| 루트 배경 | `YGAtomicColors.Transparency.Black5` |
| 루트 radius | `YGTheme.shapes.radius.medium1` |
| 루트 폭 | `IntrinsicSize.Max` |
| 구분선 | `YGHorizontalDivider(color = YGAtomicColors.Transparency.White25)` |
| 구분선 좌우 패딩 | `YGTheme.layout.padding.padding6` |

## 파일 구성
- `core/designsystem/.../component/ygdangerzone/YGDangerZone.kt` — public `YGDangerZone`.
- 프리뷰: `@Preview` + `YGCustomTheme`(검정 배경 위에 `YGActionItem` 2개 조합).

## 주의 / 열린 질문
- **원자 색 직접 참조(과도기)**: 배경·구분선 색이 `YGAtomicColors.Transparency.*` 직접 참조(시맨틱 슬롯 미경유). 설계 전반의 과도기 패턴과 동일 → [design-system](../../../architecture/design-system.md) · [open-questions](../../../synthesis/open-questions.md).
- **프리뷰 방식**: `@Preview`+`YGCustomTheme` 계열(그룹 폴더 `etc/`의 `@YGPreview`/`PreviewBox`와 상이) — 표준화 미확정.
