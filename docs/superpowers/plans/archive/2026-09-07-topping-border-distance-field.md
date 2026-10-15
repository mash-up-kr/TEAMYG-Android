---
id: topping-border-distance-field
title: 토핑 테두리 거리장 렌더링 통일
status: done
type: work-order
created: 2026-09-07
updated: 2026-09-08
platforms: android
owner: Parfait 팀
related_adr: ADR-0030, ADR-0025
related_spec: topping-border-distance-field
related_code:
  - ToppingOutline.kt#ToppingOutline
  - ToppingOutlineSpec.kt#ToppingOutlineSpec
  - ToppingOutlineBitmap.kt#toBorderAlphaBitmap
  - ToppingOutlineCache.kt#ToppingOutlineCache
  - ToppingBorderPlateCache.kt#ToppingBorderPlateCache
  - YGToppingCutoutImage.kt#YGToppingCutoutImage
  - ToppingHitTarget.kt#ToppingHitTarget
  - FloatArrayExtension.kt#fillWithSquaredDistance
archived_reason: 8 Task 전량 수행·develop 머지(2026-09-08, PR #464 `23675cc1f`)
tags: [plan, parfait, topping, border, rendering, hit-test]
---

# 토핑 테두리 거리장 렌더링 통일 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는
> superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 토핑 테두리를 그리는 코드 둘(편집 화면의 거리장, 나머지 화면의 여덟 방향 스탬프)을 거리장
하나로 합치고, 같은 거리장이 터치 판정까지 먹이게 한다.

**Architecture:** 거리장 계산기를 `feature/segmentation/impl`의 `internal`에서
`core:util:jvm`(순수 로직)과 `core:util:android`(비트맵 변환)로 승격한다. 캐시는 `core:ui`에 새로
세워 한 번의 디코딩으로 거리판을 낸다. `YGToppingCutoutImage`는 스탬프 여덟 장 대신 띠 한 장 +
원본 한 장을 그리고, `ToppingHitTarget`은 여덟 방향 되밀기 대신 거리판을 읽는다.

**Tech Stack:** Kotlin, Jetpack Compose, Coil 3, `kotlin.test` 유닛 테스트, Gradle 컨벤션 플러그인.

**Spec:** [`parfait/specs/archive/2026-09-07-topping-border-distance-field.md`](../../specs/archive/2026-09-07-topping-border-distance-field.md)

> 📌 **이 계획은 서브에이전트 검수 3회(코드 대조·Task 순서·알고리즘)를 반영한 2판이다.**
> 초판의 치명 결함 일곱을 고쳤다 — 판 좌표 매핑이 실루엣을 여백까지 늘이던 것, 양자화한 상자를
> 기하로 쓰던 것, 판 밖 좌표 클램프로 여백이 통째로 칠해지고 판정이 부풀던 것, 컴파일이 깨진
> 모듈에서 유닛 테스트를 돌리라던 것, `ToppingAlphaMaskTest.kt` 삭제 누락, `kotlin.math.min`
> import 누락, 회귀 테스트가 스탬프 방식에서도 통과하던 것이다.

> ✅ **완료·develop 머지(2026-09-08, PR #464 `23675cc1f`).** 8 Task를 전량 수행했고 머지 트리가
> 브랜치 팁과 같다. **계획과 갈린 자리는 넷이다.**
> ① **값 타입과 상수가 `outline/` 밖으로 나갔다** — 계획은 `ToppingOutline.kt` 한 파일이
> `ToppingBorderBand`·`ToppingBorderTarget`·`OUTLINE_ALPHA_THRESHOLD` 를 함께 내놓게 했는데,
> 머지본은 셋을 `core:util:jvm` 의 `model/` 로 옮기고 담기 규격을 `ToppingOutlineSpec`
> (알파 문턱·거리 담기 배수·상한·가장자리 물림) 한 곳에 모았다. 판을 만드는 쪽과 읽는 쪽이 같은
> 값을 봐야 거리가 어긋나지 않기 때문이다.
> ② **띠 판 캐시가 새로 생겼다** — Task 4가 예정에 없던 `ToppingBorderPlateCache`
> (`core:designsystem`, 전역 LRU 32칸)를 낳았다. 컴포저블이 다시 만들어질 때 판까지 다시 만들면
> 그동안 테두리를 안 그려 깜빡였다. 판을 다시 쓸지는 `ToppingBorderPlate.fitsSubject` 가 배율로
> 판정한다.
> ③ **지연 재생성 갈래가 통째로 사라졌다** — Task 4의 `BORDER_REBUILD_DELAY_MS` 와 Task 8 Step 3이
> 함께 무효가 됐다(위 Task 8 각주).
> ④ **굵기 범위가 2~30dp 로 좁혀지고 `domain` 한 곳으로 모였다** — 아래 Global Constraints의
> 사후 정정과 같은 자리다.

## Global Constraints

- **커밋하지 않는다.** 사용자가 요청하지 않았다. Task 끝의 검증이 통과하면 다음 Task로 간다.
- **모든 Task 경계에서 빌드가 초록이어야 한다.** 컴파일이 깨진 채 넘어가는 Task는 없다.
- **작업 대상 저장소는 `TJYG-Android`다.** 브랜치는 `refactor/#337-topping-border-optimization`.
  Task 8의 문서 갱신만 이 문서 저장소에서 한다.
- **굵기 규칙을 바꾸지 않는다.** `MIN_BORDER_WIDTH_DP = 2f`, `MAX_BORDER_WIDTH_DP = 50f`,
  화면 dp 고정. 이 값들은 읽기만 한다.
  > **사후 정정(2026-09-08)** — 이 계획 밖의 후속 라운드가 범위를 2~30dp 로 좁히고
  > `ToppingBorder.WIDTH_RANGE_DP` 한 곳에 뒀다. 아래 본문의 `50f` 는 계획 당시 값이다.
- **기존 파일을 전문으로 덮어쓰지 않는다.** 추가·치환으로 고친다. 계획이 코드 블록을 통째로 주는
  자리(Task 1·2·5의 신규 파일, Task 3 Step 1과 Task 4 Step 1의 명시적 치환)는 예외다.
- **주석 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다). 필요하면 근거 문서를 가리킨다.
- **ktlint 한 줄 120자**를 넘기지 않는다. `.editorconfig`가 `no-unused-imports`와
  `no-empty-class-body`를 켜 두었으므로 **미사용 import와 빈 `companion object`가 CI를 깬다.**
- Gradle 태스크 이름이 모듈 종류마다 다르다. `core:util:jvm`은 kotlin-jvm이라 `:test`이고,
  나머지(`core:util:android`·`core:ui`·`core:designsystem`·feature impl)는 Android 라이브러리라
  `:testDebugUnitTest`·`:compileDebugKotlin`이다.

---

## 파일 구성

| 파일 | 책임 | Task |
|------|------|------|
| `core/util/jvm/src/main/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutline.kt` | 거리판 보유·보간·띠 채우기·불투명 판정 (신설) | 1 |
| `core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutlineTest.kt` | 위의 유닛 (신설) | 1 |
| `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/outline/ToppingOutlineBitmap.kt` | `Bitmap` → 거리판, 거리판 → 띠 `Bitmap` 2종 (신설) | 2 |
| `feature/segmentation/impl/.../editor/ToppingBorderOutline.kt` | `toBorderBands`만 남는다 (축소) | 3 |
| `feature/segmentation/impl/.../screen/ToppingBorderEditScreen.kt` | 새 코어 사용 + 여백을 굵기 상한에서 파생 | 3 |
| `feature/segmentation/impl/.../viewmodel/ToppingEditViewModel.kt` | 굵기 상한을 `internal`로 연다 | 3 |
| `core/designsystem/.../component/ygtoppingcutout/YGToppingCutoutImage.kt` | 띠 1장 + 원본 1장 | 4, 6 |
| `core/ui/src/main/java/com/teamyg/parfait/core/ui/outline/ToppingOutlineCache.kt` | LRU 캐시·in-flight 합류·Coil 디코딩 (신설) | 5 |
| `feature/groups/canvas/impl/.../util/ToppingHitTarget.kt` | 거리판 조회 판정 | 6 |
| `feature/groups/canvas/impl/.../util/ToppingAlphaMask.kt` | 삭제 | 6 |
| `feature/groups/canvas/impl/.../util/ToppingAlphaMaskCache.kt` | 삭제 | 6 |
| `feature/groups/canvas/impl/src/test/.../util/ToppingAlphaMaskTest.kt` | 삭제 | 6 |
| `feature/groups/canvas/impl/src/test/.../util/ToppingHitTestTest.kt` | 거리판 기준으로 갱신 | 6 |
| `feature/groups/canvas/impl/.../component/CanvasToppingLayer.kt` | 거리판 결선, `loadMasks` 제거 | 6 |
| `feature/groups/canvas/impl/.../screen/CanvasBGEditScreen.kt` | 거리판 결선, 인셋 우회 **유지** | 6 |
| `feature/groups/canvas/impl/.../screen/CanvasToppingPlaceScreen.kt` | 거리판 결선 | 7 |
| `feature/segmentation/impl/.../screen/SegmentationConfirmScreen.kt` | 거리판 결선 | 7 |

---

### Task 1: 거리판 코어를 `core:util:jvm`에 세운다

**Files:**
- Create: `core/util/jvm/src/main/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutline.kt`
- Test: `core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutlineTest.kt`

**Interfaces:**
- Consumes: 같은 모듈 `extension` 패키지의 `fillWithSquaredDistance`·`SQUARED_DISTANCE_UNSET`·
  `fadeArgb`·`mixArgb`
- Produces:
  - `class ToppingOutline` — `width: Int`·`height: Int`·`hasAnySeed: Boolean`
  - `ToppingOutline.of(width: Int, height: Int, alphaAt: (x: Int, y: Int) -> Int): ToppingOutline`
  - `fun distanceAt(x: Float, y: Float): Float`
  - `fun isOpaqueAt(x: Float, y: Float): Boolean`
  - `fun buildBorderAlpha(target: ToppingBorderTarget, outsetPx: Float): ByteArray?`
  - `fun buildBorderPixels(target: ToppingBorderTarget, bands: List<ToppingBorderBand>): IntArray?`
  - `data class ToppingBorderBand(val outsetPx: Float, val colorArgb: Int)`
  - `data class ToppingBorderTarget(width, height, subjectLeft, subjectTop, subjectWidth, subjectHeight)`
  - `const val OUTLINE_ALPHA_THRESHOLD = 128`

**초판에서 고친 것 둘**

1. **판 안에서 알맹이가 놓이는 자리를 따로 받는다.** 초판은 목표 크기만 받아 실루엣을 여백까지
   늘려 그렸다. 굵기가 굵을수록 배율이 커져 200px 토핑에 150px 테두리면 실루엣이 2.5배로 부풀었다.
2. **판 밖 좌표를 가장자리 값으로 고정하지 않는다.** 누끼는 보통 트림되어 실루엣이 판 변에 닿으므로,
   고정하면 여백이 통째로 거리 0으로 답해 칠해지고 판정도 사각형 밖으로 부푼다.

- [x] **Step 1: 실패하는 테스트를 쓴다**

`core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutlineTest.kt`:

```kotlin
package com.teamyg.parfait.core.util.jvm.outline

import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val OPAQUE = 255
private const val TRANSPARENT = 0
private const val TOLERANCE = 0.2f

/** 실루엣을 그림으로 준다. `#` 이 불투명한 자리다 */
private fun outlineOf(vararg rows: String): ToppingOutline =
    ToppingOutline.of(width = rows.first().length, height = rows.size) { x, y ->
        if (rows[y][x] == '#') OPAQUE else TRANSPARENT
    }

/** 알맹이가 판을 그대로 채우는 목표. 여백을 따지지 않는 테스트가 쓴다 */
private fun wholeTarget(
    width: Int,
    height: Int,
): ToppingBorderTarget = ToppingBorderTarget(
    width = width,
    height = height,
    subjectLeft = 0,
    subjectTop = 0,
    subjectWidth = width,
    subjectHeight = height,
)

/** ByteArray 는 부호가 있어 255 가 -1 로 담긴다. 눈으로 읽을 0~255 로 되돌린다 */
private fun ByteArray.alphaAt(index: Int): Int = this[index].toInt() and 0xFF

class ToppingOutlineTest {
    @Test
    fun buildBorderAlpha_fillsEveryCellWithinTheOutset() {
        // Given 한가운데 한 칸만 불투명한 15x15 판
        val size = 15
        val center = 7
        val outline = ToppingOutline.of(size, size) { x, y ->
            if (x == center && y == center) OPAQUE else TRANSPARENT
        }

        // When 거리 5 까지 두른다
        val outset = 5f
        val alpha = outline.buildBorderAlpha(wholeTarget(size, size), outsetPx = outset)

        // Then 거리 5 이하인 칸이 하나도 빠짐없이 칠해진다.
        // 여덟 방향 스탬프는 여기서 여덟 갈래 꽃잎을 만들어 스탬프 사이가 빈다 — 이 단언이
        // 이번 작업의 목적을 코드로 고정한다
        assertNotNull(alpha)
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (hypot((x - center).toFloat(), (y - center).toFloat()) > outset) continue
                assertTrue(alpha.alphaAt(y * size + x) > 0, "거리 $outset 안쪽 ($x,$y) 이 비었다")
            }
        }

        // 그리고 스탬프가 절대 못 닿는 자리 하나를 따로 못박는다 — 씨앗에서 (3,-3) 은
        // 여덟 방향 어느 이동으로도 나오지 않는다
        assertTrue(alpha.alphaAt(4 * size + 10) > 0)
    }

    @Test
    fun buildBorderAlpha_edgeSitsAtTheOutsetDistance() {
        // Given 한가운데 한 칸만 불투명한 7x7 판
        val outline = outlineOf(
            ".......",
            ".......",
            ".......",
            "...#...",
            ".......",
            ".......",
            ".......",
        )

        // When 거리 2 까지 칠한다
        val alpha = outline.buildBorderAlpha(wholeTarget(7, 7), outsetPx = 2f)

        // Then 거리 2 인 자리는 남고 거리 3 인 자리는 비어 있다
        assertNotNull(alpha)
        assertTrue(alpha.alphaAt(3 * 7 + 1) > 0, "거리 2 인 자리가 비었다")
        assertEquals(0, alpha.alphaAt(3 * 7 + 0), "거리 3 인 자리가 칠해졌다")
    }

    @Test
    fun buildBorderAlpha_keepsTheSubjectInsideThePadding() {
        // Given 4x4 실루엣을 사방 3 씩 비운 10x10 판에 앉힌다
        val outline = outlineOf(
            "####",
            "####",
            "####",
            "####",
        )
        val target = ToppingBorderTarget(
            width = 10,
            height = 10,
            subjectLeft = 3,
            subjectTop = 3,
            subjectWidth = 4,
            subjectHeight = 4,
        )

        // When 거리 2 까지 두른다
        val alpha = outline.buildBorderAlpha(target, outsetPx = 2f)

        // Then 알맹이 자리는 채워지고, 여백 바깥 끝은 비어 있다.
        // 알맹이 자리를 안 받으면 실루엣이 판 전체로 늘어나 이 칸까지 칠해진다
        assertNotNull(alpha)
        assertTrue(alpha.alphaAt(5 * 10 + 5) > 0, "알맹이 한가운데가 비었다")
        assertEquals(0, alpha.alphaAt(5 * 10 + 0), "여백 바깥 끝이 칠해졌다")
    }

    @Test
    fun buildBorderAlpha_isNullWhenNothingIsOpaque() {
        // Given 전부 투명한 판 — 실루엣이 없으면 두를 대상도 없다
        val outline = outlineOf(
            "....",
            "....",
        )

        assertFalse(outline.hasAnySeed)
        assertNull(outline.buildBorderAlpha(wholeTarget(4, 2), outsetPx = 1f))
    }

    @Test
    fun distanceAt_interpolatesBetweenCells() {
        // Given 왼쪽 한 줄만 불투명하다 — 거리가 x 그대로다
        val outline = outlineOf(
            "#....",
            "#....",
        )

        // Then 칸 사이를 읽으면 두 칸 값의 중간이 나온다
        assertEquals(2.5f, outline.distanceAt(2.5f, 0f), TOLERANCE)
    }

    @Test
    fun distanceAt_outsideTheField_addsTheOverflowInsteadOfClamping() {
        // Given 왼쪽 끝 한 칸만 불투명하다
        val outline = outlineOf("#..")

        // Then 판 왼쪽 밖 2 칸은 거리 0 이 아니라 2 다.
        // 가장자리 값으로 고정하면 실루엣이 판 변에 닿은 토핑에서 여백이 통째로 칠해진다
        assertEquals(2f, outline.distanceAt(-2f, 0f), TOLERANCE)
    }

    @Test
    fun isOpaqueAt_readsTheNearestCellWithoutInterpolating() {
        // Given 불투명한 칸과 투명한 칸이 붙어 있다
        val outline = outlineOf("#.")

        // Then 경계 근처는 보간값이 아니라 가장 가까운 칸의 답을 준다
        assertTrue(outline.isOpaqueAt(0.4f, 0f))
        assertFalse(outline.isOpaqueAt(0.6f, 0f))
    }

    @Test
    fun distanceAt_quantizationErrorStaysWithinAnEighthOfAPixel() {
        // Given 왼쪽 위 한 칸만 불투명해 대각 거리가 1/8 의 배수가 아닌 판
        val outline = outlineOf(
            "#..",
            "...",
        )

        // Then √5 = 2.2360…은 눈금에 딱 안 맞지만 오차가 1/16 을 넘지 않는다
        assertEquals(sqrt(5f), outline.distanceAt(2f, 1f), 1f / 16f)
    }

    @Test
    fun buildBorderPixels_paintsEachBandWithItsOwnColor() {
        // Given 한가운데 한 칸만 불투명한 9x9 판에 색이 다른 두 겹을 두른다
        val outline = ToppingOutline.of(9, 9) { x, y ->
            if (x == 4 && y == 4) OPAQUE else TRANSPARENT
        }
        val red = 0xFFFF0000.toInt()
        val blue = 0xFF0000FF.toInt()

        // When 안쪽 겹이 거리 2 까지, 바깥 겹이 거리 4 까지다
        val pixels = outline.buildBorderPixels(
            target = wholeTarget(9, 9),
            bands = listOf(
                ToppingBorderBand(outsetPx = 2f, colorArgb = red),
                ToppingBorderBand(outsetPx = 4f, colorArgb = blue),
            ),
        )

        // Then 실루엣 자리는 안쪽 겹 색, 바깥 겹 한복판은 바깥 겹 색이다
        assertNotNull(pixels)
        assertEquals(red, pixels[4 * 9 + 4])
        assertEquals(blue, pixels[4 * 9 + 1])

        // 겹 경계(거리 2)는 두 색을 반씩 섞은 자리라 어느 쪽 원색도 아니다
        val boundary = pixels[4 * 9 + 2]
        assertNotEquals(red, boundary)
        assertNotEquals(blue, boundary)
    }
}
```

- [x] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :core:util:jvm:test --tests "com.teamyg.parfait.core.util.jvm.outline.ToppingOutlineTest"
```

