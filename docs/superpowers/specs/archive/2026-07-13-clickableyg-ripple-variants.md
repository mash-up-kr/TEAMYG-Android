---
id: clickableyg-ripple-variants
title: clickableYG 리플 변형 세트 + ygScaleRipple
status: implemented
category: behavior-spec
platforms: android
verified: 2026-07-15
related_code: core:util:android clickable/ — clickableYGDimRipple, clickableYGScaleRipple, clickableYGMergeRipple, ygScaleRipple
related_adr: ADR-0010
related_spec: clickableyg-throttle, ygripple
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: clickableYG 리플 변형 세트 + ygScaleRipple

- 대상: `core:util:android` — `clickable/` (2026-07-14 `core:designsystem`에서 이동)
- 관련: [[2026-07-12-clickableyg-throttle|clickableYG]](코어 throttle) · [[2026-07-13-ygripple|ygDimRipple]] · [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md) · [design-system](../../../architecture/design-system.md) · 이슈 #94

## 목표
clickableYG(중복 클릭 throttle)에 **리플 종류별 공개 변형**을 제공한다: dim ripple / scale(누르면 축소) / 둘 병합(merge). scale 효과용 `ygScaleRipple` `IndicationNodeFactory`를 신설(skt `ScaleNodeFactory` 포팅).

> **구현 방식 갱신(Approach 2, 2026-07-14)** — 최초 "모두 non-`@Composable`, 커스텀 Node가 다중 delegate" 설계였으나, 접근성 패리티(focus/키/hover) 확보를 위해 [[2026-07-12-clickableyg-throttle|throttle 스펙]]에서 **`Modifier.clickable` 위 throttle 래핑 + `@Composable`(remember)**으로 재설계됨. 변형은 여전히 얇은 팩토리(리플 리스트만 다름).

## 범위
- **포함**: `ygScaleRipple` 신설. 코어 throttle modifier를 `indications: List<Indication>` 수용으로 전환. 공개 변형 `clickableYGDimRipple`·`clickableYGScaleRipple`·`clickableYGMergeRipple`·`clickableYG`(=Dim 위임). `YGRipple.kt`를 `YGDimRipple.kt`/`YGScaleRipple.kt`로 분리.
- **제외**: 리플 색·scaleValue 토큰화(과도기, 후속), 추가 리플 종류, 기존 clickable 사용처 마이그레이션.

## API / 인터페이스

### ygScaleRipple (`YGScaleRipple.kt`)
```kotlin
@Stable
fun ygScaleRipple(
    scaleEnabled: Boolean = true,
    scaleValue: Float = 0.98f,
): IndicationNodeFactory
```
- Press 시 컨텐츠를 `scaleValue`로 축소(tween 150ms `FastOutSlowInEasing`), Release/Cancel 시 `1f`로 복귀(spring `MediumBouncy`/`StiffnessMedium`).

### 코어 (internal, `YGClickable.kt`)
```kotlin
internal fun Modifier.clickableYGThrottle(
    interactionSource: MutableInteractionSource? = null,
    indications: List<Indication>,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier
```
- 기존 clickableYG Node를 `indication: Indication?` 단일 → **`indications: List<Indication>`** 로 전환. Node가 리스트의 각 `IndicationNodeFactory`를 **자기 `interactionSource`에 delegate**(다중 delegate).

