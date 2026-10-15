---
id: ygmodalpopup
title: YGModalPopup Implementation Plan
status: done
type: work-order
created: 2026-07-15
updated: 2026-07-18
platforms: android
owner:
related_adr: ADR-0010
related_spec: ygmodalpopup
related_code: YGModalPopup, YGModalPopupContent, YGButton, YGButtonType
archived_reason: PR #151(feature/#135-modal-component) develop 머지 완료(2026-07-18 기준선 점검). 스펙 대조 드리프트 없음.
tags: [plan, parfait, designsystem, modal, dialog]
---

# YGModalPopup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `core:designsystem`에 아이콘 + 제목 + 본문 + 확인/취소 2버튼 중앙 모달 `YGModalPopup`을 Compose `Dialog` 래핑으로 구현한다.

**Architecture:** public `YGModalPopup`(`Dialog(onDismissRequest, properties)` 래퍼) → private `YGModalPopupContent`(실제 시각 `Column`). Content 분리 이유: `@Preview`가 `Dialog` 래퍼 없이 시각을 안정적으로 렌더하도록. Content = 바깥 `Column`(bg+radius+clip+padding, `fillMaxWidth`) 안에 (1) Contents `Column`(아이콘 + 텍스트 `Column`), (2) Action `Row`(확인 Secondary + 취소 Primary, 각 `weight(1f)`). stateless presentational.

**Tech Stack:** Kotlin, Jetpack Compose(`androidx.compose.ui.window.Dialog`/`DialogProperties`, foundation `Column`/`Row`/`Image`/`background`/`clip`/`padding`/`size`, material3 `Text`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)), 기존 `YGButton`(`YGButtonType.Medium.Secondary`/`Medium.Primary`) 재사용.

**Spec:** [specs/archive/2026-07-15-ygmodalpopup.md](../../specs/archive/2026-07-15-ygmodalpopup.md)

> ⚠️ **이 계획은 최초 구현 시점(2026-07-15) 기준 기록.** 이후 #135 브랜치 refactor(`ba8d4aa`)로 API가 바뀜: `confirmText`/`onConfirm`/`cancelText`/`onCancel` → `secondaryText`/`onSecondaryClick`/`primaryText`/`onPrimaryClick`(confirm/cancel 의미 제거), `confirmEnabled`/`cancelEnabled` → 단일 `isEnabledButton`. **현행 API는 스펙이 SoT** — 아래 task 본문의 시그니처는 당시 스냅샷.

## Global Constraints
- 대상 repo: `TJYG-Android`. 브랜치: 스텁(`YGModalPopup.kt`)이 `feature/#135-modal-component`에 존재 — 동일 브랜치 계속.
- 패키지: `com.teamyg.parfait.core.designsystem.component.modal`.
- 색은 `YGAtomicColors.Gray.*` / `YGAtomicColors.Cherry.*` 직접 참조(YGButton 선례, 시맨틱 슬롯 없음 → 과도기 [open-questions](../../../synthesis/open-questions.md)). `YGAtomicColors`는 `internal`이지만 동일 모듈(`core:designsystem`)이라 접근 가능.
- Title 색은 **`YGAtomicColors.Gray.Gray900`**(#135 `5dcd419`에서 하드코딩 `Color(0xFF333333)` → 아토믹 토큰으로 전환). Body는 `YGAtomicColors.Gray.Gray500`.
- 타이포 `YGTheme.typography.title.t03SB`(제목) / `body.b02R`(본문), 간격·모양 토큰 `YGTheme.layout.*` / `YGTheme.shapes.radius.medium1`, 아이콘 크기 `SizeTokens.Size48.getDp()`.
- 버튼 role↔style: 확인(`confirmText`/`onConfirm`)=`Medium.Secondary`(좌), 취소(`cancelText`/`onCancel`)=`Medium.Primary`(우). 활성은 `confirmEnabled`/`cancelEnabled` param(기본 `true`)로 제어.
- 제목·본문·버튼 문구·아이콘은 파라미터 주입. 컴포넌트 내부 텍스트 리터럴 없음(프리뷰 제외).
- width 고정 제어 없음(`DialogProperties()` 기본, `usePlatformDefaultWidth` 미변경).
- 검증: `:core:designsystem:compileReleaseKotlin` + `:core:designsystem:ktlintMainSourceSetCheck` + `@YGPreview` 육안. 유닛 테스트 없음(모듈 관례).

---

### Task 1: YGModalPopup 컴포넌트 + 프리뷰

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/modal/YGModalPopup.kt` (기존 stub 전체 대체)

**Interfaces:**
- Consumes: `YGButton(text: String, buttonType: YGButtonType, isEnabled: Boolean, onClick: () -> Unit, modifier: Modifier)` — 기존. `YGButtonType.Medium.Secondary`, `YGButtonType.Medium.Primary` — 기존.
- Produces:
  - `fun YGModalPopup(title: String, body: String, @DrawableRes iconRes: Int, confirmText: String, onConfirm: () -> Unit, cancelText: String, onCancel: () -> Unit, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, confirmEnabled: Boolean = true, cancelEnabled: Boolean = true, iconTint: Color = YGAtomicColors.Cherry.Cherry600, properties: DialogProperties = DialogProperties())`
  - `private fun YGModalPopupContent(...)` — 시각 전용, 프리뷰용.

- [ ] **Step 1: stub 전체를 아래 구현으로 대체**

```kotlin
package com.teamyg.parfait.core.designsystem.component.modal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGModalPopup(
    title: String,
    body: String,
    @DrawableRes iconRes: Int,
    confirmText: String,
    onConfirm: () -> Unit,
    cancelText: String,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    iconTint: Color = YGAtomicColors.Cherry.Cherry600,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        YGModalPopupContent(
            title = title,
            body = body,
            iconRes = iconRes,
            confirmText = confirmText,
            onConfirm = onConfirm,
            cancelText = cancelText,
            onCancel = onCancel,
            modifier = modifier,
            confirmEnabled = confirmEnabled,
            cancelEnabled = cancelEnabled,
            iconTint = iconTint,
        )
    }
}