Expected: 컴파일 실패 — `Unresolved reference: outline`.

- [x] **Step 3: 구현한다**

`core/util/jvm/src/main/kotlin/com/teamyg/parfait/core/util/jvm/outline/ToppingOutline.kt`:

```kotlin
package com.teamyg.parfait.core.util.jvm.outline

import com.teamyg.parfait.core.util.jvm.extension.SQUARED_DISTANCE_UNSET
import com.teamyg.parfait.core.util.jvm.extension.fadeArgb
import com.teamyg.parfait.core.util.jvm.extension.fillWithSquaredDistance
import com.teamyg.parfait.core.util.jvm.extension.mixArgb
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 실루엣 안으로 볼 알파 문턱. 이보다 옅은 자리는 실루엣 바깥으로 친다 */
const val OUTLINE_ALPHA_THRESHOLD = 128

/** 1 필드픽셀을 이 수만큼 쪼개 담는다 */
private const val DISTANCE_STEPS_PER_PX = 8

/** 담을 수 있는 가장 먼 거리(필드픽셀). 판의 대각선보다 한참 크다 */
private const val MAX_STORED_DISTANCE_PX = Short.MAX_VALUE / DISTANCE_STEPS_PER_PX

/** 가장자리 한 겹을 반 픽셀씩 물려 칠해 계단이 지지 않게 한다 */
private const val EDGE_FEATHER_PX = 0.5f

private const val ALPHA_MAX = 255

/**
 * 테두리 한 겹이 차지하는 구간.
 *
 * @param outsetPx 실루엣에서 이 겹의 바깥 끝까지 거리. 겹은 아래 겹을 감싸며 쌓이므로
 *   자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다
 */
data class ToppingBorderBand(
    val outsetPx: Float,
    val colorArgb: Int,
)

/**
 * 띠를 칠할 판과, 그 판 안에서 알맹이가 놓이는 자리.
 *
 * 알맹이 자리를 따로 받는 것이 핵심이다 — 판은 알맹이보다 사방으로 넓고, 실루엣은 판 전체가
 * 아니라 그 안쪽 사각형에 대응한다. 이 값이 없으면 실루엣이 여백까지 채우도록 늘어난다.
 */
data class ToppingBorderTarget(
    val width: Int,
    val height: Int,
    val subjectLeft: Int,
    val subjectTop: Int,
    val subjectWidth: Int,
    val subjectHeight: Int,
)

/**
 * 실루엣에서 떨어진 거리를 픽셀마다 담아 둔 판.
 *
 * 실루엣 사본을 원 둘레에 빙 둘러 찍어 테두리를 만들면 굵어질수록 찍은 자국 사이가 벌어져
 * 가장자리가 갈라진다. 거리를 한 번 재 두면 굵기는 '거리가 얼마 이하인 자리를 칠하는' 문제가 되어
 * 어떤 굵기에서도 가장자리가 실루엣에서 같은 거리인 곡선으로 이어진다.
 *
 * 굵기를 바꿔도 거리는 그대로라, 슬라이더를 움직이는 동안에는 칠하는 일만 다시 하면 된다.
 */
class ToppingOutline internal constructor(
    val width: Int,
    val height: Int,
    /** 실루엣까지의 거리를 [DISTANCE_STEPS_PER_PX] 눈금으로 담는다 */
    private val distances: ShortArray,
) {
    val hasAnySeed: Boolean = distances.any { step -> step.toInt() == 0 }

    /**
     * 판 좌표계 거리.
     *
     * 판 밖 좌표는 가장자리 값으로 고정하지 않고 벗어난 만큼을 함께 잰다. 씨앗이 모두 판 안에
     * 있으므로 `√(가장자리 거리² + 벗어난 거리²)` 가 참값의 하한이고, 가장 가까운 씨앗이 축에
     * 나란할 때 참값과 같다. 고정하면 실루엣이 판 변에 닿은 토핑에서 판 밖이 통째로 거리 0 이 된다.
     */
    fun distanceAt(
        x: Float,
        y: Float,
    ): Float {
        val clampedX = x.coerceIn(0f, (width - 1).toFloat())
        val clampedY = y.coerceIn(0f, (height - 1).toFloat())
        val inside = interpolatedAt(clampedX, clampedY)

        val overflowX = x - clampedX
        val overflowY = y - clampedY
        if (overflowX == 0f && overflowY == 0f) return inside

        return sqrt(inside * inside + overflowX * overflowX + overflowY * overflowY)
    }

    /**
     * [distanceAt] 과 달리 보간하지 않고 가장 가까운 칸을 읽는다 — 판정이 칸 단위로 답하던
     * 기존 동작을 그대로 유지하기 위해서다.
     */
    fun isOpaqueAt(
        x: Float,
        y: Float,
    ): Boolean {
        val cellX = floor(x + 0.5f).toInt()
        val cellY = floor(y + 0.5f).toInt()
        if (cellX < 0 || cellY < 0 || cellX >= width || cellY >= height) return false
        return distances[cellY * width + cellX].toInt() == 0
    }

    /**
     * 색을 태우지 않은 단색 띠. 칸마다 0~255 의 덮은 정도만 담는다.
     *
     * @return 실루엣이 없거나 판이 비었으면 `null`
     */
    fun buildBorderAlpha(
        target: ToppingBorderTarget,
        outsetPx: Float,
    ): ByteArray? {
        if (!hasAnySeed || !target.isUsable || outsetPx <= 0f) return null

        val alpha = ByteArray(target.width * target.height)
        forEachBandPixel(target, floatArrayOf(outsetPx)) { index, _, coverage ->
            alpha[index] = (coverage * ALPHA_MAX).roundToInt().toByte()
        }
        return alpha
    }

    /**
     * 겹을 안쪽부터 겹겹이 칠한 그림. 알맹이는 이 위에 원래 자리 그대로 얹히므로 실루엣 안쪽도
     * 가장 안쪽 겹 색으로 채워 둔다.
     */
    fun buildBorderPixels(
        target: ToppingBorderTarget,
        bands: List<ToppingBorderBand>,
    ): IntArray? {
        if (!hasAnySeed || bands.isEmpty() || !target.isUsable) return null

        val colors = IntArray(bands.size) { index -> bands[index].colorArgb }
        val outsets = FloatArray(bands.size) { index -> bands[index].outsetPx }
        val pixels = IntArray(target.width * target.height)

        forEachBandPixel(target, outsets) { index, bandIndex, coverage ->
            // 겹의 끝에 걸친 자리는 반씩 물려, 가장 바깥이면 투명하게 안쪽이면 다음 겹 색으로 이어 준다
            pixels[index] = if (bandIndex == colors.lastIndex) {
                colors[bandIndex].fadeArgb(coverage)
            } else {
                colors[bandIndex].mixArgb(colors[bandIndex + 1], coverage)
            }
        }
        return pixels
    }

    /**
     * 띠 안에 드는 칸만 골라 [onPixel] 에 넘긴다. 단색과 여러 겹이 이 순회를 함께 쓴다.
     *
     * 목표 좌표를 판 좌표로 옮길 때 알맹이가 놓인 자리를 빼고 축마다 따로 배율을 잰다. 굵기는
     * 등방이라 한 축으로만 환산하는데, 알맹이가 실루엣 비율을 지켜 앉으므로 두 배율이 거의 같다.
     */
    private inline fun forEachBandPixel(
        target: ToppingBorderTarget,
        outsetsPx: FloatArray,
        onPixel: (index: Int, bandIndex: Int, coverage: Float) -> Unit,
    ) {
        val fieldPerTargetX = width.toFloat() / target.subjectWidth
        val fieldPerTargetY = height.toFloat() / target.subjectHeight
        val targetPxPerField = 1f / fieldPerTargetX

        val edges = FloatArray(outsetsPx.size) { index -> outsetsPx[index] * fieldPerTargetX }
        val outermostEdge = edges.last() + EDGE_FEATHER_PX * fieldPerTargetX

        for (y in 0 until target.height) {
            val fieldY = (y + 0.5f - target.subjectTop) * fieldPerTargetY - 0.5f
            val rowStart = y * target.width

            for (x in 0 until target.width) {
                val fieldX = (x + 0.5f - target.subjectLeft) * fieldPerTargetX - 0.5f
                val distance = distanceAt(fieldX, fieldY)
                if (distance > outermostEdge) continue

                var bandIndex = 0
                while (bandIndex < edges.lastIndex && distance > edges[bandIndex]) bandIndex++

                val coverage = ((edges[bandIndex] - distance) * targetPxPerField + EDGE_FEATHER_PX)
                    .coerceIn(0f, 1f)
                onPixel(rowStart + x, bandIndex, coverage)
            }
        }
    }

    /** 판 밖 보정을 태우지 않는 안쪽 전용 읽기. 네 칸을 섞어 칸 사이도 이어지게 한다 */
    private fun interpolatedAt(
        x: Float,
        y: Float,
    ): Float {
        val leftIndex = floor(x).toInt().coerceIn(0, width - 1)
        val topIndex = floor(y).toInt().coerceIn(0, height - 1)
        val rightIndex = (leftIndex + 1).coerceAtMost(width - 1)
        val bottomIndex = (topIndex + 1).coerceAtMost(height - 1)

        val rightWeight = (x - leftIndex).coerceIn(0f, 1f)
        val bottomWeight = (y - topIndex).coerceIn(0f, 1f)

        val topRow = topIndex * width
        val bottomRow = bottomIndex * width
        val top = lerp(rawAt(topRow + leftIndex), rawAt(topRow + rightIndex), rightWeight)
        val bottom = lerp(rawAt(bottomRow + leftIndex), rawAt(bottomRow + rightIndex), rightWeight)

        return lerp(top, bottom, bottomWeight)
    }

    private fun rawAt(index: Int): Float = distances[index].toInt().toFloat() / DISTANCE_STEPS_PER_PX

    companion object {
        /**
         * @param alphaAt 그 자리 픽셀의 알파를 0~255 로 답한다
         */
        fun of(
            width: Int,
            height: Int,
            alphaAt: (x: Int, y: Int) -> Int,
        ): ToppingOutline {
            val squared = FloatArray(width * height) { index ->
                val opaque = alphaAt(index % width, index / width) >= OUTLINE_ALPHA_THRESHOLD
                if (opaque) 0f else SQUARED_DISTANCE_UNSET
            }
            squared.fillWithSquaredDistance(width, height)

            return ToppingOutline(
                width = width,
                height = height,
                distances = ShortArray(squared.size) { index ->
                    val distance = sqrt(squared[index]).coerceAtMost(MAX_STORED_DISTANCE_PX.toFloat())
                    (distance * DISTANCE_STEPS_PER_PX).roundToInt().toShort()
                },
            )
        }
    }
}

private val ToppingBorderTarget.isUsable: Boolean
    get() = width > 0 && height > 0 && subjectWidth > 0 && subjectHeight > 0

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction
```

