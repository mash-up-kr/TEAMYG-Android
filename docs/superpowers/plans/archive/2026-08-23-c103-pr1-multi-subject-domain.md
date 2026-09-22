---
id: c103-pr1-multi-subject-domain
title: C-103 다중 후보 PR1 — data·domain 다중화 (ML Kit enableMultipleSubjects)
status: done
archived_reason: PR #342(`34bf1939`)로 develop 머지 — PR2 브랜치가 이 스택을 안은 채 한 PR로 들어왔다
type: work-order
created: 2026-08-23
updated: 2026-08-24
platforms: android
owner: Parfait 팀
related_adr: ADR-0026
related_spec: c103-multi-subject-selection
related_code:
  - SegmentationCandidate
  - SegmentationCandidateFilter.kt#filterCandidates
  - ImageSegmentationRepositoryImpl.kt#segmentImage
  - ImageSegmentationRepositoryImpl.kt#persistSubject
  - ImageSegmentationRepository
  - SegmentationResult
  - SegmentImageUseCase
  - PersistSubjectUseCase
  - SegmentationMask.kt#maskSubjectPixels
  - SegmentationViewModel.kt#SegmentationViewModel
tags: [plan, parfait, segmentation, topping, c-103]
---

# C-103 다중 후보 PR1 — data·domain 다중화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development`(권장) 또는 `superpowers:executing-plans`로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** ML Kit 세그멘테이션이 피사체 후보를 **여러 개** 돌려주게 하고, 파일 저장을 별도 계약으로 갈라낸다. 화면은 아직 후보 하나만 쓴다.

**Architecture:** 지금은 `segmentImage` 하나가 세그멘테이션과 파일 저장을 함께 하고 결과를 단수로 돌려준다. 이 라운드는 그것을 둘로 가른다 — `segmentImage`는 디스크를 건드리지 않고 후보 목록을 주고, `persistSubject`가 후보 하나를 받아 PNG 두 장을 만든다. ML Kit 옵션을 `enableMultipleSubjects` + `enableSubjectBitmap`으로 바꾸면 `Subject.getBitmap()`이 이미 bounds 크기로 잘린 판을 주므로, 후보마다 전체 픽셀을 훑고 다시 자르는 일이 사라진다. 다만 **기존 전경 마스크 경로는 지우지 않는다** — 후보가 0건일 때 폴백으로 쓴다. 화면 쪽은 이 라운드에서 `candidates.firstOrNull()` 하나만 골라 지금과 같이 동작하고, 저장 시점이 탭으로 옮겨 가는 것은 PR2다.

**Tech Stack:** Kotlin · Hilt · ML Kit Subject Segmentation 16.0.0-beta1 · kotlinx-coroutines-test · MockK · Turbine · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-23-c103-multi-subject-selection.md`](../../specs/archive/2026-08-23-c103-multi-subject-selection.md) — 「PR 분할」 표 **1번 행**

**작업 대상 저장소:** `TJYG-Android` (이 저장소가 아니다). 브랜치는 `develop`에서 새로 판다.

## Global Constraints

- **커밋하지 않는다.** 각 Task 마지막의 commit 단계는 **사용자가 명시적으로 커밋을 요청했을 때만** 실행한다. 기본은 미커밋이고, 변경 내용을 요약해 보고하는 것으로 Task를 닫는다. `git push`와 PR 생성은 어떤 경우에도 사용자 승인 없이 하지 않는다.
- **코드가 이미 말하는 것은 주석에 쓰지 않는다.** 뻔하지 않은 의도와 함정만 쓴다.
- **`@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.** 고정 틀(의도/반환값/파라미터)을 항상 두지 않는다.
- **다른 컴포넌트의 현재 상태를 단정하는 주석을 쓰지 않는다** — 낡는다. 써야 하면 근거 문서를 가리킨다.
- 아키텍처 결정 설명은 코드가 아니라 `parfait/adr/`·`parfait/architecture/` 몫이다. 코드에는 포인터 한 줄만 둔다.
- 테스트 이름은 `대상_상황_기대` 형식이고 본문에 `// Given` · `// When` · `// Then` 주석을 단다(이 저장소 전체 관례).
- 매퍼 단독 테스트(`XxxVOMapperTest`)를 만들지 않는다.
- 매 Task 끝에 `./gradlew test ktlintCheck` 가 통과해야 한다.