@Composable
private fun YGModalPopupContent(
    title: String,
    body: String,
    @DrawableRes iconRes: Int,
    confirmText: String,
    onConfirm: () -> Unit,
    cancelText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    iconTint: Color = YGAtomicColors.Cherry.Cherry600,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = YGAtomicColors.Gray.White,
                shape = YGTheme.shapes.radius.medium1,
            ).clip(shape = YGTheme.shapes.radius.medium1)
            .padding(
                start = YGTheme.layout.padding.padding6,
                top = YGTheme.layout.padding.padding5,
                end = YGTheme.layout.padding.padding6,
                bottom = YGTheme.layout.padding.padding6,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.padding.padding2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(SizeTokens.Size48.getDp()),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = body,
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            modifier = Modifier.fillMaxWidth(),
        ) {
            YGButton(
                text = confirmText,
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = confirmEnabled,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
            YGButton(
                text = cancelText,
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = cancelEnabled,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGModalPopup() = PreviewBox {
    YGModalPopupContent(
        title = "그룹에서 나갈까요?",
        body = "그룹에서 나가도\n그룹에 올렸던 사진은 지워지지 않아요",
        iconRes = R.drawable.ic_warning_round,
        confirmText = "나가기",
        onConfirm = {},
        cancelText = "취소",
        onCancel = {},
    )
}
```

- [ ] **Step 2: 컴파일 검증**

Run: `./gradlew :core:designsystem:compileReleaseKotlin`
Expected: `BUILD SUCCESSFUL`. 실패 시 import·심볼(`SizeTokens.Size48`, `YGTheme.layout.gap.gap2/gap3/gap5`, `padding.padding2/5/6`, `shapes.radius.medium1`, `typography.title.t03SB`, `typography.body.b02R`) 존재 재확인.

- [ ] **Step 3: ktlint 검증**

Run: `./gradlew :core:designsystem:ktlintMainSourceSetCheck`
Expected: `BUILD SUCCESSFUL`. 실패 시 리포트 지적대로 포맷 수정 후 재실행.

- [ ] **Step 4: 프리뷰 육안 확인**

Android Studio에서 `YGModalPopup.kt` 열어 `PreviewYGModalPopup` 렌더. 확인:
- 흰(#FAFAFA) 라운드 카드, 상단 중앙 cherry-600 warning 아이콘.
- 제목("그룹에서 나갈까요?") 진회색 볼드 center, 본문 2줄 회색 center.
- 하단 확인(좌, 회색)·취소(우, 검정) 버튼 균등 폭.
- 아이콘 48dp 스케일 링 두께가 Figma와 크게 어긋나면 [스펙 열린 질문](../../specs/archive/2026-07-15-ygmodalpopup.md) "아이콘 에셋 스케일" 기록대로 48dp 전용 에셋 검토(이 계획 범위 밖).

- [ ] **Step 5: 커밋** (사용자 승인 후 — AI repo 규칙 아님, TJYG-Android repo 커밋)

```bash
cd <TJYG-Android>
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/modal/YGModalPopup.kt
git commit -m "feat: YGModalPopup 모달 팝업 구현 (#135)"
```

---

## Self-Review
- **스펙 커버리지**: API 14파라미터(confirmEnabled/cancelEnabled 포함)·버튼 role↔style·토큰 매핑·content 분리·프리뷰 모두 Task 1에 포함. width 미제어(DialogProperties 기본)·iconTint 기본 cherry-600 반영. ✅
- **플레이스홀더**: 없음(전체 코드 인라인). ✅
- **타입 일관성**: `YGModalPopup`↔`YGModalPopupContent` 파라미터명·타입 일치, `YGButton` 시그니처 기존과 일치, `SizeTokens.Size48.getDp()`·`YGTheme.*` 심볼 실재 확인. ✅
- **주의**: `@Preview`는 `Dialog` 미포함 `YGModalPopupContent` 렌더(안정성). 실기기 dismiss 동작은 프리뷰로 검증 불가 — 소비 feature 통합 시 확인.