⚠️ `forEachBandPixel`이 `private inline`이므로 `rawAt`·`interpolatedAt`을 `private`로 두어도 된다.
비공개 접근 제한(`NON_PUBLIC_CALL_FROM_PUBLIC_INLINE`)은 **public API인 inline 함수**에만 걸린다.
`@PublishedApi`를 붙이지 마라 — 이유 없이 모듈 API가 넓어진다.

- [x] **Step 4: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :core:util:jvm:test --tests "com.teamyg.parfait.core.util.jvm.outline.ToppingOutlineTest"
```

Expected: 9건 PASS.

- [x] **Step 5: 모듈 전체 유닛이 여전히 초록인지 본다**

```bash
./gradlew :core:util:jvm:test
```

Expected: 기존 테스트 포함 전부 PASS.

---

### Task 2: 비트맵 변환을 `core:util:android`에 세운다

**Files:**
- Create: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/outline/ToppingOutlineBitmap.kt`

**Interfaces:**
- Consumes: Task 1의 `ToppingOutline`·`ToppingOutline.of`·`buildBorderAlpha`·`buildBorderPixels`·
  `ToppingBorderBand`·`ToppingBorderTarget`
- Produces:
  - `fun Bitmap.toToppingOutline(fieldLongSide: Int): ToppingOutline`
  - `fun ToppingOutline.toBorderAlphaBitmap(target: ToppingBorderTarget, outsetPx: Float): Bitmap?`
  - `fun ToppingOutline.toBorderArgbBitmap(target: ToppingBorderTarget, bands: List<ToppingBorderBand>): Bitmap?`

