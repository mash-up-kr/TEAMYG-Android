---
id: designsystem-bar-listdate-components
title: 디자인시스템 List-Date·Floating Bar 신설 + Top Bar Canvas 변형 (Bar & List-Date Components)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-04
related_code:
  - YGListDate.kt#YGListDate
  - YGFloatingBar.kt#YGFloatingBarBackClose
  - YGFloatingBar.kt#YGFloatingBarClose
  - YGFloatingBar.kt#YGFloatingBarEdit
  - YGFloatingBar.kt#YGFloatingBarEditTab
  - YGFloatingBar.kt#YGFloatingBarTitle
  - YGTopBar.kt#YGTopBarCanvas
  - YGTopBar.kt#YGTopBarEmpty
  - YGTopBar.kt#YGTopBarContent
  - YGTopBar.kt#ygTopBarBackdrop
  - YGTopBarDefaults.kt#BackdropBlurRadius
  - YGChipButtonColorsDefaults.kt#GrayOutline
  - YGDateButton.kt#YGDateButton
  - YGChipColorIndicator.kt#YGChipColorIndicator
  - YGCircleButton.kt#YGCircleButton
  - YGEditTabButton.kt#YGEditTabButton
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
related_adr:
  - ADR-0018
related_spec:
  - designsystem-canvas-components
  - designsystem-grouptag-topping-components
  - designsystem-button-missing-components
  - app-preview-component-gallery
related_architecture:
  - design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, figma-sync, top-bar, floating-bar, c-201]
---

