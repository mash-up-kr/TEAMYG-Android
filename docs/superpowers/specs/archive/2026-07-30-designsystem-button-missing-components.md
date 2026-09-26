---
id: designsystem-button-missing-components
title: 디자인시스템 버튼 영역 미구현 컴포넌트 신설 (Design System Missing Button Components)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-01
related_code:
  - YGEditTabButton.kt#YGEditTabButton
  - YGEditButton.kt#YGEditButton
  - YGCircleButton.kt#YGCircleButton
  - YGCircleButtonType.kt#YGCircleButtonType
  - YGEditActionButton.kt#YGEditActionButton
  - YGCameraShutter.kt#YGCameraShutter
  - SizeTokens.kt#SizeTokens
  - YGToggleButton.kt#YGToggleButton
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
related_adr:
  - ADR-0010
related_spec:
  - designsystem-button-component-sync
  - ygtogglebutton
  - app-preview-component-gallery
related_architecture:
  - design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, figma-sync]
---

# Spec: 디자인시스템 버튼 영역 미구현 컴포넌트 신설

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
>
> **구현 상태 — ✅ develop 머지 완료(PR #183, 2026-08-01)**. 5종 신설 + `YGToggleButton` 삭제 +
> `SizeTokens` `Size18`·`Size28` 추가 전량 반영. 신규 5종 갤러리 등록(`BUTTON` 카테고리)도 확인.
>
> **as-built 정정 3건(2026-08-01 기준선 점검, 코드가 정본)** — 아래 API 표의 서술을 머지 코드 기준으로 고친다.
> - **`YGCircleButtonType`이 `@get:Composable`이 아니다.** 인터페이스에 `@Immutable`을 달고 전 속성이
>   평범한 `val`(테마 미경유, `YGAtomicColors`·`SizeTokens` 상수 직접 대입)이다. 세 변형이 모두
>   `data object`라 컴포지션 시점 값이 필요 없다는 판단으로 보인다 — `YGButtonType`의 "변형이 자기 토큰을
>   `@get:Composable`로 노출" 패턴과는 갈린다.
> - **`paintsOuterCircle: Boolean` 속성이 추가됐다.** 스펙은 "`Small`만 바깥 터치 영역이 별도로 필요하므로
>   컴포저블이 타입으로 분기"라고만 했는데, 그 분기 조건이 타입 속성으로 올라갔다. `Default`·`Secondary`는
>   `true`(바깥 원에 배경·테두리), `Small`은 `false`(내부 `Size28` 원에만 배경·테두리 + 투명 44 래핑).
> - **`Default`·`Small`의 `iconTint`가 `Gray.Gray850`이다**(스펙 표는 `Gray.Gray900`). `Secondary`는
>   설계대로 `Gray.White`. Figma가 tint를 노출하지 않아 팔레트 근사로 정한 값이라 실물 기준으로 한 단계
>   내려간 것이며, 확인 대상은 [open-questions](../../../synthesis/open-questions.md) [2026-07-30] 아이콘 tint 항목에 남아 있다.
>
> **상호작용 관용구 as-built** — `YGEditButton`은 `selectable(role = Role.Button)`, `YGEditTabButton`은
> `selectable(role = Role.Tab)`을 쓴다(`isSelected`가 prop이라 선택형 시맨틱). `YGCircleButton`·
> `YGEditActionButton`·`YGCameraShutter`는 `clickable(indication = null)` + `semantics { role = Role.Button }`.
>
> **함께 온 아이콘 반입** — 신설 컴포넌트 프리뷰가 쓰는 `ic_rotate`·`ic_minus_round` 등은 같은 PR의
> 아이콘 현행화분이다(선행 스펙 [designsystem-button-component-sync](2026-07-30-designsystem-button-component-sync.md) 노트 참고).
>
> **설계에서 달라진 점 2건** — 둘 다 갤러리 실기기 검증에서 드러난 결함을 고친 것이다.
> - **`YGCircleButtonType`에 `iconTint` 추가** — 스펙은 "Figma가 아이콘 색을 에셋에 담아 대조값이
>   없으니 리소스 색을 그대로 쓴다"고 했으나, 저장소 아이콘 드로어블이 전부 검정이어서
>   `Type=Secondary`(어두운 원)에서 아이콘이 배경에 묻혔다. Figma 스크린샷으로 Secondary 아이콘이
>   흰색임을 확인하고 tint를 타입 속성으로 올렸다: `Default`·`Small` = `Gray.Gray850`(머지 코드 기준),
>   `Secondary` = `Gray.White`.
> - **`YGEditTabButton` 밑줄 폭 제약** — 밑줄에 `fillMaxWidth`만 걸면 "부모가 준 최대 폭"을 채워
>   화면 전체로 늘어나고 나머지 탭이 밀려난다. 컴포넌트 `Column`에 `width(IntrinsicSize.Max)`를
>   더해 텍스트 폭으로 묶었다.
>
> **미검증**: pressed 상태 전반(자동 입력이 Compose `interactionSource`에 반영되지 않는다 —
> 선행 라운드와 같은 한계). 손으로 눌러 확인해야 한다.

## 목표

Figma "버튼" 영역 14종 대조에서 **대응 구현체가 아예 없던 5종**을 신설한다.
드리프트 제거분 7종은 선행 스펙 [designsystem-button-component-sync](2026-07-30-designsystem-button-component-sync.md)가 다뤘다.

함께 `YGToggleButton`을 삭제한다. 이 컴포넌트는 대응 Figma 원본을 찾을 수 없었고(pill·토글 반전·아이콘 앞
구조가 `Button-Edit`도 `Button-Stroke`도 아니다) 실화면 사용처가 없다. 대체물인 `Button-Edit`가
이 스펙의 산출물이므로 같은 라운드에서 지운다.

## 범위

- **포함**
  - `YGEditTabButton`(Figma `Button-Edit-Tab`) — 하단 밑줄 탭
  - `YGEditButton`(Figma `Button-Edit`) — 각짐 + 테두리, 텍스트 뒤 아이콘, Default/Selected
  - `YGCircleButton`(+`YGCircleButtonType`, Figma `Button-Circle`) — Default/Secondary/Small 3변형
  - `YGEditActionButton`(Figma `Button-Edit-Action`) — 반투명 원형 액션 버튼
  - `YGCameraShutter`(Figma `Camera-Shutter`) — 이중 원 셔터
  - `SizeTokens`에 28·18 스케일 추가
  - `YGToggleButton`·`YGToggleButtonPreviewData` 삭제 + `:app-preview` 잔재 정리
  - 신규 5종을 `:app-preview` 컴포넌트 갤러리에 등록
- **제외**
  - **`feature/camera`의 임시 구현체 정리** — `ShutterButton`·`FlipCameraButton`·취소 `TextButton`을
    그대로 둔다. `YGCameraShutter`를 designsystem에 만들지만 카메라 화면을 치환하지 않는다.
    화면 쪽 교체는 C-101 카메라 화면 라운드에서 한다(아래 [주의 / 열린 질문](#주의--열린-질문))
  - `Button-Stroke`(Figma) — 이번 대조 대상 14종 밖
  - 색 주입 API(`*Colors` data class) — 아래 [Colors 분리 판단](#colors-분리-판단)
  - 신규 ADR — 아키텍처 결정 변화 없음

## 명명·패키지

기존 "컴포넌트당 폴더 + `YG` 접두사" 규약을 따른다. Figma 변형명은 KDoc으로 병기한다
(`YGToastType`에서 쓴 방식 — 코드는 의미 네이밍, 추적은 KDoc).

| Figma | 심볼 | 패키지 |
|---|---|---|
| `Button-Edit-Tab` | `YGEditTabButton` | `component/ygedittabbutton/` |
| `Button-Edit` | `YGEditButton` | `component/ygeditbutton/` |
| `Button-Circle` | `YGCircleButton` + `YGCircleButtonType` | `component/ygcirclebutton/` |
| `Button-Edit-Action` | `YGEditActionButton` | `component/ygeditactionbutton/` |
| `Camera-Shutter` | `YGCameraShutter` | `component/ygcamerashutter/` |

## 치수 도출 원칙

**패딩으로 도출되는 치수는 하드코딩하지 않는다.** Figma가 패딩 + 자식 크기로 프레임을 잡은 곳은
같은 구조(패딩 토큰 + 아이콘 크기 토큰)로 옮기고, Figma가 지름을 직접 고정한 곳만 크기 토큰으로 못박는다.

`Button-Circle` `Type=Small`은 **2026-07-30 재조회에서 정수 치수로 정리됐다**(내부 원 28 명시,
아이콘 18, 글리프 12, 바깥 폭 44 명시). 이전 판에서는 `Type=Default` 인스턴스를 균일 축소한
소수값(아이콘 17.818 등)이었다. 구조도 다르다 — Small의 아이콘은 패딩으로 밀리는 것이 아니라
**28 원 안에 절대 중앙 정렬**이다.

| 대상 | 조합 | 도출 지름 | Figma |
|---|---|---|---|
| Circle `Default`·`Secondary` | `padding3` + 아이콘 `Size28` | 44 | 44 / 아이콘 28 — 일치 |
| Circle `Small` 바깥 | `padding3` + 내부 원 | 44 | 44 — 일치 |
| Circle `Small` 내부 원 | 지름 `Size28` 고정 + 중앙 아이콘 `Size18` | 28 | 28 / 아이콘 18 — 일치 |
| Edit-Action 내부 원 | `padding3` + 아이콘 `Size24` | 40 | 38 / 아이콘 22 — 2dp 큼 |
| Edit-Action 바깥 | `padding1` 래핑 | 44 | 42 — 2dp 큼 |
| Camera-Shutter | `padding2` + 내부 원 `Size48` | 56 | 56 / 내부 48 — 일치 |

`SizeTokens`에 `Size28`·`Size18`을 추가한다([design-system](../../../architecture/design-system.md)
"신규 토큰 값 추가 체크리스트" 3항). 이 둘로 `Button-Circle` 3변형이 Figma와 정확히 일치한다.

만들지 않는 것:
- `Size56` — 셔터 외곽은 `padding2` + 내부 `Size48`로 도출된다.
- `Size22` — `Button-Edit-Action` 아이콘만 쓰는 값이라 스케일을 늘리지 않고 `Size24`로 옮긴다.
  그 결과 내부 원·바깥 프레임이 2dp 커진다(열린 질문).

## API / 인터페이스

### `YGCircleButtonType`

```kotlin
@Immutable
sealed interface YGCircleButtonType {
    val backgroundColor: Color

    val pressedBackgroundColor: Color

    val borderColor: Color

    val iconTint: Color

    val iconSize: Dp

    /** true = 바깥 원에 배경·테두리, false = 내부 원에만 */
    val paintsOuterCircle: Boolean

    data object Default : YGCircleButtonType

    data object Secondary : YGCircleButtonType

    /** 44 터치 영역 안에 작은 원을 그린다 */
    data object Small : YGCircleButtonType
}
```

as-built에서는 변형별 KDoc을 달지 않았다 — 파일 상단 컴포넌트 KDoc(`Figma Button-Circle`)이 대응을 이미 밝히고, 변형명이 Figma 변형명(`Type=Default`/`Secondary`/`Small`)과 같아 중복이다.
아래 "명명·패키지"의 KDoc 병기 규약은 **컴포넌트 단위**에 적용된다.

`YGButtonType`과 같은 "변형이 자기 토큰을 `@get:Composable`로 노출" 패턴이다.
`Small`만 바깥 터치 영역이 별도로 필요하므로 컴포저블이 타입으로 분기한다.

```kotlin
@Composable
fun YGCircleButton(
    @DrawableRes iconResource: Int,
    type: YGCircleButtonType,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

- 아이콘을 주입받는다 — Figma가 변형별로 다른 글리프(`ic_caret_left`/`ic_plus`/`ic_rotate`)를 쓰지만
  그것은 사용처의 의미이고 컴포넌트의 고정 속성이 아니다. `YGIconButton`과 같은 계약이다.
- `contentDescription`은 필수 인자다(`YGIconButton` 선례) — 아이콘만 있는 버튼이라 생략하면 접근성이 빈다.

### `YGEditButton`

```kotlin
@Composable
fun YGEditButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

- 폭은 호출자가 정한다(Figma 162는 캔버스 기준폭) — `YGButton` 관례와 동일.
- 아이콘은 텍스트 **뒤**에 온다. Figma에 아이콘과 텍스트 사이 간격 토큰이 없으므로 `Arrangement` 간격을 두지 않는다.
- `isSelected`는 prop이다(`YGToggleButton`·`YGInputNumber`와 동일) — 런타임 pressed와 무관하다.

### `YGEditTabButton`

```kotlin
@Composable
fun YGEditTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

밑줄은 텍스트 폭에만 걸린다 — 바깥 패딩(`padding4`/`padding3`)은 터치 영역이고,
밑줄은 그 안쪽 텍스트 컨테이너 하단에 그린다. 두께는 `1.4.dp` 리터럴을 쓴다
(`YGDate`의 `0.75.dp` 선례 — 테두리 두께에 토큰 스케일이 없다).

**폭 제약이 필수다.** 밑줄에 `fillMaxWidth`만 주면 부모가 준 최대 폭을 채워 화면 전체로 늘어난다.
컴포넌트 `Column`에 `width(IntrinsicSize.Max)`를 걸어야 텍스트 폭으로 묶인다(구현 중 확인).

### `YGEditActionButton`

```kotlin
@Composable
fun YGEditActionButton(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

3상태(default/pressed/disabled)를 가지므로 `isEnabled`를 노출한다. 테두리 두께 `1.5.dp` 리터럴.

### `YGCameraShutter`

```kotlin
@Composable
fun YGCameraShutter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)
```

아이콘이 없다 — 흰 외곽 원 + 어두운 내부 원 두 도형이다. Figma는 두 원을 래스터 에셋으로 내보내지만
도형 2개로 재현하므로 에셋을 가져오지 않는다.

## 동작 / 상태

### `YGCircleButton`

| 타입 | 배경 | pressed 배경 | 테두리 | 아이콘 | 아이콘 tint | 기본 사용 글리프 |
|---|---|---|---|---|---|---|
| `Default` | `Gray.White` | `Gray.Gray100` | 1dp `Transparency.Black5` | `Size28` | `Gray.Gray850` | `ic_caret_left` |
| `Secondary` | `Gray.Gray900` | `Gray.Gray950` | 1dp `Transparency.White25` | `Size28` | `Gray.White` | `ic_plus` |
| `Small` | `Gray.White` | `Gray.Gray100` | 1dp `Transparency.Black5` | `Size18` | `Gray.Gray850` | `ic_rotate` |

공통: `shapes.radius.round`.
`Default`·`Secondary`는 원 지름이 `padding3` + 아이콘에서 도출되고, 배경·테두리가 그 원에 걸린다.
`Small`은 **바깥 `padding3` 래핑(투명, 터치 영역 44) + 내부 원 `Size28`(배경·테두리) + 중앙 아이콘 `Size18`** 구조다.
Figma `Small`의 테두리만 아직 소수(0.636)로 남아 있어 1dp로 정규화한다.

### `YGEditButton`

| 상태 | 배경 | 테두리 | 전경 |
|---|---|---|---|
| `Default` | `Gray.White` | `Gray.Gray100` | `Gray.Gray900` |
| `Selected` | `Gray.Gray900` | `Gray.Gray900` | `Gray.White` |

공통: `shapes.radius.none`(각짐), 테두리 1dp, 세로 `padding3`, `typography.body.b02SB`, 아이콘 `Size24`.
아이콘 tint는 전경색을 따른다.

### `YGEditTabButton`

| 상태 | 텍스트 | 밑줄 |
|---|---|---|
| `Default` | `body.b01R` / `Gray.Gray500` | 없음 |
| `Selected` | `body.b01SB` / `Gray.Gray900` | 1.4dp `Gray.Gray900` |

공통: 가로 `padding4` / 세로 `padding3`, 텍스트 컨테이너 하단 `padding2`.

### `YGEditActionButton`

| 상태 | 배경 | 테두리 | 아이콘 tint |
|---|---|---|---|
| default | `Transparency.Black50` | 1.5dp `Transparency.White25` | `Gray.White` |
| pressed | `Transparency.Black75` | 1.5dp `Transparency.White25` | `Gray.White` |
| disabled | `Transparency.Black5` | 1.5dp `Transparency.White25` | `Gray.White` |

공통: `radius.round`, 내부 `padding3`, 바깥 `padding1`, 아이콘 `Size24`.
Figma가 아이콘 색을 에셋에 구워 tint를 노출하지 않으므로 `Gray.White` 고정으로 둔다(열린 질문).

### `YGCameraShutter`

| 상태 | 외곽 원 | 내부 원 |
|---|---|---|
| default | `Gray.White` | `Gray.Gray900` |
| pressed | `Gray.White` | `Gray.Gray950` |

공통: 두 원 모두 `radius.round`, 외곽에 `padding2`, 내부 원 `Size48`.

## Colors 분리 판단

신규 5종은 `YGButtonColors`·`YGChipButtonColors` 같은 색 주입 data class를 **만들지 않는다**.
다섯 컴포넌트 모두 Figma가 변형별 색을 고정하고 있고, 색을 바꿔 쓸 사용처가 없다.
`YGCircleButton`만 변형이 3개라 `YGCircleButtonType`이 색을 들고 있고, 나머지는 컴포저블 본문에서
상태 분기한다.

[design-system](../../../architecture/design-system.md)의 컴포넌트 작성 규약은 `YGButton` 기준으로
"Colors data class 분리"를 적어 두었으므로 이 판단은 규약과 갈린다. 어떤 조건에서 분리가 필요한지
(주입 요구 유무 기준) 규약을 다듬어야 한다 → [주의 / 열린 질문](#주의--열린-질문).

## `YGToggleButton` 삭제

| 파일 | 처리 |
|---|---|
| `component/ygtogglebutton/YGToggleButton.kt` | 삭제 |
| `component/ygtogglebutton/YGToggleButtonPreviewData.kt` | 삭제 |
| `:app-preview` `navigation/key/NavKeyYGToggleButton.kt` | 삭제 |
| `:app-preview` `screen/component/YGToggleButtonPreviewScreen.kt` | 삭제 |
| `:app-preview` `model/ComponentCatalog.kt` | 항목 제거 |
| `:app-preview` `navigation/entry/ComponentEntryBuilders.kt` | `entry` 블록 + import 제거 |

실화면 사용처가 없어 삭제로 깨지는 호출부가 없다. 삭제하면 [2026-07-16 open-questions](../../../synthesis/open-questions.md)
"YGToggleButton 규약 이탈" 항목이 해소된다(대상 코드가 사라지므로).

## 파일 구성

### `core:designsystem` (신규 6 / 수정 1 / 삭제 2)

| 파일 | 역할 |
|---|---|
| `component/ygedittabbutton/YGEditTabButton.kt` (신규) | 컴포저블 + 프리뷰 |
| `component/ygeditbutton/YGEditButton.kt` (신규) | 컴포저블 + 프리뷰 |
| `component/ygcirclebutton/YGCircleButton.kt` (신규) | 컴포저블 + 프리뷰 |
| `component/ygcirclebutton/YGCircleButtonType.kt` (신규) | 3변형 토큰 |
| `component/ygeditactionbutton/YGEditActionButton.kt` (신규) | 컴포저블 + 프리뷰 |
| `component/ygcamerashutter/YGCameraShutter.kt` (신규) | 컴포저블 + 프리뷰 |
| `theme/size/SizeTokens.kt` (수정) | `Size28`·`Size18` 추가 |
| `component/ygtogglebutton/*` (삭제) | 위 삭제 표 |

프리뷰는 `@YGPreview` + `PreviewBox`, 함수는 `private`. 상태가 여러 개인 컴포넌트는
`PreviewParameterProvider` 대신 `Column` 나열로 둔다 — 변형 수가 적고 한눈에 비교해야 한다.

### `:app-preview` (신규 10 / 수정 2 / 삭제 2)

| 파일 | 역할 |
|---|---|
| `navigation/key/NavKeyYG{EditTabButton,EditButton,CircleButton,EditActionButton,CameraShutter}.kt` (신규 5) | `@Serializable data object … : NavKey` |
| `screen/component/YG{…}PreviewScreen.kt` (신규 5) | 변형·상태 showcase |
| `model/ComponentCatalog.kt` (수정) | `BUTTON` 카테고리에 5줄 추가, `YGToggleButton` 줄 제거 |
| `navigation/entry/ComponentEntryBuilders.kt` (수정) | `entry` 5블록 추가, 토글 블록 제거 |

`navigation/di/ComponentEntryModule.kt`는 수정하지 않는다 — `@IntoSet` 바인딩이 함수 단위다
([app-preview-component-gallery](2026-07-21-app-preview-component-gallery.md) 구조).

### 갤러리 showcase 구성 요구

`isSelected`·`isEnabled`를 갖는 컴포넌트는 정적 나열과 함께 **`remember` 인터랙션**을 둔다
(갤러리 규약). pressed는 정적 렌더로 재현되지 않으므로 실기기에서 눌러 확인한다.

| 화면 | 나열 |
|---|---|
| `YGEditTabButton` | Default / Selected + 탭 2개를 `remember`로 토글 |
| `YGEditButton` | Default / Selected + 아이콘 유·무 + `remember` 토글 |
| `YGCircleButton` | 3변형 × 기본 글리프 |
| `YGEditActionButton` | enabled / disabled |
| `YGCameraShutter` | 단일(pressed는 실기기 확인) |

## 검증

테스트를 쓰지 않는다(선행 스펙과 동일 결정, `core:designsystem`에 테스트 소스셋 없음).

1. `./gradlew :core:designsystem:assembleDebug :app-preview:assembleDebug`
2. `./gradlew ktlintCheck`
3. `YGToggleButton` 잔존 참조 0건 확인
4. `:app-preview` 실기기 — 5종 화면에서 Figma와 나란히 육안 대조. 원 지름·밑줄 두께·반투명 배경·셔터 이중 원
5. pressed·selected 상호작용을 눌러 확인

## 주의 / 열린 질문

- **`feature/camera` 임시 구현체 잔존** — `YGCameraShutter`를 만들지만 `ShutterButton`(72dp 리터럴)·
  `FlipCameraButton`(이모지 텍스트 + 리터럴 색)·취소 `TextButton`을 이번에 치환하지 않는다.
  즉 셔터 구현이 두 곳에 공존한다. 카메라 화면 sync 라운드에서 정리해야 한다.
  → [parfait open-questions](../../../synthesis/open-questions.md) 등록
- **`Button-Circle` 변형과 카메라 컨트롤의 대응 미확정** — `Small`의 `ic_rotate` 글리프가 카메라 전환
  버튼처럼 보이지만 Figma 컴포넌트 시트만으로는 단정할 수 없다. 화면 노드 대조가 필요하다. → 위 항목과 함께 추적
- **`Button-Edit-Action` 2dp 오차** — 아이콘 22를 `Size24`로 옮겨 내부 원(38→40)과 바깥 프레임(42→44)이
  2dp 커진다. `Size22`를 만들지 않는 절충이다. `Button-Circle`은 2026-07-30 Figma 정수화 + `Size18` 추가로
  오차가 없어졌다. 디자이너가 Edit-Action도 정수 치수로 정리해주면 오차가 사라진다.
  → open-questions 등록
- **`Camera-Shutter`의 `Transparency.Black5` 용도 미확인** — Figma 변수 목록에 잡히지만 두 원의
  채움 색으로는 설명되지 않는다(외곽 테두리나 그림자일 가능성). 이번 구현은 두 원만 그린다.
  → open-questions 등록
- **`Button-Edit-Action` 아이콘 tint 미대조** — Figma가 색을 에셋에 구워 노출하지 않는다.
  선행 스펙의 같은 항목([2026-07-30 open-questions](../../../synthesis/open-questions.md))과 동일 사유이며 그 항목에서 함께 추적한다.
- **신규 버튼군 Colors 미분리** — 위 [Colors 분리 판단](#colors-분리-판단). 규약을 "색 주입 요구가
  있을 때만 분리"로 다듬을지 결정이 필요하다. → open-questions 등록
- **원자 색 직접 참조** — 5종 모두 `YGAtomicColors`를 직접 읽는다. 기존 등록 사항이며 이번에도 유지한다.