⚠️ **이 Task에는 자동 테스트가 없다.** `Bitmap`은 Android 런타임 타입이고 이 모듈에 Robolectric이
없다. 검증은 컴파일과 Task 4의 실기기 게이트다.

- [x] **Step 1: 구현한다**

```kotlin
package com.teamyg.parfait.core.util.android.outline

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderBand
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ALPHA_SHIFT = 24

/**
 * 알파가 남아 있는 자리를 실루엣으로 보고 거리를 잰다.
 *
 * @param fieldLongSide 거리를 잴 때 긴 변을 이 길이까지만 쓴다. 테두리 경계는 굵기만큼 완만한
 *   곡선이라 촘촘히 재도 모양이 달라지지 않는데, 원본 해상도로 재면 사진 크기에 비례해 시간과
 *   메모리만 늘어난다
 */
fun Bitmap.toToppingOutline(fieldLongSide: Int): ToppingOutline {
    val fieldScale = min(1f, fieldLongSide.toFloat() / max(width, height))
    val fieldWidth = max(1, (width * fieldScale).roundToInt())
    val fieldHeight = max(1, (height * fieldScale).roundToInt())

    val source = if (fieldWidth == width && fieldHeight == height) this else scale(fieldWidth, fieldHeight)
    val pixels = IntArray(fieldWidth * fieldHeight)
    source.getPixels(pixels, 0, fieldWidth, 0, 0, fieldWidth, fieldHeight)
    if (source !== this) source.recycle()

    return ToppingOutline.of(fieldWidth, fieldHeight) { x, y ->
        pixels[y * fieldWidth + x] ushr ALPHA_SHIFT
    }
}

/** 색을 태우지 않은 띠. 그리는 쪽이 `ColorFilter` 로 물들인다 */
fun ToppingOutline.toBorderAlphaBitmap(
    target: ToppingBorderTarget,
    outsetPx: Float,
): Bitmap? {
    val alpha = buildBorderAlpha(target, outsetPx) ?: return null
    return createBitmap(target.width, target.height, Bitmap.Config.ALPHA_8).apply {
        copyPixelsFromBuffer(ByteBuffer.wrap(alpha))
    }
}

/** 색까지 태운 띠. 겹이 여럿이거나 알파 판이 안 통하는 자리가 쓴다 */
fun ToppingOutline.toBorderArgbBitmap(
    target: ToppingBorderTarget,
    bands: List<ToppingBorderBand>,
): Bitmap? {
    val pixels = buildBorderPixels(target, bands) ?: return null
    return createBitmap(target.width, target.height)
        .apply { setPixels(pixels, 0, target.width, 0, 0, target.width, target.height) }
}
```

- [x] **Step 2: 컴파일을 확인한다**

```bash
./gradlew :core:util:android:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

---

### Task 3: 편집 화면을 새 코어로 옮기고 옛 거리장을 지운다

이 Task가 코드 중복을 남기지 않고 끝낸다. 편집 화면은 이 Task 동안 계속 정상 동작해야 한다.

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/.../editor/ToppingBorderOutline.kt`
- Modify: `feature/segmentation/impl/src/main/java/.../screen/ToppingBorderEditScreen.kt`
- Modify: `feature/segmentation/impl/src/main/java/.../viewmodel/ToppingEditViewModel.kt`

**Interfaces:**
- Consumes: Task 1의 `ToppingBorderBand`·`ToppingBorderTarget`·`ToppingOutline`,
  Task 2의 `Bitmap.toToppingOutline`·`toBorderArgbBitmap`
- Produces: `internal fun List<ToppingBorderLayer>.toBorderBands(pxPerDp: Float): List<ToppingBorderBand>`,
  `internal const val MAX_BORDER_WIDTH_DP`

- [x] **Step 1: `ToppingBorderOutline.kt`를 `toBorderBands`만 남기고 비운다**

파일 전체를 아래로 치환한다. `ToppingOutlineDistanceField`·`toOutlineDistanceField`·
`ToppingBorderBand`·상수 넷이 사라진다 — 전부 `core:util:{jvm,android}`로 갔다.

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.editor

import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderBand
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer

/**
 * 겹을 구간으로 펴 놓는다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로 바깥 끝은 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 *
 * 굵기는 dp 로 들고 있으므로 [pxPerDp] 를 곱해 그리는 쪽 좌표계로 환산한다.
 */
internal fun List<ToppingBorderLayer>.toBorderBands(pxPerDp: Float): List<ToppingBorderBand> {
    var outsetDp = 0f
    return map { layer ->
        outsetDp += layer.widthDp
        ToppingBorderBand(outsetPx = outsetDp * pxPerDp, colorArgb = layer.colorArgb)
    }
}
```

- [x] **Step 2: `ToppingEditViewModel.kt`의 굵기 상한을 모듈 안에서 읽을 수 있게 연다**

`private const val MAX_BORDER_WIDTH_DP = 50f` 를 아래로 치환한다. 값은 바뀌지 않는다.

```kotlin
/** 편집 미리보기 여백이 이 값을 따라간다 — 상한이 올라가면 여백도 함께 올라가야 한다 */
internal const val MAX_BORDER_WIDTH_DP = 50f
```

- [x] **Step 3: `ToppingBorderEditScreen.kt`가 새 코어를 쓰고 여백을 상한에서 파생시킨다**

네 자리를 고친다.

첫째, 파일 상단의 상수 둘. `MAX_BORDER_WIDTH_DP`가 `const`이므로 여백도 `const`로 둘 수 있다.

```kotlin
/** 사방에 남겨 두는 여백. 가장 굵은 테두리도 다 받아낸다 */
private const val MAX_BORDER_PADDING_DP = MAX_BORDER_WIDTH_DP

