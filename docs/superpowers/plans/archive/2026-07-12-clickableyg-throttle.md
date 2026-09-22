---
id: clickableyg-throttle
title: clickableYG (Node throttle) Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-15
platforms: android
owner:
related_adr:
related_spec: clickableyg-throttle
related_code: YGClickable.kt#clickableYG
archived_reason:
tags: [plan, parfait, designsystem]
---

# clickableYG (Node throttle) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `core:designsystem`에 연타·중복 클릭을 막는 `Modifier.clickableYG`를 커스텀 `Modifier.Node`(leading-edge throttle)로 구현한다. (초기 `core:ui`에 구현 후 리베이스에서 `core:designsystem`으로 이동 — `ygDimRipple` 기본 indication 주입 위해 같은 모듈 필요.)

**Architecture:** `Modifier.clickableYG` 팩토리가 `ClickableYGElement`(`ModifierNodeElement`)를 붙이고, `ClickableYGNode`(`DelegatingNode`)가 (1) delegated `SuspendingPointerInputModifierNode`로 탭 감지 + press/release 인터랙션 emit, (2) `IndicationNodeFactory` 인디케이션 delegate, (3) `SemanticsModifierNode`로 role/onClick 시맨틱을 담당. throttle 상태(`lastMark: TimeSource.Monotonic.ValueTimeMark?`)를 노드에 보관하고 `elapsedNow() >= window`일 때만 통과. `composed{}` 미사용.

**Tech Stack:** Kotlin, Jetpack Compose foundation/ui(Modifier.Node, foundation `1.11.0` / BOM `2026.06.00`), `kotlin.time.TimeSource.Monotonic`(stdlib).

**Spec:** [specs/2026-07-12-clickableyg-throttle.md](../../specs/archive/2026-07-12-clickableyg-throttle.md)

> **구현 후 갱신** — 아래 Task 1 코드블록은 최초 설계(단일 `indication`, 커스텀 `ClickableYGNode`)의 스냅샷이다. 이후:
> - (2026-07-13) [리플 변형 플랜](2026-07-13-clickableyg-ripple-variants.md)에서 `indications: List` + 공개 변형 4종으로 대체.
> - (2026-07-14, **PR 리뷰 P1**) 접근성 패리티(focus/하드웨어 키/hover) 확보 위해 **커스텀 노드를 버리고 표준 `Modifier.clickable` 위에 throttle 게이트를 얹는 방식(Approach 2)으로 재설계** — `clickableYG`·변형이 `@Composable`로 전환, `ClickableYGElement`/`ClickableYGNode` 제거. compile+ktlint 통과.
> - (2026-07-14) **`core:designsystem` → `core:util:android` `clickable/`로 이동** — 테마 의존 끊고 ripple 기본색 리터럴화(`YGDimRippleColor`), util:android에 compose 플러그인 추가. 아래 코드블록의 패키지·경로(`core.designsystem.utils.clickable`)는 옛 위치.
>
> **현재 코드 기준은 [throttle 스펙](../../specs/archive/2026-07-12-clickableyg-throttle.md) "구조(Approach 2)"·"파일 구성" 참조.** 아래 코드블록은 역사 스냅샷.

## Global Constraints
- 대상 repo: `TJYG-Android`, 브랜치 `feature/#94-solve-duplicate-clickable-issue`.
- 패키지: `com.teamyg.parfait.core.designsystem.utils.clickable`.
- 시간원 `kotlin.time.TimeSource.Monotonic`만(monotonic). `System.currentTimeMillis()`·kotlinx.datetime `Clock` 금지(wall clock 점프).
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`:core:designsystem:compileReleaseKotlin`) + `:core:designsystem:ktlintMainSourceSetCheck` + 기기 연타 육안**으로 대체. Node API 시그니처는 **compile이 최종 판정**(foundation 1.11.0 기준 아래 코드 작성, 불일치 시 컴파일 에러 따라 최소 보정).
- ktlint 엄격. 커밋 전 `ktlintMainSourceSetFormat`.
- public API: `clickableYG` 시그니처(= 기존 stub + `windowMillis`) 확정.