---

## File Structure

| 파일 | 책임 | 상태 |
|---|---|---|
| `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationCandidate.kt` | 후보 하나(좌표계 + 잘린 비트맵) | 신설 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationResult.kt` | 저장된 두 경로 | 수정(`subjectBounds` 제거) |
| `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt` | 계약 | 수정(`segmentImage` 반환 변경 + `persistSubject` 추가) |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/SegmentImageUseCase.kt` | 세그멘테이션 진입 | 수정(반환 타입) |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PersistSubjectUseCase.kt` | 후보 저장 진입 | 신설 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt` | 후보 걸러내기·정렬 (순수) | 신설 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt` | ML Kit 호출·폴백·파일 저장 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationMask.kt` | 전경 마스크 → bounds (순수) | 유지 |
| `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt` | 필터 검증 | 신설 |
| `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt` | 화면 상태 | 수정(최소 적응) |
| `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt` | 화면 검증 | 수정 |

---

### Task 1: 후보 모델과 필터 순수 함수

ML Kit가 돌려준 후보를 어디까지 신뢰할지 정하는 판단이다. 기기 없이 검증되도록 순수 함수로 뺀다 — 같은 디렉토리의 `SegmentationMask.kt`가 `Bitmap` 대신 `IntArray`·`FloatBuffer`를 받는 것과 같은 이유다.

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationCandidate.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt`

**Interfaces:**
- Consumes: `SegmentationBounds`(기존, `right`·`bottom`이 exclusive), `BitmapWrapper`(기존, 빈 인터페이스)
- Produces:
  - `data class SegmentationCandidate(bounds: SegmentationBounds, bitmap: BitmapWrapper, canvasWidth: Int, canvasHeight: Int)`
  - `internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate>`
  - `internal const val MIN_SUBJECT_AREA_RATIO = 0.01f`
  - `internal const val MAX_SUBJECT_COUNT = 5`

- [ ] **Step 1: 후보 모델을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationCandidate.kt`:

