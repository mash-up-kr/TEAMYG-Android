---
id: g001-group-list-topping-border
title: G-001 그룹 목록 최신 토핑에 테두리를 그린다 (Group list topping border)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-16
related_code:
  - YGToppingGroup
  - YGToppingImage.Remote
  - YGToppingCutoutImage
  - ToppingBorderPlateCache
  - ToppingBorderPlate#fitsSubject
  - rememberToppingOutlines
  - ToppingOutlineCache
  - MyParfaitGroupVO#recentImageBorder
  - ToppingBorder.solidClamped
  - toToppingImage
  - GroupListContent
  - CanvasToppingLayer#ToppingImage
  - YGToppingGroupPreviewScreen
related_adr: ADR-0025, ADR-0030
related_spec: designsystem-grouptag-topping-components, g001-group-list
related_architecture:
  - design-system.md
  - module-structure.md
supersedes:
superseded_by:
tags: [spec, parfait, g001, topping, border]
---

# Spec: G-001 그룹 목록 최신 토핑 테두리

> ✅ **구현 완료·develop 머지(2026-09-16, PR #497 `a1fc2377f` — 머지 트리가 브랜치 팁 `108b4fa7d`와 같다, 충돌 해소 편집 0건).**
> 10파일 · 삽입 449줄 · 삭제 29줄, 커밋 5개. 유닛 1292 → **1298건**(+6 `ToppingImageTest`),
> 계측 39 → **46건**(+7 `ToppingBorderPlateCacheTest`). **머지본과 이 스펙 사이에 어긋난 조항은 없다** —
> 결정 1~5, API 시그니처, 모디파이어 순서, painter 상태 표가 모두 코드와 같다. 스펙에 이름이 없던 것은
> `ToppingImage.kt`의 비공개 헬퍼 `drawableBorder()` 하나이고, "판정 기준을 두 곳에 두지 않는다"는
> 스펙 조항의 실현이다.
>
> 티켓 #494, TJYG-Android 브랜치 `feature/#494-group-list-topping-border`.
> 데이터 계층은 PR #496(`1b21725ba`)으로 이미 develop에 들어와 있었다. 이 스펙은 그 값을 **화면에 그리는 일**만 다룬다.

## 목표

G-001 그룹 목록의 카드는 그룹마다 오늘 캔버스의 최신 토핑([[토핑]] (link))을
하나씩 보여 준다. 그 토핑에 테두리가 있으면 캔버스와 같은 모양의 테두리를 목록에서도 그린다.
지금은 서버가 `GET /api/parfait-groups` 응답에 테두리 필드 셋을 주고 앱의 `MyParfaitGroupVO.recentImageBorder`까지
받지만, `YGToppingGroup`이 그 값을 받을 자리가 없어 테두리 없이 그린다([open-questions](../../../synthesis/open-questions.md) OQ-P-316).

## 범위

- 포함
  - `YGToppingImage.Remote`에 테두리를 싣는 자리를 연다.
  - `YGToppingGroup`의 `Remote` 분기가 `YGToppingCutoutImage`로 테두리를 그린다.
  - G-001이 테두리가 있는 토핑의 거리판을 불러와 컴포넌트에 넘긴다.
  - `ToppingBorderPlateCache`가 한 열쇠에 크기가 다른 띠 판을 여러 장 두고, `YGToppingCutoutImage`가 크기에 맞는 판을
    골라 그린다(결정 5). 목록과 캔버스가 같은 토핑의 판을 서로 덮어쓰지 않게 하기 위해서다.
  - app-preview 카탈로그에 테두리 샘플을 하나 더한다.
- 제외
  - **템플릿 6종·조회 실패 그래픽의 테두리** — 정책이 비어 있다(OQ-P-316 ③).
  - **목록 토핑의 알파 판정·토핑 단위 클릭** — 카드 전체가 클릭 범위인 지금 구조를 바꾸지 않는다(OQ-P-316 ④).
  - **테두리 그리기 규칙 변경** — 굵기 dp 고정, 판을 매번 지금 크기에 맞춰 다시 만드는 것, 1.25배 재사용 한도
    (`ToppingBorderPlate#fitsSubject`)는 그대로다. 결정 5는 판을 **보관하고 꺼내는 방식**만 바꾼다.
  - `ToppingOutlineCache` 수명·상한 조정(OQ-P-317).

## 확정 결정

브레인스토밍에서 사용자가 고른 것이다. 이유를 함께 적는다.

1. **두께는 서버 dp를 그대로 쓴다.** 캔버스와 같은 규칙이다(`YGToppingCutoutImage`의 `borderWidth`는 화면 기준 dp).
   목록 토핑은 96dp 프레임이고 캔버스 토핑의 기준 긴 변은 캔버스 너비의 40%라, 같은 dp가 목록에서
   **상대적으로 더 굵어 보인다.** 목록 응답에는 토핑 `scale`이 없어 비율로 줄여도 근사치밖에 안 되므로 줄이지 않는다.
2. **토핑을 두께만큼 안쪽으로 줄여 테두리까지 96dp 프레임 안에 넣는다.** `YGToppingCutoutImage`의 띠는 상자 밖으로
   굵기만큼 나가는데, `YGToppingGroup`은 인접 셀을 덮지 않으려고 `clip(RectangleShape)`을 건다. clip 상자를 키우는 안과
   clip을 없애는 안도 있었으나, **바깥 틀과 배치를 한 치도 바꾸지 않는 안**이 선택됐다.
   ⚠️ 대가: 앱이 가두는 상한(`ToppingBorder.WIDTH_RANGE_DP`, 서버는 범위를 검사하지 않는다)인 30dp면
   토핑 본체의 긴 변이 36dp까지 줄어든다. 알고 고른 결과다.
3. **거리판은 feature가 불러와 컴포넌트에 넘긴다.** `rememberToppingOutlines`는 `:core:ui`에 있고
   `:core:designsystem`은 `:core:ui`에 의존하지 않는다(`:core:ui`는 `:domain`에 의존한다). 컴포넌트가 직접 부르게
   하려면 디자인시스템이 도메인에 닿아야 해서 기각했다. 캔버스(`CanvasToppingLayer`)와 같은 분업이다.
4. **이미지 슬롯 API로 바꾸지 않는다.** `YGToppingImage` 3상태를 주입받아 렌더만 하는 지금 계약을 유지한다.
   슬롯으로 열면 에러 폴백이 호출부로 흩어지고 app-preview 호출부가 전부 바뀐다.
5. **띠 판 캐시가 한 열쇠에 크기별 판을 여러 장 둔다.** 스펙 검수에서 드러났다. `ToppingBorderPlateCache`의 열쇠
   (`PlateKey`: 거리판 인스턴스·굵기·비율)에는 표시 크기가 없고 열쇠당 판이 한 장이다. 목록 토핑과 캔버스 토핑은 같은
   URL이라 `ToppingOutlineCache`가 같은 거리판 인스턴스를 주고, 굵기도 같으므로 열쇠가 겹칠 수 있다. 그런데 긴 변이
   목록은 `96dp − 2w`, 캔버스는 캔버스 너비의 40% × scale이라 1.25배를 넘게 어긋난다. 그러면 한쪽이 만든 판이 다른
   쪽 판을 덮어쓰고, 다른 쪽은 `fitsSubject` 불합격으로 새 판이 나올 때까지 테두리를 그리지 않는다.
   **카드를 눌러 캔버스로 들어갈 때마다 그 토핑의 테두리가 깜빡인다** — ADR-0030이 판 캐시를 둔 이유를 되돌리는 결과다.
   열쇠에 크기를 넣는 대신 선반을 두는 이유: ADR-0030이 크기를 열쇠에서 뺀 근거(핀치 한 번에 항목이 쏟아진다)를
   열쇠당 장수 상한으로 막을 수 있고, 크기를 모르는 첫 컴포지션에서도 지금처럼 판을 꺼낼 수 있다.

## API / 인터페이스

### `:core:designsystem` — `component/ygtoppinggroup/`

```kotlin
@Immutable
data class YGToppingBorder(
    val color: Color,
    val width: Dp,
    val outline: ToppingOutline?,
)

@Immutable
sealed interface YGToppingImage {
    @Immutable
    data class Remote(
        val url: String,
        val border: YGToppingBorder? = null,
    ) : YGToppingImage
    // Template·Error 는 그대로
}
```

- `border == null`은 "테두리 없음"이다. `ToppingBorder.None`, 색을 읽지 못한 `Solid`가 모두 여기로 접힌다.
  디자인시스템이 도메인 타입을 모르므로 판정은 feature에서 끝내고 **그릴 수 있는 값만** 넘긴다.
- `outline == null`은 "테두리는 있는데 거리판이 아직 없다"이다. `null`인 동안 크기는 줄어든 채로 알맹이만 그린다.
- `ToppingOutline`은 `:core:util:jvm` 타입이고 `:core:designsystem`은 이미 그 모듈에 의존한다.
- 기본값 `null` 덕분에 기존 호출부(`YGToppingGroupPreviewScreen`, `ToppingImageTest`의 `Remote(url)` 비교)는 바뀌지 않는다.

### `:feature:groups:list:impl` — `util/ToppingImage.kt`

```kotlin
internal fun MyParfaitGroupVO.toToppingImage(
    outlines: Map<String, ToppingOutline>,
): YGToppingImage

internal fun List<MyParfaitGroupVO>.borderedImageUrls(): List<String>
```

- `toToppingImage`: `recentImageUrl`이 없으면 지금처럼 `groupId`로 템플릿을 고른다(테두리 값은 보지 않는다).
  있으면 `Remote(url, border)`를 만들고, `recentImageBorder`가 `Solid`이면서 `color.toColorOrNull()`이 성공할 때만
  `YGToppingBorder(color, width.dp, outlines[url])`을 채운다.
- `borderedImageUrls`: 위와 **같은 판정**으로 테두리를 그릴 그룹의 URL만 모은다. 판정 기준을 두 곳에 두지 않으려고
  같은 파일의 비공개 헬퍼 하나를 두 함수가 함께 쓴다.
- 두께 범위는 다시 가두지 않는다 — 데이터 계층의 `ToppingBorder.solidClamped`가 이미 2~30dp로 가뒀다.

### `:core:designsystem` — `component/ygtoppingcutout/ToppingBorderPlateCache.kt`

```kotlin
internal object ToppingBorderPlateCache {
    /** subjectLongSide 가 null 이면 가장 최근 판, 값이 있으면 그 크기에 fitsSubject 로 맞는 판 중 가장 최근 판 */
    fun get(
        outline: ToppingOutline,
        outsetPx: Float,
        aspectRatio: Float,
        subjectLongSide: Int? = null,
    ): ToppingBorderPlate?

    fun put(outline: ToppingOutline, outsetPx: Float, aspectRatio: Float, plate: ToppingBorderPlate)

    fun clear()
}
```

- 값이 판 한 장에서 **선반**(최근 순 판 목록)으로 바뀐다. 열쇠(`PlateKey`)와 열쇠 LRU 상한은 그대로다.
- `put`: 같은 선반에서 새 판의 `subjectLongSide`와 `fitsSubject`로 맞는 판을 걷어 내고 새 판을 맨 앞에 넣는다.
  선반이 장수 상한(목록·캔버스 두 크기 + 핀치 중간 크기 한 장 = 3장)을 넘으면 가장 오래된 판을 버린다.
  드래그로 크기가 연속으로 바뀌어도 한 구간(1.25배) 안의 판은 서로 대체되므로 선반이 쏟아지지 않는다.
- `get`: 찾은 판을 선반 맨 앞으로 옮긴다(선반 안 LRU).
- 인자 없는 기존 `get` 호출부(첫 컴포지션의 `remember`, `LaunchedEffect` 첫 줄)는 바꾸지 않는다.

## 동작 / 상태

### `YGToppingGroup`의 `Remote` 분기

`AsyncImage`를 걷어 내고 painter를 직접 만든 뒤 `painter.state`를 구독한다.

```kotlin
val sizePx = with(LocalDensity.current) { SizeTokens.Size96.getDp().roundToPx() }
val request = remember(url, sizePx) {
    ImageRequest.Builder(context).data(url).size(sizePx).build()
}
val painter = rememberAsyncImagePainter(model = request, contentScale = ContentScale.Fit)
```

- ⚠️ **요청에 크기를 반드시 넣는다.** Coil 3의 `AsyncImagePainter#updateRequest`는 요청에 크기가 없으면
  `SizeResolver.ORIGINAL`로 원본을 디코딩한다. `AsyncImage`는 레이아웃 제약(`ConstraintsSizeResolver`)으로 크기를
  정해 주었으므로, `model = url`만 넘기면 목록의 모든 원격 토핑이 96dp 대신 원본 크기로 디코딩된다.
  테두리가 없는 카드도 이 경로를 타므로 영향 범위가 목록 전체다.
- 크로스페이드(`newParfaitImageLoader`)와 실패 시 즉시 뜨는 에러 그래픽은 painter 경로에서도 그대로다.

| painter 상태 | 그리는 것 | 토핑 축소(`padding`) | `borderColor` |
|---|---|---|---|
| `Error` | `painterResource(TOPPING_ERROR_DRAWABLE)`를 `Image`로 | 없음(96dp) | — |
| `Success` | `YGToppingCutoutImage` | `border?.width ?: 0.dp` | `border?.color` |
| 그 밖(`Empty`·`Loading`) | `YGToppingCutoutImage` | `border?.width ?: 0.dp` | `null` |

- **로딩 중에 색을 넘기지 않는 이유**: 테두리 판은 알맹이 아래에 깔리므로, 알맹이가 뜨기 전에 그리면 실루엣 모양의
  색 덩어리만 보인다. 캔버스 `CanvasToppingLayer#ToppingImage`와 같은 조건이다.
- **축소를 painter 상태가 아니라 데이터로 정하는 이유**: 거리판·이미지 도착 시점에 토핑 크기가 튀지 않게 한다.
  로딩 중에는 그림이 비어 있어 줄어든 크기가 보이지 않고, 성공하면 줄어든 크기로 떠서 테두리만 뒤따라 붙는다.
- **에러에서만 축소를 풀어 주는 이유**: 에러 그래픽은 테두리가 없는데 줄어들 까닭이 없다. 로딩(빈 그림)에서 에러로
  넘어가는 순간의 크기 변화는 보이지 않는다.
- `Template`·`Error` 분기는 바꾸지 않는다.

### 모디파이어 순서

```text
size(Size96) → offset(type.imageOffset) → rotate(type.rotation) → clip(RectangleShape) → padding(border width)
```

- `padding`이 `clip` **아래**여야 테두리 띠(알맹이 밖으로 굵기만큼)가 clip 상자 안에 들어온다. 위에 두면 clip 상자가
  함께 줄어 띠가 잘린다.
- `clip`이 `rotate` 안쪽이어야 한다는 기존 조건은 그대로다.
- **`padding`은 공유 `imageModifier`에 넣지 않는다.** `Remote` 분기에서 `YGToppingCutoutImage`를 그릴 때만 붙인다.
  `Template`·`Error` 분기와 `Remote`의 로드 실패 그래픽은 지금처럼 96dp다.

### `YGToppingCutoutImage` — 그리기 단계의 판 고르기

`ToppingBorder`의 `Canvas` 블록은 지금 `plate` 상태가 알맹이 크기에 맞지 않으면 그리지 않고 새 판을 기다린다.
여기에 한 단계를 더한다.

```text
current = plate?.takeIf { fitsSubject(realSubjectLongSide) }
    ?: ToppingBorderPlateCache.get(outline, outsetPx, aspectRatio, realSubjectLongSide)
    ?: return  // 맞는 판이 어디에도 없을 때만 새 판을 기다린다
```

- 목록에서 캔버스로 들어간 첫 프레임에 `plate`가 목록 크기 판이어도, 선반에 남은 캔버스 크기 판을 곧바로 그린다.
- 캐시 조회는 스냅샷 상태가 아니다. 새 판이 나오면 `plate` 상태가 바뀌어 다시 그려지므로 그 경로로 충분하다.
  조회는 동기화된 맵 읽기 한 번이고, 상태 판이 맞지 않을 때만 일어난다.
- 판을 만드는 `LaunchedEffect`는 바꾸지 않는다. 캐시에서 맞는 판을 꺼내 그려도 지금 크기에 정확한 판은 여전히 만들어
  갈아 끼운다. 그래서 굵기가 1.25배 안에서 틀어진 판이 계속 남는 일은 없다.

### G-001 — `GroupListContent`

```kotlin
val borderedImageUrls = remember(groupList) { groupList.borderedImageUrls() }
val outlines = rememberToppingOutlines(models = borderedImageUrls, retryKey = 0)
// …
YGToppingGroup(image = group.toToppingImage(outlines), …)
```

- 목록 전체가 아니라 **테두리를 그릴 토핑만** 거리판을 뜬다. 거리판 한 장은 이미지 디코딩 + 전 픽셀 순회다.
- 새로고침 중에는 `GroupListScreen`이 `groupList`를 비우므로 `models`도 빈다. 이미 맵에 들어온 거리판은 그대로 남아
  같은 URL이면 곧바로 쓰인다. 맵에는 없고 `ToppingOutlineCache`에만 있는 URL은 `LaunchedEffect`가 URL을 **순서대로**
  불러오므로, 앞선 캐시 미스 URL의 로드가 끝난 뒤에 들어온다.
- `retryKey`는 0으로 고정한다. 목록에는 이미지 재시도 경로가 없다. 그래서 거리판 로드가 실패하면 목록이 바뀔 때까지
  다시 시도하지 않고, 그동안 토핑은 줄어든 크기로 뜨되 테두리만 없다. 받아들인다.

## 파일 구성

| 모듈 | 파일 | 변경 |
|---|---|---|
| `:core:designsystem` | `component/ygtoppinggroup/YGToppingBorder.kt` | 신설 |
| `:core:designsystem` | `component/ygtoppinggroup/YGToppingImage.kt` | `Remote`에 `border` |
| `:core:designsystem` | `component/ygtoppinggroup/YGToppingGroup.kt` | `Remote` 분기 교체(프리뷰는 그대로) |
| `:core:designsystem` | `component/ygtoppingcutout/ToppingBorderPlateCache.kt` | 열쇠당 판 한 장 → 선반, `get`에 `subjectLongSide` |
| `:core:designsystem` | `component/ygtoppingcutout/YGToppingCutoutImage.kt` | `ToppingBorder`의 그리기 단계에서 크기가 맞는 판을 캐시에서 고른다 |
| `:core:designsystem` | `androidTest/.../component/ygtoppingcutout/ToppingBorderPlateCacheTest.kt` | 신설(기존 androidTest 하니스) |
| `:feature:groups:list:impl` | `util/ToppingImage.kt` | 시그니처 변경 + `borderedImageUrls` |
| `:feature:groups:list:impl` | `route/GroupListScreen.kt` | `GroupListContent`가 거리판을 불러 넘긴다 |
| `:feature:groups:list:impl` | `test/.../util/ToppingImageTest.kt` | 케이스 추가, 기존 호출을 `toToppingImage(emptyMap())`로 |
| `:app-preview` | `screen/component/YGToppingGroupPreviewScreen.kt` | "Remote + 테두리" 샘플(`rememberToppingOutlines` 사용 — 모듈이 `:core:ui`에 의존한다) |

## 테스트

새 테스트 하니스·빌드 설정은 만들지 않는다.

**`ToppingImageTest`(JVM 유닛)** — 거리판은 `ToppingOutline.of`로 만든다.

| 경우 | 기대 |
|---|---|
| `Solid`(유효 색) + 거리판 있음 | `Remote(url, YGToppingBorder(color, width.dp, outline))` |
| `Solid`(유효 색) + 거리판 없음 | `border`는 있고 `outline == null` |
| `Solid` + 파싱 불가 색 | `border == null` |
| `None` | `border == null` |
| `recentImageUrl == null` (+ 테두리 값 아무거나) | `Template` — 기존 템플릿 케이스 그대로 |
| `borderedImageUrls` | 유효 `Solid`인 그룹의 URL만, 목록 순서대로 |

**`ToppingBorderPlateCacheTest`(`:core:designsystem` androidTest)** — 판에 `ImageBitmap`이 들어 Android 런타임이
필요하므로 이미 있는 계측 테스트 소스셋에 둔다. 캐시가 프로세스 전역이므로 `@Before`에서 `clear()`를 부른다.
실행에는 기기나 에뮬레이터가 필요하다.

| 경우 | 기대 |
|---|---|
| 1.25배 넘게 다른 두 크기를 `put` | 두 크기 모두 `get(…, 그 크기)`로 꺼내진다 — 결정 5의 회귀 고정 |
| 1.25배 안의 크기를 다시 `put` | 이전 판이 대체되어 선반에 한 장만 남는다 |
| 장수 상한을 넘게 서로 다른 크기를 `put` | 가장 오래된 판이 빠진다 |
| `get(…)`(크기 없음) | 가장 최근에 넣거나 꺼낸 판 |
| 크기에 맞는 판이 선반에 없음 | `null` |
| 서로 대체되지 않는 두 판이 한 크기에 함께 맞음 | 더 최근 판 |
| 굵기가 다른 열쇠 | 서로 섞이지 않는다 |

**렌더(`YGToppingGroup`)** — 자동 테스트를 새로 두지 않는다. app-preview 카탈로그 샘플과 실기기로 확인한다.
`@YGPreview`로는 확인할 수 없다: 원격 이미지가 프리뷰에서 `Success`에 닿지 않아 테두리 색이 넘어가지 않고,
거리판을 불러오는 `rememberToppingOutlines`는 `:core:designsystem`에서 부를 수 없다.

실기기에서 볼 것:
1. 테두리가 잘리지 않는다.
2. 테두리 없는 토핑의 크기가 지금과 같다.
3. 이미지 로드 실패 시 에러 그래픽이 96dp다.
4. 테두리가 있는 그룹 카드를 눌러 캔버스로 들어갔다가 돌아오기를 반복해도, 양쪽 모두 그 토핑의 테두리가 깜빡이지 않는다.
   정사각과 비정사각 토핑을 **둘 다** 보고, 첫 복귀와 두 번째 이후 복귀를 따로 본다. 비정사각 토핑의 첫 복귀에서 한 번 늦게
   붙는 것은 알려진 동작이다(주의 절 "Coil 메모리 캐시" 항목).
5. 캔버스 화면 넷(캔버스 메인·배경 편집·배치·누끼 확인)에서 토핑 크기를 드래그로 바꾸거나 Spotlight를 전환할 때
   테두리 동작이 지금과 같다 — 판 캐시를 네 화면이 함께 쓰기 때문이다.
6. 목록을 당겨 새로고침한 뒤에도 테두리가 다시 붙는다.

## 머지 후 문서 반영 (완료)

기준선 점검 80회차(2026-09-16, `f37a76540`)에서 아래를 반영했다.

- [design-system.md](../../../architecture/design-system.md) — `YGToppingGroup` 항목에 `Remote`의 `border`·축소 규칙·
  painter 경로 전환을 적었고, 테두리 우회를 쓰는 화면 수를 넷에서 **다섯**으로 고쳤다.
- [ADR-0030](../../../adr/0030-topping-outline-distance-field.md) — 화면 수 서술을 고치고, "위험·방어" 절의
  "크기는 열쇠에 안 넣는다"에 **열쇠당 선반**이 붙었다는 후속 기록을 달았다(결정 5). 결정의 방향을 뒤집지
  않으므로 새 ADR은 만들지 않았다.
- [open-questions](../../../synthesis/open-questions.md) OQ-P-316 — 렌더가 닫혔고 ③④는 잔존한다.
- [open-questions](../../../synthesis/open-questions.md) OQ-P-317 — 판 캐시의 총 장수 상한이 열쇠 수 × 선반 장수로
  늘었다는 점, `clear()`에 테스트 호출부가 생겨 ②(테스트 격리)에 첫 대응이 들어왔다는 점을 적었다.

## 주의 / 열린 질문

- **거리판 캐시를 캔버스와 나눠 쓴다.** `ToppingOutlineCache` 상한은 64칸이다. 한 사용자가 속할 수 있는 그룹 수의
  상한은 계약 문서([api/parfait-group.md](../../../api/parfait-group.md))에서 찾지 못했다. 넘어도 깨지지 않고 LRU로 밀려나
  다시 뜰 뿐이다. 캐시 수명 문제는 OQ-P-317이 추적한다.
- **띠 판 캐시의 총 장수 상한이 세 배가 된다**(결정 5, 열쇠 상한 × 선반 3장). 판 한 장의 크기는 알맹이 + 사방 굵기라
  원래 총량 상한이 없었고, 이번 변경이 그 성질을 바꾸지는 않는다. 목록 판은 캔버스 판보다 작다.
- **목록 → 캔버스 첫 프레임 테두리는 선반에 캔버스 판이 남아 있을 때만 즉시 뜬다.** 그 토핑을 캔버스에서 한 번도
  그린 적이 없거나 판이 밀려났으면 지금처럼 새 판을 기다린다. **반대 방향도 같다** — 캔버스에서 토핑을 1.25배 구간
  셋 이상에 걸쳐(대략 1.7배 넘게) 키우면 목록 판이 선반에서 밀려나, 목록으로 돌아갈 때 한 번 깜빡인다.
- **목록과 캔버스는 Coil 메모리 캐시 항목 하나를 함께 쓴다.** 목록 요청은 크기만 주고 `precision`을 주지 않아 INEXACT이고,
  transformation이 없으면 메모리 캐시 열쇠에 크기가 들어가지 않는다(Coil 3.5.0 `MemoryCacheService#newCacheKey`). 그래서
  판 캐시 열쇠의 비율(`painter.intrinsicSize`)은 이렇게 움직인다.
  1. 목록 첫 방문: 축소 디코딩본의 비율로 목록 판을 만든다. 비정사각 토핑은 반올림 때문에 원본 비율과 float이 다를 수 있다.
  2. 캔버스 진입: 원본 디코딩이 같은 캐시 항목을 덮어쓴다.
  3. 목록 복귀: 축소하지 않은 원본 비트맵을 받아(`isSampled = false`, INEXACT) 비율이 원본으로 바뀐다. 1의 목록 판을 못 찾아
     **비정사각 토핑은 첫 복귀 때 한 번 테두리가 늦게 붙는다.** 알려진 동작으로 받아들인다.
  4. 그 뒤로는 비율과 무관하게 **모든 토핑**에서 목록과 캔버스 열쇠가 겹치므로 결정 5가 전부에 걸린다.
- **판 캐시 열쇠 32칸을 목록이 새로 쓴다.** 테두리 있는 그룹마다 열쇠 하나(위 3의 전환 때는 잠시 둘)를 쓰고, 그룹 수에는 상한이
  없다. 그룹 수와 캔버스 토핑 수가 32를 넘으면 오래된 열쇠의 선반이 통째로 밀려나 결정 5의 효과가 조용히 사라진다.
  `ToppingOutlineCache`(64칸)에서 거리판이 밀려났다가 다시 뜨면 새 인스턴스가 되어 기존 선반이 전부 고아가 된다(OQ-P-317).
- **실루엣이 긴 변 끝에 닿는 이미지는 최외곽 약 1px이 clip에 걸릴 수 있다.** 띠 가장자리의 반투명 처리
  (`ToppingOutlineSpec#EDGE_FEATHER_PX`)와 `fitSize`·`scaledPadding`의 반올림 때문이다. 실기기에서 눈에 띄면 그때 다룬다.
- **목록 두께가 캔버스보다 굵어 보인다**(결정 1). 디자인 확인에서 다른 값이 나오면 이 스펙의 결정 1을 고친다.
- 템플릿·조회 실패 그래픽 테두리, 목록 토핑 알파 판정은 OQ-P-316 ③④에 그대로 남는다.
