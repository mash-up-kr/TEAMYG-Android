---
id: clickableyg-throttle
title: clickableYG — Node 기반 중복 클릭 방지 Modifier
status: implemented
category: behavior-spec
platforms: android
verified: 2026-07-15
related_code: core:util:android clickable/YGClickable.kt — Modifier.clickableYG
related_adr: ADR-0010
related_spec: ygripple
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: clickableYG — Node 기반 중복 클릭 방지 Modifier

- 대상: `core:util:android` — `clickable/YGClickable.kt` (2026-07-14 `core:designsystem`에서 이동 — 테마 비의존 범용 clickable 유틸로 재배치)
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md) · [design-system](../../../architecture/design-system.md) · [[2026-07-13-ygripple|ygDimRipple]](기본 indication) · 이슈 #94

## 목표
연타·중복 탭으로 `onClick`이 여러 번 발화하는 문제를 막는 재사용 clickable modifier `Modifier.clickableYG`를 만든다. 커스텀 `Modifier.Node`로 구현해 `composed{}` 오버헤드 없이 리스트 등 광범위 재사용에 적합하게 한다. (프로젝트 첫 `Modifier.Node`.)

## 범위
- **포함**: leading-edge throttle 클릭 게이트를 가진 `Modifier.clickableYG`, 이를 뒷받침하는 `ModifierNodeElement` + `Modifier.Node`. **접근성 패리티**(아래).
- **제외**: 기존 `clickable` 사용처의 `clickableYG` 마이그레이션(후속), trailing debounce 시맨틱(버튼엔 leading throttle이 맞음).

## 접근성 패리티 (필수 — PR 리뷰 P1, 2026-07-13)
`clickableYG`는 **앱 전역 `Modifier.clickable` 대체**가 목표다(네이밍대로). 따라서 표준 `clickable`이 기본 제공하는 상호작용을 모두 갖춰야 하며, 초기 구현에서 누락된 아래 3종은 **회귀 방지를 위해 반드시 구현**한다(기존 "터치 전용이라 수용"은 앱 전역 스코프로 무효):
- **focusable**: 키보드/DPAD 포커스 대상이 되고 `FocusInteraction`을 `interactionSource`에 emit.
- **하드웨어 키 활성화**: 포커스 상태에서 Enter/Spacebar/DPAD-center 키로 `onClick`(throttle 게이트 경유) 발화.
- **hover**: 포인터/마우스 hover 시 `HoverInteraction.Enter/Exit` emit(hover indication 반응).

> **구현 방식 (Approach 2 확정, 2026-07-14)** — focus/키/hover를 손으로 재구현(clickable 재발명)하지 않고 **표준 `Modifier.clickable` 위에 throttle을 얹는다.** 3종 접근성은 `clickable`에서 그대로 확보. 구조:
> - `clickableYG`·변형은 **`@Composable` Modifier 확장**으로 전환(`remember`로 상태 유지 — 초기 non-composable 목표는 `clickable`이 이미 Node 기반이라 근거 소멸).
> - throttle: `remember`된 게이트 객체(`lastMark: TimeSource.Monotonic.ValueTimeMark`)가 `onClick`을 감싸 leading-edge 통과 판정. 탭·키보드·TalkBack 모든 활성화 경로가 이 단일 `onClick`을 지나 throttle이 균일 적용.
> - `interactionSource`는 `clickable`에 그대로 전달. null이면 `clickable`이 내부에서 source를 lazy 생성(indication이 `IndicationNodeFactory`라 가능) — YG 자체 fallback 없음.
> - 커스텀 리플: 리스트를 단일 `Indication`으로 접어 `clickable(indication=…)`에 전달(size 1은 그대로, 다중은 자식 delegate하는 합성 `IndicationNodeFactory`).
> - `ClickableYGElement`/`ClickableYGNode`(커스텀 노드)는 제거. (TalkBack 시맨틱은 `clickable`이 role/onClickLabel로 제공 — 회귀 없음.)