# Spec: 디자인시스템 List-Date·Floating Bar 신설 + Top Bar Canvas 변형

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> ✅ **develop 머지(2026-08-04, PR #188 `feature/sync-component`)** — 아래 "구현 상태" 블록들이
> "미커밋"으로 적어둔 작업 트리가 그대로 커밋·머지됐다. 머지 시점 재대조에서 **설계와 갈리는 것은
> 없고**, 스펙이 시그니처를 적지 않았던 자리에 as-built 3건이 확정됐다.
> - 배경 블러 분기가 `YGTopBar.kt`의 private `Modifier.ygTopBarBackdrop(hazeState)`로 묶였다 —
>   `null`이면 `drawBehind { drawRect(White75) }`, 아니면 `hazeEffect { blurRadius·backgroundColor·tints }`.
> - `YGTopBarEmpty(date, day, onIconClick, modifier, hazeState = null, rightContent = {})` 순서로 확정.
>   날짜·요일은 `titleContent` 안쪽 `weight(1f)` `Row`(`gap3`)에 들어가고 `rightContent`가 그 형제다.
>   (**#194로 `hazeState`와 `rightContent` 사이에 `windowInsets`가 끼어들었다** — 상단 인셋을 탑바가
>   흡수한다. 상세 → [ygtopbar 스펙](2026-07-18-ygtopbar.md) "주의 / 열린 질문".)
> - `YGListDate`의 날짜 버튼 크기는 `Modifier.size(SizeTokens.Size44.getDp())`(아래 표기는 토큰명만).
>
> 스펙 범위 밖 동반 변경 없음. 같은 라운드에 **PR #189**(chore)가 G-001 라벨 3종을 `strings.xml`로
> 옮겨, 이 스펙이 건드린 `GroupListScreen`·`GroupListAddGroupScreen`의 리터럴이 함께 사라졌다.

> ⚠️ **개정(2026-08-01, PR #173 develop 머지 반영)** — 이 스펙이 대상으로 삼던 `YGTopBarDefault`가
> **develop에서 삭제**되고 `YGTopBarEmpty(rightContent)` 슬롯으로 통합됐다. 그 결과 원안의
> "`Default` 드리프트 제거"는 **대상이 사라졌고**(칩 색·문구는 이제 호출 화면 몫), 남은 드리프트는
> 컴포넌트 프리뷰의 칩 색 하나다. `YGTopBarContent` 확장·`YGTopBarCanvas` 신설은 그대로 유효하되
> 기준 시그니처를 #173 이후 코드로 갱신했다. 이 개정 시점에는 코드 미착수였고, 구현은 그 뒤에 이뤄졌다
> (아래 구현 상태).

> **구현 상태(2026-08-01)** — 3종 + 갤러리 등록 전량 완료. repo 전체 `assembleDebug` + `ktlintCheck`
> 통과, 실기기(Galaxy A35, SM-A356N) 갤러리에서 `YGListDate` 4상태×upload 2, `YGFloatingBar` 4변형 +
> 탭 전환, `YGTopBar` 5섹션을 Figma와 육안 대조 완료.
> 이 시점에는 **TJYG-Android 커밋을 하지 않았다**(작업자 지시). 브랜치 `feature/sync-component`에
> 작업 트리 변경만 남아 있었고(신규 6파일 + 수정 2파일 + `YGTopBar.kt`·`YGTopBarPreviewScreen.kt`),
> 이후 그대로 커밋돼 **PR #188로 develop 머지**됐다(위 머지 블록).
>
> **설계대로 확인된 것** — `YGListDate`의 미업로드 셀이 자리를 유지해 두 섹션의 셀 높이가 같고,
> `YGFloatingBarClose`가 우측 끝에 붙으며(`Arrangement.End`), `Edit-Tab`에서 탭을 누르면 밑줄이
> 옮겨간다. `YGTopBarContent`에 파라미터 2개를 더한 것이 기존 세 변형과 `GroupListScreen`의
> `YGTopBarEmpty(onIconClick, rightContent)` 호출에 영향을 주지 않았다.
>
> **최종 전체 리뷰가 잡은 결함 2건(수정 완료)** — Task 단위 리뷰 3회가 전부 통과한 뒤 나왔고,
> 둘 다 **같은 종류**다: 가중치 없는 `Text`가 가중치 있는 형제보다 먼저 측정돼, 긴 문자열이 잔여
> 폭을 다 먹으면 옆 요소가 0dp로 밀린다.
> - `YGTopBarCanvas` — 긴 그룹명이 **멤버 칩을 소리 없이 지운다.** 제목도 2줄로 감겨 바 높이가 변한다.
> - `YGFloatingBarEdit` — 긴 문구가 **확인 버튼을 0dp로 민다.**
>
> 둘 다 `Modifier.weight(1f)` + `maxLines = 1` + `TextOverflow.Ellipsis`로 해소했다(`Edit`은
> `TextAlign.Center` 추가, Canvas는 `Spacer` 제거). **이 결함이 프리뷰·갤러리 육안 검증을 통과한
> 이유는 모든 샘플 제목이 4자였기 때문이다** — 이 라운드의 가장 큰 교훈이고, 재발 방지로 두 컴포넌트
> 프리뷰에 긴 제목 변형을 상설했다. 사용자 입력값을 받는 텍스트에는 프리뷰에 긴 문자열 케이스를
> 반드시 둔다.
>
> **미검증**: pressed 상태(자동 캡처 불가), 긴 제목 케이스의 **실기기** 렌더 — 갤러리 화면에는 긴
> 제목 섹션을 두지 않아 컴포넌트 프리뷰 정의로만 확인했다. 갤러리에도 추가하는 것은 후속 과제.
>
> **이월 관찰 2건** — `YGTopBarEmpty.rightContent`(안쪽 슬롯)와 새 `trailingContent`(바깥 슬롯)의
> 측정 의미가 달라 다음 변형 작성자가 헷갈릴 수 있다(다음 Top Bar 라운드에서 통합 검토).
> `YGListDate`의 업로드 점이 TalkBack에 노출되지 않는다(모듈 전체에 상태 접근성 기준이 없어 별건).
>
> **2026-08-01 Figma 재조회로 범위가 늘었다** — 구현 완료 후 Top Bar를 다시 확인하니 `Default`·`Empty`
> 두 변형과 공유 컴포넌트 `Button-Chip-Left`가 바뀌어 있었다. 칩 프리셋 교체·개명, 날짜 표시, 반투명
> 배경 + 배경 블러 세 축이 추가됐다(아래 [`Default`·`Empty` 개편](#figma-defaultempty-개편-2026-08-01-figma-재조회)).
> **직전에 확정한 "프리뷰 칩을 `CherrySubtle`로 정정"은 이 재조회로 무효**가 됐다. `Back`·`Detail`·
> `Canvas` 3변형은 무변경이라 `YGListDate`·`YGFloatingBar`·`YGTopBarCanvas` 산출물은 그대로 살아 있다.

> **2차 라운드 구현 상태(2026-08-01)** — 세 축 전량 완료. repo 전체 `assembleDebug` + `ktlintCheck`
> 통과, 실기기(Galaxy A35 / SM-A356N, **API 36**) 갤러리에서 육안 대조 완료.
>
> **설계대로 확인된 것**
> - 칩이 흰 배경 + 회색 테두리 + 진한 글씨로 바뀌었고, 프리셋 하나를 고치자 소비처 6곳이 전부 따라왔다.
>   `GroupListScreen`·`GroupListAddGroupScreen`의 드리프트도 함께 닫혔다.
> - `Default`·`Empty`에 "December 31 (Wed)"가 표시되고 로고 `ic_plus` placeholder가 사라졌다.
> - 배경 블러가 동작한다 — 바 뒤 텍스트가 읽을 수 없게 뭉개지고 바 경계 밖은 선명하다(반경 4·40 대조).
>
> **블러 구현이 두 번 뒤집혔고, 그 사이 오검증이 있었다.**
> 1. 초안은 `dev.chrisbanes.haze` 도입이었다.
> 2. 작업자가 `androidx.compose.ui.graphics.BlurEffect`로 되지 않느냐고 물어 재검토했고, `RenderEffect`
>    기반이라 haze도 API 하한이 31로 동일하며 C-101이 이미 `GraphicsLayer` 관용으로 확정돼 있다는
>    이유로 **자체 구현으로 전환**했다.
> 3. 그런데 **자체 구현은 실제로 동작하지 않았다.** 40dp 극단값으로 대조하자 블러가 전혀 걸리지 않는
>    것이 드러났고, 세 가지 형태(중간 레이어 복사 / `record`·`renderEffect` 순서 교체 / 배경이 직접
>    record)를 모두 시도했으나 전부 실패했다. **Haze로 되돌려 즉시 동작을 확인했다** —
>    경위와 실측은 [ADR-0018](../../../adr/0018-backdrop-blur-haze.md).
>
> ⚠️ **오검증 기록** — 자체 구현 상태에서 한 번 "블러 동작·좌표 정합 확인"으로 보고했으나 **틀렸다.**
> `White75` 틴트만으로도 바 뒤 텍스트의 대비가 낮아져 흐린 것처럼 보였고, 대조군 없이 사양값(당시 2dp)
> 하나만 보고 판단한 것이 원인이다. **블러는 어긋나도 눈에 잘 띄지 않으므로 반드시 극단값 대조로
> 검증한다.** 같은 이유로 Figma 저작값 4를 CSS 환산값 2로 잘못 받아쓴 것도 육안 검증을 통과했다.
>
> **미검증**: pressed 상태. API 31 미만 폴백(검증 기기가 API 36). 실제 화면(G-001)에서의 블러 —
> `hazeSource` 배선이 범위 밖이라 갤러리 데모로만 확인했다.

> **코드리뷰 반영 2건(2026-08-01)** — 브랜치 리뷰가 잡은 것으로, 둘 다 **계획서가 지시한 대로 구현한
> 결과**라 구현 결함이 아니라 계획 결함이다.
> - `GroupListScreen` 상단바 날짜 하드코딩 → `uiState` 결선(위 [열린 질문 5](#열린-질문)). 계획서가
>   "화면이 아직 날짜 데이터를 갖고 있지 않다"고 전제했으나 **사실이 아니었다** — 대상 화면의
>   ViewModel 현재 상태를 읽지 않고 쓴 지시였다.
> - `YGListDate`가 C-201 예외 규칙(Disabled면 인디케이터 항상 False)을 강제하지 않아
>   `isEnabled = false, isUploaded = true` 조합이 렌더됐고, 갤러리 프리뷰가 실제로 그 조합을 노출하고
>   있었다 → 컴포넌트 내부에서 `isEnabled && isUploaded`로 강제. 계획서 코드블록이 정책 문서의 예외
>   조항을 옮기지 않은 것이 원인.
>
> 교훈: **호출부를 건드리는 Task는 그 호출부의 상태 보유 여부를 계획 단계에서 확인**하고,
> 합성 컴포넌트를 설계할 때 **대응 정책 문서의 "예외" 조항을 시그니처 표에 함께 옮긴다.**

> ➕ **Title 변형 추가(2026-08-28 작성 · PR #406 develop 머지, 2026-08-30, 이슈 #381)** —
> Figma `Floating Bar`에 `Status=Title`이 추가돼 공개 함수가 **5종**이 됐다.
>
> ```kotlin
> @Composable fun YGFloatingBarTitle(title: String, onCloseClick: () -> Unit, modifier: Modifier = Modifier)
> ```
>
> | 변형 | 컨테이너 | 좌 | 중앙 | 우 |
> |---|---|---|---|---|
> | `Title` | 공용 `Row` 아닌 전용 `Box` | — | `Text(body.b01R, Gray800, align(Center))` | Circle `ic_close` |
>
> **이 변형만 `YGFloatingBarContent`를 공유하지 않는다.** 그 `Row`는 좌우 버튼이 44dp로 대칭일 때만
> 중앙이 실제 중앙이 되는데(아래 [공개 4종](#공개-4종)의 버튼 폭 항목), `Title`은 우측 버튼 하나뿐이라
> 같은 방식으로는 제목이 버튼 반지름만큼 왼쪽으로 치우친다. Figma는 제목을 바 전체 폭의 정중앙에 두므로
> 전용 `Box`로 조립하고, 패딩 3종(`padding7`·`padding6`·`padding7`)만 공용 컨테이너와 같은 값으로 맞춘다.
> 높이는 상단 `padding6` + 버튼 44로 Figma의 60과 일치하므로 고정 높이를 박지 않는다.
>
> 제목의 잘림 처리는 `Edit`과 같은 `maxLines = 1` + `TextOverflow.Ellipsis`이지만, 겹침을 막는 방식이
> 다르다. `Box`에서는 긴 제목이 버튼을 밀어내지 못하고 **버튼 아래로 깔리므로**, 좌우를 버튼 지름(`Size44`)만큼
> 비워 그 폭 안에서 잘리게 한다. 이 이유로 `SizeTokens`가 이 파일의 새 import로 들어왔다.
>
> 첫 소비처는 C-102 갤러리다 → [c102 스펙](2026-08-04-c102-custom-gallery-picker.md)의 같은 날짜 개정.
> 프리뷰는 `@YGPreview`에 정상·긴 제목 2건, `:app-preview` 갤러리 `Title` 섹션에 정상 1건을 등록했다.
>
> ⚠️ **이 라운드의 검증은 기계 검사뿐이다** — 세 모듈 `compileDebugKotlin`·`ktlintMainSourceSetCheck`와
> `:feature:gallery:impl:testDebugUnitTest`를 통과했고, **프리뷰·실기기 육안 대조는 하지 않았다.** 위
> "미검증"이 이월한 긴 제목의 실기기 렌더도 그대로 남는다.

## 목표

Figma 컴포넌트 3종(`List-Date`·`Top Bar`·`Floating Bar`)을 현재 구현과 1:1 대조해 신설·수정한다.
`List-Date`와 `Floating Bar`는 대응 구현체가 없고, `Top Bar`는 Figma 5변형 중 `Canvas` 1종이 빠져 있다.
Figma의 `Default`에 해당하는 구성은 **#173 이후 `Empty` + `rightContent` 조립**으로 표현된다.

선행 라운드가 남긴 두 이월 항목을 함께 닫는다 — 캔버스 스펙이 C-201 라운드로 미룬 `List-Date`, 그리고
Grouptag·Topping 스펙이 "다른 브랜치 작업 중"으로 제외한 `Chip-Indicator`·`List-Date`. `Chip-Indicator`는
그 사이 `YGChipColorIndicator`로 머지돼 이번엔 재사용만 한다.

## 범위

- **포함**
  - `YGListDate`(Figma `List-Date`) — `YGDateButton` + `YGChipColorIndicator` 합성, 신규 파일 1개
  - `YGFloatingBar*`(Figma `Floating Bar`) — 변형별 공개 함수 4종 + 공통 private 컨테이너
  - `YGTopBarCanvas`(Figma `Top Bar` `Status=Canvas`) — 신규 변형
  - `YGTopBarContent` 확장 — `contentPadding`·`trailingContent` 파라미터 추가
  - **`Button-Chip-Left` 프리셋 교체 + 개명** — `CherrySubtle` → `GrayOutline`(흰 배경 + `Gray500` 테두리)
  - **`YGTopBarEmpty` 날짜 표시** — 로고 placeholder → `date`·`day` 파라미터
  - **`Default`·`Empty` 반투명 배경 + 배경 블러** — `White75` 틴트 + Haze([ADR-0018](../../../adr/0018-backdrop-blur-haze.md))
  - 3종을 `:app-preview` 컴포넌트 갤러리에 등록·갱신
- **제외**
  - **`YGTopBarDefault` 재도입** — #173에서 삭제된 변형이다. 칩 색·문구는 호출 화면이 정한다
  - `Button-Chip-Right`(`CherrySolid`) — Figma 무변경
  - C-101 카메라 블러의 Haze 전환 — 같은 함정에 걸릴 설계지만 그 라운드 몫이다(ADR-0018)
  - **List-Member 실물** — Canvas Top Bar가 슬롯만 열고 겹침 배치·`+N` 계산은 호출자 책임(아래 [List-Member](#list-member))
  - C-201 캘린더 패널 실물 — `YGListDate`를 격자로 배치하는 쪽
  - `YGTopBar`의 logo placeholder(`ic_plus`) 치환 — 이월 todo
  - `YGColorChipType` 13종 + Plus ↔ 정책 12종 드리프트 — 기존 이월 미결
  - `Back`/`Detail`/`Empty` 3변형 — Figma와 일치, 무변경

## 신규 토큰·에셋 없음

`Size44`·`gap.gap1`·`padding3`·`padding6`·`padding7`, 아이콘 `ic_caret_left`·`ic_close`·`ic_check`·
`ic_hamburger`가 모두 존재한다. 버튼 라운드에서 세운 "Figma가 고정한 치수만 토큰으로 못박는다" 원칙에
비춰 추가할 것이 없다.

## `YGListDate`

C-201 캘린더의 날짜 셀 1개. 날짜 버튼 아래에 업로드 여부 점을 붙인다.

```kotlin
@Composable
fun YGListDate(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    isUploaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

구조 — `Column(verticalArrangement = spacedBy(gap.gap1), horizontalAlignment = CenterHorizontally)`:

| 요소 | 규격 |
|---|---|
| 날짜 | `YGDateButton(modifier = Modifier.size(SizeTokens.Size44))` |
| 인디케이터 | `YGChipColorIndicator(isChecked = isEnabled && isUploaded)` |

- 전체 44×50dp(44 + gap 2 + dot 4). Figma 프레임과 일치
- `Upload=False`는 Figma에서 `opacity-0`이고 `YGChipColorIndicator`가 미체크 시 `Color.Transparent`를
  그리므로 **자리를 유지한 채 비노출**된다 — 선택 상태가 바뀌어도 셀 높이가 흔들리지 않는다
- 상태 4종(`isSelected`·`isToday`·`isEnabled`·기본)은 `YGDateButton`이 이미 처리하므로 그대로 위임한다.
  여기서 다시 분기하지 않는다
- **Disabled면 인디케이터는 항상 False** — 정책 [[캘린더-컴포넌트]]
  (link)의 List-Date 예외 규칙("Button-Date가 Disabled면 항상
  False")을 **컴포넌트가 내부에서 강제**한다. `isEnabled`·`isUploaded`를 독립 파라미터로 받으면
  `isEnabled = false, isUploaded = true` 같은 정책 위반 조합이 호출부마다 재현될 수 있어, 합성체인
  `YGListDate`가 규칙을 들고 있는 쪽으로 정했다(코드리뷰 반영, 2026-08-01)
- `onClick`은 `YGDateButton`에 그대로 넘긴다. 인디케이터는 터치 대상이 아니다

부품 2종은 **수정하지 않는다.** 합성 파일 1개만 늘어난다.

## `YGTopBar`

### `YGTopBarContent` 확장

기존 private 컨테이너에 파라미터 2개를 더한다. 기존 3변형(`Back`·`Detail`·`Empty`)의 호출부는 바뀌지
않는다(기본값이 현재 동작과 동일).

> `Empty`가 #173에서 받은 `rightContent`는 **안쪽 `weight(1f)` Row 안**의 형제이고, 여기서 더하는
> `trailingContent`는 **그 Row 바깥**의 형제다. 위치가 달라 이름을 합치지 않는다 — 합치려면 `Empty`의
> 로고 `Box(weight(1f))` 구성까지 다시 짜야 해서 이번 범위 밖 회귀를 산다.

```kotlin
@Composable
private fun YGTopBarContent(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        start = padding3, top = padding3, end = padding7, bottom = padding3,
    ),
    titleContent: @Composable RowScope.() -> Unit = { },
    trailingContent: @Composable () -> Unit = { },
)
```

- `trailingContent`는 `weight(1f)` Row **바깥**의 형제로 배치한다. Figma가 Info-Group을 flex-1로 두고
  우측 아이콘을 그 형제로 두는 구조와 같다
- `contentPadding`을 뺀 이유는 `Canvas`만 사방 `padding3`이고 나머지 4변형은 좌 `padding3`·우 `padding7`로
  비대칭이기 때문이다. 값 하나 때문에 컨테이너를 복제하지 않는다

대안으로 검토한 것: (a) Canvas를 독립 `Row`로 새로 짜기 — 패딩·아이콘 배치가 두 곳으로 갈라져 다음 Figma
변경 때 드리프트가 난다. (b) 완전 슬롯 API(`leading`/`center`/`trailing`)로 리팩터 — 기존 4변형까지
재작성해야 해서 이번 범위 밖 회귀 위험을 산다. 필요한 차이가 `trailing`과 `padding` 둘뿐이라 확장을 택했다.

### `YGTopBarCanvas` (신규)

C-001 캔버스 화면의 상단 바. 뒤로가기 + 그룹명 + 멤버 목록 + 메뉴.

```kotlin
@Composable
fun YGTopBarCanvas(
    title: String,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    memberContent: @Composable RowScope.() -> Unit = { },
)
```

| 요소 | 규격 |
|---|---|
| 컨테이너 | `contentPadding = PaddingValues(padding3)` — 사방 8dp |
| leading | `YGIconButton(ic_caret_left, SIZE_44)` |
| 제목 | `typography.body.b01R`, `Gray.Gray800` |
| 멤버 | `memberContent` 슬롯 |
| trailing | `YGIconButton(ic_hamburger, SIZE_44)` |

제목이 좌측, 멤버가 우측에 붙는다. **안쪽 Row의 `Arrangement`는 건드리지 않는다** — 그 Row를
나머지 세 변형이 공유하기 때문이다. 대신 **제목 `Text`가 `Modifier.weight(1f)`를 갖는다.**

> 초안은 `Text` → `Spacer(weight(1f))` → `memberContent()` 순서였다. Compose가 가중치 없는 자식을
> 먼저 전체 잔여 폭에 대해 측정하므로, 긴 그룹명이 행을 다 먹으면 Spacer가 0으로 접히고 **멤버 칩이
> 0dp가 돼 소리 없이 사라진다.** 제목도 2줄로 감겨 바 높이가 변한다. `title`은 사용자 입력값인데
> 프리뷰가 전부 짧은 문자열이라 육안 검증으로 드러나지 않는 종류의 결함이다.
> 가중치를 `Text`에 주고 Spacer를 없애면 해소된다 — `maxLines = 1` + `TextOverflow.Ellipsis` 동반.

### List-Member

Figma의 List-Member(Nametag-Chip 5개를 -12dp씩 겹치고 끝에 `+N` 카운트 칩)는 **컴포넌트로 만들지 않고
슬롯으로 연다.** Figma가 이것을 별도 컴포넌트로 등록해두지 않았고, 겹침 개수·`+N` 임계값·어떤 유저를
앞에 세울지는 그룹 데이터에 걸린 판단이라 디자인시스템이 정할 근거가 없다.

호출자가 `YGNametagChip(Style28)`을 `Row(horizontalArrangement = Arrangement.spacedBy((-12).dp))`로
겹쳐 나열하고(음수 간격 — `Modifier.offset`과 달리 **측정 폭 자체가 줄어들어** 상위 Row가 실제
차지 폭을 알 수 있다), 초과분은
`YGColorChipType`의 Plus 타입 칩에 `+N` 문자열을 넣어 그린다. 갤러리 프리뷰에 **조립 예시**를 두어
사용법을 남긴다.

> ⚠️ Plus 타입은 `YGColorChipType` 13종 + Plus ↔ 정책 12종 드리프트에 걸려 있는 이월 미결 항목이다.
> 이번 라운드에서 정리하지 않는다.

### Figma `Default`·`Empty` 개편 (2026-08-01 Figma 재조회)

> **경위** — 이 라운드 구현을 끝낸 뒤 Figma를 다시 확인하니 `Top Bar`의 `Default`·`Empty` 두 변형과
> 공유 컴포넌트 `Button-Chip-Left`가 바뀌어 있었다. `Back`·`Detail`·`Canvas` 3변형은 무변경이라
> 이 라운드의 `YGTopBarCanvas`·`YGTopBarContent` 확장 산출물은 그대로 유효하다.
> 직전에 확정했던 "프리뷰 칩을 `CherrySubtle`로 정정" 결론은 **이 재조회로 무효가 됐다** —
> `CherrySubtle`도 더 이상 정본이 아니다.

세 축이 바뀌었다.

**① `Button-Chip-Left` 컴포넌트 자체가 바뀜** — Cherry 계열을 버리고 흰 배경 + 회색 테두리로 갔다.

| 상태 | 배경 | 테두리 | 전경 |
|---|---|---|---|
| Default | `Gray.White` | `Gray.Gray500` 1px | `Gray.Gray900` |
| Pressed | `Gray.Gray200` | `Gray.Gray500` 1px | `Gray.Gray950` |

`YGChipButtonColorsDefaults.CherrySubtle` 프리셋의 **값을 교체하고 이름을 `GrayOutline`으로 바꾼다.**
값만 바꾸면 이름이 내용과 어긋난다(Cherry가 한 톤도 안 들어간다). 선행 라운드에 같은 이유로
`CherryBorderPressed`→`CherrySubtle` 개명을 한 선례가 있다. 프리셋 하나를 고치면 소비처가 전부 따라오므로
`GroupListScreen`·`GroupListAddGroupScreen`의 드리프트도 함께 닫힌다.

> `Button-Chip-Right`는 **무변경**이다(`Cherry100`/`Cherry200` + `Gray950`). `CherrySolid`는 그대로 둔다.

**② 로고 placeholder 자리가 날짜로 확정** — `[logo]`가 사라지고 **"December 31" + "(Wed)"** 가 들어간다.
`b01R`, 사이 `gap.gap3`, 날짜 `Gray.Gray800` / 요일 `Gray.Gray300`.

`YGTopBarEmpty`가 `date`·`day` 문자열을 받아 내부에서 텍스트 2개로 그린다. 기존 `YGDate` 컴포넌트는
**쓰지 않는다** — 그쪽은 흰 배경 + `Gray800` 테두리 + 패딩을 갖는 별개 표현이고, Figma도 Top Bar 안에는
`Date` 심볼 인스턴스가 아니라 인라인 텍스트 그룹을 두었다. 이로써 `YGTopBarEmpty`에 남아 있던
로고 `ic_plus` placeholder todo가 닫힌다.

**③ 컨테이너에 반투명 배경 + 배경 블러** — `Transparency.White75` 틴트 위 **배경 블러 4**.

> ⚠️ Figma MCP는 CSS/Tailwind로 내보내면서 블러를 절반으로 환산한다(저작값 4 → `backdrop-blur-[2px]`).
> Compose에는 **저작값 4를 그대로** 쓴다.

구현은 **Haze**(`dev.chrisbanes.haze` 1.7.2)로 한다 — 배경이 `Modifier.hazeSource(state)`,
Top Bar가 `Modifier.hazeEffect(state)`. `HazeState`는 호출 화면이 소유하고 Top Bar는 nullable
파라미터로 받는다(`null`이면 틴트만). 반경은 `YGTopBarDefaults.BackdropBlurRadius`로 노출한다.

자체 `GraphicsLayer` + `BlurEffect` 구현은 **실기기에서 세 형태 모두 블러가 걸리지 않아 기각**했다.
경위·실측·기전은 [ADR-0018](../../../adr/0018-backdrop-blur-haze.md).

> ⚠️ **API 31 미만에서는 블러가 없다.** `RenderEffect`가 API 31+이고 `minSdk`는 26이다. 26~30에서는
> `White75` 틴트만 남는다. 틴트는 `hazeEffect`의 `tints`로 넣어 블러 유무와 무관하게 항상 그려진다.

### 무변경 3변형

`Back`·`Detail`·`Empty`는 Figma와 일치한다(`Empty`는 슬롯이 비었을 때 기준). Figma의 `Detail`·`Back`에
있는 List-Member는 `opacity-0`이고 제목이 좌측 정렬이라 렌더 결과가 같으므로 비노출이 정답이다.

## `YGFloatingBar`

캔버스·편집 화면 위에 떠 있는 액션 바. 4변형이 컨테이너를 공유한다.

### 공통 컨테이너

```kotlin
@Composable
private fun YGFloatingBarContent(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable RowScope.() -> Unit,
)
```

- `Row(padding(top = padding6, start = padding7, end = padding7))`, `verticalAlignment = CenterVertically`
- 폭은 `modifier`로 호출자가 정한다. Figma의 375는 프레임 폭일 뿐이라 컴포넌트에 박지 않는다

### 공개 4종

> 📌 이 표는 2026-08-01 라운드 기준이다. **`Title` 변형이 2026-08-28에 더해져 #406으로 머지됐다**(2026-08-30) → 위 개정 블록.

```kotlin
@Composable fun YGFloatingBarBackClose(onBackClick: () -> Unit, onCloseClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun YGFloatingBarClose(onCloseClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun YGFloatingBarEdit(title: String, onCloseClick: () -> Unit, onConfirmClick: () -> Unit, modifier: Modifier = Modifier)
@Composable fun YGFloatingBarEditTab(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    onCloseClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

| 변형 | Arrangement | 좌 | 중앙 | 우 |
|---|---|---|---|---|
| `BackClose` | `SpaceBetween` | Circle `ic_caret_left` | — | Circle `ic_close` |
| `Close` | `End` | — | — | Circle `ic_close` |
| `Edit` | `SpaceBetween` | Circle `ic_close` | `Text(body.b01R, Gray800, weight(1f))` | Circle `ic_check` |
| `EditTab` | `SpaceBetween` | Circle `ic_close` | `YGEditTabButton × n` | Circle `ic_check` |

- 원형 버튼은 전부 `YGCircleButton(type = YGCircleButtonType.Default)` — `White` 배경 / `Black5` 테두리 /
  `Gray900` 아이콘 28dp / `padding3`로 총 44dp. Figma `Button-Circle` `Type=Default`와 일치
- `Close`만 `Arrangement.End`다. `SpaceBetween`에 자식이 하나면 좌측으로 붙어 Figma(`justify-end`)와 어긋난다
- `Edit`의 중앙 텍스트는 **`Modifier.weight(1f)` + `TextAlign.Center` + `maxLines = 1` +
  `TextOverflow.Ellipsis`**를 갖는다. 가중치가 없으면 긴 문구가 잔여 폭을 다 먹어 **확인 버튼이
  0dp로 밀린다**(`YGTopBarCanvas`와 같은 종류의 결함). 가중치를 주면 텍스트가 제 박스 안에서
  중앙 정렬되므로 `SpaceBetween`이 두 버튼을 양 끝에 그대로 고정한다.
  `EditTab`의 중앙은 텍스트가 아니라 탭 `Row`라 이 처리를 적용하지 않는다.
- 좌우 버튼 폭이 44dp로 같아 `SpaceBetween`에서 실질 중앙에 온다. Figma도 같은 구조라
  중앙 정렬을 별도로 강제하지 않는다
- `EditTab`의 탭은 `tabs`·`selectedIndex`·`onTabSelect`로 받아 내부에서 `YGEditTabButton`을 나열한다.
  Figma는 "영역"/"테두리" 2탭이지만 문자열을 주입받으면 개수와 무관하게 동작하므로 2탭으로 박지 않는다
- `tabs: List<String>`은 unstable 타입이지만 strong skipping이 인스턴스 동등성으로 스킵을 유지한다.
  프로젝트에 immutable collections 의존성이 없고, 이것 하나 때문에 도입하지 않는다

부품 2종(`YGCircleButton`·`YGEditTabButton`)은 **수정하지 않는다.**

### 반복 버튼 추출

닫기(`ic_close` + `"닫기"`)는 네 변형 중 넷, 확인(`ic_check` + `"확인"`)은 둘에서 같은 인자로 반복된다.
파일 안 private 컴포저블 `YGFloatingBarCloseButton(onClick)`·`YGFloatingBarConfirmButton(onClick)`으로
묶어 호출 지점을 하나로 만든다 — 아이콘·contentDescription·버튼 타입이 바뀔 때 고칠 자리가 한 곳이다.

- `YGFloatingBarBackClose`의 뒤로가기 버튼은 **묶지 않는다.** 한 번만 쓰이므로 감싸면 읽기만 나빠진다.
- 두 private 함수에 `modifier` 파라미터를 두지 않는다. 호출부 여섯 곳이 전부 넘기지 않는다.

## 검증

선행 디자인시스템 라운드(버튼·캔버스·Grouptag/Topping)와 동일하게 **테스트 없이 프리뷰 + 실기기 갤러리
육안 검증**으로 간다. 세 컴포넌트 모두 상태 없는 순수 렌더라 단위 테스트가 잡을 회귀가 거의 없고,
실제 결함(정렬·잘림·색 대비)은 육안에서만 드러난다는 판단을 유지한다.

- `@YGPreview` + `PreviewBox` 프리뷰
  - `YGListDate` — 상태 4종 × `isUploaded` 2
  - `YGTopBar` — `Back`·`Detail`·`Empty`(슬롯 없음/칩 슬롯 2례)·`Canvas`(List-Member 조립 예시 포함)
  - `YGFloatingBar` — 4변형
- `:app-preview` 갤러리
  - `YGListDate` → `ComponentCategory.BUTTON` (`YGDateButton` 옆)
  - `YGFloatingBar` → `ComponentCategory.BAR`
  - 기존 `YGTopBar` 화면에 Canvas 변형 추가
- `:core:designsystem` + `:app-preview` `assembleDebug`, repo 전체 `ktlintCheck`
- 실기기(Galaxy A35) 갤러리에서 Figma와 육안 대조
- **TJYG-Android 커밋은 하지 않는다**(작업자 지시). 작업 트리 변경만 남기고 보고한다
  → 이후 작업자가 직접 커밋해 **PR #188로 develop 머지**(2026-08-04)

## 열린 질문

1. **`+N` 카운트 칩 타입** — `YGColorChipType`이 13종 + Plus라 정책 12종과 어긋난 상태가 이어진다.
   Canvas Top Bar 갤러리 예시는 Plus 타입으로 그리되, 정리는 Nametag 라운드로 넘긴다.
2. **`YGFloatingBarEdit`의 중앙 문구 출처** — Figma가 `Text` placeholder만 두어 실제 문구(편집 대상 이름인지
   모드 라벨인지)를 알 수 없다. 파라미터로 열어두고 호출 화면 구현 때 확정한다.
3. **Floating Bar의 배치 책임** — Figma가 상단 패딩 16dp만 주고 화면 어디에 떠 있는지(상단 고정 / 하단 /
   오버레이)는 컴포넌트 밖 정보다. 호출 화면이 정한다.
4. **API 31 미만의 배경 블러 부재** — `minSdk` 26이라 26~30에서는 `White75` 틴트만 남는다. 플랫폼
   제약이라 해결책이 아니라 **수용 여부**의 문제다. 디자인 쪽에 "저사양 기기에서는 블러 없음"이
   허용되는지 확인이 필요하다.
5. **`Default`·`Empty`의 날짜 출처** — Figma가 `December 31 (Wed)`를 영문 표기로 고정했는데, 이 앱은
   한국어 UI다. 로케일·포맷 규칙이 미정이라 컴포넌트는 **완성된 문자열 2개를 받기만** 한다.
   포맷 책임은 호출 화면/도메인이고 그 규칙은 아직 정해지지 않았다.
   > **호출부 결선은 닫혔다(2026-08-01, 코드리뷰 반영)** — 계획서가 `GroupListScreen`에
   > `"December 31"`·`"Wed"` 샘플 + TODO를 남기라고 지시했으나, 그 시점에 이미 `GroupListViewModel`이
   > `init`에서 `dateString`·`dayOfWeekString`을 계산하고 있었고 같은 화면의 `YGDate`가 그 값을 쓰고
   > 있었다. 상단바만 하드코딩으로 남아 "항상 December 31 (Wed)" 고정 노출 상태였다 →
   > `date = uiState.dateString`, `day = uiState.dayOfWeekString`으로 교체. **미결로 남는 것은 포맷·로케일
   > 규칙뿐**(현재 `DateFormat.FullMonthWithDay`·`AbbreviatedDayOfWeek` 영문 표기).
6. **블러 대상 화면 배선** — Top Bar가 레이어를 받는 구조는 정했으나, G-001 그룹 목록이 실제로 배경을
   record하도록 배선하는 것은 이 라운드 범위 밖이다(디자인시스템만 손댄다). 그 전까지 실사용 화면에서는
   블러가 꺼진 상태(틴트만)로 동작한다.
