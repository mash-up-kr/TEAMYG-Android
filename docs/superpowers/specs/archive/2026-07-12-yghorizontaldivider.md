---
id: yghorizontaldivider
title: YGHorizontalDivider
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-12
related_code: core:designsystem component/etc/ YGHorizontalDivider
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: YGHorizontalDivider

- 대상: `core:designsystem` — `component/etc/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md)(컴포넌트 작성 규약) · 이슈 #136

## 목표
피그마 Divider 스펙의 1px 수평 구분선을 `core:designsystem` 컴포넌트로 만든다. 리스트·섹션 사이 경계를 그리는 최소 컴포넌트.

## 범위
- **포함**: 부모 가로폭을 채우는 1dp 수평선, 두께·색 오버라이드.
- **제외**: `YGVerticalDivider`(필요 시 후속, YAGNI), `Colors`/`Defaults` 색 홀더(1px 선엔 과함 — 직접 파라미터로 대체), inset/들여쓰기 variant(피그마에 없음).

## API / 인터페이스
```kotlin
@Composable
fun YGHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = SizeTokens.Size1.getDp(),
    color: Color = YGAtomicColors.Gray.Gray100,
)
```
- `modifier`: 기본은 `fillMaxWidth`로 부모 가로를 채움. 호출측이 `Modifier.width(...)` 등으로 폭 제한 가능.
- `thickness`: 선 높이. 기본 `SizeTokens.Size1.getDp()`(1dp, 피그마 border 1px).
- `color`: 선 색. 기본 `YGAtomicColors.Gray.Gray100`(피그마 `Color/GrayScale/gray-100`).

## 동작 / 상태
- 런타임 상태 없음(정적 컴포넌트). 상태별 색·분기 없음.
- **구현**: `Spacer` + `Modifier.fillMaxWidth().height(thickness).background(color)`. 자식 없는 leaf라 divider 의도에 정확히 부합(`Box` 대비 content 슬롯 없음).
- 피그마 `Height 0px + Border 1px`는 실질 1px 선 → `height(thickness)`로 표현. 피그마 `Center alignment`는 border 정렬 속성이라 `Spacer` 채움 구현엔 해당 없음(N/A).

## 표시·제어 규칙
- 조건부 노출·입력 제어 없음. 항상 렌더.

## 파일 구성 (`component/etc/`)
- `YGHorizontalDivider.kt` — public `YGHorizontalDivider` 컴포저블 + `@YGPreview`/`PreviewBox` 프리뷰(기본선 + 두께/색 오버라이드 예시).

## 주의 / 열린 질문
- **과도기**: `gray-100`은 시맨틱 슬롯(`YGTheme.colorScheme`)에 없어 `YGAtomicColors.Gray.Gray100` 직접 참조 — YGButton·YGTextField 선례와 동일. 디자인 토큰 규칙 확정 시 시맨틱 정리 대상 → [open-questions 2026-07-10 YGButton 디자인 토큰](../../../../wiki/open-questions.md).
- `YGVerticalDivider`가 필요해지면 별도 스펙 없이 이 파일에 대칭 추가 검토(축만 다름).