---

### Task 1: clickableYG 팩토리 + ClickableYGElement + ClickableYGNode

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/utils/clickable/YGClickable.kt` (초기 `core/ui/.../utils/extensions/Modifier.kt` stub 교체 후 리베이스에서 이 경로로 이동)

**Interfaces:**
- Consumes: `ModifierNodeElement`, `DelegatingNode`, `SemanticsModifierNode`(ui.node), `SuspendingPointerInputModifierNode`(ui.input.pointer), `detectTapGestures`(foundation.gestures), `Indication`/`IndicationNodeFactory`/`MutableInteractionSource`/`PressInteraction`(foundation), `TimeSource`(kotlin.time).
- Produces: public `fun Modifier.clickableYG(interactionSource: MutableInteractionSource? = null, indication: Indication? = ygDimRipple(), enabled: Boolean = true, onClickLabel: String? = null, role: Role? = null, windowMillis: Long = 300L, onClick: () -> Unit): Modifier`. (`ygDimRipple`은 같은 패키지 [[2026-07-13-ygripple|YGRipple.kt]].)

- [ ] **Step 1: 파일 전체 교체** — 스텁을 아래로 교체.

```kotlin
package com.teamyg.parfait.core.designsystem.utils.clickable

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

fun Modifier.clickableYG(
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = ygDimRipple(),
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    windowMillis: Long = 300L,
    onClick: () -> Unit,
): Modifier = this then ClickableYGElement(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    windowMillis = windowMillis,
    onClick = onClick,
)

private data class ClickableYGElement(
    val interactionSource: MutableInteractionSource?,
    val indication: Indication?,
    val enabled: Boolean,
    val onClickLabel: String?,
    val role: Role?,
    val windowMillis: Long,
    val onClick: () -> Unit,
) : ModifierNodeElement<ClickableYGNode>() {
    override fun create(): ClickableYGNode = ClickableYGNode(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        windowMillis = windowMillis,
        onClick = onClick,
    )

    override fun update(node: ClickableYGNode) {
        node.update(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            windowMillis = windowMillis,
            onClick = onClick,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clickableYG"
        properties["enabled"] = enabled
        properties["windowMillis"] = windowMillis
        properties["role"] = role
        properties["onClickLabel"] = onClickLabel
    }
}

private class ClickableYGNode(
    private var interactionSource: MutableInteractionSource?,
    private var indication: Indication?,
    private var enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    private var windowMillis: Long,
    private var onClick: () -> Unit,
) : DelegatingNode(), SemanticsModifierNode {

    private var lastMark: TimeSource.Monotonic.ValueTimeMark? = null

    // interactionSource 미지정 시 내부 생성(노드 수명 동안 안정).
    private var ownSource: MutableInteractionSource? = null
    private val source: MutableInteractionSource
        get() = interactionSource ?: ownSource ?: MutableInteractionSource().also { ownSource = it }

    private var indicationNode: DelegatableNode? = null

    // 탭 감지: 핸들러가 노드의 가변 필드(onClick/enabled/windowMillis)를 매 이벤트마다 읽으므로
    // 파라미터 변경 시 핸들러 재설정(resetPointerInputHandler) 불필요. delegate 부수효과만 필요해
    // 반환 참조는 보관하지 않고 init 블록에서 호출(unused val 회피).
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapGestures(
                    onPress = { offset ->
                        if (!enabled) return@detectTapGestures
                        val press = PressInteraction.Press(offset)
                        source.emit(press)
                        val released = tryAwaitRelease()
                        source.emit(
                            if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                        )
                    },
                    onTap = { performClick() },
                )
            },
        )
    }

    override fun onAttach() {
        attachIndication()
    }

    private fun attachIndication() {
        val current = indicationNode
        if (current != null) {
            undelegate(current)
            indicationNode = null
        }
        val ind = indication
        if (ind is IndicationNodeFactory) {
            indicationNode = delegate(ind.create(source))
        }
    }

    private fun performClick() {
        if (!enabled) return
        val mark = lastMark
        if (mark == null || mark.elapsedNow() >= windowMillis.milliseconds) {
            lastMark = TimeSource.Monotonic.markNow()
            onClick()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this@ClickableYGNode.role?.let { role = it }
        onClick(label = onClickLabel) {
            performClick()
            true
        }
        if (!enabled) disabled()
    }

    fun update(
        interactionSource: MutableInteractionSource?,
        indication: Indication?,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        windowMillis: Long,
        onClick: () -> Unit,
    ) {
        val indicationChanged = this.indication != indication || this.interactionSource != interactionSource
        val semanticsChanged = this.enabled != enabled || this.role != role || this.onClickLabel != onClickLabel

        this.interactionSource = interactionSource
        this.indication = indication
        this.enabled = enabled
        this.onClickLabel = onClickLabel
        this.role = role
        this.windowMillis = windowMillis
        this.onClick = onClick

        if (indicationChanged) attachIndication()
        if (semanticsChanged) invalidateSemantics()
    }
}
```

- [ ] **Step 2: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL`
불일치 발생 시(예: `SuspendingPointerInputModifierNode` 팩토리 시그니처, `detectTapGestures` 파라미터, `undelegate` 이름) — 컴파일 에러 메시지에 맞춰 **동일 의미 유지**하며 심볼만 보정(스펙 동작 불변). foundation `1.11.0` 기준.