```kotlin
package com.teamyg.parfait.domain.model

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper

/**
 * 사용자가 고를 수 있는 피사체 후보 하나.
 *
 * [canvasWidth] 와 [canvasHeight] 는 [bounds] 가 어느 좌표계의 값인지를 말한다. 한 번의
 * 세그멘테이션에서 나온 후보끼리 같은 값이 복제되지만, 그 대가로 후보 하나만 넘겨도 좌표계가
 * 온전히 따라간다 — 저장할 때 다른 크기를 실어 보내 그림이 어긋나는 조합이 성립하지 않는다.
 */
data class SegmentationCandidate(
    val bounds: SegmentationBounds,
    /**
     * **반드시 [bounds] 크기로 잘린 판이어야 한다.** 저장이 이 판을 원본 크기 캔버스의
     * `(bounds.left, bounds.top)` 에 그대로 얹으므로, 원본 크기 판을 실으면 오른쪽과 아래가
     * 잘린 채 저장된다 — 예외가 아니라 조용한 파손이라 늦게 드러난다.
     */
    val bitmap: BitmapWrapper,
    val canvasWidth: Int,
    val canvasHeight: Int,
)
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CANVAS_SIDE = 100

/** 필터는 비트맵을 보지 않는다 — 자리만 채운다 */
private object FakeBitmap : BitmapWrapper

class SegmentationCandidateFilterTest {
    /** 원본은 100×100(면적 10000)이라, 한 변이 10이면 면적 100 = 정확히 1% 다 */
    private fun candidate(
        left: Int = 0,
        top: Int = 0,
        width: Int = 50,
        height: Int = 50,
    ) = SegmentationCandidate(
        bounds = SegmentationBounds(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        ),
        bitmap = FakeBitmap,
        canvasWidth = CANVAS_SIDE,
        canvasHeight = CANVAS_SIDE,
    )

    @Test
    fun filterCandidates_areaIsExactlyTheThreshold_keepsIt() {
        // Given 면적이 원본의 정확히 1% 인 후보
        val onePercent = candidate(width = 10, height = 10)

        // When 거른다
        val filtered = filterCandidates(listOf(onePercent))

        // Then 임계 "미만" 만 버리므로 남는다
        assertEquals(listOf(onePercent), filtered)
    }

    @Test
    fun filterCandidates_areaIsBelowTheThreshold_dropsIt() {
        // Given 면적이 원본의 0.99% 인 후보(99 픽셀)
        val tooSmall = candidate(width = 9, height = 11)

        // When 거른다
        val filtered = filterCandidates(listOf(tooSmall))

        // Then 손톱만 한 파편은 화면에 올리지 않는다
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterCandidates_everyCandidateIsBelowTheThreshold_returnsEmpty() {
        // Given 전부 임계 미만인 후보 셋
        val all = listOf(
            candidate(left = 0, width = 5, height = 5),
            candidate(left = 20, width = 6, height = 6),
            candidate(left = 40, width = 7, height = 7),
        )

        // When 거른다
        val filtered = filterCandidates(all)

        // Then 빈 목록이다 — 호출부가 이걸 보고 폴백을 태운다
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterCandidates_moreThanTheLimit_keepsTheBiggestOnes() {
        // Given 상한(5)보다 하나 많은 후보 여섯. 면적이 제각각이다
        val sizes = listOf(20, 60, 30, 50, 40, 70)
        val candidates = sizes.mapIndexed { index, side ->
            candidate(left = index, top = index, width = side, height = side)
        }

        // When 거른다
        val filtered = filterCandidates(candidates)

        // Then 면적 큰 것부터 다섯만 남는다
        assertEquals(5, filtered.size)
        assertEquals(listOf(70, 60, 50, 40, 30), filtered.map { it.bounds.width })
    }

    @Test
    fun filterCandidates_sameArea_ordersByTopThenLeft() {
        // Given 면적이 모두 같고 위치만 다른 후보 셋(입력 순서는 뒤섞여 있다)
        val bottomLeft = candidate(left = 0, top = 40, width = 20, height = 20)
        val topRight = candidate(left = 40, top = 0, width = 20, height = 20)
        val topLeft = candidate(left = 0, top = 0, width = 20, height = 20)

        // When 거른다
        val filtered = filterCandidates(listOf(bottomLeft, topRight, topLeft))

        // Then top → left 오름차순으로 갈린다. ML Kit 반환 순서에 기대면 테스트가 흔들린다
        assertEquals(listOf(topLeft, topRight, bottomLeft), filtered)
    }

    @Test
    fun filterCandidates_duplicateBounds_keepsOnlyOne() {
        // Given 좌표가 완전히 같은 후보 둘
        val first = candidate(left = 10, top = 10, width = 30, height = 30)
        val second = candidate(left = 10, top = 10, width = 30, height = 30)

        // When 거른다
        val filtered = filterCandidates(listOf(first, second))

        // Then 하나만 남는다 — 탭 판정이 겹친 둘 중 하나를 영영 못 고른다
        assertEquals(1, filtered.size)
    }

    @Test
    fun filterCandidates_emptyInput_returnsEmpty() {
        // Given 후보가 없는 입력
        // When 거른다
        val filtered = filterCandidates(emptyList())

        // Then 빈 목록이다
        assertTrue(filtered.isEmpty())
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationCandidateFilterTest*"`
Expected: 컴파일 실패 — `Unresolved reference: filterCandidates`

- [ ] **Step 4: 필터를 구현한다**

`data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationCandidate

/** 원본 면적 대비 이 비율 **미만** 인 후보는 버린다 */
internal const val MIN_SUBJECT_AREA_RATIO = 0.01f

/** 화면에 동시에 올리는 후보 수 상한 */
internal const val MAX_SUBJECT_COUNT = 5

/**
 * ML Kit 가 돌려준 후보에서 화면에 올릴 것만 남긴다.
 *
 * 정렬이 결정적이어야 하는 이유가 둘이다 — 테스트가 ML Kit 반환 순서에 흔들리지 않아야 하고,
 * 탭 판정이 목록 순서를 근거로 삼지 않더라도 상한 절단 결과가 매번 같아야 한다.
 */
internal fun filterCandidates(candidates: List<SegmentationCandidate>): List<SegmentationCandidate> = candidates
    .distinctBy { it.bounds }
    .filter { it.isLargeEnough() }
    .sortedWith(
        compareByDescending<SegmentationCandidate> { it.area }
            .thenBy { it.bounds.top }
            .thenBy { it.bounds.left },
    ).take(MAX_SUBJECT_COUNT)

/**
 * 면적을 마스크의 실제 객체 픽셀이 아니라 bounds 로 재는 것은, 이 판정이 거르려는 것이 손톱만 한
 * 파편이라 사각형만으로 충분해서다.
 */
private val SegmentationCandidate.area: Int
    get() = bounds.width * bounds.height

private fun SegmentationCandidate.isLargeEnough(): Boolean {
    val canvasArea = canvasWidth.toLong() * canvasHeight
    if (canvasArea <= 0L) return false

    return area >= canvasArea * MIN_SUBJECT_AREA_RATIO
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*SegmentationCandidateFilterTest*"`
Expected: PASS (7건)

