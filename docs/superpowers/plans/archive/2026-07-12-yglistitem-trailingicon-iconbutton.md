---
id: yglistitem-trailingicon-iconbutton
title: YGListItem trailing 아이콘 → YGIconButton 교체 Implementation Plan
status: done
type: work-order
created: 2026-07-12
updated: 2026-07-12
platforms: android
owner:
related_adr: ADR-0010
related_spec: yglistitem-trailingicon-iconbutton
related_code: YGListItem.kt#YGListItem
archived_reason: 구현 완료
tags: [plan, parfait, designsystem]
---

# YGListItem trailing 아이콘 → YGIconButton 교체 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `YGListItem`의 trailing 아이콘 인라인 `Box`+`Image`(`// TODO IconButton 컴포넌트`)를 공통 컴포넌트 `YGIconButton`으로 교체하고, 잉여가 된 `trailingIconColor` 파라미터를 제거한다.

**Architecture:** trailing 블록을 `YGIconButton(iconResource, size = SIZE_44, contentDescription = null, onClick)` 호출로 치환(크기·기본 tint·아이콘·라벨·onClick 무손실 매핑). `trailingIconColor`는 YGIconButton 내부 상태 tint로 대체되어 파라미터에서 제거. 단일 파일·단일 Task([[2026-07-12-ygtextfield-clear-iconbutton]]는 색이 별도 Colors 홀더라 2 Task였으나, 여기선 색이 직접 파라미터라 1 Task).

**Tech Stack:** Kotlin, Jetpack Compose(자체 테마 [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)), `YGIconButton`([[2026-07-12-ygiconbutton|spec]]).

**Spec:** [specs/archive/2026-07-12-yglistitem-trailingicon-iconbutton.md](../../specs/archive/2026-07-12-yglistitem-trailingicon-iconbutton.md)

## Global Constraints
- 대상 repo: `TJYG-Android`(별도 repo). 브랜치 `feature/#136-etc-component`에 `YGListItem`·`YGIconButton` 모두 존재.
- 패키지: `com.teamyg.parfait.core.designsystem.component.etc`. `YGIconButton`은 `...component.ygiconbutton`.
- 검증: 유닛 TDD 인프라 없음(Compose UI). **compile(`:core:designsystem:compileReleaseKotlin`) + `ktlintMainSourceSetCheck` + `@Preview` 육안**으로 대체.
- ktlint 엄격(미사용 import 검출). 커밋 전 `ktlintMainSourceSetFormat`(미사용 import 자동 제거).
- public API는 `trailingIconColor` 제거 외 불변.

---

### Task 1: YGListItem trailing 블록을 YGIconButton으로 교체 + trailingIconColor 제거

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/etc/YGListItem.kt`

**Interfaces:**
- Consumes: `YGIconButton(@DrawableRes iconResource: Int, size: YGIconButtonSize, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, interactionSource: MutableInteractionSource = ..., isEnabled: Boolean = true)`, `YGIconButtonSize.SIZE_44`(둘 다 `...component.ygiconbutton`).
- Produces: `YGListItem`에서 `trailingIconColor` 파라미터 소멸(호출부 영향: 기본값 Gray300 외 지정 사용처 없음 전제).

- [ ] **Step 1: trailing 블록 교체** — `trailingIcon?.let { ... }` 내부 인라인 `Box`+`Image`를 `YGIconButton` 호출로 치환.

교체 전:
```kotlin
        trailingIcon?.let {
            // TODO IconButton 컴포넌트
            Box(
                modifier = Modifier
                    .clickable(role = Role.Button) { onClick() }
                    .size(SizeTokens.Size44.getDp()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(trailingIconColor),
                    modifier = Modifier.size(SizeTokens.Size24.getDp()),
                )
            }
        }
```

교체 후:
```kotlin
        trailingIcon?.let {
            YGIconButton(
                iconResource = trailingIcon,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClick,
            )
        }
```

- [ ] **Step 2: trailingIconColor 파라미터 제거** — `YGListItem` 파라미터 목록에서 아래 줄 삭제.

```kotlin
    trailingIconColor: Color = YGAtomicColors.Gray.Gray300,
```

(`Color` import는 `textColor`·`subTextColor`가 계속 사용하므로 유지. `YGAtomicColors`도 `Gray.Gray800`·`Gray.Gray400` 기본값에서 계속 사용.)

- [ ] **Step 3: import 정리** — trailing 인라인 전용이라 이제 미사용이 된 import 제거, `YGIconButton`·`YGIconButtonSize` 추가.

제거(모두 인라인 아이콘 전용이었음):
```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
```

추가:
```kotlin
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
```

유지(타 사용처 있음): `androidx.annotation.DrawableRes`(trailingIcon), `androidx.compose.ui.Alignment`(Row `verticalAlignment`), `androidx.compose.ui.graphics.Color`(textColor/subTextColor), `...R`(프리뷰 `ic_caret_right`).

- [ ] **Step 4: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin --offline`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetFormat :core:designsystem:ktlintMainSourceSetCheck --offline`
Expected: `BUILD SUCCESSFUL` (Format이 잔여 미사용 import 자동 정리)

- [ ] **Step 6: 프리뷰 육안 확인** — Android Studio에서 `YGListItemPreview`. trailing caret이 우측 표시, 탭 시 `onClick`, **누르는 순간 tint가 진해짐(pressed Gray400, 신규)** 확인. 평상 외형은 기존과 동일(Gray300).

- [ ] **Step 7: 커밋** (사용자 승인 후)

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/etc/YGListItem.kt
git commit -m "refactor: YGListItem trailing 아이콘 YGIconButton으로 교체"
```

---

## Self-Review
- **Spec coverage**: 목표(trailing→YGIconButton)·범위(블록 치환 + trailingIconColor 제거, YGIconButton/타 동작 불변)·무손실 매핑 표(SIZE_44/Gray300/아이콘/라벨/onClick)·동작 변화(pressed Gray400 신규)·파일 구성(YGListItem.kt 단일) — Task 1에 대응.
- **Placeholder**: 없음(교체 전/후 코드 전량 기재, 제거 import·파라미터 명시).
- **Type consistency**: `YGIconButton`·`YGIconButtonSize.SIZE_44` 시그니처는 [[2026-07-12-ygiconbutton|YGIconButton spec]]·구현과 일치. `trailingIcon`·`onClick`·`contentDescription = null`은 기존 코드 심볼 그대로.

## 검증 결과 (2026-07-12)
- `:core:designsystem:ktlintMainSourceSetFormat` → BUILD SUCCESSFUL.
- `:core:designsystem:compileReleaseKotlin` → BUILD SUCCESSFUL.
- `:core:designsystem:ktlintMainSourceSetCheck` → BUILD SUCCESSFUL.
- 코드는 미커밋(사용자 별도 처리). 프리뷰 육안은 기기/스튜디오 확인 권장.

## 열린 질문
- `contentDescription = null` 유지(장식 취급, 리소스화·라벨링 범위 밖).
- `trailingIconColor` 커스터마이즈 소멸 — 색 분기 필요 시 YGIconButton에 색 파라미터 도입 재검토.
