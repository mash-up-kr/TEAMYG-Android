---
id: designsystem-preview-migration
title: designsystem 프리뷰 관용구 통일 Implementation Plan
status: done
type: work-order
created: 2026-07-18
updated: 2026-07-19
platforms: android
owner:
related_adr: ADR-0010
related_spec: designsystem-preview-migration
related_code: YGPreview, PreviewBox, YGColorChip, YGButton, YGChipButton, YGIconButton, YGInputNumber, YGToggleButton, YGActionItem, YGDate, YGLabel, YGTopBar, YGDateButton, YGDangerZone
archived_reason: PR #158(refactor/design-system-preview) develop 머지 완료(2026-07-19, ce4e9b8)
tags: [plan, parfait, designsystem, refactor, preview]
---

# designsystem 프리뷰 관용구 통일 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `core:designsystem` 컴포넌트 프리뷰 12개를 `@Preview`+`YGCustomTheme { }` 관용구에서 `@YGPreview`+`PreviewBox { }` 관용구로 통일한다.

> 📌 **심볼 개명(2026-07-31, PR #165)** — 아래 코드 스니펫의 `YGColorChip`·`YGColorChipStyle`·파라미터 `text`는 현재 코드에서 `YGNametagChip`·`YGNametagChipStyle`·`userFirstName`이다. 이 계획서는 작성 시점(#158) 기준이므로 스니펫은 그대로 둔다 → 현행은 [ygcolorchip 스펙](../../specs/archive/2026-07-18-ygcolorchip.md).

**Architecture:** 프리뷰 전용 애노테이션·테마 래퍼만 교체(런타임 코드·API 불변). 공용 유틸(`YGPreview`·`PreviewBox`)은 손대지 않는다. 배경은 `PreviewBox`가 아니라 content 람다 내부 `Modifier`로 보존. `@PreviewParameter`는 유지한 채 애노테이션만 교체.

**Tech Stack:** Kotlin, Jetpack Compose Preview(`@Preview` 다중 애노테이션 `@YGPreview`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/archive/2026-07-18-designsystem-preview-migration.md](../../specs/archive/2026-07-18-designsystem-preview-migration.md)

**작업 repo:** TJYG-Android, 브랜치 `refactor/design-system-preview` → **PR #158 develop 머지 완료(2026-07-19, `ce4e9b8`)**. 로컬 경로는 `wiki/personal-private/project-paths.md`.

## Global Constraints
- **TJYG-Android 코드는 커밋하지 않는다**(사용자 지시). 각 task는 컴파일·ktlint까지만 하고 git commit 스텝 없음. (parfait 문서만 별도 커밋 대상.)
- 공용 유틸 `utils/preview/YGPreview.kt`·`utils/preview/PreviewComponent.kt` **변경 금지**.
- 컴포넌트 본체(public API·상태 로직) **변경 금지** — 프리뷰 함수만 수정.
- `PreviewBox`에 배경 파라미터 추가 금지 — 배경은 content 내부 `Modifier.background(...)`.
- 이미 `@YGPreview`+`PreviewBox`인 6파일(YGTextField·YGTextFormField·YGListItem·YGHorizontalDivider·YGInviteCard·YGModalPopup) **변경 금지**.
- import 패키지: `com.teamyg.parfait.core.designsystem.utils.preview.YGPreview`, `...utils.preview.PreviewBox`.
- 컴포넌트 경로 접두사: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/` (이하 `…/component/`).
- 각 파일 변환 공통 규칙:
  1. `@Preview`(옵션 포함) → `@YGPreview`
  2. `YGCustomTheme { X }` → `PreviewBox { X }`(내부 content X 그대로)
  3. 프리뷰 전용이던 `import …tooling.preview.Preview`·`import …theme.YGCustomTheme` 제거, `YGPreview`·`PreviewBox` import 추가
  4. 명시 배경은 content 내부 `Modifier`로 유지(PreviewBox엔 미주입)
  5. `@PreviewParameter`는 그대로 유지

---

## Task 1 (파일럿): YGColorChip — @YGPreview + @PreviewParameter 조합 검증

**먼저 하는 이유:** `@YGPreview`(6-config) + `@PreviewParameter` 조합은 코드베이스 첫 사례. 가장 리스크 큰 패턴이자 프리뷰 수 최다(14타입×6). 여기서 컴파일·IDE 렌더가 되면 나머지 param 파일도 동일 안전.

**Files:**
- Modify: `…/component/ygcolorchip/YGColorChip.kt` (프리뷰 함수 `YGChipPreview`)

**Interfaces:**
- Consumes: `YGPreview`, `PreviewBox`(공용 유틸), `YGColorChipPreviewParameterProvider`·`YGChipPreviewData`(기존 프리뷰 데이터, 불변).
- Produces: 없음(프리뷰 전용).

- [ ] **Step 1: import 교체**

제거: `import androidx.compose.ui.tooling.preview.Preview`, `import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme`
추가: `import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview`, `import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox`
(`@PreviewParameter`·`PreviewParameter` import는 유지.)

- [ ] **Step 2: 프리뷰 함수 변환**

Before:
```kotlin
@Preview
@Composable
private fun YGChipPreview(
    @PreviewParameter(YGColorChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) {
    YGCustomTheme {
        Column {
            Text(data.name)
            Spacer(modifier = Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                YGColorChip(colorChipType = data.colorChipType, text = "문", chip = YGColorChipStyle.Style28)
                YGColorChip(colorChipType = data.colorChipType, text = "문", chip = YGColorChipStyle.Style40)
            }
        }
    }
}
```
After:
```kotlin
@YGPreview
@Composable
private fun YGChipPreview(
    @PreviewParameter(YGColorChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) = PreviewBox {
    Column {
        Text(data.name)
        Spacer(modifier = Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YGColorChip(colorChipType = data.colorChipType, text = "문", chip = YGColorChipStyle.Style28)
            YGColorChip(colorChipType = data.colorChipType, text = "문", chip = YGColorChipStyle.Style40)
        }
    }
}
```

- [ ] **Step 3: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (`@YGPreview`+`@PreviewParameter` 조합이 컴파일됨).

- [ ] **Step 4: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintCheck` (또는 프로젝트 ktlint task)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: IDE 프리뷰 렌더 확인(수동)**

Android Studio에서 `YGColorChip.kt` 프리뷰 창 열기. 각 config × 각 타입(14) 렌더 확인. (파일럿 게이트 — 여기서 문제면 나머지 param 파일 전략 재검토.)

> **commit 하지 않음**(Global Constraints).

---

## Task 2: 나머지 @PreviewParameter 5파일

**Files (each Modify, 프리뷰 함수만):**
- `…/component/ygbutton/YGButton.kt` (`YGButtonPreview`)
- `…/component/ygchipbutton/YGChipButton.kt` (`YGChipButtonPreview`)
- `…/component/ygiconbutton/YGIconButton.kt` (`YGIconButtonPreview`)
- `…/component/yginputnumber/YGInputNumber.kt` (`YGInputNumberPreview`)
- `…/component/ygtogglebutton/YGToggleButton.kt` (`YGToggleButtonPreview`)

**Interfaces:**
- Consumes: `YGPreview`, `PreviewBox`, 각 파일의 기존 `*PreviewParameterProvider`·`*PreviewData`(불변).

- [ ] **Step 1: YGButton 변환**

import: 제거 `…tooling.preview.Preview`·`…theme.YGCustomTheme`, 추가 `YGPreview`·`PreviewBox`.
`@Preview(showBackground = true)` → `@YGPreview`. 본문 `YGCustomTheme { Column(...) { … } }` → `PreviewBox { Column(...) { … } }`(Column 내부 content·`Arrangement.spacedBy(16.dp)`·`padding(16.dp)` 그대로).
```kotlin
@YGPreview
@Composable
private fun YGButtonPreview(
    @PreviewParameter(YGButtonPreviewParameterProvider::class)
    data: YGButtonPreviewData,
) = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(data.name)
        YGButton(text = "Button Enabled", buttonType = data.buttonType, isEnabled = true, modifier = Modifier.fillMaxWidth(), onClick = {})
        YGButton(text = "Button Disabled", buttonType = data.buttonType, isEnabled = false, modifier = Modifier.fillMaxWidth(), onClick = {})
        YGButton(text = "Button Start", buttonType = data.buttonType, isEnabled = true, startIconResource = R.drawable.ic_plus, modifier = Modifier.fillMaxWidth(), onClick = {})
        YGButton(text = "Button End", buttonType = data.buttonType, isEnabled = true, endIconResource = R.drawable.ic_plus, modifier = Modifier.fillMaxWidth(), onClick = {})
    }
}
```
(`@Preview(showBackground=true)`의 배경은 버튼 자체 색이 있어 미보존 — YAGNI. content 무변경.)

- [ ] **Step 2: YGChipButton 변환**

```kotlin
@YGPreview
@Composable
private fun YGChipButtonPreview(
    @PreviewParameter(YGChipButtonPreviewParameterProvider::class)
    data: YGChipButtonPreviewData,
) = PreviewBox {
    Box(modifier = Modifier.fillMaxWidth()) {
        YGChipButton(
            text = "Parfait",
            onClick = {},
            startIconResource = data.startIconResource,
            colors = data.colors,
            endIconResource = data.endIconResource,
        )
    }
}
```

- [ ] **Step 3: YGIconButton 변환**

```kotlin
@YGPreview
@Composable
private fun YGIconButtonPreview(
    @PreviewParameter(YGIconButtonPreviewParameterProvider::class)
    data: YGIconButtonPreviewData,
) = PreviewBox {
    Box(modifier = Modifier.fillMaxWidth()) {
        YGIconButton(
            iconResource = R.drawable.ic_close_round,
            size = data.buttonIconSize,
            onClick = {},
            contentDescription = null,
            isEnabled = data.isEnabled,
        )
    }
}
```

- [ ] **Step 4: YGInputNumber 변환**

```kotlin
@YGPreview
@Composable
private fun YGInputNumberPreview(
    @PreviewParameter(YGInputNumberPreviewParameterProvider::class)
    data: YGInputNumberPreviewData,
) = PreviewBox {
    Box(modifier = Modifier.fillMaxWidth()) {
        YGInputNumber(
            number = 3,
            isSelected = data.isSelected,
            onClick = {},
        )
    }
}
```

- [ ] **Step 5: YGToggleButton 변환** (명시 배경 `Cherry.Cherry50` content 내부 유지)

```kotlin
@YGPreview
@Composable
private fun YGToggleButtonPreview(
    @PreviewParameter(YGToggleButtonPreviewParameterProvider::class)
    data: YGToggleButtonPreviewData,
) = PreviewBox {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(YGAtomicColors.Cherry.Cherry50),
    ) {
        YGToggleButton(
            text = "Parfait",
            isSelected = data.isSelected,
            onClick = {},
            iconResource = data.iconResource,
        )
    }
}
```
(각 파일: import 제거 `…tooling.preview.Preview`·`…theme.YGCustomTheme`, 추가 `YGPreview`·`PreviewBox`. YGToggleButton은 `YGAtomicColors` import 유지.)

- [ ] **Step 6: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL.

> **commit 하지 않음.**

---

## Task 3: @PreviewParameter 없는 6파일 (배경 보존 포함)

**Files (each Modify, 프리뷰 함수만):**
- `…/component/ygactionitem/YGActionItem.kt` (`YGActionItemPreview`)
- `…/component/ygtext/YGDate.kt` (`YGDatePreview`)
- `…/component/ygtext/YGLabel.kt` (`YGLabelPreview`)
- `…/component/ygtopbar/YGTopBar.kt` (`YGTopBarPreview`) — 배경 White
- `…/component/ygdatebutton/YGDateButton.kt` (`YGDateButtonPreview`) — 배경 White
- `…/component/ygdangerzone/YGDangerZone.kt` (`YGDangerZonePreview`) — 배경 **Black**

- [ ] **Step 1: YGActionItem 변환**

```kotlin
@YGPreview
@Composable
fun YGActionItemPreview() = PreviewBox {
    Box(modifier = Modifier.fillMaxWidth()) {
        YGActionItem(
            text = "그룹 나가기",
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
        )
    }
}
```

- [ ] **Step 2: YGDate 변환**

```kotlin
@YGPreview
@Composable
private fun YGDatePreview() = PreviewBox {
    YGDate(text = "7월 14일의 파르페")
}
```

- [ ] **Step 3: YGLabel 변환**

```kotlin
@YGPreview
@Composable
private fun YGLabelPreview() = PreviewBox {
    YGLabel(text = "레이블")
}
```

- [ ] **Step 4: YGTopBar 변환** (배경 White content 내부 유지)

`@Preview` → `@YGPreview`. 기존 `YGCustomTheme { Column(Modifier.fillMaxWidth().background(Color.White)) { 4변형 } }` → `PreviewBox { Column(...동일...) { 4변형 } }`. content(4개 YGTopBar 변형 호출)·`background(Color.White)` 그대로.
```kotlin
@YGPreview
@Composable
fun YGTopBarPreview() = PreviewBox {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        YGTopBarBack(onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarDetail(title = "그룹이름", onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarEmpty(onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarDefault(onChipClick = { }, onIconClick = { }, modifier = Modifier.fillMaxWidth())
    }
}
```

- [ ] **Step 5: YGDateButton 변환** (배경 White 유지)

```kotlin
@YGPreview
@Composable
private fun YGDateButtonPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White),
    ) {
        YGDateButton(text = "31", isSelected = false, isToday = false, isEnabled = true, onClick = {}, modifier = Modifier.size(44.dp))
        YGDateButton(text = "31", isSelected = true, isToday = false, isEnabled = true, onClick = {}, modifier = Modifier.size(44.dp))
        YGDateButton(text = "31", isSelected = false, isToday = true, isEnabled = true, onClick = {}, modifier = Modifier.size(44.dp))
        YGDateButton(text = "31", isSelected = false, isToday = false, isEnabled = false, onClick = {}, modifier = Modifier.size(44.dp))
    }
}
```

- [ ] **Step 6: YGDangerZone 변환** (배경 **Black** 유지 — 필수)

```kotlin
@YGPreview
@Composable
private fun YGDangerZonePreview() = PreviewBox {
    Box(modifier = Modifier.background(Color.Black)) {
        YGDangerZone(
            topZone = { YGActionItem(text = "로그아웃", onClick = {}) },
            bottomZone = { YGActionItem(text = "서비스 탈퇴하기", onClick = {}, modifier = Modifier) },
            modifier = Modifier,
        )
    }
}
```

- [ ] **Step 7: import 정리 (각 파일)**

각 파일: 제거 `import androidx.compose.ui.tooling.preview.Preview`·`import …theme.YGCustomTheme`; 추가 `import …utils.preview.YGPreview`·`import …utils.preview.PreviewBox`. `Color`·`background`·`Column`·`Box` 등 배경/레이아웃 import는 사용처 있으면 유지.

- [ ] **Step 8: 컴파일 + ktlint**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL.

> **commit 하지 않음.**

---

## Task 4: 전체 검증 + parfait 문서 갱신

**Files:**
- (검증만) TJYG-Android `core:designsystem`
- Modify(parfait): `parfait/architecture/design-system.md`, `parfait/synthesis/open-questions.md`

- [ ] **Step 1: 전체 모듈 컴파일**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: ktlint 전체**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 잔여 @Preview/YGCustomTheme 프리뷰 없음 확인**

Run: `grep -rn '@Preview\b' core/designsystem/src/main --include=*.kt | grep -v utils/preview/YGPreview.kt`
Expected: 결과 없음(공용 유틸 정의 제외 모든 프리뷰가 `@YGPreview`).

- [ ] **Step 4: design-system.md 프리뷰 방식 노트 갱신**

"컨벤션 분기" 프리뷰 방식 서술을 "혼재 → `@YGPreview`+`PreviewBox` 표준 통일(refactor/design-system-preview)"로 수정. (파일: `parfait/architecture/design-system.md`)

- [ ] **Step 5: open-questions "[2026-07-12] 컨벤션 분기" 프리뷰 항목 부분 해소**

프리뷰 방식 항목을 해소 처리(패키지 네이밍 항목은 잔존). 해소 메모에 `@YGPreview`+`PreviewBox` 표준 채택 기록. (파일: `parfait/synthesis/open-questions.md`)

- [ ] **Step 6: parfait 문서 커밋 여부 사용자 확인**

TJYG-Android 코드는 **커밋 안 함**(working tree 유지). parfait 문서(spec/plan/design-system/open-questions)만 브랜치→PR 대상 — 커밋 전 사용자 확인.

---

## Self-Review 메모
- 스펙 12파일 전부 task에 매핑(Task1: 1 / Task2: 5 / Task3: 6). ✅
- placeholder 없음(전 파일 after-code 명시). ✅
- 위험 조합(@YGPreview+@PreviewParameter) 파일럿(Task1) 선행. ✅
- 배경 보존 3파일(TopBar/DateButton White, DangerZone Black) + YGToggleButton(Cherry50) content 내부 유지 명시. ✅
- TJYG-Android 커밋 제외, parfait 문서만 커밋(확인 후). ✅
