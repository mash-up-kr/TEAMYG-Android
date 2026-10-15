---
id: designsystem-text-component-sync
title: 디자인시스템 텍스트 영역 컴포넌트 Figma 동기화 (Design System Text Components Figma Sync)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-29
related_code:
  - YGDate.kt#YGDate
  - YGLabel.kt#YGLabel
  - YGToast.kt#YGToast
  - YGToast.kt#YGToastType
  - YGToastPolicy.kt#YGToastPolicy
  - YGToastPolicy.kt#YGToastHost
  - YGAlert.kt#YGAlert
  - YGAlertPolicy.kt#YGAlertPolicy
  - YGAlertPolicy.kt#YGAlertItem
  - YGAlertPolicy.kt#YGAlertHost
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
related_adr:
  - ADR-0010
related_spec:
  - ygtoast
  - ygalert
  - ygtext-date-label
  - app-preview-component-gallery
  - designsystem-preview-migration
related_architecture:
  - design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, figma-sync]
---

# Spec: 디자인시스템 텍스트 영역 컴포넌트 Figma 동기화

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
>
> **구현 완료(2026-07-29, PR #181 develop 머지)** — 코드=설계 일치. D1~D4·T1~T4·A1·A3·P1·P2 전부 반영,
> 갤러리 showcase 2화면(`YGToast`·`YGAlert`) 등록 확인. `YGAlert` 트리거의 `onButtonClick`은 스펙 요구대로
> 관찰 가능한 동작(`title = "clicked"` 재노출)으로 배선됨. 남은 항목(호스트 애니메이션·스택 결함,
> `YGChipButton` 세로 패딩, `Record` 문구 하드코딩)은 [open-questions](../../../synthesis/open-questions.md) [2026-07-27]에서 추적.

## 목표

Figma `[디자인] 파르페 v0.1` 파일의 **Components 섹션 > Detail Type "텍스트"** 영역(Label·Date·Toast·Alert)을
현행 코드와 1:1 대조해 발견한 드리프트를 제거한다. 4종 중 `YGLabel`은 일치, 나머지 3종에서
치수·채움·타입 누락·파라미터 오배선이 확인됐다.

동시에, 이 4종 중 갤러리에 없던 `YGToast`·`YGAlert`을 `:app-preview` 컴포넌트 갤러리에 등록해
이후 sync 라운드에서 눈으로 대조할 수 있는 상태를 만든다.

## 범위

- **포함**
  - `YGDate` — 배경 채움 누락, `modifier` 오배선, 텍스트 간격 시맨틱 교정
  - `YGToast` — 패딩 교정, `Fail` 타입 신설
  - `YGAlert` — 컴포저블 무변경. `YGAlertPolicy`/`YGAlertItem`/`YGAlertHost`에 버튼 변형 전달 경로 추가
  - `YGDate`·`YGToast`·`YGAlert` 프리뷰를 `@YGPreview` + `PreviewBox` 규약으로 통일
    ([designsystem-preview-migration](2026-07-18-designsystem-preview-migration.md)에서 누락된 3건)
  - `YGToastHost`·`YGAlertHost` 프리뷰 신설 — 정책 파일 2개에 프리뷰가 아예 없었다(P1·P2)
  - `:app-preview` 갤러리에 `YGToast`·`YGAlert` showcase 화면 추가
- **제외**
  - `YGLabel` — Figma와 일치. 변경 없음
  - `YGChipButton` 세로 패딩(현행 높이 39 vs Figma 29) — 칩 영역 컴포넌트이며 `YGTopBar` 등으로 전파.
    칩 영역 sync 라운드에서 처리 (아래 [주의 / 열린 질문](#주의--열린-질문))
  - `YGToastPolicy`/`YGAlertHost`의 노출·소멸 정책 — 코드가 Figma 참고사항과 이미 일치(아래 표)
  - 신규 ADR — 아키텍처 결정 변화 없음

## Figma ↔ 코드 대조 결과

대조 기준 Figma 노드: `Label`, `Date`, `Toast`(4변형), `Alert`.
`Date-수정 전`·`Input-수정 전` 등 `-수정 전` 접미 노드는 구판이므로 대조 대상에서 제외한다.

### 일치 확인 (변경 없음)

| 대상 | 확인 내용 |
|---|---|
| `YGLabel` | `body.b02R` + `Gray.Gray400` — Figma `Body/B02/B02_R` + `grayscale/gray-400` 일치 |
| `YGAlert` 레이아웃 | 배경 `Transparency.Black75`, 가로 `padding7`, 세로 `padding5`, 본문 열 `gap2` |
| `YGAlert` 색 | 제목 `Cherry.Cherry200`, 부제 `Transparency.White75` |
| `YGAlert` 칩 | `YGChipButtonColorsDefaults.CherryBackgroundPressed`(`Cherry100` 배경 / `Gray950` 전경 / 투명 테두리) + `ic_caret_right` |
| 노출·소멸 정책(일부) | Toast 2초·Alert 2.5초 자동 소멸, 위로 스와이프 닫기 — Figma 참고사항과 일치 |

> ⚠️ **정정(2026-07-27, 최종 리뷰)** — 초안은 위 행에 "슬라이드 인/아웃"과 "다중 스택"까지 일치로 적었으나
> **둘 다 코드에서 동작하지 않는다.** 이 스펙이 다루는 4종 컴포넌트가 아니라 호스트(`YGToastPolicy`·
> `YGAlertPolicy`)의 **기존 결함**이며, 이번 sync가 건드리지 않은 코드다. 갤러리 추가로 두 호스트가
> 처음 실행되면서 드러났다.
>
> - **애니메이션 미동작** — `AnimatedVisibility`가 `visible = true` 상태로 최초 컴포즈돼 입장 transition이
>   돌지 않고, 퇴장은 `visible = false` 기록과 목록 제거가 같은 프레임에서 일어난다(Alert은 `alert = null`로
>   즉시 해체). `visible` 필드·`setVisible()`·`exit =` 인자가 모두 死코드.
> - **Toast 다중 스택 미동작** — `YGToastHost`가 `Box`라 동시 토스트가 쌓이지 않고 겹쳐 그려진다.
>   `show`가 `add(0, …)`로 앞에 넣으므로 최신 토스트가 오히려 아래 깔린다.
>
> 둘 다 [parfait open-questions](../../../synthesis/open-questions.md) [2026-07-27] 항목으로 등록. 별도 라운드에서 처리한다.

### 드리프트 (수정 대상)

| # | 대상 | Figma | 현행 코드 | 조치 |
|---|---|---|---|---|
| D1 | `YGDate` 배경 | `color/base/white` 채움 | 채움 없음(투명) | `Gray.White` 배경 추가 |
| D2 | `YGDate` `modifier` | — | 호출자 `modifier`가 `Row`가 아닌 **두 번째 `Text`**에 붙음 | `Row`로 이동 |
| D3 | `YGDate` 텍스트 간격 | `gap-3` | 두 번째 `Text`의 `start` 패딩 | `Arrangement.spacedBy(gap.gap3)`로 교체(값 동일, 시맨틱 교정) |
| D4 | `YGDate` 프리뷰 | — | `@YGPreview` + `YGCustomTheme`(반쪽 이관) | `@YGPreview` + `PreviewBox` |
| T1 | `YGToast` 패딩 | 가로 `padding-6` / 세로 `padding-5` | 가로 `padding4` / 세로 `padding6` | 가로 `padding6` / 세로 `padding5`로 교정 |
| T2 | `YGToast` 타입 수 | 4변형(`Alert`/`Warning`/`Success`/`Error`) | 3종 — `Error` 대응 없음 | `Fail` 타입 신설 |
| T3 | `YGToast` 타입명 | `Alert`/`Warning`/`Success`/`Error` | `Record`/`Edit`/`InviteCode` | **리네임하지 않음** — 의미 네이밍 유지 + Figma 변형명 KDoc 병기 |
| T4 | `YGToast` 프리뷰 | — | `@Preview` + `YGCustomTheme` | `@YGPreview` + `PreviewBox` |
| A1 | `YGAlert` 프리뷰 | — | `@Preview` + `YGCustomTheme` | `@YGPreview` + `PreviewBox` |
| A3 | `YGAlertPolicy` | Alert은 버튼 유/무 2변형 | `show(title, sub)`만 — 호스트가 버튼 변형을 띄울 수 없음 | `buttonText`·`onButtonClick` 전달 경로 추가 |
| P1 | `YGAlertPolicy` 프리뷰 | — | 프리뷰 없음 — 호스트 렌더를 IDE에서 볼 수 없음 | `YGAlertHostPreview` 신설(`@YGPreview` + `PreviewBox`) |
| P2 | `YGToastPolicy` 프리뷰 | — | 프리뷰 없음 (동일) | `YGToastHostPreview` 신설(`@YGPreview` + `PreviewBox`) |

T1의 패딩 방향이 뒤바뀐 결과 현행 Toast 높이가 Figma 설계치보다 커져 있다. 교정 후 Figma 변형 높이와 일치한다.

### 호스트 프리뷰 구성 (P1·P2)

정적 프리뷰는 `LaunchedEffect`를 실행하지 않는다. 따라서 `rememberYGAlertPolicy()` + `LaunchedEffect { show(…) }`
로 짜면 `alert`/`toasts`가 비어 프리뷰가 빈 화면으로 렌더된다. **최초 컴포지션 시점에 상태를 채워야 한다:**

```kotlin
val policy = remember { YGAlertPolicy().apply { show(title = "Title", sub = "Sub") } }
```

부수효과로 호스트 내부의 자동 소멸 `LaunchedEffect`도 돌지 않아 프리뷰에 인스턴스가 계속 남는다(의도).

- `YGAlert`은 단일 슬롯이라 **정책 인스턴스 2개**(버튼 유/무)를 `Column`으로 나열한다.
- `YGToast`는 타입 4종이지만 **호스트 4개**로 나열한다 — 호스트 1개에 4건을 `show`하면
  `Box` 컨테이너 때문에 같은 원점에 겹쳐 그려져(위 정정 노트의 스택 결함) 맨 위 1건만 보인다.

## API / 인터페이스

### `YGDate`

```kotlin
@Composable
fun YGDate(
    date: String,
    day: String,
    modifier: Modifier = Modifier,
)
```

시그니처 무변경. 내부 구현만 교정한다.

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    modifier = modifier
        .background(color = YGAtomicColors.Gray.White)
        .border(width = 0.75.dp, color = YGAtomicColors.Gray.Gray800)
        .padding(
            vertical = YGTheme.layout.padding.padding3,
            horizontal = YGTheme.layout.padding.padding4,
        ),
) { /* date, "($day)" */ }
```

- `background`는 `border` **앞**에 체이닝한다 — 테두리가 배경 위에 그려져야 한다.
- 두 `Text`의 `modifier` 인자는 제거한다(간격은 `Arrangement`가 담당).
- 테두리 두께 `0.75.dp`·패딩 토큰은 Figma와 이미 일치하므로 유지한다.

### `YGToastType`

```kotlin
sealed interface YGToastType {
    /** Figma Toast Type=Success */
    data class InviteCode(val text: String) : YGToastType

    /** Figma Toast Type=Warning */
    data class Edit(val text: String) : YGToastType

    /** Figma Toast Type=Alert */
    data class Record(val userName: String, val time: String) : YGToastType

    /** Figma Toast Type=Error */
    data class Fail(val text: String) : YGToastType
}
```

- 타입명은 **현행 의미 기반 네이밍을 유지**하고, Figma 변형명은 KDoc으로 병기해 추적한다.
  Figma의 `Type=Alert`을 그대로 쓰면 `YGAlert` 컴포넌트와 이름이 충돌해 혼동을 부른다.
- `Fail`은 완성 문장을 주입받는다 — `InviteCode`·`Edit`과 동일 규약.
- `YGToast`·`YGToastPolicy`는 `YGToastType`을 그대로 받으므로 시그니처 변경 없음.
  `when(type)`은 sealed interface라 새 분기를 컴파일러가 강제한다.

### `YGAlertPolicy`

```kotlin
data class YGAlertItem(
    val id: String,
    val title: String,
    val sub: String,
    val buttonText: String? = null,
    val onButtonClick: (() -> Unit)? = null,
    val visible: Boolean = true,
)

fun show(
    title: String,
    sub: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
)
```

- 두 신규 인자는 기본값 `null` — 기존 `show(title, sub)` 호출 형태가 그대로 유효하다.
- `YGAlertHost`는 두 값을 `YGAlert`에 그대로 전달한다. `YGAlert`의 기존 계약
  (`buttonText`가 `null`이면 칩 미노출)이 변형 분기를 담당하므로 호스트에 조건 분기를 두지 않는다.
- `YGAlert` 컴포저블 자체는 변경하지 않는다.

## 동작 / 상태

### `YGToast` 타입 → 토큰 매핑

| 타입 | Figma 변형 | 타이포 | 색 |
|---|---|---|---|
| `Record` | `Type=Alert` | 이름 `body.b02SB` / 본문 `body.b02R` | 이름 `Pudding.Pudding500` / 본문 `Gray.Gray100` |
| `Edit` | `Type=Warning` | `body.b02SB` | `Pudding.Pudding600` |
| `InviteCode` | `Type=Success` | `body.b02SB` | `Melon.Melon600` |
| `Fail` | `Type=Error` | `body.b02SB` | `Cherry.Cherry500` |

공통: 배경 `Transparency.Black75`, 가로 `padding.padding6`, 세로 `padding.padding5`, `fillMaxWidth`.
`Record`만 `buildAnnotatedString`으로 2색 조합이고 나머지 3종은 단색 단일 `Text`다 —
`Fail`은 `Edit`·`InviteCode`와 같은 단색 분기로 넣는다.

Figma는 375 고정폭이지만 코드는 `fillMaxWidth`를 유지한다 — 375는 Figma 캔버스 기준폭이고
실기기에서는 화면 폭을 채우는 것이 의도다.

### `YGDate` 색 매핑

| 요소 | 토큰 |
|---|---|
| 배경 | `Gray.White` |
| 테두리 | `Gray.Gray800` (0.75dp) |
| `date` 텍스트 | `body.b01R` / `Gray.Gray800` |
| `(day)` 텍스트 | `body.b01R` / `Gray.Gray300` |

## 표시·제어 규칙

- `YGAlert` 칩은 `buttonText != null`일 때만 노출된다(기존 계약 유지).
  `YGAlertPolicy.show`에 `buttonText`를 주지 않으면 버튼 없는 변형이 뜬다.
- `YGToast`는 다중 스택 **의도**(`add(0, …)`), `YGAlert`은 단일 슬롯 — 기존 정책 코드를 그대로 둔다.
  단, 스택은 실제로 동작하지 않는다(호스트가 `Box`) — 위 정정 노트 참고.
- 자동 소멸·스와이프 임계·애니메이션 시간 상수는 변경하지 않는다(애니메이션 상수는 현재 死코드).

## 파일 구성

### `core:designsystem` (수정)

| 파일 | 변경 |
|---|---|
| `component/ygtext/YGDate.kt` | D1·D2·D3 교정, D4 프리뷰 규약 |
| `component/ygtoast/YGToast.kt` | T1 패딩 교정, `Fail` 타입 + 렌더 분기, T4 프리뷰 규약(+`Fail` 변형 추가) |
| `component/ygalert/YGAlert.kt` | A1 프리뷰 규약만 (런타임 무변경) |
| `component/ygalert/YGAlertPolicy.kt` | A3 — `YGAlertItem` 필드 2개, `show` 인자 2개, `YGAlertHost` 전달 + P1 호스트 프리뷰 |
| `component/ygtoast/YGToastPolicy.kt` | P2 호스트 프리뷰만 (런타임 무변경 — `YGToastType`을 그대로 전달) |

`component/ygtext/YGLabel.kt`는 변경하지 않는다.

### `:app-preview` (신규 + 수정)

| 파일 | 역할 |
|---|---|
| `navigation/key/NavKeyYGToast.kt` (신규) | `@Serializable data object NavKeyYGToast : NavKey` |
| `navigation/key/NavKeyYGAlert.kt` (신규) | `@Serializable data object NavKeyYGAlert : NavKey` |
| `screen/component/YGToastPreviewScreen.kt` (신규) | 정적 4변형 + 정책 트리거 + `YGToastHost` |
| `screen/component/YGAlertPreviewScreen.kt` (신규) | 정적 2변형(버튼 유/무) + 정책 트리거 + `YGAlertHost` |
| `model/ComponentCatalog.kt` | `TEXT` 카테고리에 `YGToast`·`YGAlert` 2줄 등록 |
| `navigation/entry/ComponentEntryBuilders.kt` | `entry<NavKeyYGToast>`·`entry<NavKeyYGAlert>` 2블록 추가 |

`navigation/di/ComponentEntryModule.kt`는 수정하지 않는다 — `@IntoSet` 바인딩이
`componentEntryBuilders` 함수 단위라 그 안에 `entry`만 추가하면 된다.

### showcase 화면 구조

두 화면 모두 동일 골격을 쓴다. 자동 소멸 컴포넌트라 정적 렌더와 실동작을 함께 둔다 —
정적 쪽은 색·패딩을 천천히 검수하는 용도, 트리거 쪽은 스와이프·자동 소멸 확인용이다.

> 정적 섹션은 `LazyColumn`의 `contentPadding`(16dp) 안에 놓이므로 **가로 패딩 검수에는 쓸 수 없다** —
> T1이 고친 좌우 여백은 트리거로 띄운(호스트가 그린) 인스턴스에서 봐야 한다.

```
Box                                  // Host 오버레이를 얹기 위한 루트
├─ Column
│  ├─ YGTopBarBack(onIconClick = onBack)
│  └─ LazyColumn
│     ├─ PreviewSection("static")    // 전 변형 정적 렌더
│     └─ PreviewSection("policy")    // 변형별 "띄우기" 버튼
└─ <Component>Host(policy, …)        // 상단 정렬 오버레이
```

`policy`는 `rememberYGToastPolicy()` / `rememberYGAlertPolicy()`로 화면 안에서 보유한다.
`YGAlert` 트리거는 버튼 있는 변형과 없는 변형을 각각 띄워 A3 경로를 실제로 검증한다.
버튼 있는 변형의 `onButtonClick`은 **빈 람다가 아니라 관찰 가능한 동작**(`title = "clicked"`로 재노출)을
넣는다 — 빈 람다면 칩이 보이는 것만 확인될 뿐 콜백 전달이 끊겨도 갤러리에 드러나지 않는다.
테스트 인프라가 없는 repo라 갤러리가 유일한 검증 수단이다.

## 주의 / 열린 질문

- **`YGChipButton` 세로 패딩** — 현행 `padding3` 대비 Figma `Button-Chip-Right`는 `padding-2`.
  칩 높이가 39 vs 29로 어긋난다. `YGAlert`·`YGTopBar` 등 공통 사용처에 전파되므로
  이번 텍스트 영역 sync에서 제외하고 칩 영역 sync 라운드에서 처리한다.
  → [parfait open-questions](../../../synthesis/open-questions.md) 등록
- **`YGToast.Record`의 한국어 하드코딩** — `"님이 … 전에 쌓았어요"` 문구가
  `core:designsystem` 안에 문자열로 박혀 있다. 표시 문자열은 표현 계층에서 매핑한다는
  [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md) 방향과 상충 소지가 있다.
  이번 범위에서는 손대지 않는다. → open-questions 등록
- **원자 색 직접 참조** — 네 컴포넌트 모두 `YGAtomicColors`를 직접 읽는다(시맨틱 색 미경유).
  기존 `ygtoast`·`ygalert` 스펙에서 이미 open-questions에 등록된 사항으로, 이번에도 유지한다.
- **Figma `-수정 전` 노드** — `Date-수정 전`·`Input-수정 전`·`Invite-Card-수정 전`·`Danger-Zone-수정 전`이
  캔버스에 남아 있다. 구판 참고용이므로 향후 sync에서도 대조 대상에서 제외한다.
