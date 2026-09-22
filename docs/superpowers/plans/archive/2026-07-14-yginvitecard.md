---
id: yginvitecard
title: YGInviteCard Implementation Plan
status: done
type: work-order
created: 2026-07-14
updated: 2026-07-18
platforms: android
owner:
related_adr: ADR-0010
related_spec: yginvitecard
related_code: YGInviteCard, YGInviteCardStatus, YGButton, YGButtonType
archived_reason: PR #148(feature/#136-etc-component) develop 머지 완료(2026-07-18 기준선 점검). 스펙 대조 드리프트 없음.
tags: [plan, parfait, designsystem, card]
---

# YGInviteCard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `core:designsystem`에 그룹 초대 코드 카드 `YGInviteCard`(라벨 + subText + 코드 박스 + 복사 버튼)를 Active/Invalid 상태별 색·버튼 활성 분기로 구현한다.

**Architecture:** `Column`(border+clip+background+padding) 안에 두 개의 `Row` — (1) 라벨 Row("그룹 초대 코드" + subText, subText는 `weight(1f)`+우측 정렬), (2) 코드박스 Row(private `InviteCodeBox` `weight(1f)`+`fillMaxHeight` + `YGButton` SmallSquare). 상태는 `status: YGInviteCardStatus` prop, 색은 `when(status)`로 파생. stateless presentational.

**Tech Stack:** Kotlin, Jetpack Compose(foundation `Column`/`Row`/`Box`/`border`/`background`/`clip`/`padding`, material3 `Text`), 자체 테마([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)), 기존 `YGButton`(`YGButtonType.SmallSquare`) 재사용.

**Spec:** [specs/archive/2026-07-14-yginvitecard.md](../../specs/archive/2026-07-14-yginvitecard.md)

## Global Constraints
- 대상 repo: `TJYG-Android`. 브랜치: 스텁(`YGInviteCard.kt`)이 `feature/#136-etc-component`에 존재 — 동일 브랜치 계속 or 신규 feature 브랜치는 담당자 확정.
- 패키지: `com.teamyg.parfait.core.designsystem.component.card`.
- 색은 상태별 `YGAtomicColors.Gray.*` / `YGAtomicColors.Cherry.*` 직접 참조(YGButton·YGIconButton 선례, 시맨틱 슬롯 없음 → 과도기 [open-questions](../../../synthesis/open-questions.md)).
- 타이포 `YGTheme.typography.body.b02R`(라벨·subText) / `b01SB`(코드), 간격·모양 토큰 `YGTheme.layout.*` / `YGTheme.shapes.radius.*`.
- 복사 버튼 색은 손대지 않는다(`YGButtonType.SmallSquare` 소관, 별도 브랜치). 카드는 `isEnabled`만 제어.
- 검증: `:core:designsystem:compileReleaseKotlin` + `:core:designsystem:ktlintMainSourceSetCheck` + `@YGPreview` 육안(Active/Invalid). 유닛 테스트 없음(모듈 관례).
- 라벨(`label`)·복사 버튼 텍스트(`copyButtonText`)·복사 아이콘(`endIconResource`)은 파라미터로 주입. `endIconResource` 기본값만 `R.drawable.ic_copy`. 컴포넌트 내부 텍스트 리터럴 없음.

---

### Task 1: YGInviteCardStatus enum

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/card/YGInviteCardStatus.kt`

**Interfaces:**
- Produces: `enum class YGInviteCardStatus { Active, Invalid }`

- [ ] **Step 1: enum 작성**

```kotlin
package com.teamyg.parfait.core.designsystem.component.card

/** 그룹 초대 카드 상태. Active=정원 여유, Invalid=정원 초과. */
enum class YGInviteCardStatus {
    Active,
    Invalid,
}
```

- [ ] **Step 2: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/card/YGInviteCardStatus.kt
git commit -m "feat(designsystem): YGInviteCardStatus enum 추가"
```

---