## 동작
- **leading-edge throttle**: 첫 클릭은 **즉시** `onClick` 실행. 이후 `windowMillis` 이내의 클릭은 무시. 창이 지나면 다음 클릭이 다시 즉시 통과.
- **시간원**: **`kotlin.time.TimeSource.Monotonic`**(stdlib) — monotonic 보장(경과시간 정확), `android.os`·kotlinx.datetime 의존 없음, 테스트 시 `TimeSource` 대체 가능. wall clock(`System.currentTimeMillis()`·kotlinx.datetime `Clock.System.now()`)은 시계 점프 위험으로 **사용 금지**.
- **게이트 상태**: 마지막 통과 시각을 `TimeSource.Monotonic.ValueTimeMark`(`lastMark`)로 **`Modifier.Node` 인스턴스에 보관**(composition 재구성과 무관하게 유지). 게이트: `lastMark == null || lastMark.elapsedNow() >= window`면 통과 후 `lastMark = TimeSource.Monotonic.markNow()`. `window`는 내부 `Duration`(파라미터 `windowMillis`를 `.milliseconds`로 변환).
- **enabled=false**: 클릭·시맨틱 액션 무반응. `onPress`도 진입 시 live `enabled` 체크(`if (!enabled) return`)로 press interaction을 emit하지 않음 → **ripple도 안 뜸**(clickable disabled와 동일). `enabled`를 이벤트 시점에 읽으므로 `resetPointerInputHandler()` 불필요.

## API / 인터페이스

> **갱신(2026-07-13)** — 최초 이 스펙은 단일 `indication: Indication?` 파라미터를 받았으나, [[2026-07-13-clickableyg-ripple-variants|리플 변형 스펙]]에서 **`indication` 파라미터를 제거**하고 리플 종류별 공개 변형(`clickableYGDimRipple`/`clickableYGScaleRipple`/`clickableYGMergeRipple`)으로 대체했다. 코어는 `indications: List<Indication>`을 받는 `internal clickableYGThrottle`. **공개 API의 정본은 변형 스펙 참조.** 아래는 현재 코드 기준 코어 진입점만.

