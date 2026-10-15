---
id: ygtextformfield
title: YGTextFormField Implementation Plan
status: done
type: work-order
created: 2026-07-10
updated: 2026-07-12
platforms: android
owner:
related_adr:
related_spec: ygtextformfield
related_code: YGTextFormFieldColors, YGTextFormFieldDefaults
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGTextFormField Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** [YGTextField](2026-07-10-ygtextfield.md) 아래 errorDescription(에러/안내 텍스트)을 얹은 `YGTextFormField`를 `core:designsystem`에 추가한다.

**Architecture:** 기존 `internal YGTextFieldImpl`을 재사용하고 그 아래 `errorDescription`을 `Column`으로 얹는 얇은 래퍼. 필드 로직은 재구현하지 않는다. 색은 전용 `YGTextFormFieldColors`(필드용 `textFieldColors` + `errorDescriptionColor`)로 묶고 `YGTextFormFieldDefaults.colors()`가 기본값(errorDescription = error danger) 제공. errorDescription 색은 항상 error 단일(일반 헬퍼 슬롯 없음).

**Tech Stack:** Kotlin, Jetpack Compose(foundation/layout, material3 `Text`), 자체 테마(`YGTheme.*`).

**Spec:** [specs/2026-07-10-ygtextformfield.md](../../specs/archive/2026-07-10-ygtextformfield.md)

## Global Constraints
- 대상 repo: `TJYG-Android`, 브랜치 `feature/#134-text-field-form`.
- 패키지: `com.teamyg.parfait.core.designsystem.component.textfield`.
- 테마 값은 `YGTheme.*`로만 접근.
- **무변경**: `YGTextField.kt`·`YGTextFieldColors.kt`·`YGTextFieldDefaults.kt`. `YGTextFieldImpl`(internal) 재사용만.
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`compileReleaseKotlin`) + `ktlintMainSourceSetCheck` + `@YGPreview` 육안**으로 대체.
- 커밋 전 `ktlintMainSourceSetFormat`.

---

### Task 1: YGTextFormFieldColors + Defaults — 전용 색 홀더

**Files:**
- Create: `.../component/textfield/YGTextFormFieldColors.kt`
- Create: `.../component/textfield/YGTextFormFieldDefaults.kt`

**Interfaces:**
- Consumes: `YGTextFieldColors`, `YGTextFieldDefaults.colors()`, `YGTheme.colorScheme.danger`.
- Produces: `data class YGTextFormFieldColors(textFieldColors: YGTextFieldColors, errorDescriptionColor: Color)` + `object YGTextFormFieldDefaults { @Composable @ReadOnlyComposable fun colors(...): YGTextFormFieldColors }`.

- [ ] **Step 1: YGTextFormFieldColors.kt 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGTextFormFieldColors(
    val textFieldColors: YGTextFieldColors,
    val errorDescriptionColor: Color,
)
```

- [ ] **Step 2: YGTextFormFieldDefaults.kt 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.YGTheme

object YGTextFormFieldDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        textFieldColors: YGTextFieldColors = YGTextFieldDefaults.colors(),
        errorDescriptionColor: Color = YGTheme.colorScheme.danger,
    ): YGTextFormFieldColors = YGTextFormFieldColors(
        textFieldColors = textFieldColors,
        errorDescriptionColor = errorDescriptionColor,
    )
}
```

- [ ] **Step 3: 컴파일** — BUILD SUCCESSFUL.

### Task 2: YGTextFormField — 필드 + description 래퍼

**Files:**
- Create: `.../component/textfield/YGTextFormField.kt`

**Interfaces:**
- Consumes: `YGTextFieldImpl(..., colors: YGTextFieldColors)`(internal), `YGTextFormFieldColors.{textFieldColors, errorDescriptionColor}`(Task 1), `YGTextFormFieldDefaults.colors()`, `YGTheme.{layout.gap.gap2, typography.caption.c01R}`, `PreviewBox`·`@YGPreview`.
- Produces: `@Composable fun YGTextFormField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", enabled: Boolean = true, isError: Boolean = false, maxLength: Int? = null, errorDescription: String? = null, colors: YGTextFormFieldColors = YGTextFormFieldDefaults.colors())`.

- [ ] **Step 1: 컴포저블 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGTextFormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLength: Int? = null,
    errorDescription: String? = null,
    colors: YGTextFormFieldColors = YGTextFormFieldDefaults.colors(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
    ) {
        YGTextFieldImpl(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            enabled = enabled,
            isError = isError,
            maxLength = maxLength,
            colors = colors.textFieldColors,
        )
        if (errorDescription != null) {
            Text(
                text = errorDescription,
                style = YGTheme.typography.caption.c01R,
                color = colors.errorDescriptionColor,
            )
        }
    }
}
```

- [ ] **Step 2: 프리뷰 작성** (같은 파일 하단)

```kotlin
@YGPreview
@Composable
private fun YGTextFormFieldPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("with description")
        YGTextFormField(
            value = "Text",
            onValueChange = {},
            maxLength = 15,
            errorDescription = "닉네임은 15자까지만 입력 가능해요",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("error with description")
        YGTextFormField(
            value = "Text",
            onValueChange = {},
            isError = true,
            maxLength = 15,
            errorDescription = "닉네임은 15자까지만 입력 가능해요",
            modifier = Modifier.fillMaxWidth(),
        )
        Text("no description")
        YGTextFormField(
            value = "",
            onValueChange = {},
            placeholder = "Text를 입력해 주세요",
            maxLength = 15,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 프리뷰 육안 확인** — Android Studio에서 `YGTextFormFieldPreview` 3종(with description / error(빨강 description) / no description) 렌더 확인. error description이 danger 색인지 확인.

- [ ] **Step 6: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFormField.kt \
        core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFormFieldColors.kt \
        core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFormFieldDefaults.kt
git commit -m "feat: YGTextFormField 추가 (YGTextField + description + 전용 colors)"
```

---

## Self-Review
- **Spec coverage**: 목표(errorDescription 얹기)·API(`errorDescription` 파라미터, 전용 `YGTextFormFieldColors`)·구성(Column+YGTextFieldImpl 재사용)·errorDescription 색(error 단일 슬롯)·타이포(caption.c01R)·간격(gap2)·파일(3종 신설)·무변경 원칙 — Task 1·2에 대응.
- **Placeholder**: 없음(코드 전량 기재).
- **Type consistency**: `YGTextFieldImpl(colors: YGTextFieldColors)`·`colors.textFieldColors`·`colors.errorDescriptionColor`·`caption.c01R`·`layout.gap.gap2` 모두 코드 심볼과 일치.

## 열린 질문
- errorDescription 색은 error 단일(`errorDescriptionColor`)로 확정. 일반(비에러) 헬퍼용 `descriptionColor` 슬롯·resolver는 미도입 — 필요 시 후속 추가.
