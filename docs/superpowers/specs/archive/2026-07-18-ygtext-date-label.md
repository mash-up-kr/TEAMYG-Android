---
id: ygtext-date-label
title: 텍스트 프리셋 (YGDate / YGLabel)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-23
related_code: YGDate.kt#YGDate, YGLabel.kt#YGLabel
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: 텍스트 프리셋 (YGDate / YGLabel)

- 대상: `core:designsystem` — `component/ygtext/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · PR #150(`feature/design-system-component-colorchip`)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 🔁 **YGDate 재설계 반영(#149 develop 머지, 2026-07-23)**: `YGDate(text)` 단일 텍스트 → `YGDate(date, day)` 2텍스트 `Row`(테두리 포함)로 변경, 패딩도 하드코딩 → 레이아웃 토큰화. 아래는 as-built 기준. `YGLabel`은 불변.

## 목표
자주 쓰는 텍스트 스타일 두 종을 미리 묶은 얇은 래퍼. 타이포·색을 매번 지정하지 않도록 프리셋으로 노출한다.
- `YGDate`: 파르페 날짜 라벨(날짜 + 요일, 예: `December 31 (Wed)`).
- `YGLabel`: 보조 라벨(폼 필드 라벨 등).

## 범위
- 포함: `Text` + 고정 타이포/색 매핑, 프리뷰.
- 제외: 문자열 생성·포매팅(날짜 포맷·요일 계산 등) — 호출자 소유. 완성 문자열만 받는다.

## API / 인터페이스
```kotlin
@Composable
fun YGDate(date: String, day: String, modifier: Modifier = Modifier)

@Composable
fun YGLabel(text: String, modifier: Modifier = Modifier)
```
- `YGDate`: `date`(날짜 문자열) + `day`(요일 문자열, 컴포넌트가 `(day)`로 감쌈). 완성 문자열 주입.
- `YGLabel`: `text`(표시 문자열).
- `modifier`: 기본 `Modifier`.

## 레이아웃 / 토큰 매핑 (심볼명)
- **`YGDate`**: `Row` — `border(0.75.dp, YGAtomicColors.Gray.Gray800)` + 패딩 세로 `YGTheme.layout.padding.padding3` / 가로 `padding4`(토큰화).
  - `date` 텍스트: `body.b01R` · `YGAtomicColors.Gray.Gray800`.
  - `day` 텍스트(`"(" + day + ")"`): `body.b01R` · `YGAtomicColors.Gray.Gray300`, 앞 패딩 `padding.padding3`. (`modifier`는 이 day 텍스트에 위임됨.)

| 컴포넌트 | 타이포 | 색 | 패딩 |
|----------|--------|-----|------|
| `YGLabel` | `YGTheme.typography.body.b02R` | `YGAtomicColors.Gray.Gray400` | 없음(`modifier` 위임) |

## 파일 구성
- `component/ygtext/YGDate.kt` — public `YGDate`. 프리뷰 `@YGPreview` + `YGCustomTheme`.
- `component/ygtext/YGLabel.kt` — public `YGLabel`. 프리뷰 `@YGPreview` + `PreviewBox`.

## 주의 / 열린 질문
- **`YGDate` 패딩 토큰화 완료(#149)**: 구 하드코딩(`horizontal 12dp / vertical 8dp`) → `YGTheme.layout.padding.*`로 승격. v0.1의 "패딩 하드코딩 열린 질문" **해소**.
- **`YGDate` 테두리 색/두께 리터럴**: `border(0.75.dp, Gray800)` — 두께 `0.75.dp`가 `SizeTokens`/`shapes` 미경유 리터럴(토큰화 여지).
- **원자 색 직접 참조(과도기)**: `YGAtomicColors.Gray.*` 직접 참조(시맨틱 미경유).
- **`modifier` 위임 위치**: `YGDate`의 `modifier`가 루트 `Row`가 아니라 `day` 텍스트에 붙어 있음(관례상 루트 위임과 어긋남 — 정리 여지).