- [ ] **Step 6: 전체 검증**

Run: `./gradlew test ktlintCheck`
Expected: 기존 테스트 전부 통과. 이 Task는 아직 아무도 호출하지 않는 코드만 더한다.

- [ ] **Step 7: 커밋 (사용자가 요청했을 때만)**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationCandidate.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilter.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationCandidateFilterTest.kt
git commit -m "feat: 세그멘테이션 후보 모델과 필터를 더한다"
```

---

### Task 2: 후보 하나를 파일로 떨구는 계약

저장을 세그멘테이션에서 떼어 낸다. 이 Task가 끝나도 **호출자는 0건**이다 — 다음 Task가 `segmentImage`를 바꾸면서 붙인다. 소비자 없이 먼저 만드는 이유는 리뷰의 초점을 좌표 합성 하나로 좁히기 위해서다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PersistSubjectUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: Task 1의 `SegmentationCandidate`
- Produces:
  - `suspend fun ImageSegmentationRepository.persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult>`
  - `class PersistSubjectUseCase { suspend operator fun invoke(candidate: SegmentationCandidate): Result<SegmentationResult> }`

- [ ] **Step 1: 계약에 메서드를 더한다**

`ImageSegmentationRepository.kt`의 `segmentImage` 선언 **아래**에 추가한다(`segmentImage` 자체는 이 Task에서 건드리지 않는다):

```kotlin
    /**
     * 고른 후보를 캐시에 PNG 두 장으로 떨군다.
     *
     * 두 장인 이유는 쓰는 곳이 요구하는 좌표계가 달라서다 — 수동 편집은 원본과 픽셀로 겹쳐
     * 그려야 해서 원본 크기 판이 필요하고, 미리보기·배치는 여백 없는 실제 크기가 필요하다.
     */
    suspend fun persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult>
