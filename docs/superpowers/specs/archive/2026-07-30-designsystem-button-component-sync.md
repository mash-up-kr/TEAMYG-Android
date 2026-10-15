---
id: designsystem-button-component-sync
title: 디자인시스템 버튼 영역 컴포넌트 Figma 동기화 (Design System Button Components Figma Sync)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-01
related_code:
  - YGButton.kt#YGButton
  - YGButtonType.kt#YGButtonType
  - YGButtonColors.kt#YGButtonColors
  - YGIconButtonSize.kt#YGIconButtonSize
  - YGActionItem.kt#YGActionItem
  - YGChipButton.kt#YGChipButton
  - YGChipButtonColorsDefaults.kt#YGChipButtonColorsDefaults
  - YGChipButtonPreviewData.kt#YGChipButtonPreviewParameterProvider
  - YGInputNumber.kt#YGInputNumber
  - YGAlert.kt#YGAlert
  - YGTopBar.kt#YGTopBarContent
  - YGChipButtonPreviewScreen.kt#YGChipButtonPreviewScreen
related_adr:
  - ADR-0010
related_spec:
  - designsystem-text-component-sync
  - designsystem-radius-none-sync
  - ygchipbutton
  - ygiconbutton
  - ygactionitem
  - yginputnumber
related_architecture:
  - design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, figma-sync]
---

