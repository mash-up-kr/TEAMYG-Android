---
id: yghorizontaldivider
title: YGHorizontalDivider Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: yghorizontaldivider
related_code: YGHorizontalDivider
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGHorizontalDivider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `core:designsystem`에 피그마 Divider 스펙의 1dp 수평 구분선 `YGHorizontalDivider`를 구현한다.

**Architecture:** 자식 없는 `Spacer`에 `Modifier.fillMaxWidth().height(thickness).background(color)`를 얹은 최소 leaf 컴포넌트. 상태·분기 없음. 두께·색은 토큰 기본값(`SizeTokens.Size1`, `YGAtomicColors.Gray.Gray100`) + named 파라미터 오버라이드. YGButton·YGTextField 컴포넌트 작성 컨벤션(component/<name>/ 배치, `@YGPreview`/`PreviewBox`)을 따른다.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `Spacer`/`background`/layout), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/2026-07-12-yghorizontaldivider.md](../../specs/archive/2026-07-12-yghorizontaldivider.md)

## Global Constraints
- 대상 repo: `TJYG-Android`, 브랜치 `feature/#136-etc-component`.
- 패키지: `com.teamyg.parfait.core.designsystem.component.etc`.
- 크기는 `SizeTokens.*.getDp()`, 색은 `YGAtomicColors.*`(gray-100은 시맨틱 슬롯 없어 원자색 직접 참조 — YGButton·YGTextField 선례, 과도기 → [open-questions](../../../../wiki/open-questions.md)).
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`compileReleaseKotlin`) + `ktlintMainSourceSetCheck` + `@Preview` 육안**으로 대체.
- ktlint 엄격(단일 표현식 함수 한 줄). 커밋 전 `ktlintMainSourceSetFormat`.

---

### Task 1: YGHorizontalDivider — 컴포저블 본체 + Preview

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/etc/YGHorizontalDivider.kt` (현재 빈 스텁을 채운다)

**Interfaces:**
- Consumes: `SizeTokens.Size1`(theme.size), `YGAtomicColors.Gray.Gray100`(theme.colors), `PreviewBox`·`@YGPreview`(utils.preview).
- Produces: public `@Composable fun YGHorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = SizeTokens.Size1.getDp(), color: Color = YGAtomicColors.Gray.Gray100)`.

- [ ] **Step 1: 본체 작성** — 스텁 파일 전체를 아래로 교체.

```kotlin
package com.teamyg.parfait.core.designsystem.component.etc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Composable
fun YGHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = SizeTokens.Size1.getDp(),
    color: Color = YGAtomicColors.Gray.Gray100,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}
```

- [ ] **Step 2: Preview 작성** — 같은 파일 하단에 추가. 기본선 + 두께/색 오버라이드 예시를 세로 스택으로.

```kotlin
@YGPreview
@Composable
private fun YGHorizontalDividerPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("default (1dp, gray-100)")
        YGHorizontalDivider()
        Text("thick (4dp)")
        YGHorizontalDivider(thickness = SizeTokens.Size4.getDp())
        Text("custom color")
        YGHorizontalDivider(color = YGAtomicColors.Gray.Gray400)
    }
}
```

추가 import (Step 1 import에 병합):

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 프리뷰 육안 확인** — Android Studio에서 `YGHorizontalDividerPreview` 렌더 확인. default가 얇은 1dp 회색선, thick/custom color가 각각 반영되는지.

- [ ] **Step 6: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/etc/YGHorizontalDivider.kt
git commit -m "feat: YGHorizontalDivider 구현"
```

---

## Self-Review
- **Spec coverage**: 목표(1dp 수평선)·API(modifier/thickness/color 기본값)·동작(Spacer + fillMaxWidth + height + background)·범위 제외(Colors 홀더·vertical variant 없음)·파일(단일 `component/etc/YGHorizontalDivider.kt`)·과도기(gray-100 원자색 직접) — Task 1에 대응.
- **Placeholder**: 없음(본체 코드 전량 기재). 프리뷰 thick 예시만 토큰 존재 조건부(참고 노트로 대체 지시 명시).
- **Type consistency**: `SizeTokens.Size1.getDp()`·`YGAtomicColors.Gray.Gray100`·`Dp`·`Color`·`Modifier` 모두 스펙·코드 심볼과 일치.

## 열린 질문
- gray-100 원자색 직접 참조 → 시맨틱 정리 대상([open-questions](../../../../wiki/open-questions.md), YGButton 디자인 토큰 항목과 동일 성격).