### 공개 변형 (`YGClickable.kt`, 모두 plain fun)
```kotlin
fun Modifier.clickableYGDimRipple(interactionSource: MutableInteractionSource? = null, enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null, windowMillis: Long = 300L, onClick: () -> Unit): Modifier
fun Modifier.clickableYGScaleRipple(/* 동일 파라미터 */): Modifier
fun Modifier.clickableYGMergeRipple(/* 동일 파라미터 */): Modifier
fun Modifier.clickableYG(/* 동일 파라미터 */): Modifier
```
- `clickableYGDimRipple` → `clickableYGThrottle(indications = listOf(ygDimRipple()), ...)`.
- `clickableYGScaleRipple` → `listOf(ygScaleRipple())`.
- `clickableYGMergeRipple` → `listOf(ygDimRipple(), ygScaleRipple())`.
- `clickableYG` → `clickableYGDimRipple(...)` 위임(기본 진입점).
- 공통 파라미터: `interactionSource`(기본 null → Node 자체 생성), `enabled`/`onClickLabel`/`role`/`windowMillis`(300)/`onClick`. `indication`은 변형이 고정하므로 공개 시그니처에서 노출 안 함.
- 🔁 **2026-08-04 as-built(PR #192)** — 변형이 **5종**이 됐다. `clickableYGNoRipple`(리플 없음, `interactionSource` 파라미터 없음)이 추가되고 코어가 `indications: List<Indication>?` **nullable**을 받는다(`null` → `indication = null`로 `clickable`에 전달). 이 변형은 `YGScreen` 배경 탭 포커스 해제용으로 만들어졌다가 그 결선이 철회돼 **사용처 0으로 머지**됐다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-03] · [clearfocusontap 스펙](2026-08-03-clearfocusontap-modifier.md).

## 동작 / 구조
- **다중 indication 합성**: (Approach 2, 2026-07-14) 코어는 커스텀 노드가 아니라 표준 `Modifier.clickable` 위에 얹는다. `indications`를 `toYGIndication()`으로 단일 `Indication?`으로 접어(다중이면 자식을 `onAttach`서 `delegate`하는 `YGCompositeIndicationNodeFactory`) `clickable(indication = …)`에 전달. focus/키/hover는 `clickable`이 제공. 상세 [[2026-07-12-clickableyg-throttle|throttle 스펙]] "구조(Approach 2)".
- **merge draw 순서**: dim(리플 draw)과 scale(`DrawModifierNode`로 컨텐츠 scale)의 delegate 순서가 draw 레이어링에 영향 가능 → `ygDimRipple()` 먼저, `ygScaleRipple()` 나중 delegate를 기본으로 하되 **기기 육안으로 확정**(리플이 축소 콘텐츠 위/아래 어디 그려지는지).
- **throttle**: [[2026-07-12-clickableyg-throttle|clickableYG]] 코어 그대로(leading-edge, `TimeSource.Monotonic`). 상태는 `remember`된 `YGClickThrottleGate`(`lastMark`)가 `clickable`의 `onClick`을 감싸 판정. `enabled`/press/focus/키/hover는 `clickable`이 처리.
- **@Composable 근거**: `Modifier.clickable` 위에 얹으려면 throttle 게이트·fallback source를 `remember`로 유지해야 함 → 변형·코어 모두 `@Composable` Modifier 확장. (초기 non-composable 목표는 `clickable`이 이미 Node 기반이라 성능 근거 소멸.)

### ygScaleRipple 구조 (`YGScaleRipple.kt`)
- `YGScaleNodeFactory(scaleEnabled, scaleValue) : IndicationNodeFactory` — `create` → `DelegatingYGScaleRippleNode`, `equals`/`hashCode`(파라미터 기반).
- `DelegatingYGScaleRippleNode : DelegatingNode, DrawModifierNode` — `onAttach`서 `interactionSource.interactions` 수집해 `Animatable` 애니메이트, `draw()`서 `scale(animatable.value) { drawContent() }`. `scaleEnabled=false`면 애니메이션·scale 없이 `drawContent()`.

## 파일 구성 (`core:util:android` `clickable/`)
- `YGDimRipple.kt` — 기존 `YGRipple.kt`를 리네임. `ygDimRipple` + `YGDimRippleAlpha` + `YGDimRippleNodeFactory` + `DelegatingYGDimRippleNode`.
- `YGScaleRipple.kt` — 신설. `ygScaleRipple` + `YGScaleNodeFactory` + `DelegatingYGScaleRippleNode`.
- `YGClickable.kt` — 코어 `internal clickableYGThrottle`(indications: List, 🔁 2026-08-04 nullable) + 공개 `clickableYG`·`clickableYGDimRipple`·`clickableYGScaleRipple`·`clickableYGMergeRipple`(+ 🔁 2026-08-04 `clickableYGNoRipple`).

## 주의 / 열린 질문
- **merge delegate 순서**: draw 레이어링 정답은 기기 확인 후 확정(dim↔scale 순서).
- **테마 비의존 리터럴**: 리플 색(`YGDimRippleColor = Color(0xFF29292C)`)·`scaleValue`(0.98)가 토큰 아닌 리터럴. `core:util:android`가 designsystem 테마 비의존이라 색은 리터럴 고정 또는 호출측 주입 → [open-questions](../../../synthesis/open-questions.md).
- **기존 스펙 관계**: [[2026-07-12-clickableyg-throttle]](코어 throttle)·[[2026-07-13-ygripple]](ygDimRipple)의 API 일부(단일 `indication`, `YGRipple.kt` 파일명)를 이 스펙이 갱신. 구현 시 두 스펙의 해당 부분 동기화.
- **검증**: compile + ktlint + 기기 육안(dim 리플/scale 축소/merge 동시). 정적 프리뷰로 리플·애니메이션 안 보임.