/** 미리보기 거리판의 긴 변 상한. 원본 해상도로 재면 사진 크기에 비례해 무거워진다 */
private const val PREVIEW_FIELD_LONG_SIDE = 1440
```

둘째, import를 갈아 끼운다. **`ToppingOutlineDistanceField`와 `toOutlineDistanceField` import를
둘 다 지우고** `buildCutoutBitmap`은 남긴다.

```kotlin
import com.teamyg.parfait.core.util.android.outline.toBorderArgbBitmap
import com.teamyg.parfait.core.util.android.outline.toToppingOutline
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.MAX_BORDER_WIDTH_DP
```

셋째, `ToppingBorderStamp`의 필드 타입을 바꾼다.

```kotlin
private data class ToppingBorderStamp(
    val image: ImageBitmap,
    val outline: ToppingOutline,
    val offset: IntOffset,
)
```

`stamp`를 만드는 `produceState` 안에서 마지막 생성부를 이렇게 바꾼다. 알맹이가 이미 여백 안에
앉은 `padded`에서 거리를 재므로 **판과 목표가 일대일**이다.

```kotlin
ToppingBorderStamp(
    image = padded.asImageBitmap(),
    outline = padded.toToppingOutline(fieldLongSide = PREVIEW_FIELD_LONG_SIDE),
    offset = IntOffset(layout.offsetX, layout.offsetY),
)
```

넷째, `borderImage`를 만드는 `produceState` 안을 이렇게 바꾼다.

```kotlin
current.outline
    .toBorderArgbBitmap(
        target = ToppingBorderTarget(
            width = current.image.width,
            height = current.image.height,
            // 거리판이 여백까지 포함한 판에서 나왔으므로 알맹이가 목표 전체다
            subjectLeft = 0,
            subjectTop = 0,
            subjectWidth = current.image.width,
            subjectHeight = current.image.height,
        ),
        bands = borderLayers.toBorderBands(density),
    )?.asImageBitmap()
```

⚠️ `padded`의 긴 변이 `PREVIEW_FIELD_LONG_SIDE`를 넘으면 `toToppingOutline`이 판을 줄인다. 그래도
위 대응은 옳다 — `subject*`는 **목표 좌표계**의 값이고 판이 줄어든 것은 `outline.width`가 흡수한다.

- [x] **Step 4: 기존 유닛과 컴파일을 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest
```

Expected: 기존 테스트 전부 PASS.

- [x] **Step 5: 사람이 확인한다 — 편집 화면이 그대로다**

앱을 띄워 사진 → 누끼 → 테두리 탭에서 색을 고르고 슬라이더를 끝까지 민다.
**이 Task는 겉보기 동작을 바꾸지 않는다.** 이전과 같은 모양이 나와야 하고, 가장 굵은 테두리가
미리보기 가장자리에서 깎이지 않아야 한다.

---

### Task 4: `YGToppingCutoutImage`를 거리판 렌더로 바꾼다 (ALPHA_8 게이트)

**Files:**
- Modify: `core/designsystem/src/main/kotlin/.../component/ygtoppingcutout/YGToppingCutoutImage.kt`

**Interfaces:**
- Consumes: Task 1의 `ToppingOutline`·`ToppingBorderTarget`, Task 2의 `toBorderAlphaBitmap`
- Produces: `@Composable fun YGToppingCutoutImage(painter, borderColor, borderWidth, modifier, outline: ToppingOutline? = null)`
- 남긴다: `TOPPING_OUTLINE_STAMP_COUNT` — `ToppingHitTarget`이 아직 읽는다. Task 6이 지운다.
- 지운다: `FULL_TURN_DEGREES` — 이 파일에서 `private`이라 밖에서 안 쓴다.

**초판에서 고친 것 셋**

1. **`outline`에 기본값 `null`을 준다.** 초판은 호출부 넷을 이 Task에서 건드려 임시 비계를 만들었다.
   기본값이면 이 Task가 `core:designsystem` 안에서 닫혀 리뷰어가 단독으로 판정할 수 있다.
2. **표시 크기를 양자화하지 않는다.** 초판은 32px 격자로 올림한 값을 기하 계산에 넣어, 띠가 실제
   알맹이보다 최대 31px 크게, 중심이 최대 15.5px 어긋나게 그려졌다.
3. **띠 판이 상자 밖으로 나간다는 사실을 계약으로 적는다.** 초판은 이것을 근거로 배경 편집의
   인셋 우회를 걷으라고 했는데 정반대다.

- [x] **Step 1: 컴포넌트를 다시 쓴다**

`YGToppingCutoutImage.kt`를 아래로 치환한다.

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.outline.toBorderAlphaBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 누끼 외곽선을 찍는 방향 수. 터치 판정이 같은 방향으로 되민 점을 읽으므로 이 값이 정본이다.
 *
 * ⚠️ 그리는 쪽은 더 이상 이 값을 쓰지 않는다. 판정이 거리판으로 옮겨 가면 함께 사라진다.
 */
const val TOPPING_OUTLINE_STAMP_COUNT = 8

/**
 * 크기가 연달아 바뀌는 동안에는 띠를 만들지 않고 멎기를 기다린다.
 *
 * 다음 크기 변화가 이 대기를 취소하므로 핀치 한 번에 띠를 한 벌만 만든다. 그래도 되는 이유는
 * 핀치로 크기가 변하는 화면에서 움직이는 토핑이 하나이기 때문이다.
 */
private const val BORDER_REBUILD_DELAY_MS = 48L

/**
 * 누끼 이미지와 그 실루엣을 따르는 테두리를 함께 그린다. 사각 테두리를 두르면 잘라 낸 배경이 다시
 * 드러나므로, 실루엣에서 잰 거리로 띠를 만들어 알맹이 아래에 깔고 그 위에 원본을 얹는다.
 *
 * 테두리를 그리는 화면이 여럿이라 여기서 한 벌만 둔다(`adr/0030-topping-outline-distance-field.md`).
 *
 * ⚠️ **띠는 이 컴포저블의 상자 밖으로 [borderWidth] 만큼 나간다.** 부르는 쪽이 클리핑 레이어나
 * `alpha < 1` 을 씌우면 그만큼 잘리므로, 그런 자리는 상자를 굵기만큼 키우고 안쪽으로 덜어내야 한다.
 *
 * @param outline 준비되기 전에는 `null` 이다 — 그동안은 테두리 없이 알맹이만 그린다
 * @param borderWidth 화면 기준 dp 다 — 토핑을 키워도 굵기는 그대로다
 */
