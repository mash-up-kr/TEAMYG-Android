---
id: g001-group-list-topping-border
title: G-001 그룹 목록 최신 토핑 테두리 구현 계획 (5 Task)
status: done
type: work-order
created: 2026-09-11
updated: 2026-09-16
platforms: android
owner: android
related_adr: ADR-0025, ADR-0030
related_spec: g001-group-list-topping-border
related_code: YGToppingBorder, YGToppingImage.Remote, YGToppingGroup, YGToppingCutoutImage, ToppingBorderPlateCache, ToppingBorderPlate#fitsSubject, ToppingImage.kt#toToppingImage, ToppingImage.kt#borderedImageUrls, GroupListScreen.kt#GroupListContent, YGToppingGroupPreviewScreen
archived_reason: develop 머지(PR #497 `a1fc2377f`, 2026-09-16)
tags: [plan, parfait]
---

# G-001 그룹 목록 최신 토핑 테두리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** G-001 그룹 목록 카드의 최신 토핑에 서버가 준 테두리를 캔버스와 같은 모양으로 그린다.

**Architecture:** `YGToppingImage.Remote`에 `YGToppingBorder`(색·dp·거리판)를 싣고, `YGToppingGroup`의 `Remote` 분기가 크기를 명시한 Coil painter와 기존 렌더러 `YGToppingCutoutImage`로 그린다. 거리판은 `:core:ui`의 `rememberToppingOutlines`로 feature(`GroupListContent`)가 불러와 넘긴다. 목록과 캔버스가 같은 토핑의 띠 판을 서로 덮어쓰지 않도록 `ToppingBorderPlateCache`가 열쇠당 크기별 판 선반을 두고, `YGToppingCutoutImage`가 그리기 단계에서 크기에 맞는 판을 고른다.

**Tech Stack:** Kotlin, Jetpack Compose, Coil 3.5.0, kotlin.test(JVM 유닛), AndroidJUnit4 + JUnit4 `Assert`(계측 테스트), ktlint

**Spec:** [`parfait/specs/2026-09-11-g001-group-list-topping-border.md`](../../specs/archive/2026-09-11-g001-group-list-topping-border.md)

## Global Constraints

- 작업 대상 저장소는 **`TJYG-Android`**다. 이 계획 문서가 있는 저장소가 아니다.
- 브랜치는 이미 있는 **`feature/#494-group-list-topping-border`**(develop `1b21725ba` 기준)다. `main`·`develop`에 직접 커밋하지 않는다. **워크트리를 만들지 않는다** — 본 체크아웃에서 작업한다.
- **커밋하지 않는다.** 각 태스크의 커밋 단계는 사용자가 명시적으로 커밋을 요청했을 때만 실행한다. 기본값은 미커밋이다.
- **주석 규약**(`parfait/CLAUDE.md`): 코드가 이미 말하는 것은 쓰지 않는다. 뻔하지 않은 의도와 함정만 쓴다. **다른 컴포넌트의 현재 상태는 쓰지 않는다** — 낡기 때문이다. 써야 하면 단정 대신 근거 문서를 가리킨다. KDoc에 "의도/반환값/파라미터" 고정 틀을 두지 않는다. `@return`은 타입과 이름이 말하지 못할 때만, `@param`은 이름이 오해를 부를 때만 쓴다.
- 아키텍처 결정은 코드가 아니라 `parfait/adr/`·스펙에 있다. 코드에는 포인터 한 줄만 둔다.
- 새 테스트 하니스·빌드 설정을 만들지 않는다. JVM 유닛 테스트는 `kotlin.test`, 계측 테스트는 기존 `core/designsystem/src/androidTest` 관례(`@RunWith(AndroidJUnit4::class)`, `org.junit.Assert`)를 따른다.
- **바꾸지 않는 것**: `ToppingBorderPlate`·`fitsSubject`·`PLATE_REUSE_RATIO_LIMIT = 1.25f`, `PlateKey`의 필드(거리판·굵기·비율), `YGToppingCutoutImage`의 `buildBorderPlate`와 판을 만드는 `LaunchedEffect`, `YGToppingGroup`의 `Template`·`Error` 분기, 배치(`size(Size96)`·`offset`·`rotate`·`clip`) 순서.
- 상수(정본): `PLATES_PER_KEY = 3`(신설), 토핑 프레임 `SizeTokens.Size96`, 거리판 재시도 키 `retryKey = 0`.
- 두께를 feature에서 다시 가두지 않는다. 데이터 계층의 `ToppingBorder.solidClamped`가 이미 가뒀다.
- 기기·에뮬레이터가 필요한 단계에서 연결된 기기가 없으면(`adb devices`가 비어 있으면) 추측으로 넘어가지 말고 멈춰서 보고한다.

---

## File Structure

| 파일 | 책임 | 조건 |
|---|---|---|
| `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCache.kt` | 띠 판 보관 — 열쇠당 크기별 선반 | 수정 |
| `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCacheTest.kt` | 선반 규칙 회귀 | 신규 |
| `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt` | 그리기 단계에서 크기에 맞는 판 고르기 | 수정 |
| `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingBorder.kt` | 컴포넌트에 넘기는 테두리 값 | 신규 |
| `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingImage.kt` | `Remote`에 `border` | 수정 |
| `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingGroup.kt` | `Remote` 분기 교체 | 수정 |
| `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImage.kt` | 도메인 테두리 → 컴포넌트 값, 거리판 대상 URL | 수정 |
| `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImageTest.kt` | 변환 규칙 회귀 | 수정 |
| `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt` | `GroupListContent`가 거리판을 불러 넘긴다 | 수정 |
| `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToppingGroupPreviewScreen.kt` | "Remote + 테두리" 샘플 | 수정 |

---

## Task 1: 띠 판 캐시에 크기별 선반을 둔다

목록(긴 변 `96dp − 2w`)과 캔버스(캔버스 너비의 40% × scale)가 같은 토핑을 그리면 `PlateKey`가 겹쳐 서로의 판을 덮어쓴다. 열쇠는 그대로 두고 값을 판 한 장에서 최근 순 선반으로 바꾼다. 이 태스크만으로는 동작이 바뀌지 않는다 — 크기를 넘기는 호출부는 Task 2가 만든다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCache.kt`
- Test: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCacheTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `ToppingBorderPlateCache.get(outline: ToppingOutline, outsetPx: Float, aspectRatio: Float, subjectLongSide: Int? = null): ToppingBorderPlate?` — `null`이면 선반 맨 앞 판, 값이 있으면 `fitsSubject(subjectLongSide)`를 만족하는 판 중 가장 최근 판. 찾은 판은 선반 맨 앞으로 옮긴다.
  - `ToppingBorderPlateCache.put(outline, outsetPx, aspectRatio, plate)` — 시그니처 불변. 같은 선반에서 새 판 크기와 맞는 판을 걷어 내고 맨 앞에 넣는다. 3장을 넘으면 가장 오래된 판을 버린다.
  - `ToppingBorderPlateCache.clear()` — 시그니처 불변.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCacheTest.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.ui.graphics.ImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ToppingBorderPlateCacheTest {
    private val outline = ToppingOutline.of(OUTLINE_SIDE, OUTLINE_SIDE) { _, _ -> 255 }

    // 캐시가 프로세스 전역이라 테스트 사이에 판이 넘어간다
    @Before
    fun setUp() {
        ToppingBorderPlateCache.clear()
    }

    @Test
    fun sizesFarApart_bothStayRetrievable() {
        // Given 같은 토핑을 목록 크기와 캔버스 크기로 그려 넣은 판(1.25배 넘게 차이)
        val listPlate = plate(subjectLongSide = 264)
        val canvasPlate = plate(subjectLongSide = 432)
        put(listPlate)
        put(canvasPlate)

        // When 각 크기로 꺼낸다
        // Then 나중에 넣은 판이 먼저 넣은 판을 덮어쓰지 않는다
        assertSame(listPlate, get(subjectLongSide = 264))
        assertSame(canvasPlate, get(subjectLongSide = 432))
    }

    @Test
    fun sizeWithinReuseRatio_replacesOlderPlate() {
        // Given 1.25배 안으로 크기가 가까운 두 판
        val olderPlate = plate(subjectLongSide = 400)
        val newerPlate = plate(subjectLongSide = 440)
        put(olderPlate)
        put(newerPlate)

        // When 옛 판에만 맞는 크기로 꺼낸다(330/400 은 맞고 330/440 은 안 맞는다)
        // Then 옛 판은 이미 대체되어 없다
        assertNull(get(subjectLongSide = 330))
        assertSame(newerPlate, get(subjectLongSide = 400))
    }

    @Test
    fun morePlatesThanShelfHolds_dropsOldest() {
        // Given 서로 맞지 않는 크기의 판 넷(선반은 셋을 담는다)
        put(plate(subjectLongSide = 100))
        put(plate(subjectLongSide = 200))
        put(plate(subjectLongSide = 400))
        put(plate(subjectLongSide = 800))

        // Then 가장 먼저 넣은 판만 빠진다
        assertNull(get(subjectLongSide = 100))
        assertNotNull(get(subjectLongSide = 200))
        assertNotNull(get(subjectLongSide = 400))
        assertNotNull(get(subjectLongSide = 800))
    }

    @Test
    fun withoutSize_returnsMostRecentlyUsedPlate() {
        // Given 두 크기의 판
        val smallPlate = plate(subjectLongSide = 100)
        val largePlate = plate(subjectLongSide = 400)
        put(smallPlate)
        put(largePlate)

        // Then 크기 없이 꺼내면 마지막에 넣은 판이다
        assertSame(largePlate, get(subjectLongSide = null))

        // When 작은 판을 크기로 꺼내 쓴다
        get(subjectLongSide = 100)

        // Then 크기 없이 꺼내면 방금 쓴 판이다
        assertSame(smallPlate, get(subjectLongSide = null))
    }

    @Test
    fun noPlateFitsSize_returnsNull() {
        // Given 작은 판 하나
        put(plate(subjectLongSide = 100))

        // Then 네 배 큰 크기에는 꺼낼 판이 없다
        assertNull(get(subjectLongSide = 400))
    }

    @Test
    fun twoPlatesFitSize_returnsMoreRecentOne() {
        // Given 서로 대체되지 않지만(130/100 = 1.3) 크기 115 에는 둘 다 맞는 두 판
        put(plate(subjectLongSide = 100))
        val newerPlate = plate(subjectLongSide = 130)
        put(newerPlate)

        // Then 더 최근에 넣은 판이 나온다
        assertSame(newerPlate, get(subjectLongSide = 115))
    }

    @Test
    fun differentOutset_doesNotShareShelf() {
        // Given 굵기 4px 로 만든 판
        put(plate(subjectLongSide = 100), outsetPx = 4f)

        // Then 굵기 8px 로는 같은 크기라도 꺼내지지 않는다
        assertNull(get(subjectLongSide = 100, outsetPx = 8f))
    }

    private fun plate(subjectLongSide: Int) = ToppingBorderPlate(
        image = ImageBitmap(1, 1),
        padding = 0,
        subjectLongSide = subjectLongSide,
    )

    private fun put(
        plate: ToppingBorderPlate,
        outsetPx: Float = OUTSET_PX,
    ) = ToppingBorderPlateCache.put(outline, outsetPx, ASPECT_RATIO, plate)

    private fun get(
        subjectLongSide: Int?,
        outsetPx: Float = OUTSET_PX,
    ): ToppingBorderPlate? = ToppingBorderPlateCache.get(outline, outsetPx, ASPECT_RATIO, subjectLongSide)

    private companion object {
        const val OUTLINE_SIDE = 8
        const val OUTSET_PX = 12f
        const val ASPECT_RATIO = 1f
    }
}
```

- [ ] **Step 2: 컴파일이 실패하는지 확인한다**

Run: `./gradlew :core:designsystem:compileDebugAndroidTestKotlin`
Expected: FAIL — `get` 호출에 인자가 넷이라 `Too many arguments for public final fun get(...)` 류의 오류.

- [ ] **Step 3: 선반을 구현한다**

`ToppingBorderPlateCache.kt`에서 `ToppingBorderPlate`·`PLATE_REUSE_RATIO_LIMIT`·`fitsSubject`는 그대로 둔다. **object 위 KDoc(`/** 컴포저블이 다시 만들어질 때 …`)부터 `private data class PlateKey(` 바로 위 KDoc 끝까지**를 통째로 다음으로 바꾼다. 기존 object KDoc을 남기면 KDoc 두 개가 연달아 붙는다.

```kotlin
/**
 * 컴포저블이 다시 만들어질 때 판까지 다시 만들면 그동안 테두리를 안 그려 깜빡인다. 프로세스
 * 전역이고 비우는 주체가 없다(`synthesis/open-questions.md` OQ-P-317).
 *
 * 열쇠마다 판을 크기별로 몇 장 둔다. 같은 토핑을 크기가 크게 다른 두 화면이 그리면 한 장짜리 자리는
 * 서로 덮어써 화면을 오갈 때마다 테두리가 깜빡인다(`adr/0030-topping-outline-distance-field.md`).
 */
internal object ToppingBorderPlateCache {
    /** 항목 크기가 알맹이 + 사방 굵기라 굵기에 상한이 없는 한 **칸 수만 묶이고 총량은 안 묶인다** */
    private const val MAX_ENTRIES = 32

    /** 한 토핑을 서로 다른 크기로 그리는 두 화면 + 드래그 중간 크기 한 장 */
    private const val PLATES_PER_KEY = 3

    private const val LOAD_FACTOR = 0.75f

    /** 접근 순서 갱신이 곧 쓰기라 모든 접근을 [entries] 자신에 대해 동기화한다 */
    private val entries =
        object : LinkedHashMap<PlateKey, ArrayDeque<ToppingBorderPlate>>(MAX_ENTRIES, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: Map.Entry<PlateKey, ArrayDeque<ToppingBorderPlate>>) =
                size > MAX_ENTRIES
        }

    /**
     * 크기가 안 맞는 판도 돌려준다 — 어긋난 정도는 [ToppingBorderPlate.fitsSubject] 로 잰다
     *
     * @param subjectLongSide 모르면 `null` 이다 — 상자 크기를 재기 전에 꺼내는 자리가 있다
     */
    fun get(
        outline: ToppingOutline,
        outsetPx: Float,
        aspectRatio: Float,
        subjectLongSide: Int? = null,
    ): ToppingBorderPlate? {
        synchronized(entries) {
            val shelf = entries[PlateKey(outline, outsetPx, aspectRatio)] ?: return null
            val index = if (subjectLongSide == null) {
                if (shelf.isEmpty()) -1 else 0
            } else {
                shelf.indexOfFirst { plate -> plate.fitsSubject(subjectLongSide) }
            }
            if (index < 0) return null

            val found = shelf.removeAt(index)
            shelf.addFirst(found)
            return found
        }
    }

    fun put(
        outline: ToppingOutline,
        outsetPx: Float,
        aspectRatio: Float,
        plate: ToppingBorderPlate,
    ) {
        synchronized(entries) {
            val shelf = entries.getOrPut(PlateKey(outline, outsetPx, aspectRatio)) { ArrayDeque() }

            // 드래그로 크기가 연속으로 바뀌어도 한 구간 안의 판은 서로 대체되어 선반이 쏟아지지 않는다
            shelf.removeAll { stored -> stored.fitsSubject(plate.subjectLongSide) }
            shelf.addFirst(plate)
            while (shelf.size > PLATES_PER_KEY) shelf.removeLast()
        }
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
    }
}

/**
 * **표시 크기는 안 넣는다** — 넣으면 핀치 한 번에 항목이 쏟아지고 화면 전환마다 미스가 난다.
 * 크기가 다른 판은 한 열쇠의 선반에 함께 두고, 어긋난 정도는 꺼낸 쪽이 [ToppingBorderPlate.fitsSubject] 로
 * 잰다. **비율은 넣는다** — 늘려 그리는 배율이 가로 하나뿐이라 비율이 다른 판은 세로가 어긋난다.
 *
 * [ToppingOutline] 은 동등성을 재정의하지 않으므로 **같은 인스턴스**일 때만 같은 열쇠가 된다.
 */
```

`private data class PlateKey(...)` 선언 자체는 바꾸지 않는다. `ArrayDeque`는 `kotlin.collections.ArrayDeque`다(import 불필요) — `java.util.ArrayDeque`를 import하지 않는다.

- [ ] **Step 4: 컴파일과 기존 호출부를 확인한다**

Run: `./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL. `YGToppingCutoutImage`의 기존 세 인자 `get` 호출은 기본값으로 컴파일된다.

- [ ] **Step 5: 계측 테스트를 돌린다**

먼저 `adb devices`로 기기 연결을 확인한다. 없으면 멈추고 보고한다.

Run: `./gradlew :core:designsystem:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.ToppingBorderPlateCacheTest`
Expected: 7 tests PASS.

- [ ] **Step 6: ktlint**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL. 실패하면 `./gradlew :core:designsystem:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 7: 커밋 (사용자가 요청했을 때만)**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCache.kt \
  core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/ToppingBorderPlateCacheTest.kt
git commit -m "feat: 띠 판 캐시가 한 열쇠에 크기별 판을 여러 장 둔다"
```

---

## Task 2: 그리기 단계에서 크기에 맞는 판을 캐시에서 고른다

지금은 `plate` 상태가 알맹이 크기에 맞지 않으면 새 판이 나올 때까지 테두리를 그리지 않는다. 목록에서 캔버스로 들어간 첫 프레임에 `plate`가 목록 크기 판이면 캔버스 판이 선반에 있어도 깜빡인다. 그리기 단계에서 선반을 한 번 더 본다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt` (`private fun BoxScope.ToppingBorder`의 `Canvas` 블록만)

**Interfaces:**
- Consumes: Task 1의 `ToppingBorderPlateCache.get(outline, outsetPx, aspectRatio, subjectLongSide)`
- Produces: 외부 시그니처 변경 없음

- [ ] **Step 1: `Canvas` 블록의 판 선택을 바꾼다**

`ToppingBorder` 안의 `Canvas(...) { ... }`에서 다음 부분을

```kotlin
        val current = plate ?: return@Canvas
        val currentBoxWidth = size.width.roundToInt()
        val currentBoxHeight = size.height.roundToInt()

        // 판은 지금 상자와 크기가 다를 수 있어, 자리와 크기를 지금 상자 기준으로 다시 잰다
        val realSubject = fitSize(aspectRatio, IntSize(currentBoxWidth, currentBoxHeight))

        // 너무 어긋난 판을 늘려 그리면 굵기 dp 고정이 깨진다 — 새 판이 올 때까지 안 그린다
        if (!current.fitsSubject(max(realSubject.width, realSubject.height))) return@Canvas
```

다음으로 바꾼다. 그 아래 `plateSubjectWidth`부터 `drawImage(...)`까지는 그대로 둔다.

```kotlin
        val currentBoxWidth = size.width.roundToInt()
        val currentBoxHeight = size.height.roundToInt()

        // 판은 지금 상자와 크기가 다를 수 있어, 자리와 크기를 지금 상자 기준으로 다시 잰다
        val realSubject = fitSize(aspectRatio, IntSize(currentBoxWidth, currentBoxHeight))
        val realSubjectLongSide = max(realSubject.width, realSubject.height)

        // 너무 어긋난 판을 늘려 그리면 굵기 dp 고정이 깨진다. 상태의 판이 안 맞아도 같은 토핑을 이 크기로
        // 그려 둔 판이 캐시에 있으면 그것을 쓰고, 어디에도 없을 때만 새 판이 올 때까지 안 그린다
        val current = plate?.takeIf { made -> made.fitsSubject(realSubjectLongSide) }
            ?: ToppingBorderPlateCache.get(outline, outsetPx, aspectRatio, realSubjectLongSide)
            ?: return@Canvas
```

`LaunchedEffect`와 `remember(outline) { mutableStateOf(ToppingBorderPlateCache.get(...)) }`는 바꾸지 않는다 — 캐시 판을 그리는 동안에도 지금 크기에 정확한 판은 여전히 만들어 갈아 끼운다.

- [ ] **Step 2: 컴파일한다**

Run: `./gradlew :core:designsystem:compileDebugKotlin :feature:groups:canvas:impl:compileDebugKotlin :feature:segmentation:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 기존 계측 테스트가 깨지지 않았는지 확인한다**

기기가 없으면 멈추고 보고한다.

Run: `./gradlew :core:designsystem:connectedDebugAndroidTest`
Expected: 전체 PASS(Task 1의 7건 포함)

- [ ] **Step 4: ktlint**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋 (사용자가 요청했을 때만)**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt
git commit -m "feat: 테두리 판이 지금 크기에 안 맞으면 캐시에서 맞는 판을 골라 그린다"
```

---

## Task 3: `YGToppingGroup`이 원격 토핑에 테두리를 그린다

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingBorder.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingImage.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/YGToppingGroup.kt`

**Interfaces:**
- Consumes: `YGToppingCutoutImage(painter: Painter, borderColor: Color?, borderWidth: Dp, modifier: Modifier = Modifier, outline: ToppingOutline?)`(기존)
- Produces:
  - `data class YGToppingBorder(val color: Color, val width: Dp, val outline: ToppingOutline?)` — `com.teamyg.parfait.core.designsystem.component.ygtoppinggroup`
  - `data class YGToppingImage.Remote(val url: String, val border: YGToppingBorder? = null)`

- [ ] **Step 1: `YGToppingBorder`를 만든다**

`YGToppingBorder.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline

/**
 * @param outline 거리판을 받기 전에는 `null` 이다 — 그동안 토핑은 굵기만큼 줄어든 채 알맹이만 그린다
 */
@Immutable
data class YGToppingBorder(
    val color: Color,
    val width: Dp,
    val outline: ToppingOutline?,
)
```

- [ ] **Step 2: `Remote`에 `border`를 연다**

`YGToppingImage.kt`의 `Remote`를 다음으로 바꾼다.

```kotlin
    @Immutable
    data class Remote(
        val url: String,
        val border: YGToppingBorder? = null,
    ) : YGToppingImage
```

- [ ] **Step 3: `Remote` 분기를 교체한다**

`YGToppingGroup.kt`의 `when (image)`에서 `is YGToppingImage.Remote -> AsyncImage(...)` 갈래를 다음으로 바꾼다. `Template`·`Error` 갈래와 `imageModifier`는 그대로 둔다.

```kotlin
            is YGToppingImage.Remote -> RemoteToppingImage(
                image = image,
                modifier = imageModifier,
            )
```

같은 파일의 `YGToppingGroup` 함수 아래(프리뷰 위)에 추가한다.

```kotlin
@Composable
private fun RemoteToppingImage(
    image: YGToppingImage.Remote,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val sizePx = with(LocalDensity.current) { SizeTokens.Size96.getDp().roundToPx() }

    // AsyncImage 와 달리 painter 는 레이아웃 제약을 모른다 — 크기를 안 주면 원본 해상도로 디코딩한다
    val request = remember(image.url, sizePx) {
        ImageRequest
            .Builder(context)
            .data(image.url)
            .size(sizePx)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request, contentScale = ContentScale.Fit)
    val painterState by painter.state.collectAsState()

    if (painterState is AsyncImagePainter.State.Error) {
        Image(
            painter = painterResource(TOPPING_ERROR_DRAWABLE),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        val border = image.border
        val borderWidth = border?.width ?: 0.dp

        YGToppingCutoutImage(
            painter = painter,
            // 알맹이가 뜨기 전에 띠를 깔면 실루엣 모양 색 덩어리만 보인다
            borderColor = border?.color?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = borderWidth,
            // 띠는 알맹이 밖으로 굵기만큼 나간다. clip 아래에서 굵기만큼 덜어 내야 띠까지 프레임 안에 든다
            modifier = modifier.padding(borderWidth),
            outline = border?.outline,
        )
    }
}
```

import를 정리한다: `coil3.compose.AsyncImage`를 지우고 다음을 더한다.

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
```

기존 주석 `// 원격 이미지는 배경이 지워진 누끼라, Crop 으로 긴 변을 잘라 내면 피사체가 사라진다`는 `ContentScale.Fit`을 쓰는 `rememberAsyncImagePainter` 줄 위로 옮긴다(의도가 그대로 유효하다).

- [ ] **Step 4: 컴파일한다**

Run: `./gradlew :core:designsystem:compileDebugKotlin :feature:groups:list:impl:compileDebugKotlin :app-preview:compileDebugKotlin`
Expected: BUILD SUCCESSFUL — `border` 기본값 덕분에 feature·app-preview 호출부는 그대로 컴파일된다.

- [ ] **Step 5: 기존 목록 테스트가 그대로 통과하는지 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*ToppingImageTest*'`
Expected: PASS(기존 4건) — `Remote(url)` 비교는 `border = null` 기본값과 같다.

- [ ] **Step 6: ktlint**

Run: `./gradlew :core:designsystem:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋 (사용자가 요청했을 때만)**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppinggroup/
git commit -m "feat: YGToppingGroup 이 원격 토핑에 테두리를 그린다"
```

---

## Task 4: G-001이 테두리 값과 거리판을 넘긴다

**Files:**
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImage.kt`
- Modify: `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt`
- Test: `feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImageTest.kt`

**Interfaces:**
- Consumes: Task 3의 `YGToppingBorder(color, width, outline)`, `YGToppingImage.Remote(url, border)`; `:core:ui`의 `rememberToppingOutlines(models: List<String>, retryKey: Int): Map<String, ToppingOutline>`; `:core:util:android`의 `String.toColorOrNull(): Color?`
- Produces:
  - `internal fun MyParfaitGroupVO.toToppingImage(outlines: Map<String, ToppingOutline>): YGToppingImage`
  - `internal fun List<MyParfaitGroupVO>.borderedImageUrls(): List<String>`

- [ ] **Step 1: 테스트 헬퍼와 기존 호출을 새 시그니처에 맞춘다**

`ToppingImageTest.kt`의 `group` 헬퍼에 `recentImageBorder` 인자를 더한다.

```kotlin
    private fun group(
        groupId: Long,
        recentImageUrl: String?,
        recentImageBorder: ToppingBorder = ToppingBorder.None,
    ) = MyParfaitGroupVO(
        groupId = GroupId(groupId),
        groupName = GroupName("모카의 파르페"),
        recentImageUrl = recentImageUrl,
        recentImageBorder = recentImageBorder,
        recentImageUploadedAt = null,
        lastPlacedByNametagChip = NametagChipType.DEFAULT,
    )
```

기존 테스트 4건의 `toToppingImage()` 호출을 모두 `toToppingImage(emptyMap())`으로 바꾼다(5곳 — `withoutRecentImage_sameGroupKeepsSameTemplate`에 2곳, `withoutRecentImage_differentGroupsSpreadOverTemplates`의 `map` 안 1곳 포함).

- [ ] **Step 2: 실패하는 테스트를 더한다**

`ToppingImageTest` 클래스 안에 추가한다. import에 `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.unit.dp`, `com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingBorder`, `com.teamyg.parfait.core.util.jvm.outline.ToppingOutline`, `kotlin.test.assertNull`을 더한다.

```kotlin
    @Test
    fun solidBorderWithOutline_carriesColorWidthAndOutline() {
        // Given 테두리가 있는 토핑과 이미 떠 둔 거리판
        val url = "https://cdn.example.com/a.png"
        val outline = ToppingOutline.of(4, 4) { _, _ -> 255 }
        val group = group(
            groupId = 1L,
            recentImageUrl = url,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(mapOf(url to outline))

        // Then 색·굵기(dp)·거리판이 함께 실린다
        assertEquals(
            YGToppingImage.Remote(
                url = url,
                border = YGToppingBorder(color = Color.Red, width = 6.dp, outline = outline),
            ),
            image,
        )
    }

    @Test
    fun solidBorderBeforeOutlineLoads_keepsBorderWithoutOutline() {
        // Given 거리판을 아직 받지 못한 테두리 토핑
        val url = "https://cdn.example.com/a.png"
        val group = group(
            groupId = 1L,
            recentImageUrl = url,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 거리판 없이 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 테두리 값은 남아 크기가 먼저 줄고, 거리판만 비어 있다
        assertEquals(YGToppingBorder(color = Color.Red, width = 6.dp, outline = null), image.border)
    }

    @Test
    fun unreadableColor_dropsBorder() {
        // Given 색 문자열을 읽을 수 없는 테두리
        val group = group(
            groupId = 1L,
            recentImageUrl = "https://cdn.example.com/a.png",
            recentImageBorder = ToppingBorder.Solid(color = "빨강", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 임의의 색으로 칠하지 않고 테두리를 버린다
        assertNull(image.border)
    }

    @Test
    fun noneBorder_hasNoBorder() {
        // Given 테두리 없는 토핑
        val group = group(groupId = 1L, recentImageUrl = "https://cdn.example.com/a.png")

        // When 띄울 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 테두리가 없다
        assertNull(image.border)
    }

    @Test
    fun withoutRecentImage_ignoresBorder() {
        // Given 토핑 URL 은 없는데 테두리 값만 있는 그룹
        val group = group(
            groupId = 1L,
            recentImageUrl = null,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 템플릿이 걸리고 테두리는 보지 않는다
        assertIs<YGToppingImage.Template>(image)
    }

    @Test
    fun borderedImageUrls_onlyUrlsWithDrawableBorder() {
        // Given 테두리 상태가 제각각인 그룹들
        val solid = ToppingBorder.Solid(color = "#FF0000", width = 6.0)
        val groups = listOf(
            group(groupId = 1L, recentImageUrl = "https://cdn.example.com/1.png", recentImageBorder = solid),
            group(groupId = 2L, recentImageUrl = "https://cdn.example.com/2.png"),
            group(groupId = 3L, recentImageUrl = null, recentImageBorder = solid),
            group(
                groupId = 4L,
                recentImageUrl = "https://cdn.example.com/4.png",
                recentImageBorder = ToppingBorder.Solid(color = "빨강", width = 6.0),
            ),
            group(groupId = 5L, recentImageUrl = "https://cdn.example.com/5.png", recentImageBorder = solid),
        )

        // When 거리판을 뜰 URL 을 고른다
        val urls = groups.borderedImageUrls()

        // Then 테두리를 실제로 그릴 토핑만 목록 순서대로 남는다
        assertEquals(listOf("https://cdn.example.com/1.png", "https://cdn.example.com/5.png"), urls)
    }
```

- [ ] **Step 3: 실패를 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*ToppingImageTest*'`
Expected: FAIL — 테스트 컴파일 오류(`toToppingImage`에 인자가 없고 `borderedImageUrls`가 없다).

- [ ] **Step 4: 변환을 구현한다**

`ToppingImage.kt` 전체를 다음으로 바꾼다. 파일 머리의 `TODO(토핑 템플릿)` 주석과 `toToppingImage` KDoc은 그대로 옮긴다.

```kotlin
package com.teamyg.parfait.feature.groups.list.impl.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingBorder
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingTemplate
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder

// TODO(토핑 템플릿): 정책은 그룹 생성 시 6종 중 하나를 무작위로 골라 고정하는 것이다.
//  서버가 그 값을 내려주면 groupId 파생을 걷어낸다
private val TOPPING_TEMPLATES = YGToppingTemplate.entries

/**
 * 오늘 캔버스에 토핑이 없는 그룹은 조회 실패([YGToppingImage.Error])와 다른 상태라 템플릿을 띄운다.
 * ⚠️ 어제까지 토핑이 있던 그룹도 여기 걸린다 — 서버가 오늘 것만 내려주기 때문이고, 그것을 템플릿으로
 * 그리는 것이 맞는지는 아직 결정 전이다(OQ-P-336, api/parfait-group.md).
 * 목록 순서가 바뀌어도 같은 그림이 걸리도록 index 가 아니라 groupId 로 고른다.
 */
internal fun MyParfaitGroupVO.toToppingImage(outlines: Map<String, ToppingOutline>): YGToppingImage {
    val url = recentImageUrl
        ?: return YGToppingImage.Template(TOPPING_TEMPLATES[groupId.value.mod(TOPPING_TEMPLATES.size)])

    val border = drawableBorder()?.let { (color, width) ->
        YGToppingBorder(color = color, width = width.dp, outline = outlines[url])
    }

    return YGToppingImage.Remote(url = url, border = border)
}

/** 거리판 한 장은 디코딩에 전 픽셀 순회까지 들어 비싸다 — 테두리를 실제로 그릴 토핑만 뜬다 */
internal fun List<MyParfaitGroupVO>.borderedImageUrls(): List<String> = mapNotNull { group ->
    group.recentImageUrl?.takeIf { group.drawableBorder() != null }
}

// 색을 못 읽으면 테두리를 버린다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀리다
private fun MyParfaitGroupVO.drawableBorder(): Pair<Color, Double>? {
    val solid = recentImageBorder as? ToppingBorder.Solid ?: return null
    val color = solid.color.toColorOrNull() ?: return null
    return color to solid.width
}
```

- [ ] **Step 5: `GroupListContent`가 거리판을 불러 넘긴다**

⚠️ 테스트를 돌리기 전에 이 Step을 먼저 한다. `testDebugUnitTest`는 main 소스도 컴파일하는데, Step 4에서 시그니처가 바뀌어 기존 호출 `group.toToppingImage()`가 `No value passed for parameter 'outlines'`로 컴파일되지 않는다.

`GroupListScreen.kt`의 `GroupListContent` 함수 본문 맨 앞(`GroupListParfaitLayout(` 호출 전)에 추가한다.

```kotlin
    val borderedImageUrls = remember(groupList) { groupList.borderedImageUrls() }
    val outlines = rememberToppingOutlines(models = borderedImageUrls, retryKey = 0)
```

같은 함수의 `YGToppingGroup(` 호출에서

```kotlin
                        image = group.toToppingImage(),
```

를

```kotlin
                        image = group.toToppingImage(outlines),
```

로 바꾼다. import에 `com.teamyg.parfait.core.ui.outline.rememberToppingOutlines`와 `com.teamyg.parfait.feature.groups.list.impl.util.borderedImageUrls`를 더한다. 프리뷰(`GroupListScreenPreviewParameterProvider`)의 `recentImageBorder = ToppingBorder.None`은 그대로 둔다.

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest --tests '*ToppingImageTest*'`
Expected: PASS(기존 4건 + 신규 6건 = 10건)

- [ ] **Step 7: 모듈 전체 테스트·컴파일·ktlint**

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest :feature:groups:list:impl:compileDebugKotlin :feature:groups:list:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL, 전체 PASS(`GroupListViewModelTest` 등 기존 테스트 포함)

- [ ] **Step 8: 커밋 (사용자가 요청했을 때만)**

```bash
git add feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImage.kt \
  feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/route/GroupListScreen.kt \
  feature/groups/list/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImageTest.kt
git commit -m "feat: 그룹 목록이 최신 토핑의 테두리와 거리판을 카드에 넘긴다"
```

---

## Task 5: 카탈로그 샘플과 최종 검증

**Files:**
- Modify: `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToppingGroupPreviewScreen.kt`

**Interfaces:**
- Consumes: Task 3의 `YGToppingBorder`, `YGToppingImage.Remote(url, border)`; `rememberToppingOutlines`
- Produces: 없음

- [ ] **Step 1: "Remote + 테두리" 섹션을 더한다**

파일 상단 상수에 추가한다.

```kotlin
private const val SAMPLE_WIDE_TOPPING_URL = "https://picsum.photos/800/400"
```

기존 "경계 케이스" 섹션의 `YGToppingImage.Remote("https://picsum.photos/800/400")`를 `YGToppingImage.Remote(SAMPLE_WIDE_TOPPING_URL)`로 바꾼다.

`LazyColumn`의 "Remote 성공 / Remote 실패 / Error" `item` 바로 뒤에 추가한다.

```kotlin
            item {
                val outlines = rememberToppingOutlines(
                    models = listOf(SAMPLE_TOPPING_URL, SAMPLE_WIDE_TOPPING_URL),
                    retryKey = 0,
                )

                // 굵기만큼 토핑이 줄어 테두리까지 96dp 안에 드는지, 가장 굵은 값에서 얼마나 줄어드는지 확인용
                PreviewSection("Remote + 테두리 (4dp / 30dp / 비정사각 8dp)") {
                    listOf(4.dp, 30.dp).forEach { width ->
                        YGToppingGroup(
                            image = YGToppingImage.Remote(
                                url = SAMPLE_TOPPING_URL,
                                border = YGToppingBorder(
                                    color = YGAtomicColors.Cherry.Cherry200,
                                    width = width,
                                    outline = outlines[SAMPLE_TOPPING_URL],
                                ),
                            ),
                            name = "테두리 ${width.value.toInt()}dp",
                            timestamp = "3분전",
                            chipType = YGGrouptagChipType.TYPE_5_6,
                            type = YGToppingGroupType.TYPE_1_RIGHT,
                        )
                    }
                    YGToppingGroup(
                        image = YGToppingImage.Remote(
                            url = SAMPLE_WIDE_TOPPING_URL,
                            border = YGToppingBorder(
                                color = YGAtomicColors.Cherry.Cherry200,
                                width = 8.dp,
                                outline = outlines[SAMPLE_WIDE_TOPPING_URL],
                            ),
                        ),
                        name = "비정사각 테두리",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_5_6,
                        type = YGToppingGroupType.TYPE_2_LEFT,
                    )
                }
            }
```

import에 `com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingBorder`, `com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors`, `com.teamyg.parfait.core.ui.outline.rememberToppingOutlines`를 더한다.

- [ ] **Step 2: 전체 빌드·ktlint**

Run: `./gradlew :core:designsystem:ktlintCheck :feature:groups:list:impl:ktlintCheck :app-preview:ktlintCheck :app:assembleDebug :app-preview:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 자동 테스트 전체**

기기가 없으면 계측 테스트는 멈추고 보고한다.

Run: `./gradlew :feature:groups:list:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest :core:designsystem:connectedDebugAndroidTest`
Expected: 전체 PASS

- [ ] **Step 4: 실기기 확인을 사용자에게 요청한다**

`./gradlew :app:installDebug :app-preview:installDebug`로 설치한 뒤, 아래 목록을 사용자에게 전달하고 결과를 받는다. 에이전트가 직접 통과로 판정하지 않는다.

app-preview(`YGToppingGroup` 카탈로그):
1. "Remote + 테두리" 세 카드에서 테두리가 잘리지 않는다. 30dp 카드의 토핑은 눈에 띄게 작다(의도).
2. "Remote 성공" 카드(테두리 없음)의 토핑 크기가 이전과 같다.
3. "Remote 실패" 카드의 에러 그래픽이 96dp다(줄지 않는다).

앱(G-001 ↔ C-001):
4. 테두리가 있는 토핑이 오늘 캔버스에 있는 그룹 카드를 눌러 캔버스로 들어갔다가 돌아오기를 몇 번 반복해도, 양쪽 모두 그 토핑의 테두리가 깜빡이지 않는다. **정사각과 비정사각 토핑을 둘 다 보고, 첫 복귀와 두 번째 이후 복귀를 따로 본다.** 목록과 캔버스는 Coil 메모리 캐시 항목 하나를 함께 써서, 비정사각 토핑은 첫 복귀 때 비율이 원본으로 바뀌며 한 번 늦게 붙고(알려진 동작) 그 뒤로는 모든 토핑에서 판 캐시 열쇠가 겹친다(스펙 주의 절).
5. 캔버스 화면 넷(캔버스 메인·배경 편집·배치·누끼 확인)에서 토핑 크기를 드래그로 바꾸거나 Spotlight를 전환할 때 테두리 동작이 이전과 같다.
6. 목록을 당겨 새로고침한 뒤에도 테두리가 다시 붙는다.

- [ ] **Step 5: 커밋 (사용자가 요청했을 때만)**

```bash
git add app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/component/YGToppingGroupPreviewScreen.kt
git commit -m "feat: 토핑 그룹 카탈로그에 테두리 샘플을 더한다"
```