### Task 2: YGInviteCard 본체 + InviteCodeBox 헬퍼

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/card/YGInviteCard.kt` (stub 채움)

**Interfaces:**
- Consumes: `YGInviteCardStatus`(Task 1), `YGButton`/`YGButtonType.SmallSquare`, `YGAtomicColors`, `YGTheme`, `R.drawable.ic_copy`.
- Produces: `@Composable fun YGInviteCard(label: String, inviteCode: String, subText: String, status: YGInviteCardStatus, copyButtonText: String, onCopyClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes endIconResource: Int? = R.drawable.ic_copy)`

- [ ] **Step 1: import + YGInviteCard 본체 작성** (stub의 빈 `YGInviteCard()`·preview 교체)

```kotlin
package com.teamyg.parfait.core.designsystem.component.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Composable
fun YGInviteCard(
    label: String,
    inviteCode: String,
    subText: String,
    status: YGInviteCardStatus,
    copyButtonText: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes endIconResource: Int? = R.drawable.ic_copy,
) {
    val borderColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Cherry.Cherry100
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray100
    }
    val subTextColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Gray.Gray600
        YGInviteCardStatus.Invalid -> YGAtomicColors.Cherry.Cherry600
    }
    Column(
        modifier = modifier
            .border(
                width = SizeTokens.Size1.getDp(),
                color = borderColor,
                shape = YGTheme.shapes.radius.medium1,
            ).clip(YGTheme.shapes.radius.medium1)
            .background(YGAtomicColors.Gray.White)
            .padding(
                horizontal = YGTheme.layout.padding.padding6,
                vertical = YGTheme.layout.padding.padding5,
            ),
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray400,
            )
            Text(
                text = subText,
                style = YGTheme.typography.body.b02R,
                color = subTextColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InviteCodeBox(
                inviteCode = inviteCode,
                status = status,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            YGButton(
                text = copyButtonText,
                buttonType = YGButtonType.SmallSquare,
                isEnabled = status == YGInviteCardStatus.Active,
                onClick = onCopyClick,
                endIconResource = endIconResource,
            )
        }
    }
}

@Composable
private fun InviteCodeBox(
    inviteCode: String,
    status: YGInviteCardStatus,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Cherry.Cherry100
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray200
    }
    val codeColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Gray.Gray900
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray500
    }
    Box(
        modifier = modifier
            .clip(YGTheme.shapes.radius.small)
            .background(backgroundColor)
            .padding(
                horizontal = YGTheme.layout.gap.gap7,
                vertical = YGTheme.layout.gap.gap3,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = inviteCode,
            style = YGTheme.typography.body.b01SB,
            color = codeColor,
        )
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :core:designsystem:compileReleaseKotlin`
Expected: BUILD SUCCESSFUL (미해결 심볼 없음 — `ic_copy`·`SmallSquare`·토큰 모두 존재 확인됨)

- [ ] **Step 3: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/card/YGInviteCard.kt
git commit -m "feat(designsystem): YGInviteCard 본체 구현"
```

---

### Task 3: Preview (Active/Invalid)

**Files:**
- Modify: `core/designsystem/.../component/card/YGInviteCard.kt` (파일 하단 preview 추가)

**Interfaces:**
- Consumes: `YGInviteCard`(Task 2), `PreviewBox`/`YGPreview`(`utils.preview`).

- [ ] **Step 1: import 추가**

```kotlin
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

- [ ] **Step 2: preview 컴포저블 작성** (stub 잔여 빈 preview 제거 후)

```kotlin
@YGPreview
@Composable
private fun PreviewYGInviteCard() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        YGInviteCard(
            label = "그룹 초대 코드",
            inviteCode = "WDIDCJ",
            subText = "1명 남음",
            status = YGInviteCardStatus.Active,
            copyButtonText = "복사",
            onCopyClick = {},
        )
        YGInviteCard(
            label = "그룹 초대 코드",
            inviteCode = "WDIDCJ",
            subText = "최대 인원 도달",
            status = YGInviteCardStatus.Invalid,
            copyButtonText = "복사",
            onCopyClick = {},
        )
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/card/YGInviteCard.kt
git commit -m "feat(designsystem): YGInviteCard 프리뷰 추가"
```

---

### Task 4: 검증

- [ ] **Step 1: compile + ktlint**

Run: `./gradlew :core:designsystem:compileReleaseKotlin :core:designsystem:ktlintMainSourceSetCheck`
Expected: BUILD SUCCESSFUL, ktlint 위반 0

- [ ] **Step 2: 프리뷰 육안 확인**
  - Active: 카드 border 핑크(Cherry100), subText "1명 남음" 회색(Gray600), 코드박스 핑크 배경 + 코드 진한색(Gray900), 복사 버튼 활성.
  - Invalid: 카드 border 회색(Gray100), subText "최대 인원 도달" 빨강(Cherry600), 코드박스 회색 배경(Gray200) + 코드 흐린색(Gray500), 복사 버튼 비활성.
  - 코드 텍스트가 박스 내 가운데 정렬로 보이는지 확인(start 요구 시 조정).

## 열린 질문
- 코드 텍스트 수평 정렬(center vs start): Figma 미명시. 현재 `contentAlignment = Alignment.Center`. 디자이너 확인 후 확정.
- 코드박스 가로 padding·라벨 Row 간격·border 두께: 리터럴 dp → 디자인 토큰(`gap.gap7`·`gap.gap3`·`SizeTokens.Size1`) 치환됨. 토큰 실값이 Figma(24/8/1px)와 일치하는지 프리뷰 육안 확인.
- `InviteCodeBox`의 `fillMaxHeight`(부모 `IntrinsicSize.Min`)로 코드박스 높이를 복사 버튼 높이에 맞춤 — Figma "Code Fill 42" 재현. 프리뷰에서 높이 일치 확인.
