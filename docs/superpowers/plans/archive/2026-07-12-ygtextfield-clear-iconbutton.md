---
id: ygtextfield-clear-iconbutton
title: YGTextField clear → YGIconButton 교체 Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: ygtextfield-clear-iconbutton
related_code: YGTextFieldImpl.kt
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGTextField clear → YGIconButton 교체 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `YGTextFieldImpl`의 clear 버튼 인라인 `Box`+`Image`(`// TODO Change IconButton`)를 공통 컴포넌트 `YGIconButton`으로 교체하고, 잉여가 된 `clearIconTint`를 색 홀더에서 제거한다.

**Architecture:** clear 블록을 `YGIconButton(iconResource, size = SIZE_44, contentDescription, onClick)` 한 줄로 치환(크기·기본 tint·아이콘·라벨·onClick 무손실 매핑). 그 뒤 `YGTextFieldColors`/`YGTextFieldDefaults`에서 더 이상 참조되지 않는 `clearIconTint` 필드·파라미터를 제거. `YGTextFormField`는 `YGTextFieldImpl`에 위임하므로 무편집 자동 반영.

**Tech Stack:** Kotlin, Jetpack Compose(자체 테마 [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)), `YGIconButton`([[2026-07-12-ygiconbutton|spec]]).

**Spec:** [specs/2026-07-12-ygtextfield-clear-iconbutton.md](../../specs/archive/2026-07-12-ygtextfield-clear-iconbutton.md)

## Global Constraints
- 대상 repo: `TJYG-Android`(별도 repo). 현재 브랜치에 `YGTextFieldImpl`·`YGIconButton` 모두 존재.
- 패키지: `com.teamyg.parfait.core.designsystem.component.textfield`. `YGIconButton`은 `...component.ygiconbutton`.
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`:core:designsystem:compileReleaseKotlin`) + `ktlintMainSourceSetCheck` + `@Preview` 육안**으로 대체.
- ktlint 엄격(미사용 import 검출). 커밋 전 `ktlintMainSourceSetFormat`(미사용 import 자동 제거).
- public API(`YGTextField`/`YGTextFormField` 시그니처) 불변. 내부 리팩터.

---

### Task 1: YGTextFieldImpl clear 블록을 YGIconButton으로 교체

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldImpl.kt`

**Interfaces:**
- Consumes: `YGIconButton(@DrawableRes iconResource: Int, size: YGIconButtonSize, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, interactionSource: MutableInteractionSource = ..., isEnabled: Boolean = true)`, `YGIconButtonSize.SIZE_44`(둘 다 `...component.ygiconbutton`).
- Produces: 없음(내부 구현 변경, 시그니처 불변).

- [ ] **Step 1: clear 블록 교체** — `if (showClear) { ... }` 내부의 인라인 `Box`+`Image`를 `YGIconButton` 호출로 치환.

교체 전:
```kotlin
                if (showClear) {
                    // TODO Change IconButton
                    Box(
                        modifier = Modifier
                            .clickable(role = Role.Button) { onValueChange("") }
                            .size(SizeTokens.Size44.getDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_close_round),
                            contentDescription = "clear",
                            colorFilter = ColorFilter.tint(colors.clearIconTint),
                            modifier = Modifier.size(SizeTokens.Size24.getDp()),
                        )
                    }
                }
```

교체 후:
```kotlin
                if (showClear) {
                    YGIconButton(
                        iconResource = R.drawable.ic_close_round,
                        size = YGIconButtonSize.SIZE_44,
                        contentDescription = "clear",
                        onClick = { onValueChange("") },
                    )
                }
```

- [ ] **Step 2: import 정리** — clear 전용이라 이제 미사용이 된 import 제거, `YGIconButton`·`YGIconButtonSize` 추가.

제거(모두 clear 블록 전용이었음):
```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
```

추가:
```kotlin
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
```

유지(타 사용처 있음): `androidx.compose.ui.Alignment`(Row `verticalAlignment` 2곳), `...theme.size.SizeTokens`(border `Size1`), `...R`(`ic_close_round`).

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL` (이 시점에 `colors.clearIconTint`는 더 이상 참조되지 않지만 필드는 남아있어 정상 컴파일)