# Spec: 디자인시스템 버튼 영역 컴포넌트 Figma 동기화

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
>
> **구현 상태 — ✅ develop 머지 완료(PR #183, 2026-08-01)**. 드리프트 B1·B2·R1·R2·V1~V5
> **전량 코드 반영**. 2026-08-01 기준선 점검에서 머지 코드와 설계를 대조해 **드리프트 0건**
> 확인(`YGButtonColors` 테두리 3필드 + `borderColor()`, `YGButton`의 `border` 체이닝·아이콘
> `size`, `Medium.*`·`Large` `iconSize`·`radius.none`, `SIZE_48` 아이콘 32, `YGChipButton`
> 세로 `padding2`, 프리셋 `CherrySubtle`/`CherrySolid`, `Transparency.White50`,
> `YGActionItem`의 `iconResource` + `Row` 전환, `YGInputNumber` `radius.none` 3곳).
> `Medium.Transparency` pressed만 설계대로 `Gray.White.copy(alpha = …)` 유지.
>
> **스펙 범위 밖 동반 변경(같은 PR)** — 아이콘 드로어블 현행화·누락분 반입이 함께 들어왔다
> (`ic_calender`·`ic_rotate`·`ic_caret_top`/`_bottom`·`ic_add_round`·`ic_minus_round`·
> `ic_info_round`·`ic_enter`·`ic_newgroup` 신규 + `ic_arrow_left`/`ic_arrow_right` 교체).
> 이 스펙도 후속 스펙도 다루지 않은 항목이지만 신설 컴포넌트·캔버스 라운드가 이 에셋들을 쓴다.
>
> 검증 결과(`:core:designsystem`·`:app-preview` `assembleDebug` + repo 전체 `ktlintCheck` 통과,
> 실기기 갤러리 육안 대조):
> - `Medium`·`Large`·`YGInputNumber` 각짐 적용 확인
> - `Medium.Secondary` 테두리 렌더 확인 — enabled/disabled 테두리 단계 차이도 육안 구분됨
> - 아이콘 크기 파이프 작동 확인(`with icons` 섹션 아이콘이 본문 대비 작아짐)
> - `YGIconButton` `SIZE_48` 아이콘이 `SIZE_44`보다 크게 렌더
> - 칩 높이 감소 + 갤러리 라벨이 `CherrySubtle`/`CherrySolid`로 교체
> - `YGActionItem` `show icon` 섹션에 선두 아이콘 렌더, 아이콘 없는 항목은 무변화
> - **미검증: pressed 상태 전반** — `adb shell input motionevent`로 누른 상태가 Compose
>   `interactionSource`에 반영되지 않아 자동 캡처로 확인하지 못했다. 손으로 눌러 확인해야 한다.

## 목표

Figma `[디자인] 파르페 v0.1` 파일의 **Components 섹션 > Detail Type "버튼"** 영역 14개 컴포넌트를
현행 코드와 1:1 대조했다. 이 스펙은 그중 **이미 구현체가 있으나 값이 어긋난 7종**의 드리프트를 제거한다.
구현체가 아예 없는 5종은 후속 스펙(`designsystem-button-missing-components`)에서 다룬다.

드리프트는 개별 값 오차만이 아니다. `YGButton`은 `YGButtonType.iconSize`를 정의만 하고 렌더에 쓰지 않으며,
`YGButtonColors`는 테두리 색을 담을 자리가 없어 Figma `Button-Medium` `Type=Secondary`를 **표현 자체가 불가능**하다.
값을 고치기 전에 이 두 결함을 먼저 닫는다.

## 범위

- **포함**
  - `YGButton`·`YGButtonType`·`YGButtonColors` — 아이콘 크기 파이프 복구, 테두리 색 경로 복구, `Medium`·`Large` 각짐
  - `YGIconButtonSize` — `SIZE_48` 아이콘 크기 교정
  - `YGActionItem` — Figma `Show Icon: True` 변형(선두 아이콘) 신설
  - `YGChipButton`·`YGChipButtonColorsDefaults` — 세로 패딩 교정, `Button-Chip-Left` 프리셋 값 교정 + 프리셋 재명명
  - `YGInputNumber` — 각짐
  - 위 변경에 따른 프리뷰·`:app-preview` 갤러리 반영
- **제외**
  - `Button-SmallSquare`(`YGButtonType.SmallSquare`)·`Button-Date`(`YGDateButton`) — Figma와 일치. 변경 없음
  - **미구현 5종** — `Button-Edit-Tab`·`Button-Edit`·`Button-Circle`·`Button-Edit-Action`·`Camera-Shutter`.
    후속 스펙에서 신설하며, `Camera-Shutter`는 `feature/camera` 임시 구현체(`ShutterButton`)를
    `core:designsystem`으로 이관하는 작업을 포함한다
  - `YGToggleButton` 삭제 — 대상 Figma 원본이 없다고 판단해 제거하기로 했으나, 대체물 `Button-Edit`가
    후속 스펙 산출물이므로 그 스펙에서 함께 처리한다(먼저 지우면 갤러리 항목이 빈다)
  - `Button-Stroke`(Figma `1782:4459`) — 이번 대조 대상 14종 밖. 대응 구현체 없음
  - `Button-Icon` 아이콘 tint 색 — Figma가 색을 아이콘 에셋에 구워 노출하지 않아 대조 불가(아래 열린 질문)
  - 신규 ADR — 아키텍처 결정 변화 없음. `YGButtonColors` 테두리 필드는 #140에서 제거한 축의 복원이다

## Figma ↔ 코드 대조 결과

대조 기준 Figma 노드: `Button-Medium`·`Button-Large`·`Button-SmallSquare`·`Button-Icon`·`Action-Item`·
`Button-Chip-Left`·`Button-Chip-Right`·`Button-Input-Number`·`Button-Date`.
`-수정 전` 접미 노드는 구판이므로 대조 대상에서 제외한다([designsystem-text-component-sync](2026-07-27-designsystem-text-component-sync.md)와 동일 규칙).

### 일치 확인 (변경 없음)

| 대상 | 확인 내용 |
|---|---|
| `YGButtonType.SmallSquare` | 4방향 패딩(`padding5`/`padding4`/`padding4`/`padding3`)·`gap1`·`body.b02SB`·`radius.none`, 색 3상태(`Gray.Gray900`/`Gray.Black`/`Gray.Gray200`, 전경 `Gray.White`/`Gray.Gray400`) 전부 Figma 일치 |
| `YGDateButton` | `Selected` `Gray.Gray900` 채움 + `body.b02SB` + `Gray.White`, `Today` 테두리 `Gray.Gray850` + `body.b02R` + `Gray.Gray950`, `Default` `Gray.Gray800`, `Disabled` `Gray.Gray400`. 패딩으로 프레임 안쪽 원을 만드는 구현이 Figma 내부 `Fill` 지름과 결과적으로 일치 |
| `YGButtonType.Medium.Primary`·`Large` 색 | `Gray.Gray900` / pressed `Gray.Gray950` / disabled `Gray.Gray200`, 전경 `Gray.White` / disabled `Gray.Gray500` |
| `YGButton` 4방향 패딩·`gap` | `Medium`·`Large` 공통 가로 `padding4` / 세로 `padding5`, `gap2` |
| `YGActionItem` 패딩·색·타이포 | 세로 `padding5` / 가로 `padding6`, `body.b02R`, `Gray.Gray500` → pressed `Gray.Gray700` |
| `YGChipButton` 비대칭 가로 패딩 | 아이콘 있는 쪽 `padding3` / 없는 쪽 `padding5` — Figma `Chip-Left`(좌 `padding-3`/우 `padding-5`)·`Chip-Right`(반대) 일치 |
| `YGChipButtonColorsDefaults` Chip-Right 프리셋 | 배경 `Cherry.Cherry100` → pressed `Cherry.Cherry200`, 전경 `Gray.Gray950` 고정, 테두리 투명 |
| `YGInputNumber` | 고정 정사각 치수, 테두리 두께, 배경·테두리·전경 색 4쌍, `body.b01R` |

### 드리프트 (수정 대상)

| # | 대상 | Figma | 현행 코드 | 조치 |
|---|---|---|---|---|
| B1 | `YGButton` 아이콘 크기 | `Medium`·`Large` 아이콘 프레임 20 / `SmallSquare` 24 | `Image`에 `size` 미적용 — `YGButtonType.iconSize`가 死필드, 아이콘이 리소스 내재 크기로 렌더 | `Image`에 `.size(buttonType.iconSize)` 적용 + `Medium.*`·`Large`의 `iconSize`를 `SizeTokens.Size20`으로 교정 |
| B2 | `Button-Medium` `Type=Secondary` 테두리 | 1px 테두리 — default·pressed `grayscale/gray-500`, disabled `grayscale/gray-300` | 테두리 없음. `YGButtonColors`에 색 필드 자체가 없어 표현 불가(#140에서 `borderColor` 제거) | `YGButtonColors`에 테두리 3상태 필드(기본 투명) + `borderColor()` 추가, `YGButton`에 `border` 체이닝, `Medium.Secondary`만 값 지정 |
| R1 | `Button-Medium`·`Button-Large` 모양 | 각짐(코너 반경 없음) | `radius.round`(pill) | `radius.none` |
| R2 | `Button-Input-Number` 모양 | 각짐 | `radius.xSmall` | `radius.none` |
| V1 | `Button-Icon` `Size=48` 아이콘 | 아이콘 프레임 32 | `iconSize = 28.dp` | `32.dp`로 교정(`Size=44`의 24는 일치) |
| V2 | `YGChipButton` 세로 패딩 | `padding-2` | `padding3` | `padding2` — [2026-07-27 open-questions](../../../synthesis/open-questions.md) 이월 항목 해소 |
| V3 | `Button-Chip-Left` pressed | 배경 `primary/cherry-100`, 테두리 없음 | 배경 `Cherry.Cherry50` 유지(pressed 변화 없음) + pressed 테두리 `Cherry.Cherry100` | pressed 배경 `Cherry.Cherry100`, pressed 테두리 투명 |
| V4 | `Button-Medium` `Type=Transparency` 배경 | default·disabled `transparency/white-50` | `Gray.White.copy(alpha = …)` — 순백이 아닌 `Gray.White` 기반이라 토큰과 다른 색 | `YGAtomicColors.Transparency.White50`으로 교체 |
| V5 | `Action-Item` 아이콘 변형 | `Show Icon: True` — 선두 아이콘 + `gap-2` | 변형 없음(텍스트 전용) | `iconResource` 파라미터 신설, `Box` → `Row` |

B1·B2를 먼저 처리하지 않으면 V1(아이콘 크기)·R1(각짐 + 테두리 동반) 작업 중 같은 파일을 두 번 고치게 된다.

V2는 `YGAlert`·`YGTopBar`가 `YGChipButton`을 쓰므로 두 컴포넌트 높이에 전파된다 — 의도된 결과다.
V3의 프리셋은 현재 프리뷰에서만 소비되므로 값 변경의 런타임 영향 범위가 없다.

## API / 인터페이스

### `YGButtonColors`

```kotlin
@Immutable
data class YGButtonColors(
    val enabledForegroundColor: Color,
    val disabledForegroundColor: Color,
    val pressedForegroundColor: Color,
    val enabledBackgroundColor: Color,
    val disabledBackgroundColor: Color,
    val pressedBackgroundColor: Color,
    val enabledBorderColor: Color = Color.Transparent,
    val disabledBorderColor: Color = Color.Transparent,
    val pressedBorderColor: Color = Color.Transparent,
) {
    fun borderColor(isEnabled: Boolean, isPressed: Boolean): Color = when {
        isEnabled.not() -> disabledBorderColor
        isPressed -> pressedBorderColor
        else -> enabledBorderColor
    }
}
```

- 신규 3필드는 기본값 투명 — 기존 `YGButtonType` 변형 4개의 생성자 호출을 고치지 않아도 컴파일된다.
  값을 채우는 것은 `Medium.Secondary` 하나다.
- 분기 우선순위(`disabled` → `pressed` → `enabled`)는 기존 `foregroundColor`·`backgroundColor`와 동일하게 맞춘다.
- 테두리 **두께**는 Colors가 아니라 `YGButton` 본문에 둔다. Figma 전 변형이 같은 두께이고,
  `YGChipButton`·`YGInputNumber`도 본문에 리터럴로 두는 기존 관용구다.

### `YGButton`

시그니처 무변경. 내부만 교정한다.

```kotlin
Row(
    modifier = modifier
        .background(color = buttonType.colors.backgroundColor(isEnabled, isPressed), shape = buttonType.radius)
        .clip(shape = buttonType.radius)
        .border(
            width = 1.dp,
            color = buttonType.colors.borderColor(isEnabled, isPressed),
            shape = buttonType.radius,
        ).clickable(/* 기존과 동일 */)
        .semantics { role = Role.Button }
        .padding(/* 기존과 동일 */),
) {
    startIconResource?.let { /* Image(modifier = Modifier.size(buttonType.iconSize)) */ }
    /* Text */
    endIconResource?.let { /* Image(modifier = Modifier.size(buttonType.iconSize)) */ }
}
```

- `border`는 `clip` **뒤**, `clickable` **앞**에 넣는다 — `clip` 앞에 두면 테두리가 잘리고,
  `padding` 뒤에 두면 콘텐츠 박스에 그려진다. `YGChipButton`과 같은 순서다.
- 테두리 색이 투명인 변형은 `border`가 아무것도 그리지 않으므로 분기를 두지 않는다.
- `Image`의 `size`는 두 아이콘 슬롯 모두에 적용한다. 현재는 어느 쪽에도 없다.

### `YGButtonType`

`iconSize`만 변경한다. 나머지 속성 계약은 유지한다.

| 변형 | `iconSize` | `radius` |
|---|---|---|
| `SmallSquare` | `SizeTokens.Size24` (유지) | `radius.none` (유지) |
| `Medium.Primary` | `SizeTokens.Size20` | `radius.none` |
| `Medium.Secondary` | `SizeTokens.Size20` | `radius.none` |
| `Medium.Transparency` | `SizeTokens.Size20` | `radius.none` |
| `Large` | `SizeTokens.Size20` | `radius.none` |

### `YGActionItem`

```kotlin
@Composable
fun YGActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

- `iconResource`가 `null`이면 현행과 동일한 텍스트 전용 렌더(기본값이라 기존 호출자 무영향).
- 컨테이너를 `Box` → `Row`(`Arrangement.spacedBy(gap.gap2)`, `Alignment.CenterVertically`)로 바꾼다.
- 아이콘 크기는 `SizeTokens.Size24`, tint는 텍스트와 **같은 색**(`Gray.Gray500` / pressed `Gray.Gray700`)을 쓴다.
  Figma가 tint를 에셋에 구워 대조할 수 없어, 텍스트와 함께 눌림에 반응하는 쪽으로 정한다(아래 열린 질문).
- `iconResource`는 파라미터 순서상 `modifier` 뒤에 둔다 — `YGChipButton`·`YGToggleButton`과 같은 배치다.

### `YGChipButtonColorsDefaults`

```kotlin
object YGChipButtonColorsDefaults {
    /** Figma Button-Chip-Left */
    val CherrySubtle: YGChipButtonColors

    /** Figma Button-Chip-Right */
    val CherrySolid: YGChipButtonColors
}
```

- 구 `CherryBorderPressed` → `CherrySubtle`, `CherryBackgroundPressed` → `CherrySolid`.
  V3 교정 후 `Chip-Left`에 pressed 테두리가 없어져 구 이름이 거짓이 된다.
  이름은 채도 강약을 뜻하게 바꾸고 Figma 변형명은 KDoc으로 병기한다
  (`YGToastType`에서 쓴 "의미 네이밍 유지 + Figma 변형명 KDoc" 규약과 같은 처리).
- 호출처 4파일을 함께 고친다(아래 파일 구성).

## 동작 / 상태

### `Button-Medium` 타입 × 상태 → 토큰 매핑

| 타입 | 상태 | 배경 | 전경 | 테두리 |
|---|---|---|---|---|
| `Primary` | default | `Gray.Gray900` | `Gray.White` | 투명 |
| `Primary` | pressed | `Gray.Gray950` | `Gray.White` | 투명 |
| `Primary` | disabled | `Gray.Gray200` | `Gray.Gray500` | 투명 |
| `Secondary` | default | `Gray.Gray100` | `Gray.Gray900` | `Gray.Gray500` |
| `Secondary` | pressed | `Gray.Gray200` | `Gray.Gray900` | `Gray.Gray500` |
| `Secondary` | disabled | `Gray.Gray200` | `Gray.Gray500` | `Gray.Gray300` |
| `Transparency` | default | `Transparency.White50` | `Gray.Gray900` | 투명 |
| `Transparency` | pressed | `Gray.White.copy(alpha = …)` | `Gray.Gray900` | 투명 |
| `Transparency` | disabled | `Transparency.White50` | `Gray.Gray500` | 투명 |

공통: `radius.none`, 가로 `padding4` / 세로 `padding5`, `gap2`, `body.b01SB`, 아이콘 `SizeTokens.Size20`.
`Large`는 `Primary`와 같은 색 3상태를 쓰고 폭만 호출자가 정한다.

`Transparency`의 pressed만 Figma에서 디자인 변수에 바인딩되지 않은 리터럴이라, 현행 코드값을 유지한다(아래 열린 질문).

### `Button-Chip` 프리셋 → 토큰 매핑

| 프리셋 | 상태 | 배경 | 전경 | 테두리 |
|---|---|---|---|---|
| `CherrySubtle` | default | `Cherry.Cherry50` | `Gray.Gray600` | 투명 |
| `CherrySubtle` | pressed | `Cherry.Cherry100` | `Gray.Gray700` | 투명 |
| `CherrySolid` | default | `Cherry.Cherry100` | `Gray.Gray950` | 투명 |
| `CherrySolid` | pressed | `Cherry.Cherry200` | `Gray.Gray950` | 투명 |

공통: `radius.round`, 세로 `padding2`, 아이콘 있는 쪽 `padding3` / 없는 쪽 `padding5`, `gap2`, `body.b02R`,
아이콘 `SizeTokens.Size16`.

### `Button-Icon` 크기 프리셋

| 프리셋 | 컨테이너 | 아이콘 |
|---|---|---|
| `SIZE_44` | 44 | 24 (유지) |
| `SIZE_48` | 48 | 32 (교정) |

tint 3상태(`Gray.Gray300` / pressed `Gray.Gray400` / disabled `Gray.Gray200`)는 대조 불가로 현행 유지한다.

## 표시·제어 규칙

- `YGActionItem`의 아이콘은 `iconResource != null`일 때만 노출한다. Figma `Show Icon` 불리언 변형과 대응한다.
- `YGButton`의 테두리는 색이 투명이면 시각적으로 없다 — 변형별 조건 분기를 두지 않는다.
- 각짐 전환(R1·R2)은 `YGButtonType`·`YGInputNumber` 내부 값 변경만으로 끝난다.
  호출처(`YGModalPopup`, 약관 동의·그룹 생성·초대 코드·그룹 닉네임 화면, `:app-preview`)의 코드 변경은 없고 시각만 바뀐다.
- `YGInputNumber`는 배경·`clip`·테두리 3곳이 같은 `shape`를 참조하므로 한 값만 두 번 이상 어긋나지 않도록 세 곳 모두 바꾼다.

## 파일 구성

### `core:designsystem` (수정)

| 파일 | 변경 |
|---|---|
| `component/ygbutton/YGButtonColors.kt` | B2 — 테두리 3필드 + `borderColor()` |
| `component/ygbutton/YGButton.kt` | B1 아이콘 `size` 적용, B2 `border` 체이닝 |
| `component/ygbutton/YGButtonType.kt` | B1 `iconSize` 교정(`Medium.*`·`Large`), B2 `Medium.Secondary` 테두리 색, R1 `radius.none` |
| `component/ygiconbutton/YGIconButtonSize.kt` | V1 — `SIZE_48` 아이콘 크기 |
| `component/ygchipbutton/YGChipButton.kt` | V2 — 세로 패딩 |
| `component/ygchipbutton/YGChipButtonColorsDefaults.kt` | V3 값 교정 + 프리셋 재명명 + KDoc |
| `component/ygchipbutton/YGChipButtonPreviewData.kt` | 프리셋 재명명 반영 |
| `component/ygactionitem/YGActionItem.kt` | V5 — `iconResource` 신설, `Row` 전환, 프리뷰에 아이콘 변형 추가 |
| `component/yginputnumber/YGInputNumber.kt` | R2 — `radius.none` 3곳 |
| `component/ygalert/YGAlert.kt` | 프리셋 재명명 반영(런타임 색 동일) |
| `component/ygtopbar/YGTopBar.kt` | 프리셋 재명명 반영(런타임 색 동일) |

`YGDateButton`·`YGButtonType.SmallSquare`의 값은 건드리지 않는다.

### `:app-preview` (수정)

| 파일 | 변경 |
|---|---|
| `screen/component/YGChipButtonPreviewScreen.kt` | 프리셋 재명명 반영(`PreviewSection` 라벨 2개 포함) |
| `screen/component/YGActionItemPreviewScreen.kt` | 아이콘 변형 섹션 추가 |

신규 `NavKey`·카탈로그 항목은 없다 — 대상 컴포넌트 전부 이미 갤러리에 등록돼 있다.

### 프리뷰 커버리지 요구

테스트 인프라가 없는 repo라 프리뷰·갤러리가 유일한 검증 수단이다. 각 컴포넌트 프리뷰가
Figma 변형 조합을 빠짐없이 렌더해야 한다.

| 컴포넌트 | 요구 조합 |
|---|---|
| `YGButton` | 4변형 × (enabled / disabled / pressed) + 아이콘 시작·끝 |
| `YGIconButton` | 2크기 × (default / pressed / disabled) |
| `YGChipButton` | 2프리셋 × (default / pressed) |
| `YGActionItem` | 아이콘 유 / 무 × (default / pressed) |
| `YGInputNumber` | selected / default |

pressed는 정적 프리뷰로 재현되지 않으므로 갤러리에서 직접 눌러 확인한다.

## 검증

테스트를 쓰지 않기로 정했다(`core:designsystem`에 테스트 소스셋이 없고 이 스펙에서 신설하지 않는다).
따라서 다음 순서로 확인한다.

1. `./gradlew :core:designsystem:assembleDebug` — 컴파일. B2의 기본값 인자가 기존 호출을 깨지 않는지 확인
2. `./gradlew ktlintCheck` — CI(`.github/workflows/ktlint.yml`)와 같은 게이트
3. IDE 프리뷰 — 위 커버리지 표대로 렌더 확인
4. `:app-preview` 실행 — 각짐·테두리·칩 높이·아이콘 크기를 Figma 화면과 나란히 육안 대조.
   pressed 상태는 여기서만 확인 가능
5. 각짐 전파 확인 — 약관 동의·그룹 생성·초대 코드·그룹 닉네임 화면과 `YGModalPopup`이
   pill에서 각짐으로 바뀐 모습이 의도대로인지 본다

## 주의 / 열린 질문

- **`Button-Icon` tint 대조 불가** — Figma가 아이콘 색을 에셋 이미지에 구워 내보내 `get_design_context`
  응답에 색 정보가 없다. 현행 3상태 tint를 유지하되 디자이너 확인이 필요하다.
  → [parfait open-questions](../../../synthesis/open-questions.md) 등록
- **`Medium.Transparency` pressed 배경의 토큰 누락** — Figma에서 이 값만 디자인 변수에 바인딩되지 않은
  리터럴이다. 나머지 두 상태는 `transparency/white-50`을 쓴다. 토큰화 요청 필요.
  → open-questions 등록
- **`Action-Item` 아이콘 tint 규칙 미확인** — 위와 같은 이유로 Figma에서 읽을 수 없다.
  이 스펙은 텍스트 색과 동일하게(눌림에 함께 반응) 정했다. 확인 후 정정 여지가 있다.
  → open-questions 등록
- **`YGChipButton` 세로 패딩(V2)은 이월 항목 해소** — [2026-07-27 open-questions](../../../synthesis/open-questions.md)에
  "칩 영역 sync 라운드로 이월"로 등록된 항목이다. 이 스펙 구현 시 해소 처리한다.
- **`YGButtonColors` 테두리 필드는 #140 복원** — 당시 `borderColor` 제거·`iconColor`→`foregroundColor` 통합이
  있었다. 통합은 유지하고 테두리만 되살린다. 새 축이 아니라 되돌림이므로 ADR을 만들지 않고
  [design-system](../../../architecture/design-system.md) 문서를 구현 후 갱신한다.
- **원자 색 직접 참조** — 대상 컴포넌트 전부 `YGAtomicColors`를 직접 읽는다. 기존 등록 사항이며
  이번에도 유지한다(`Transparency.White50` 교체 역시 원자 팔레트 내 이동이다).
- **테두리 두께 리터럴** — `1.dp`를 본문에 둔다. `SizeTokens.Size1`이 있으나 `YGChipButton`·`YGInputNumber`가
  리터럴을 쓰고 있어 일관성을 택했다. 두께 토큰화는 별도 라운드 사안이다.