- [ ] **Step 3: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 기기 연타 육안 확인** — `clickableYG`를 임시 버튼(예: 카운터 증가)에 붙여 빠르게 연타. 첫 탭만 반영되고 300ms 내 추가 탭 무시, 창 이후 다시 반영. ripple(터치 피드백)이 정상 표시. TalkBack 더블탭 활성화 동작. (임시 확인 코드는 커밋 제외.)

- [ ] **Step 5: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/utils/clickable/YGClickable.kt
git commit -m "feat: 중복 클릭 방지 clickableYG modifier (Node throttle)"
```

---

## Self-Review
- **Spec coverage**: 목표(중복 클릭 방지)·동작(leading throttle, monotonic TimeSource, 노드 상태 lastMark, enabled 게이팅)·API(시그니처 + windowMillis 기본 300)·구조(Element/Node, delegated pointer-input·indication·semantics)·범위 밖(키보드/hover 미구현 — 이 코드도 미포함)·파일(단일 Modifier.kt) — Task 1에 대응.
- **Placeholder**: 없음(파일 전량 코드). Step 2 노트의 "심볼 보정"은 API 시그니처 확정 지시(동작 불변)이지 미완성 코드 아님.
- **Type consistency**: `ClickableYGElement`↔`ClickableYGNode` 생성자·`update()` 파라미터 순서·타입 동일. `performClick()`가 탭·시맨틱 양쪽에서 동일 게이트 경유. `source` 접근자가 pointer/indication에서 동일 인스턴스 보장.

## 열린 질문
- **Node API 시그니처 확정**: `SuspendingPointerInputModifierNode` 팩토리·`detectTapGestures(onPress,onTap)`·`undelegate`/`invalidateSemantics` 존재는 foundation 1.11.0 표준으로 작성. Step 2 컴파일이 최종 확인.
- **legacy Indication**: `IndicationNodeFactory`가 아닌 구형 `Indication`은 delegate 경로 미지원(ripple 등 현행은 모두 NodeFactory라 실사용 무영향). 필요 시 후속.
- **키보드/hover**: 미구현(스펙 수용 범위).
- **disabled ripple 차단**: `onPress` 진입에서 live `enabled` 체크(`if (!enabled) return@detectTapGestures`) → disabled 시 press interaction emit 안 함(ripple 없음)·클릭 없음. `enabled`를 이벤트 시점에 읽으므로 `resetPointerInputHandler()` 불필요. 누르는 중 enabled 전환 엣지는 미차단(과함, 수용).