```

`import com.teamyg.parfait.domain.model.SegmentationCandidate` 를 더한다.

- [ ] **Step 2: 구현을 더한다**

`ImageSegmentationRepositoryImpl.kt`의 `segmentImage` **아래**에 추가한다:

```kotlin
    override suspend fun persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult> {
        val trimmed: Bitmap = (candidate.bitmap as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        return withContext(Dispatchers.Default) {
            try {
                val trimmedFile = trimmed.saveToCacheAsPng()

                // 원본과 같은 좌표계의 판. 편집 화면이 원본 위에 픽셀로 겹쳐 그린다
                val canvas = Bitmap.createBitmap(
                    candidate.canvasWidth,
                    candidate.canvasHeight,
                    Bitmap.Config.ARGB_8888,
                )

                val subjectFile = try {
                    // 스케일하지 않고 그대로 얹는다 — ML Kit 가 준 치수와 bounds 가 어긋나더라도
                    // 그림이 찌그러지지는 않게 한다
                    Canvas(canvas).drawBitmap(
                        trimmed,
                        candidate.bounds.left.toFloat(),
                        candidate.bounds.top.toFloat(),
                        null,
                    )
                    canvas.saveToCacheAsPng()
                } finally {
                    canvas.recycle()
                }

                Result.success(
                    SegmentationResult(
                        subjectImagePath = subjectFile.absolutePath,
                        trimmedSubjectImagePath = trimmedFile.absolutePath,
                        // 이 필드는 Task 3 이 걷는다. 지금은 아직 있어서 넘겨야 컴파일된다
                        subjectBounds = candidate.bounds,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
    }
```

import 셋을 더한다: `android.graphics.Canvas` · `com.teamyg.parfait.domain.model.SegmentationCandidate`.

> `candidate.bitmap` 은 **호출자가 소유한다.** 여기서 `recycle()` 하지 않는다 — 화면이 후보 목록을 들고 있고, 저장 뒤에도 하이라이트를 계속 그린다. 같은 파일의 `saveEditedImage` 가 같은 이유로 회수하지 않는다.

- [ ] **Step 3: UseCase를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PersistSubjectUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class PersistSubjectUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "PersistSubjectUseCase::init" }
    }

    suspend operator fun invoke(candidate: SegmentationCandidate): Result<SegmentationResult> =
        repository.persistSubject(candidate)
}
```

- [ ] **Step 4: 컴파일과 기존 테스트를 확인한다**

Run: `./gradlew test ktlintCheck`
Expected: 전부 통과. `SegmentationResult` 는 아직 3필드이므로 이 Task에서 `subjectBounds` 는 기본값 없이 남아 있다 — Step 2의 `SegmentationResult(...)` 호출에 `subjectBounds = candidate.bounds` 를 함께 넘겨 컴파일을 통과시키고, **Task 3에서 필드를 걷을 때 이 인자도 함께 지운다.**

- [ ] **Step 5: 커밋 (사용자가 요청했을 때만)**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt \
        domain/src/main/java/com/teamyg/parfait/domain/usecase/image/PersistSubjectUseCase.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt
git commit -m "feat: 고른 후보를 파일로 떨구는 persistSubject 를 더한다"
```

---

### Task 3: 세그멘테이션을 다중 후보로 전환한다

`segmentImage`의 반환을 목록으로 바꾸고 ML Kit 옵션을 전환한다. 여기서 저장이 `segmentImage` 밖으로 나가므로 `SegmentationViewModel`도 함께 고쳐야 컴파일된다. 화면 동작은 지금과 같게 유지한다 — ViewModel이 `init`에서 첫 후보를 골라 즉시 저장하고 초안을 적는다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationResult.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/SegmentImageUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `filterCandidates`, Task 2의 `persistSubject`·`PersistSubjectUseCase`
- Produces:
  - `suspend fun ImageSegmentationRepository.segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>>`
  - `class SegmentImageUseCase { suspend operator fun invoke(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> }`
  - `data class SegmentationResult(subjectImagePath: String, trimmedSubjectImagePath: String)` — `subjectBounds` 제거

- [ ] **Step 1: ViewModel 테스트를 새 계약으로 고친다**

`SegmentationViewModelTest.kt`에서 픽스처와 스텁을 바꾼다. 아래 세 조각을 교체한다.

첫째, import에 `SegmentationCandidate`와 `PersistSubjectUseCase`를 더하고 `SegmentationBounds`는 그대로 둔다.

둘째, 픽스처를 바꾼다:

```kotlin
    private val persistSubject: PersistSubjectUseCase = mockk()

    private val candidate = SegmentationCandidate(
        bounds = SegmentationBounds(left = 0, top = 0, right = 10, bottom = 10),
        bitmap = bitmapWrapper,
        canvasWidth = 100,
        canvasHeight = 100,
    )

    private val success = SegmentationResult(
        subjectImagePath = SUBJECT_PATH,
        trimmedSubjectImagePath = TRIMMED_SUBJECT_PATH,
    )
```

셋째, `stubTheHappyPath`와 `viewModel()`을 바꾼다:

```kotlin
    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns Result.success(bitmapWrapper)
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate))
        coEvery { persistSubject(candidate) } returns Result.success(success)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        addRecentImageUseCase = addRecentImage,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
        persistSubjectUseCase = persistSubject,
        toppingDraftRepository = toppingDraftRepository,
    )
```

그리고 `init_noSubjectDetected_tellsTheUser`의 Given을 바꾼다 — 이제 "감지된 객체가 없음"은 `subjectBounds == null`이 아니라 빈 목록이다:

```kotlin
    @Test
    fun init_noSubjectDetected_tellsTheUser() = runTest {
        // Given 성공했지만 후보가 하나도 없는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(emptyList())

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패로 알린다 — 하이라이트도 다음 화면으로 갈 방법도 없는 화면을 말없이 남기지 않는다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }
```

마지막으로 저장 실패 케이스를 새로 더한다:

```kotlin
    @Test
    fun init_persistFails_tellsTheUser() = runTest {
        // Given 후보는 잡혔지만 파일로 떨구는 데 실패하는 상황
        coEvery { persistSubject(candidate) } returns Result.failure(IllegalStateException("no space"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 토스트로 알리고 로딩 오버레이는 걷힌다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
        assertFalse(viewModel.state.value.isLoading)
    }
```

⚠️ **`init_segmentationSucceeds_recordsTheDraft`에서 스텁 한 줄을 지운다.** 이 테스트는 `@Before`가 이미 놓은 것과 같은 스텁을 자기 안에서 다시 놓는데, 그 줄이 옛 타입이라 그대로 두면 **컴파일이 막힌다.**

```kotlin
    @Test
    fun init_segmentationSucceeds_recordsTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        // Given 정상 응답
        // When 화면이 돈다
        viewModel()
        advanceUntilIdle()
```

즉 `coEvery { segmentImage(bitmapWrapper) } returns Result.success(success)` 줄을 걷고 Given 주석을 다듬는다. 뒤의 `coVerify` 블록은 그대로 둔다.

나머지 테스트(`init_segmentationSucceeds_publishesSubjectImagePath`·`init_cacheClearThrows_stillSegments`·`init_recentImageRecordThrows_stillSegments`·`init_segmentationFails_recordsNothing`·`init_segmentationFails_tellsTheUser` 등)는 **본문을 고치지 않는다.** `Result.failure(...)`는 제네릭이 기대 타입에서 추론되므로 옛 코드 그대로 컴파일되고, 화면 동작이 같으므로 단언도 그대로 통과한다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: 컴파일 실패 셋 — `SegmentationResult` 생성자 인자 수 불일치, `SegmentationViewModel`에 `persistSubjectUseCase` 파라미터 없음, `Result.success(listOf(candidate))`의 타입 불일치(`segmentImage`가 아직 옛 계약이다). `SegmentationCandidate` 자체는 Task 1에서 `domain`에 만들었고 이 모듈이 `implementation(project(":domain"))`을 받으므로 해결된다.

- [ ] **Step 3: `SegmentationResult`에서 `subjectBounds`를 걷는다**

```kotlin
package com.teamyg.parfait.domain.model

data class SegmentationResult(
    /** 원본과 같은 캔버스 크기의 객체 이미지. 수동 편집 화면이 원본과 픽셀 단위로 맞춰 그리는 데 쓴다 */
    val subjectImagePath: String,
    /** 투명한 여백을 걷어내 객체 크기만 남긴 이미지. 미리보기·배치처럼 실제 보이는 크기가 필요할 때 쓴다 */
    val trimmedSubjectImagePath: String,
)
```

Task 2 Step 4에서 임시로 넘겼던 `subjectBounds = candidate.bounds` 인자를 `persistSubject` 구현에서 지운다.

- [ ] **Step 4: 계약과 UseCase의 반환 타입을 바꾼다**

`ImageSegmentationRepository.kt`:

```kotlin
    /**
     * 사진에서 고를 수 있는 피사체 후보를 찾는다. **디스크를 건드리지 않는다** — 고른 하나만
     * [persistSubject] 로 떨군다.
     */
    suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>>
```

`SegmentImageUseCase.kt`의 반환 타입을 `Result<List<SegmentationCandidate>>`로 바꾸고 import를 `SegmentationResult`에서 `SegmentationCandidate`로 교체한다.

- [ ] **Step 5: ML Kit 옵션을 다중으로 바꾸고 `segmentImage`를 다시 쓴다**

`ImageSegmentationRepositoryImpl.kt`의 `segmentImage`를 통째로 교체한다:

```kotlin
    override suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> {
        val bitmap: Bitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData() ?: return Result.failure(
            SegmentationException.ImageNotFound(null),
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val options = SubjectSegmenterOptions
            .Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions
                    .Builder()
                    .enableSubjectBitmap()
                    .build(),
            )
            // 후보가 0건일 때 폴백이 쓴다
            .enableForegroundConfidenceMask()
            .build()

        val segmenter = try {
            SubjectSegmentation.getClient(options)
        } catch (e: Exception) {
            return Result.failure(SegmentationException.ClientInit(e))
        }

        val result = try {
            segmenter.use { segmenter ->
                // 모델은 APK 가 아니라 Play 서비스가 내려주는 optional module 이라, 받기 전에 process 하면 실패한다
                if (!ensureModuleInstalled(segmenter)) {
                    return Result.failure(SegmentationException.ModuleNotReady(null))
                }

                withContext(Dispatchers.IO) {
                    Tasks.await(segmenter.process(image))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e.toSegmentationException())
        }

        return withContext(Dispatchers.Default) {
            try {
                val candidates = filterCandidates(result.toCandidates(bitmap))

                // 필터가 전부 걸러 낸 경우도 폴백을 태운다. 이 갈래를 빼면 지금 잘 되던 사진이
                // 다중 전환 이후 실패로 바뀐다
                val resolved = candidates.ifEmpty { result.fallbackCandidates(bitmap) }

                Result.success(resolved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
    }

    /**
     * `getBitmap()` 은 널을 돌려줄 수 있다 — `enableSubjectBitmap()` 을 켰다는 이유로 비널을
     * 단정하지 않는다. 판이 없는 후보는 고를 수 없으므로 버린다.
     */
    private fun SubjectSegmentationResult.toCandidates(origin: Bitmap): List<SegmentationCandidate> =
        subjects.mapNotNull { subject ->
            val subjectBitmap = subject.bitmap ?: return@mapNotNull null

            SegmentationCandidate(
                // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다
                bounds = SegmentationBounds(
                    left = subject.startX,
                    top = subject.startY,
                    right = subject.startX + subject.width,
                    bottom = subject.startY + subject.height,
                ),
                bitmap = subjectBitmap.toAndroidBitmap(),
                canvasWidth = origin.width,
                canvasHeight = origin.height,
            )
        }

    /**
     * 후보가 하나도 안 남았을 때 전경 마스크로 한 개를 만든다.
     *
     * 이 경로가 실제로 도달 가능한지는 아직 확인하지 못했다(`synthesis/open-questions.md` OQ-P-268).
     */
    private fun SubjectSegmentationResult.fallbackCandidates(origin: Bitmap): List<SegmentationCandidate> {
        val foregroundMask = foregroundConfidenceMask ?: return emptyList()

        val width = origin.width
        val height = origin.height

        // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
        // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
        // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
        // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
        if (foregroundMask.remaining() != width * height) return emptyList()

        val pixels = IntArray(width * height)
        origin.getPixels(pixels, 0, width, 0, 0, width, height)

        val bounds = maskSubjectPixels(pixels, foregroundMask, width, height) ?: return emptyList()

        val masked = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        // 잘라내야 한다 — 후보의 비트맵은 bounds 크기라는 것이 저장 쪽 전제다.
        // 원본 크기 판을 그대로 실으면 (left, top) 만큼 밀려 그려진다
        val trimmed = Bitmap.createBitmap(masked, bounds.left, bounds.top, bounds.width, bounds.height)

        // createBitmap 은 자를 것이 없으면 원본 인스턴스를 그대로 돌려준다.
        // 그때 회수하면 방금 만든 판이 사라진다
        if (trimmed !== masked) masked.recycle()

        return listOf(
            SegmentationCandidate(
                bounds = bounds,
                bitmap = trimmed.toAndroidBitmap(),
                canvasWidth = width,
                canvasHeight = height,
            ),
        )
    }
```

import를 **둘** 더한다: `com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult` · `com.teamyg.parfait.domain.model.SegmentationBounds`. `SegmentationCandidate`는 Task 2에서 이미 들어왔고, `toAndroidBitmap`은 기존 import에 있으며 `filterCandidates`·`maskSubjectPixels`는 같은 패키지라 추가할 것이 없다.

> 📌 **실패의 의미가 한 자리에서 뒤집힌다.** 지금은 전경 마스크가 없거나 `remaining()` 이 어긋나면 `Result.failure(SegmentationException.Process(null))` 이 나간다. 이 라운드 뒤에는 그 두 갈래가 `fallbackCandidates` 의 `emptyList()` 를 거쳐 **`Result.success(emptyList())`** 가 되고, 화면이 빈 목록을 보고 `ShowError` 를 띄운다. 사용자가 보는 것은 같지만 `Result` 의 성패가 반대가 되므로, 리뷰에서 "실패를 성공으로 바꿨다"는 지적이 나오면 이 문단을 가리킨다.

> 위 `if (trimmed !== masked)` 가드는 기존 `segmentImage` 가 `trimmedBitmap !== subjectBitmap` 으로 이미 방어하던 것과 같은 함정이다. `Bitmap.createBitmap` 이 자를 것이 없으면 원본 인스턴스를 그대로 돌려준다.

- [ ] **Step 6: ViewModel을 새 계약에 맞춘다**

`SegmentationViewModel.kt`의 생성자에 `private val persistSubjectUseCase: PersistSubjectUseCase,` 를 `segmentImageUseCase` 아래에 더하고, `segmentImageUseCase(...)` 호출 블록을 교체한다:

```kotlin
            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    // PR2 에서 사용자가 고르게 된다. 지금은 첫 후보를 자동으로 집어 화면 동작을 유지한다
                    val candidate = candidates.firstOrNull()

                    if (candidate == null) {
                        postSideEffect(SegmentationEffect.ShowError)
                        return@onSuccess
                    }

                    persistSubjectUseCase(candidate)
                        .onSuccess { result ->
                            updateState {
                                copy(
                                    subjectImagePath = result.subjectImagePath,
                                    trimmedSubjectImagePath = result.trimmedSubjectImagePath,
                                    subjectBounds = candidate.bounds,
                                )
                            }

                            // 흐름의 결과물은 초안이 나른다(`adr/0026-topping-draft-datastore-ssot.md`).
                            // 미리보기·배치에 쓸 것은 여백을 걷은 판이고, 재편집 마스크는 좌표계를 지킨 판이다
                            runSuspendCatching {
                                toppingDraftRepository.record(
                                    subjectImagePath = result.trimmedSubjectImagePath,
                                    cutoutImagePath = result.subjectImagePath,
                                    borderColorArgb = null,
                                    borderWidthDp = null,
                                )
                            }
                        }.onFailure { postSideEffect(SegmentationEffect.ShowError) }
                }.onFailure { postSideEffect(SegmentationEffect.ShowError) }
```

`SegmentationState`는 이 Task에서 **그대로 둔다**(경로 3필드 유지). 재편은 PR2 몫이다.

import를 더한다: `com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase`.

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: PASS (기존 13건 + 신규 1건 = 14건)

- [ ] **Step 8: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: 전부 통과

- [ ] **Step 9: 커밋 (사용자가 요청했을 때만)**

```bash
git add domain data feature/segmentation
git commit -m "feat: 세그멘테이션이 피사체 후보를 여러 개 돌려주게 한다"
```

---

## 검증

이 라운드는 자동 테스트로 덮이지 않는 자리가 있다. **`persistSubject`의 좌표 합성과 폴백의 크롭은 `Bitmap`·`Canvas` 의존이라 JVM 유닛 대상이 아니다.** 아래를 실기기로 확인한다.

1. 피사체 하나인 사진 — 결과가 현행과 같은가(알맹이 위치·크기).
2. 피사체 여럿인 사진 — 점선 박스가 **가장 큰 후보 하나**로 좁아지는가(이 PR의 의도된 변화다).
3. 배경이 복잡한 사진 — ML Kit가 후보를 몇 개, 어떤 크기로 돌려주는가. `MIN_SUBJECT_AREA_RATIO`·`MAX_SUBJECT_COUNT` 조정 근거를 여기서 얻는다(OQ-P-267).
4. 후보가 0건인 사진에서 폴백이 도는가. 돈다면 결과물이 어긋나지 않는가(OQ-P-268·크롭 검증).
5. 확인 화면까지 진행했을 때 "다음"이 잠기지 않는가(초안이 제대로 적혔다는 뜻이다).

## 범위 밖

- 화면에 후보를 여러 개 그리는 것 — PR2.
- 저장·초안 기록을 탭 시점으로 옮기는 것 — PR2. **이 라운드에서는 `init`에서 즉시 저장한다.**
- 후보 비트맵의 명시적 해제(OQ-P-266).
- Safe Margin +20% 캔버스(OQ-P-150).
- 선행 스펙([c103-segmentation-topping-edit](../../specs/archive/2026-08-15-c103-segmentation-topping-edit.md))의 갱신 표기 — PR2 머지 시점에 한 번에 단다.