@Composable
fun YGToppingCutoutImage(
    painter: Painter,
    borderColor: Color?,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
    outline: ToppingOutline? = null,
) {
    Box(modifier = modifier) {
        if (outline != null && borderColor != null && borderWidth > 0.dp) {
            ToppingBorder(outline = outline, color = borderColor, width = borderWidth)
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BoxScope.ToppingBorder(
    outline: ToppingOutline,
    color: Color,
    width: Dp,
) {
    val outsetPx = with(LocalDensity.current) { width.toPx() }
    val padding = ceil(outsetPx).toInt() + 1

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val plate: ToppingBorderPlate? by produceState<ToppingBorderPlate?>(
        initialValue = null,
        outline,
        boxSize,
        outsetPx,
    ) {
        val box = boxSize
        if (box.width <= 0 || box.height <= 0) return@produceState

        delay(BORDER_REBUILD_DELAY_MS)

        value = withContext(Dispatchers.Default) {
            // 알맹이는 Fit 으로 앉으므로 상자가 아니라 실루엣 비율로 그려질 자리를 구한다
            val subject = fitSize(outline.width.toFloat() / outline.height, box)
            val target = ToppingBorderTarget(
                width = subject.width + padding * 2,
                height = subject.height + padding * 2,
                subjectLeft = padding,
                subjectTop = padding,
                subjectWidth = subject.width,
                subjectHeight = subject.height,
            )

            outline.toBorderAlphaBitmap(target, outsetPx)?.asImageBitmap()?.let { image ->
                ToppingBorderPlate(
                    image = image,
                    offset = IntOffset(
                        x = (box.width - subject.width) / 2 - padding,
                        y = (box.height - subject.height) / 2 - padding,
                    ),
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .onSizeChanged { size -> boxSize = size },
    ) {
        val current = plate ?: return@Canvas
        drawImage(
            image = current.image,
            dstOffset = current.offset,
            dstSize = IntSize(current.image.width, current.image.height),
            colorFilter = ColorFilter.tint(color),
        )
    }
}

/** 알맹이와 여백을 함께 담은 띠 한 장과 그것을 놓을 자리 */
private data class ToppingBorderPlate(
    val image: ImageBitmap,
    val offset: IntOffset,
)

private fun fitSize(
    aspectRatio: Float,
    box: IntSize,
): IntSize = if (box.width / aspectRatio <= box.height) {
    IntSize(box.width, (box.width / aspectRatio).roundToInt())
} else {
    IntSize((box.height * aspectRatio).roundToInt(), box.height)
}

@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
    )
}
```

⚠️ `produceState`는 키가 바뀌어도 옛 값을 지우지 않는다. 크기를 바꾸는 동안에는 **직전 크기 기준의
띠가 잠깐 그대로 그려진다.** 지우는 편으로 바꾸면 그동안 테두리가 사라져 깜빡이므로 이쪽이 낫다.

- [x] **Step 2: 컴파일과 기존 유닛을 확인한다**

호출부 넷은 `outline`에 기본값이 있어 **한 글자도 고치지 않는다.** 이 시점에 네 화면은 테두리
없이 알맹이만 그린다.

```bash
./gradlew :core:designsystem:compileDebugKotlin \
          :feature:segmentation:impl:testDebugUnitTest \
          :feature:groups:canvas:impl:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, 기존 테스트 전부 PASS.

- [x] **Step 3: ktlint를 돌린다**

```bash
./gradlew :core:designsystem:ktlintCheck
```

Expected: BUILD SUCCESSFUL. 미사용 import가 남아 있으면 여기서 잡힌다.

- [x] **Step 4: 게이트 — `ALPHA_8` + tint 가 실기기에서 먹는지 사람이 확인한다**

프리뷰를 임시로 아래처럼 바꿔 띄운다. **확인 뒤 되돌린다.**

```kotlin
@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    val outline = remember {
        ToppingOutline.of(width = 64, height = 64) { x, y ->
            if (x in 24..39 && y in 8..55) 255 else 0
        }
    }
    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
        outline = outline,
    )
}
```

**Android Studio 프리뷰가 아니라 실기기 또는 에뮬레이터에서 봐야 한다.** 프리뷰 렌더러는
하드웨어 가속 캔버스가 아니다. 판정 항목이 둘이다.

1. `copyPixelsFromBuffer`가 예외 없이 통과하는가. `ALPHA_8` 비트맵의 `rowBytes`가 `width`와
   다르게 정렬되는 기기면 `RuntimeException("Buffer not large enough for pixels")`가 난다.
2. 세로 막대 둘레에 `Cherry200` 색 띠가 보이는가. 검게 나오면 tint가 안 먹은 것이다.

둘 다 통과하면 프리뷰를 원래대로 되돌리고 Task 5로 간다. 하나라도 실패하면 **폴백으로 간다.**

폴백은 `toBorderAlphaBitmap` 대신 `toBorderArgbBitmap`을 쓰고, `produceState` 키에 `color`를
더하고, `drawImage`의 `colorFilter` 인자를 지운다. import 셋을 함께 더한다.

```kotlin
import androidx.compose.ui.graphics.toArgb
import com.teamyg.parfait.core.util.android.outline.toBorderArgbBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderBand
```

```kotlin
outline.toBorderArgbBitmap(
    target = target,
    bands = listOf(ToppingBorderBand(outsetPx = outsetPx, colorArgb = color.toArgb())),
)?.asImageBitmap()
```

폴백을 택했다면 이 문서의 이 단계에 판정 결과를 적는다.

---

### Task 5: 거리판 캐시를 `core:ui`에 세운다 (추가만)

**Files:**
- Create: `core/ui/src/main/java/com/teamyg/parfait/core/ui/outline/ToppingOutlineCache.kt`

⚠️ **`ToppingAlphaMaskCache.kt`를 지우지 않는다.** Task 6이 소비자를 옮긴 뒤에 지운다. 잠깐의
코드 중복이 컴파일이 깨진 Task 경계보다 싸다.

**Interfaces:**
- Consumes: Task 1의 `ToppingOutline`, Task 2의 `Bitmap.toToppingOutline`
- Produces:
  - `suspend fun loadToppingOutline(context: Context, model: String, retryKey: Int): ToppingOutline?`
  - `@Composable fun rememberToppingOutlines(models: List<String>, retryKey: Int): Map<String, ToppingOutline>`
  - `fun clearToppingOutlines()`

⚠️ **이 Task에도 자동 테스트가 없다.** `Context`·Coil·`Bitmap`이 필요하다.

- [x] **Step 1: 새 파일을 만든다**

`ToppingAlphaMaskCache.kt`의 구조를 그대로 옮기되 셋이 다르다 — 캐시 값이 `ToppingOutline`이고,
키가 `"$retryKey|$model"`이고, 디코딩 뒤 비트셋 대신 거리판을 만든다.

```kotlin
package com.teamyg.parfait.core.ui.outline

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.teamyg.parfait.core.util.android.outline.toToppingOutline
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** 거리판 한 장의 긴 변. 올리면 테두리가 실루엣에 가까워지고 디코딩·메모리가 는다 */
private const val OUTLINE_LONG_SIDE = 256

/** 캔버스 하나에 올라가는 토핑 수를 넉넉히 덮는 상한 */
private const val OUTLINE_CACHE_ENTRIES = 64

private const val LOAD_FACTOR = 0.75f

/**
 * 접근 순서 갱신이 곧 쓰기라, 여럿이 잠금 없이 건드리면 상태가 깨진다. 로딩을 어느 컨텍스트에서
 * 부르는지 이 파일이 정하지 않으므로 모든 접근을 [outlineCache] 자신에 대해 동기화한다.
 */
private val outlineCache = object : LinkedHashMap<String, ToppingOutline>(
    OUTLINE_CACHE_ENTRIES,
    LOAD_FACTOR,
    true,
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ToppingOutline>): Boolean =
        size > OUTLINE_CACHE_ENTRIES
}

/**
 * 같은 모델을 아직 뜨는 중이면 그 로드에 합류시킨다. 캐시 조회부터 쓰기까지가 잠금 밖이라,
 * 두 화면이 같은 토핑을 거의 동시에 요청하면 둘 다 캐시 미스로 갈라져 같은 이미지를 각자 디코딩한다.
 */
private val inFlightOutlines = mutableMapOf<String, Deferred<ToppingOutline?>>()

/**
 * 로드를 시작한 컴포지션이 먼저 사라져도 합류한 쪽이 같이 죽으면 안 되므로, 실제 로드는 호출자
 * 스코프가 아니라 여기서 돈다.
 */
private val outlineLoadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * [model] 은 그 화면이 **실제로 그리는** 대상이어야 한다. 편집본을 그리는 화면에 원본 주소를
 * 넘기면 투명 여백이 잘려 비율이 달라 실루엣이 통째로 어긋난다.
 *
 * @param retryKey 올리면 이 모델의 캐시를 건너뛰고 다시 받는다 — 안 그러면 재시도로 그림이 돌아와도
 *   테두리와 판정이 옛 실패에 묶인다
 */
suspend fun loadToppingOutline(
    context: Context,
    model: String,
    retryKey: Int,
): ToppingOutline? {
    val key = "$retryKey|$model"
    synchronized(outlineCache) { outlineCache[key] }?.let { return it }

    // 로드가 호출자보다 오래 살 수 있어 화면 컨텍스트를 넘기면 그동안 붙잡힌다
    val appContext = context.applicationContext

    return synchronized(inFlightOutlines) {
        inFlightOutlines.getOrPut(key) {
            outlineLoadScope.async { decodeToppingOutline(appContext, model, key) }.also { started ->
                started.invokeOnCompletion {
                    synchronized(inFlightOutlines) {
                        if (inFlightOutlines[key] === started) inFlightOutlines.remove(key)
                    }
                }
            }
        }
    }.await()
}

private suspend fun decodeToppingOutline(
    context: Context,
    model: String,
    key: String,
): ToppingOutline? {
    val request = ImageRequest
        .Builder(context)
        .data(model)
        .size(OUTLINE_LONG_SIDE)
        .allowHardware(false)
        // 거리판으로 접고 나면 버릴 비트맵이다. 표시용과 크기가 달라 키도 다르니, 얹어 두면
        // 토핑 수만큼 쓸모없는 항목이 표시용 비트맵을 밀어낸다
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()

    val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null

    // execute 는 자기 디스패처에서 돌지만 그 뒤는 부르는 쪽 컨텍스트다. 전 픽셀 순회를
    // 그대로 두면 호출부가 메인 스레드일 때 토핑 수만큼 메인이 잡힌다
    val outline = withContext(Dispatchers.Default) {
        image.toBitmap().toToppingOutline(fieldLongSide = OUTLINE_LONG_SIDE)
    }

    synchronized(outlineCache) { outlineCache.put(key, outline) }
    return outline
}

/**
 * 메모리 압박이나 테스트에서 캐시를 비우는 수단. 아직 부르는 곳을 두지 않았다 — 항목 수에
 * 상한이 있어 누수가 아니라서 호출부 신설을 미뤘다.
 */
fun clearToppingOutlines() {
    synchronized(outlineCache) { outlineCache.clear() }
}

/** [models] 가 비면 아무것도 로드하지 않는다 */
@Composable
fun rememberToppingOutlines(
    models: List<String>,
    retryKey: Int,
): Map<String, ToppingOutline> {
    val context = LocalContext.current
    val loaded = remember { mutableStateMapOf<String, ToppingOutline>() }

    LaunchedEffect(models, retryKey) {
        models
            .distinct()
            .forEach { model ->
                loadToppingOutline(context, model, retryKey)?.let { loaded[model] = it }
            }
    }

    return loaded
}
```

⚠️ 기존 `rememberToppingAlphaMasks`는 `filterNot { loaded.containsKey(it) }`로 이미 뜬 것을
건너뛰었다. `retryKey`가 바뀌면 같은 모델을 **다시** 받아야 하므로 그 필터를 걷었다. 캐시가 그
자리를 대신한다 — `retryKey`가 그대로면 첫 조회에서 곧바로 맞는다.

- [x] **Step 2: 컴파일과 유닛을 확인한다**

옛 캐시가 그대로 있으므로 **어느 모듈도 깨지지 않는다.**

```bash
./gradlew :core:ui:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, 기존 테스트 전부 PASS.

---

### Task 6: 캔버스 모듈을 거리판으로 옮긴다

canvas impl의 변경을 한 Task에 모은다. **이 Task 안에서만 모듈이 잠깐 깨지고, 끝에서 초록으로
돌아온다.** 초판은 이 일을 Task 셋으로 쪼개 그 사이에 유닛 테스트를 돌리라고 했는데, 모듈 main이
안 컴파일되면 `--tests` 필터를 줘도 유닛이 돌지 않으므로 성립하지 않았다.

**Files:**
- Modify: `feature/groups/canvas/impl/.../util/ToppingHitTarget.kt`
- Modify: `feature/groups/canvas/impl/src/test/.../util/ToppingHitTestTest.kt`
- Modify: `feature/groups/canvas/impl/.../component/CanvasToppingLayer.kt`
- Modify: `feature/groups/canvas/impl/.../screen/CanvasBGEditScreen.kt`
- Modify: `core/designsystem/.../component/ygtoppingcutout/YGToppingCutoutImage.kt` (상수 삭제)
- Delete: `feature/groups/canvas/impl/.../util/ToppingAlphaMask.kt`
- Delete: `feature/groups/canvas/impl/.../util/ToppingAlphaMaskCache.kt`
- Delete: `feature/groups/canvas/impl/src/test/.../util/ToppingAlphaMaskTest.kt`

**Interfaces:**
- Consumes: Task 1의 `ToppingOutline`, Task 5의 `rememberToppingOutlines`, Task 4의 `outline` 파라미터
- Produces: `data class ToppingHitTarget(..., val outline: ToppingOutline?)` — `mask` 가 `outline` 이 된다

- [x] **Step 1: 판정 테스트를 먼저 고친다**

`ToppingHitTestTest.kt`에서 `ToppingAlphaMask`를 만드는 헬퍼(`leftHalfMask` 등)와
`targetCenteredAt(... mask = ...)` 헬퍼의 인자 이름을 `ToppingOutline.of` / `outline =`으로 바꾼다.

**판 해상도를 40×40으로 키운다.** 4×4 판으로는 8px 테두리를 해상할 수 없어
`containsPoint_withBorder_extendsBeyondSilhouette`가 반 칸 편향 때문에 실패한다.

그리고 **두 회귀 테스트의 기대값을 뒤집고 그 이유를 주석에 남긴다.**

```kotlin
    @Test
    fun containsPoint_leftOfImageRect_isHitWhenTheBorderReachesThere() {
        // 옛 판정은 그림 사각형 밖을 무조건 투명으로 답해 여기서 미스였다. 새 판정은 판 밖 거리를
        // 재므로, 실루엣이 그림 왼쪽 변에 닿아 있으면 테두리를 그린 자리까지 눌린다.
        // 그리는 모양과 판정을 일치시키는 것이 이 라운드의 목적이라 이쪽이 맞다
        ...
        assertTrue(target.containsPoint(...))
    }
```

`containsPoint_aboveImageRect_…`도 같은 방식으로 뒤집는다. 이름도 사실에 맞게 바꾼다.

이어서 신규 3건을 더한다.

```kotlin
    @Test
    fun containsPoint_borderWidth_extendsTheHitAreaByThatDistance() {
        // Given 가운데 한 칸만 불투명한 40x40 실루엣을 40x40 픽셀로 그린다
        val outline = ToppingOutline.of(width = 40, height = 40) { x, y ->
            if (x == 20 && y == 20) 255 else 0
        }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 5f,
            outline = outline,
        )

        // Then 실루엣에서 5 떨어진 자리는 눌리고 7 떨어진 자리는 안 눌린다
        assertTrue(target.containsPoint(15.5f, 20.5f))
        assertFalse(target.containsPoint(13.5f, 20.5f))
    }

    @Test
    fun containsPoint_withoutBorder_onlyTheSilhouetteIsHit() {
        val outline = ToppingOutline.of(width = 40, height = 40) { x, y ->
            if (x == 20 && y == 20) 255 else 0
        }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 0f,
            outline = outline,
        )

        // Then 안 그린 테두리만큼 판정이 넓어지면 안 된다
        assertTrue(target.containsPoint(20.5f, 20.5f))
        assertFalse(target.containsPoint(17.5f, 20.5f))
    }

    @Test
    fun containsPoint_fallsBackToTheRectangleWhenNothingIsOpaque() {
        val outline = ToppingOutline.of(width = 40, height = 40) { _, _ -> 0 }
        val target = ToppingHitTarget(
            centerXPx = 20f,
            centerYPx = 20f,
            imageWidthPx = 40f,
            imageHeightPx = 40f,
            rotationDegrees = 0f,
            borderWidthPx = 0f,
            outline = outline,
        )

        // Then 실루엣을 못 읽으면 사각형으로 받는다 — 아무 데도 안 눌리는 것보다 낫다
        assertTrue(target.containsPoint(2f, 2f))
    }
```

- [x] **Step 2: `ToppingAlphaMaskTest.kt`를 지운다**

```bash
rm feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskTest.kt
```

이 파일의 `@Test` 여덟 건은 전부 `ToppingAlphaMask.of`·`ALPHA_THRESHOLD`를 쓴다. 알파 문턱과
범위 밖 좌표 규칙은 Task 1의 `ToppingOutlineTest`가 이미 덮는다.

- [x] **Step 3: `ToppingHitTarget`을 고친다**

`mask: ToppingAlphaMask?`를 `outline: ToppingOutline?`으로 바꾸고, `containsPoint`의 뒷부분과
`isOpaqueAtLocal`을 아래로 치환한다. 앞부분(회전 되돌리기·사각형 검사)은 그대로 둔다.

```kotlin
        // 실루엣을 못 읽으면 사각형 판정이다 — 여기까지 왔으면 사각형 안이다
        val usableOutline = outline?.takeIf { it.hasAnySeed } ?: return true

        // 테두리는 실루엣에서 굵기만큼 떨어진 자리까지라, 판정도 같은 거리로 답한다
        val fieldX = (localX + imageWidthPx / 2f) * usableOutline.width / imageWidthPx - 0.5f
        val fieldY = (localY + imageHeightPx / 2f) * usableOutline.height / imageHeightPx - 0.5f

        if (borderWidthPx <= 0f) return usableOutline.isOpaqueAt(fieldX, fieldY)

        val fieldPerImagePx = usableOutline.width / imageWidthPx
        return usableOutline.distanceAt(fieldX, fieldY) <= borderWidthPx * fieldPerImagePx
```

지울 것: `TOPPING_OUTLINE_STAMP_COUNT` import, `floor` import, `isOpaqueAtLocal` 함수,
`companion object`와 그 안의 `FULL_TURN_DEGREES`. **companion object 자체를 지운다** — 안이
비면 ktlint `no-empty-class-body`가 잡는다.

더할 것: `import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline`.

남길 것: `cos`·`sin` — 회전 되돌리기가 계속 쓴다.

- [x] **Step 4: `CanvasToppingLayer`를 결선한다**

세 곳이다.

첫째, `ToppingHitEntry`에 거리판을 싣는다.

```kotlin
internal data class ToppingHitEntry(
    val topping: CanvasToppingVO,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val outline: ToppingOutline?,
    val target: ToppingHitTarget,
    val imageState: CanvasLoadState,
)
```

둘째, `rememberToppingHitEntries`에서 마스크를 거리판으로 갈고 `loadMasks` 파라미터를 없앤다.

```kotlin
    val outlines = rememberToppingOutlines(
        models = toppings.map { it.imageUrl },
        retryKey = retryKey,
    )
```

`ToppingHitEntry`에 `outline = outlines[topping.imageUrl]`을 넣고,
`ToppingHitTarget(... mask = masks[topping.imageUrl])`을 `outline = outlines[topping.imageUrl]`로
바꾼다. 호출부에서 `loadMasks = ...` 인자를 지운다.

셋째, `ToppingImage`가 거리판을 받아 넘긴다.

```kotlin
@Composable
private fun ToppingImage(
    painter: AsyncImagePainter,
    outline: ToppingOutline?,
    border: ToppingBorder,
) {
    val painterState by painter.state.collectAsState()
    val solidBorder = border as? ToppingBorder.Solid

    YGToppingCutoutImage(
        painter = painter,
        // 색을 못 읽으면 테두리를 걸러 낸다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀리다
        borderColor = solidBorder
            ?.color
            ?.toColorOrNull()
            ?.takeIf { painterState is AsyncImagePainter.State.Success },
        borderWidth = (solidBorder?.width?.toFloat() ?: 0f).dp,
        modifier = Modifier.fillMaxSize(),
        outline = outline,
    )
}
```

`CanvasTopping` 안의 호출을
`ToppingImage(painter = entry.painter, outline = entry.outline, border = entry.topping.border)`로 바꾼다.

⚠️ **낡는 KDoc 셋을 함께 고친다**(`parfait/CLAUDE.md` 규약).

- `CanvasToppingLayer`의 `hitTestEnabled` 설명 — "끄면 마스크 로딩도 안 단다"가 거짓이 된다.
  거리판은 그리기에 필요해 판정과 무관하게 뜬다.
- `rememberToppingHitEntries`의 `@param loadMasks` — 파라미터 자체가 사라진다.
- 같은 함수의 `@param retryKey` — "알파 마스크는 여기 딸려 오지 않는다"가 거짓이 된다.
  이제 캐시 키에 들어간다.

- [x] **Step 5: `CanvasBGEditScreen`을 결선한다**

거리판을 `drawEntries`와 `hitEntries` 둘이 함께 봐야 하므로, **두 `remember` 함수의 바깥**
(`drawEntries`를 만드는 자리 바로 아래)에 한 번만 둔다.

```kotlin
    val outlines = rememberToppingOutlines(
        models = drawEntries.map { it.topping.drawnModel },
        retryKey = 0,
    )
```

`rememberBGEditHitEntries(drawEntries)`를 `rememberBGEditHitEntries(drawEntries, outlines)`로 바꾸고,
그 안에서 `mask = masks[entry.topping.drawnModel]`을 `outline = outlines[entry.topping.drawnModel]`로
바꾼다. 함수 안의 `rememberToppingAlphaMasks` 호출은 지운다.

`CanvasToppingImage`에 `outline: ToppingOutline?` 파라미터를 더하고 호출부에서 넘긴다.
`YGToppingCutoutImage(...)`에 `outline = outline`을 더한다.

⚠️ **인셋 우회(`outlineInset` 만큼 키운 `requiredSize` + `.padding(outlineInset)`)를 걷지 않고
그대로 둔다.** 띠 판은 정의상 상자 밖으로 나가므로 `alpha < 1`이 만드는 오프스크린 버퍼가 여전히
자른다. 그 사실을 적은 KDoc 문단도 남기되 "여덟 방향으로 밀어 찍는다"는 서술만 "거리판으로 만든
띠가 상자 밖으로 나간다"로 고친다.

⚠️ **`isMine` 필터가 사라진다.** 지금은 내 토핑만 마스크를 뜨는데, 그리기에 거리판이 필요해
남의 토핑까지 디코딩한다. `rememberBGEditDrawEntries`의 KDoc이 "알파 마스크를 요청하지 않는다"고
적고 있으니 함께 고친다.

- [x] **Step 6: 옛 마스크와 스탬프 상수를 지운다**

```bash
rm feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMask.kt
rm feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskCache.kt
```

`YGToppingCutoutImage.kt`에서 `TOPPING_OUTLINE_STAMP_COUNT` 선언과 그 KDoc을 지운다.

- [x] **Step 7: 모듈 전체가 초록인지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest :core:designsystem:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL, 신규 3건 + 갱신된 기존 테스트 전부 PASS.

- [x] **Step 8: ktlint를 돌린다**

```bash
./gradlew :feature:groups:canvas:impl:ktlintCheck :core:designsystem:ktlintCheck
```

Expected: BUILD SUCCESSFUL.

---

### Task 7: 토핑 하나짜리 화면 둘을 결선한다

**Files:**
- Modify: `feature/groups/canvas/impl/.../screen/CanvasToppingPlaceScreen.kt`
- Modify: `feature/segmentation/impl/.../screen/SegmentationConfirmScreen.kt`

**Interfaces:**
- Consumes: Task 5의 `loadToppingOutline`, Task 4의 `outline` 파라미터
- Produces: 없음(결선만)

- [x] **Step 1: `CanvasToppingPlaceScreen`을 고친다**

이 화면은 `toppingImagePath`를 `File(path).toUri().toString()`으로 바꿔 painter에 넘긴다. 그
표현식이 이미 `remember(toppingImagePath)`로 묶여 있으므로 지역 변수로 빼서 **그림과 거리판이
같은 문자열**을 보게 한다.

```kotlin
    val toppingImageModel = remember(toppingImagePath) {
        toppingImagePath?.let { path -> File(path).toUri().toString() }
    }
    val painter = rememberAsyncImagePainter(
        model = toppingImageModel,
        // 나머지 인자는 그대로
    )

    val context = LocalContext.current
    val outline by produceState<ToppingOutline?>(initialValue = null, toppingImageModel) {
        val model = toppingImageModel ?: return@produceState
        value = loadToppingOutline(context, model, retryKey = 0)
    }
```

`YGToppingCutoutImage(...)` 호출에 `outline = outline`을 더한다.

⚠️ **그림과 거리판이 다른 문자열을 보면 안 된다.** 편집본은 투명 여백이 잘려 원본과 비율이 달라
실루엣이 통째로 어긋난다.

더할 import: `androidx.compose.runtime.produceState`, `androidx.compose.runtime.getValue`,
`androidx.compose.ui.platform.LocalContext`, `com.teamyg.parfait.core.ui.outline.loadToppingOutline`,
`com.teamyg.parfait.core.util.jvm.outline.ToppingOutline`.

- [x] **Step 2: `SegmentationConfirmScreen`을 고친다**

```kotlin
    val context = LocalContext.current
    val outline by produceState<ToppingOutline?>(initialValue = null, subjectImagePath) {
        value = loadToppingOutline(context, subjectImagePath, retryKey = 0)
    }
```

`YGToppingCutoutImage(...)`에 `outline = outline`을 더한다. import는 Step 1과 같은 다섯이다
(`File`은 필요 없다).

- [x] **Step 3: 전체 컴파일과 유닛, ktlint를 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest \
          :feature:segmentation:impl:testDebugUnitTest \
          :core:util:jvm:test
./gradlew ktlintCheck
```

Expected: 전부 PASS.

- [x] **Step 4: 앱 전체가 빌드되는지 본다**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

---

### Task 8: 실기기 육안 확인과 문서 갱신

- [x] **Step 1: 실기기에서 여섯 가지를 확인한다**

1. 같은 토핑·같은 굵기가 **테두리 편집 → 누끼 확인 → 토핑 배치 → 캔버스** 넷에서 같은 모양인가.
   ⚠️ **굵기를 최소(2dp)로 놓고도 본다.** 편집 화면은 거리판을 화면 크기로 재고 나머지 셋은 긴 변
   256으로 재므로, 굵기가 얇을수록 256 격자가 실루엣 잔주름을 뭉갠다.
2. 빨대처럼 가는 부위가 갈라지지 않는가.
3. 누끼 확인 화면에서 테두리가 화면 가장자리에 잘리지 않는가. 배경 편집에서 딤이 걸린 토핑의
   테두리도 잘리지 않는가.
4. 토핑이 여럿인 캔버스에서 진입·스크롤이 버벅이지 않는가.
5. 배치 화면에서 핀치로 크기를 바꿀 때, 손을 뗀 뒤 테두리가 새 크기로 붙기까지 눈에 띄게 느린가.
   바꾸는 동안 직전 크기의 띠가 남는 것은 의도한 동작이다.
6. 이미지 로드에 실패시킨 뒤(비행기 모드 등) 재시도했을 때 테두리와 판정이 함께 돌아오는가.

- [ ] **Step 2: 1번이 갈리면 거리판 해상도를 올린다** — 조건이 성립하지 않아 수행하지 않았다

`OUTLINE_LONG_SIDE`를 512로 올린다. 항목당 메모리가 네 배(약 512KB)가 되므로
`OUTLINE_CACHE_ENTRIES`를 32로 줄여 총량을 16MB로 묶는다. 올렸다면 그 사실과 근거를 스펙의
"열린 질문" 절에 적는다.

> **as-built(2026-09-08)** — 실기기에서 네 화면 모양이 같아 256을 그대로 뒀다. 다만 **굵기 최소
> 2dp 조건까지 대조했다는 기록이 없어** 조건이 완전히 배제되지는 않았다(OQ-P-379).

- [ ] **Step 3: 5번이 느리면 지연을 줄인다** — 지연 자체가 폐기되어 무효가 됐다

`BORDER_REBUILD_DELAY_MS`를 48에서 16으로 내리고 다시 본다. 정한 값과 이유를 스펙에 적는다.

> **as-built(2026-09-08)** — 이 Step이 겨냥한 `BORDER_REBUILD_DELAY_MS`가 머지본에 없다. 지연으로
> 재생성을 미루면 그동안 옛 판을 늘려 그려 굵기가 배율만큼 틀어져 보이다가 손을 떼는 순간 제
> 굵기로 스냅한다. 굵기 dp 고정이 눈에 보이는 성질이라 지연 갈래를 통째로 되돌리고, 대신
> `ToppingBorderPlateCache`(컴포지션 밖 전역 LRU)와 `snapshotFlow { boxSize }` + `conflate` 로
> 바꿨다(OQ-P-382 ②).

- [x] **Step 4: 문서를 갱신한다**

- `design-system.md`의 `YGToppingCutoutImage` 항목 — 여덟 방향 스탬프 서술을 거리판 렌더로 바꾸고
  `outline` 파라미터와 **"띠가 상자 밖으로 나간다"는 계약**을 적는다.
- `module-structure.md` — `core:util:jvm`·`core:util:android`·`core:ui`에 `outline/`을 더한다.
  `core:util:jvm` 항목의 픽셀 연산 문단이 "토핑 테두리를 거리장으로 그리려고" 승격했다고 적어
  두었으니 그 예고가 실현됐다는 사실을 잇는다.
- `open-questions.md` — OQ-P-208 ②, OQ-P-337 ①②, OQ-P-356을 해소로 표시한다.
  **OQ-P-208 ①(실기기 측정)과 ③(굵기 정책)은 열어 둔다.** OQ-P-317(캐시 수명 주체)에 항목당
  크기가 8KB에서 128KB로 커진 사실을 더한다. **판정이 그림 사각형 밖으로 넓어진 변화**와
  **편집 화면과 나머지 셋의 거리판 해상도 차이**를 새 항목으로 등록할지 판단한다.
- ADR-0030 `status`를 `accepted`로 올리고, Task 4 게이트의 판정 결과(`ALPHA_8` 채택 여부)를
  "위험·방어" 절에 적는다.
- 스펙 `status`를 `implemented`로 올리고 `parfait/specs/archive/`로 옮긴 뒤
  `parfait/specs/README.md`의 활성 표에서 아카이브 표로 행을 옮긴다.
- `parfait/plans/README.md`에 as-built를 덧붙인다.

---

## 검증 요약

| 무엇 | 어떻게 | Task |
|------|--------|------|
| 거리 r 안쪽이 하나도 안 빠진다(스탬프가 실패하던 성질) | `:core:util:jvm:test` | 1 |
| 띠 경계가 기대 거리에 온다 | `:core:util:jvm:test` | 1 |
| 실루엣이 여백까지 늘어나지 않는다 | `:core:util:jvm:test` | 1 |
| 판 밖 거리가 클램프되지 않는다 | `:core:util:jvm:test` | 1 |
| 거리 양자화 오차 상한 | `:core:util:jvm:test` | 1 |
| 겹별 색과 경계 섞임 | `:core:util:jvm:test` | 1 |
| 판정이 거리 기준으로 넓어진다 | `:feature:groups:canvas:impl:testDebugUnitTest` | 6 |
| `Bitmap` 변환 | **자동 검증 없음** — 컴파일 + Task 4 게이트 | 2 |
| `ALPHA_8` 버퍼 정렬 + tint | 실기기 게이트 2항목 | 4 |
| 캐시가 디코딩 한 번을 쓴다 | **자동 검증 없음** — 실기기 확인 | 5 |
| 네 화면 모양 일치(얇은 굵기 포함) | 실기기 육안 | 8 |