```kotlin
// 공개 진입점 — indication 파라미터 없음(변형이 리플 고정). 상세는 리플 변형 스펙. @Composable.
@Composable
fun Modifier.clickableYG(
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier   // → clickableYGDimRipple(...) 위임

// 코어(internal) — indication을 리스트로 수용, clickable 위에 throttle
@Composable
internal fun Modifier.clickableYGThrottle(
    interactionSource: MutableInteractionSource? = remember { MutableInteractionSource() },
    indications: List<Indication>,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier
```
- `interactionSource`: `clickable`에 그대로 전달. null이면 `clickable`이 내부 `MutableInteractionSource`를 lazy 생성. (throttle 파라미터 default `= remember { MutableInteractionSource() }`는 공개 변형이 항상 `interactionSource`를 명시 전달하므로 실제로는 안 터지는 형식상 값 — 실질 fallback은 `clickable`이 담당.)
- `indications`(코어): 코어가 받는 리플 목록. 각 `IndicationNodeFactory`를 노드가 자기 source에 delegate(다중). 기본 리플 주입은 변형 함수가 담당(`clickableYGDimRipple` → `listOf(ygDimRipple())` 등).
- 🔁 **2026-08-04 as-built(PR #192)**: 코어 시그니처가 `indications: List<Indication>?`로 **nullable화**됐고(`null` → `indication = null`), 파라미터 순서도 `indications`가 선두·`interactionSource`가 `onClick` 직전이다(위 코드블록은 초안 순서). 리플 없는 변형 `clickableYGNoRipple`(사용처 0)이 이 nullable 경로의 유일한 소비자 → [리플 변형 스펙](2026-07-13-clickableyg-ripple-variants.md) · [open-questions](../../../synthesis/open-questions.md) [2026-08-03].
- `windowMillis`: throttle 창(기본 300ms). 화면별 조정 가능.
- `onClick`: 게이트를 통과한 탭에서만 호출.

## 구조 (Approach 2 — `clickable` 위 throttle)
커스텀 `Modifier.Node`(초기 `ClickableYGElement`/`ClickableYGNode`)는 **제거**. 대신:
- **`clickable` 위임**: `clickableYGThrottle`가 `this.clickable(interactionSource = interactionSource, indication = …, enabled, onClickLabel, role) { if (gate.tryPass(windowMillis)) onClick() }`. focus·하드웨어 키·hover·press emit·시맨틱은 전부 `clickable`이 제공.
- **throttle 게이트**: `private class YGClickThrottleGate` — `lastMark: TimeSource.Monotonic.ValueTimeMark?` 보유, `tryPass(windowMillis)`가 `lastMark == null || elapsedNow() >= window`면 `markNow()` 후 `true`. `remember { YGClickThrottleGate() }`로 recomposition 무관하게 유지. 탭·키보드·TalkBack 모든 활성화가 이 단일 `onClick`을 지나 균일 throttle.
- **interactionSource**: `clickable(interactionSource = interactionSource)`로 그대로 전달. null이면 `clickable`이 내부 source를 lazy 생성(indication이 `IndicationNodeFactory`). YG 코드에 별도 fallback `remember` 없음.
- **인디케이션**: `List<Indication>`을 `toYGIndication()`으로 단일 `Indication?`으로 접음 — 비면 `null`, 1개면 그대로, 다중이면 자식들을 `onAttach`에서 `delegate`하는 `internal YGCompositeIndicationNodeFactory`(`equals`/`hashCode`는 `factories` 리스트 기반). 이를 `clickable(indication = …)`에 전달.
- **@Composable**: 위 `remember` 때문에 `clickableYG`·변형·`clickableYGThrottle`는 `@Composable` Modifier 확장이다.

> Compose BOM `2026.06.00`(`androidx.compose.foundation.clickable`) 기준. `:core:util:android:compileReleaseKotlin` + ktlint 통과 확인(2026-07-14).

## 표시·제어 규칙
- 게이트 통과 조건: `enabled` **AND** (`lastMark == null` **OR** `lastMark.elapsedNow() >= window`).
- 통과 시에만 `lastMark` 갱신·`onClick` 발화.

## 파일 구성 (`core:util:android`)
- `clickable/YGClickable.kt`(패키지 `com.teamyg.parfait.core.util.android.clickable`) — `@Composable` public `Modifier.clickableYG(...)` + 변형 3종 + `@Composable internal clickableYGThrottle(indications: List)` + `private class YGClickThrottleGate`(throttle 상태) + `private List<Indication>.toYGIndication()` + `internal YGCompositeIndicationNodeFactory`/`private YGCompositeIndicationNode`(다중 리플 합성). 커스텀 `ModifierNodeElement`/`Modifier.Node`는 없음(clickable 위임). 같은 패키지 `clickable/`의 리플 파일 [[2026-07-13-ygripple|YGDimRipple.kt]]·`YGScaleRipple.kt`와 함께 위치.
- (이력) 초기 `core:ui`의 `utils/extensions/Modifier.kt` stub → 리베이스에서 `core:designsystem utils/clickable/`로 이동(ygDimRipple 기본값 주입 위해) → **2026-07-14 `core:util:android clickable/`로 재이동**. 재이동 시 테마 의존(`YGAtomicColors`)을 끊고 ripple 기본색을 리터럴(`YGDimRippleColor = Color(0xFF29292C)`)로 바꿔 designsystem 비의존 유틸로 만듦. `core:util:android`에 `parfait.jetpack.compose` 플러그인 추가.

## 주의 / 열린 질문
- **접근성 패리티(P1, 미구현 → 반영 필요)**: 초기 구현은 focus/하드웨어 키/hover 누락(터치·TalkBack 시맨틱만). 앱 전역 대체 스코프 확정으로 위 "접근성 패리티" 3종을 구현해야 함. 구현 방식(node delegate vs `clickable` 위 throttle 래핑)은 plan에서 compile로 확정.
- **검증 한계**: throttle 타이밍은 유닛 테스트 인프라 부재(Compose UI)로 compile + ktlint + `@Preview`/기기 연타 육안으로 확인. 정밀 타이밍 테스트는 별도.
- **첫 Modifier.Node**: 프로젝트에 Node 선례 없음. 성공 시 이후 커스텀 modifier의 참조 패턴이 됨(아키텍처 결정화되면 ADR 검토).
- **indication 기본값 = ygDimRipple**: 변형 함수가 리플을 고정(`clickableYGDimRipple` → `listOf(ygDimRipple())` 등). `ygDimRipple`은 같은 모듈(`core:util:android clickable/`)에 있어 별도 wrapper 불필요.
- **테마 비의존(재이동 결과)**: `core:util:android`는 `core:designsystem`을 의존하지 않으므로 ripple 색을 테마(`YGAtomicColors`)에서 읽지 못한다. 기본색을 리터럴 `YGDimRippleColor = Color(0xFF29292C)`로 둠. 시맨틱 토큰화하려면 색을 호출측(designsystem 컴포넌트)에서 파라미터로 주입해야 함 → [open-questions](../../../synthesis/open-questions.md).