- [ ] **Step 4: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL` (Format이 잔여 미사용 import 자동 정리)

- [ ] **Step 5: 프리뷰 육안 확인** — Android Studio에서 `YGTextField`/`YGTextFormField` 프리뷰. clear 아이콘(우측 X)이 값 입력 시 표시, 탭 시 값 비워짐, **누르는 순간 아이콘 tint가 진해짐(pressed Gray400 피드백, 신규)** 확인. 평상 외형은 기존과 동일(Gray300).

- [ ] **Step 6: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldImpl.kt
git commit -m "refactor: YGTextField clear 버튼 YGIconButton으로 교체"
```

---

### Task 2: clearIconTint 제거 (YGTextFieldColors / YGTextFieldDefaults)

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldColors.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldDefaults.kt`

**Interfaces:**
- Consumes: 없음.
- Produces: `YGTextFieldColors`에서 `clearIconTint` 필드 소멸(호출부 영향: 현재 `YGTextFieldDefaults.colors()` 기본 생성 외 직접 지정 사용처 없음. `YGTextFormFieldDefaults.colors()`는 `YGTextFieldDefaults.colors()`를 인자 없이 호출하므로 영향 없음).

- [ ] **Step 1: YGTextFieldColors에서 필드 제거** — data class 본문에서 아래 줄 삭제.

```kotlin
    val clearIconTint: Color,
```

(다른 필드·메서드는 그대로. `clearIconTint`는 파생 메서드가 없어 본문 로직 영향 없음.)

- [ ] **Step 2: YGTextFieldDefaults에서 파라미터·할당 제거** — `colors()`의 파라미터 목록에서

```kotlin
        clearIconTint: Color = YGAtomicColors.Gray.Gray300,
```

와 생성자 호출부에서

```kotlin
        clearIconTint = clearIconTint,
```

를 삭제. (`YGAtomicColors` import는 `borderColor`·`textColor` 등 타 기본값이 계속 사용하므로 유지.)

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL` (Task 1에서 `clearIconTint` 참조를 이미 제거했으므로 남은 참조 없음)

- [ ] **Step 4: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 프리뷰 재확인** — `YGTextField`/`YGTextFormField` 프리뷰가 여전히 정상 렌더(clear 표시·색 동일). 회귀 없음 확인.

- [ ] **Step 6: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldColors.kt core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldDefaults.kt
git commit -m "refactor: 미사용 clearIconTint 제거"
```

---

## Self-Review
- **Spec coverage**: 목표(clear→YGIconButton)·범위(Impl 교체 + clearIconTint 제거, YGIconButton/YGListItem 불변)·무손실 매핑 표(SIZE_44/Gray300/아이콘/라벨/onClick)·동작 변화(pressed Gray400 신규)·파일 구성(Impl·Colors·Defaults)·FormField 무편집 반사 반영 — Task 1·2에 대응.
- **Placeholder**: 없음(교체 전/후 코드 전량 기재, 제거 import 명시).
- **Type consistency**: `YGIconButton`·`YGIconButtonSize.SIZE_44` 시그니처는 [[2026-07-12-ygiconbutton|YGIconButton spec]]·구현과 일치. `R.drawable.ic_close_round`·`onValueChange("")`·`contentDescription = "clear"`는 기존 코드 심볼 그대로.
- **순서 안전성**: Task 1(참조 제거) → Task 2(필드 제거) 순서라 각 Task 종료 시점 모두 컴파일 성립.

## 열린 질문
- `contentDescription = "clear"` 하드코딩 문자열 유지(리소스화는 범위 밖).
- YGListItem trailing 아이콘 동일 교체는 별도 후속(현재 브랜치 미포함).
- `isEnabled` 미지정(기본 true) — `showClear = enabled && ...`로 disabled 시 애초에 미표시라 무관.
