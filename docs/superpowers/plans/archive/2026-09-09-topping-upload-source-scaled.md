# 토핑 업로드 원본 기준 축소 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **완료·develop 머지(2026-09-09, PR #480 `93cb002b5`).** 설계대로 들어왔다. 계획이 예고한
> "PR #479가 먼저 머지되면 인자 추가 지점이 한 겹 늘어난다"가 실제로 그렇게 됐고, 인자는
> UseCase 쪽에 얹혔다. 로그 함수 신설·`Passthrough` 로그·`domain` 의존이 잠시 들어왔다 걷힌
> 경위는 [스펙 「as-built」](../../specs/archive/2026-09-09-topping-upload-source-scaled.md).
> ⚠️ **체크박스는 실행 세션이 남기지 않아 전부 미체크다**(46개). 진행의 정본은 `git log`다.

**Goal:** 누끼 업로드의 축소 기준을 잘린 판이 아니라 원본 사진의 긴 변으로 옮겨, 서버로 나가는 토핑의 바이트를 줄인다.

**Architecture:** 원본 긴 변을 값 클래스 `SourceLongSide`로 감싸 세그멘테이션에서 업로드 경계까지 나른다. 축소 자체는 지금과 같은 자리(`UploadImagePreprocessor`)에서 일어나므로 모델 입력과 로컬 편집용 파일은 원본 해상도로 남는다. `UploadImagePlan.of`가 그 값으로 `1280 ÷ 원본 긴 변` 배율을 정해 잘린 판에 적용한다.

**Tech Stack:** Kotlin, Hilt, DataStore(Preferences) + kotlinx.serialization, MockK, kotlin.test, JUnit4

**Spec:** [`parfait/specs/2026-09-09-topping-upload-source-scaled.md`](../../specs/archive/2026-09-09-topping-upload-source-scaled.md)

## Global Constraints

- 작업 대상 저장소는 **`TJYG-Android`**다. 이 계획 문서가 있는 저장소가 아니다.
- **커밋하지 않는다.** 각 태스크의 커밋 단계는 사용자가 명시적으로 커밋을 요청했을 때만 실행한다. 기본값은 미커밋이다.
- `develop`에서 딴 브랜치에서 작업한다. `main`·`develop`에 직접 커밋하지 않는다. **워크트리를 만들지 않는다** — 본 체크아웃에서 브랜치로 작업한다.
- **주석 규약**(`parfait/CLAUDE.md`): 코드가 이미 말하는 것은 쓰지 않는다. 뻔하지 않은 의도와 함정만 쓴다. **다른 컴포넌트의 현재 상태는 쓰지 않는다** — 낡기 때문이다. 써야 하면 단정 대신 근거 문서를 가리킨다. KDoc에 "의도/반환값/파라미터" 고정 틀을 두지 않는다. `@return`은 타입과 이름이 말하지 못할 때만, `@param`은 이름이 오해를 부를 때만 쓴다.
- 아키텍처 결정은 코드가 아니라 `parfait/adr/`에 쓰고 코드에는 포인터 한 줄만 둔다.
- **매퍼 단독 테스트를 만들지 않는다.** 판단이 든 변환은 DataSource 테스트 케이스로 덮는다.
- 테스트 더블은 저장소 관례대로 **MockK**를 쓴다. 순수 함수 테스트는 `kotlin.test`.
- 상수 값(정본): `NUKKI_SOURCE_LONG_SIDE = 1280`, `NUKKI_LONG_SIDE_LIMIT = 1280`, `NUKKI_MIN_LONG_SIDE = 256`, `BACKGROUND_LONG_SIDE_LIMIT = 2048`, `JPEG_QUALITY = 70`.
- **확대는 어떤 경우에도 하지 않는다.** 이 성질이 재업로드 누적을 막는다.
- 배경(`ImageType.BACKGROUND`) 동작은 바꾸지 않는다.

### 착수 전 확인 (필수)

`parfait/specs/2026-09-09-topping-draft-usecase-extraction.md`는 `status: implemented`지만 이 계획을 쓴 시점(2026-09-09)에 **그 코드는 `develop`에 없다.** 착수 시점에 아래를 실행해 현재 상태를 확인한다.

```bash
ls domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/
```

- `GetToppingDraftFlowUseCase`·`RecordToppingDraftUseCase` 등이 **없으면** 이 계획을 그대로 따른다(ViewModel이 `ToppingDraftRepository`를 직접 받는다).
- **있으면** Task 2·3·4의 `record` 호출부가 `RecordToppingDraftUseCase` 뒤로 옮겨가 있다. 인자 추가 지점이 UseCase 한 겹 늘어나므로, 각 태스크에서 `toppingDraftRepository.record(...)`를 `recordToppingDraftUseCase(...)`로 읽고 UseCase의 시그니처에도 같은 인자를 얹는다.

---

## File Structure

| 파일 | 책임 | 조건 |
|---|---|---|
| `domain/src/main/java/com/teamyg/parfait/domain/model/image/SourceLongSide.kt` | 원본 긴 변 값 클래스 | 신규 |
| `data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt` | 목표 치수 판정 + 상수 소유 | 수정 |
| `data/src/test/java/com/teamyg/parfait/data/model/image/UploadImagePlanTest.kt` | 판정 순수 함수 회귀 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessor.kt` | 업로드 전처리 계약 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessorImpl.kt` | 축소 실행 + 로깅 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt` | 초안 VO | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt` | 초안 저장 형태 + 매퍼 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt` | `record` 계약 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt` | `record` 구현 | 수정 |
| `data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt` | 저장 왕복 + 구버전 호환 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationResult.kt` | 누끼 산출물 경로 + 원본 긴 변 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt` | `persistSubject`가 값 채움 | 수정 |
| `feature/segmentation/impl/.../SegmentationViewModel.kt` | 두 경로에서 `record` | 수정 |
| `feature/segmentation/api/.../NavKeyToppingEdit.kt` | `ToppingEditResult` 필드 | 수정 |
| `feature/segmentation/impl/.../ToppingEditViewModel.kt` | 편집 결과에 값 실음 | 수정 |
| `feature/segmentation/impl/.../SegmentationConfirmViewModel.kt` | 편집 결과를 초안에 반영 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageUploadRepository.kt` | `upload` 계약 | 수정 |
| `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt` | 전처리에 전달 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/UploadImageUseCase.kt` | 배경 경로 — `null` 명시 | 수정 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt` | 값 전달 | 수정 |
| `feature/groups/canvas/impl/.../CanvasToppingPlaceViewModel.kt` | 초안 값을 UseCase로 | 수정 |
| `data/src/test/java/.../repository/image/ImageUploadRepositoryImplTest.kt` | `prepare` 스텁 5곳·`upload` 호출 16곳 | 수정 |
| `data/src/test/java/.../repository/topping/ToppingDraftRepositoryImplTest.kt` | 실구현체 `record` 직접 호출 4곳 | 수정 |
| `domain/src/test/java/.../usecase/image/UploadImageUseCaseTest.kt` | `upload` 스텁·검증 5곳 | 수정 |
| `domain/src/test/java/.../usecase/topping/AddToppingUseCaseTest.kt` | `upload` 4곳 + `invoke` 직접 호출 | 수정 |
| `feature/groups/canvas/impl/src/test/kotlin/.../CanvasBGEditViewModelTest.kt` | `ToppingEditResult` 생성 2곳 | 수정 |

---

## Task 1: 판정 로직과 값 타입

`UploadImagePlan`이 원본 긴 변을 받아 배율을 정하게 한다. 이 태스크가 끝나면 판정은 새 규칙을 따르지만 아직 아무도 실제 값을 넘기지 않아 모든 호출부가 `null`을 넘긴다 — 방어선만 동작한다.

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/SourceLongSide.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessor.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessorImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/model/image/UploadImagePlanTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt`

**Interfaces:**
- Consumes: 없음(첫 태스크)
- Produces:
  - `com.teamyg.parfait.domain.model.image.SourceLongSide` — `@JvmInline value class SourceLongSide(val px: Int)`
  - `UploadImagePlan.of(fileSize: UploadImageSize, imageType: ImageType, sourceFormat: UploadImageFormat, sourceLongSide: SourceLongSide?): UploadImagePlan`
  - `UploadImagePreprocessor.prepare(file: File, imageType: ImageType, sourceLongSide: SourceLongSide?): Result<PreparedUploadImage>`

- [ ] **Step 1: 값 클래스를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/image/SourceLongSide.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

/**
 * 누끼를 오려낸 사진 전체의 긴 변(픽셀). 잘린 알맹이의 긴 변이 아니다.
 *
 * 무엇을 「원본」으로 보는지, 벌거벗은 `Int` 를 쓰지 않는 이유, 0 이하를 「모름」과 같게
 * 취급하는 이유는 `specs/2026-09-09-topping-upload-source-scaled.md`.
 */
@JvmInline
value class SourceLongSide(val px: Int)
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`UploadImagePlanTest.kt`의 기존 테스트 8건은 `of` 인자가 셋이라 컴파일이 깨진다. 기존 8건 전부에 네 번째 인자 `null`을 더하고, 누끼 상한이 1500에서 1280으로 바뀐 것을 반영해 아래 셋을 고친다.

```kotlin
    @Test
    fun of_nukkiPngUnderLimit_passesThrough() {
        // Given 누끼 PNG 가 방어선 이하다
        val fileSize = UploadImageSize(width = 800, height = 1200)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 원본을 그대로 올린다 - 확대도 재인코딩도 하지 않는다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }
```

기존 픽스처 1000x1500 은 긴 변이 새 방어선 1280 을 넘어 더는 통과 갈래가 아니다.

```kotlin
    @Test
    fun of_nukkiPngOverLimit_scalesKeepingRatio() {
        // Given 긴 변이 누끼 방어선 1280 을 넘는데 원본 긴 변은 모른다
        val fileSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 긴 변이 방어선이 되고 짧은 변은 비율을 지킨다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.height)
        assertEquals(868, reencode.targetSize.width)
        assertEquals(UploadImageFormat.PNG, reencode.format)
    }

    @Test
    fun of_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 짧은 변이 비율대로 줄이면 0 이 되는 극단 종횡비다
        val fileSize = UploadImageSize(width = 6000, height = 2)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 0 픽셀 비트맵은 만들 수 없으므로 1 로 바닥을 친다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.width)
        assertEquals(1, reencode.targetSize.height)
    }
```

`of_sampleSizeNeverUndershootsTarget`은 목표만 1280으로 낮아지고 `sampleSize`는 그대로 2다 — 2600/4 = 650 이 목표 폭 868 보다 작아 4 로 못 올라간다. **단언 값을 건드리지 말고** 인자와 변수명만 고친다.

```kotlin
    @Test
    fun of_sampleSizeNeverUndershootsTarget() {
        // Given 큰 사진이다
        val fileSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 사전 축소판이 목표보다 작아지면 안 된다 - 그러면 확대해서 맞추게 된다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2, reencode.sampleSize)
        assertEquals(true, fileSize.width / reencode.sampleSize >= reencode.targetSize.width)
        assertEquals(true, fileSize.height / reencode.sampleSize >= reencode.targetSize.height)
    }
```

`of_limitDiffersByImageType`은 1600x1200이 누끼 1280 초과·배경 2048 이하라 그대로 성립한다. 인자만 더한다.

그리고 새 규칙 케이스 여덟을 더한다.

```kotlin
    @Test
    fun of_nukkiScalesBySourceRatio_notByOwnLongSide() {
        // Given 알맹이는 방어선 아래지만 원본 사진이 크다
        val fileSize = UploadImageSize(width = 925, height = 450)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 알맹이 자신의 긴 변이 아니라 원본 배율(1280/4032)로 줄어든다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(294, reencode.targetSize.width)
        assertEquals(143, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiSourceUnderThreshold_passesThrough() {
        // Given 원본 사진의 긴 변이 이미 1280 이하다
        val fileSize = UploadImageSize(width = 900, height = 700)
        val sourceLongSide = SourceLongSide(1200)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 확대는 어떤 경우에도 하지 않는다 - 재업로드가 누적되지 않는 근거다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_nukkiScaledBelowMinimum_isPulledBackToMinimum() {
        // Given 배율대로면 결과가 하한보다 잘아진다
        val fileSize = UploadImageSize(width = 640, height = 400)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한까지 되돌린다 - 그 아래로는 앱이 실루엣조차 구분하지 못한다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(256, reencode.targetSize.width)
        assertEquals(160, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiMinimumIsMonotonic_noCliffAtTheBoundary() {
        // Given 1px 만 다른 두 알맹이를 같은 원본에서 오려냈다
        val sourceLongSide = SourceLongSide(4032)

        // When 각각 계획을 세운다
        val smaller = UploadImagePlan.of(
            UploadImageSize(640, 400), ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide,
        )
        val larger = UploadImagePlan.of(
            UploadImageSize(641, 400), ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide,
        )

        // Then 큰 알맹이가 작은 알맹이보다 작게 올라가지 않는다 - 하한을 입력에 걸면 깨지던 성질이다
        val smallerTarget = assertIs<UploadImagePlan.Reencode>(smaller).targetSize
        val largerTarget = assertIs<UploadImagePlan.Reencode>(larger).targetSize
        assertEquals(256, smallerTarget.width)
        assertEquals(256, largerTarget.width)
    }

    @Test
    fun of_nukkiMinimumNeverEnlarges() {
        // Given 알맹이 자체가 하한보다 작다
        val fileSize = UploadImageSize(width = 100, height = 80)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한까지 키우지 않는다 - 되돌림의 상한이 알맹이 자신이다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_nukkiWellAboveMinimum_scalesByRatio() {
        // Given 배율대로 줄여도 결과가 하한보다 크다
        val fileSize = UploadImageSize(width = 641, height = 400)
        val sourceLongSide = SourceLongSide(2560)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한이 개입하지 않고 배율만 먹는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(321, reencode.targetSize.width)
        assertEquals(200, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiCorruptSourceSmallerThanFile_neverEnlarges() {
        // Given 원본 긴 변이 알맹이보다 작다고 주장하는 망가진 입력이다
        val fileSize = UploadImageSize(width = 2000, height = 1000)
        val sourceLongSide = SourceLongSide(500)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 배율 갈래를 건너뛰고 방어선만 걸린다 - 확대는 만들지 않는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.width)
        assertEquals(640, reencode.targetSize.height)
    }

    @Test
    fun of_backgroundIgnoresSourceLongSide() {
        // Given 배경인데 원본 긴 변이 실려 왔다
        val fileSize = UploadImageSize(width = 1600, height = 1200)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.BACKGROUND, UploadImageFormat.JPEG, sourceLongSide)

        // Then 배경은 이 값을 보지 않는다 - 2048 이하라 그대로 통과한다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }
```

`import com.teamyg.parfait.domain.model.image.SourceLongSide`를 파일 상단에 더한다.

- [ ] **Step 2b: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*UploadImagePlanTest*"
```

기대: 컴파일 실패. `of` 가 인자 4개를 받지 않는다.

- [ ] **Step 3: 판정 로직을 고친다**

`UploadImagePlan.kt`의 `companion object`를 통째로 아래로 바꾼다.

```kotlin
    companion object {
        /**
         * 누끼 배율의 분자. "원본이 이 크기였다면" 을 기준으로 잘린 판을 줄여, 알맹이의 절대
         * 크기가 아니라 **프레임 안에서 차지하는 비율**을 보존한다.
         *
         * 값의 근거는 `specs/2026-09-09-topping-upload-source-scaled.md`,
         * iOS 를 따르지 않는 근거는 `adr/0032-android-own-topping-upload-scale.md`.
         */
        private const val NUKKI_SOURCE_LONG_SIDE = 1280

        /** 원본 긴 변을 모를 때 걸리는 방어선. 배율 갈래를 지나면 결과가 이미 이 값 이하다 */
        private const val NUKKI_LONG_SIDE_LIMIT = 1280

        /** 축소 **결과**의 하한. 근거는 `specs/2026-09-09-topping-upload-source-scaled.md` 「하한」 */
        private const val NUKKI_MIN_LONG_SIDE = 256

        private const val BACKGROUND_LONG_SIDE_LIMIT = 2048

        /**
         * PNG 는 무손실이라 이 값을 보지 않는다. 배경 한정으로 iOS 와 맞춘 값이다
         * (근거는 `specs/archive/2026-09-08-upload-image-downscale.md` 「결정 표」).
         */
        const val JPEG_QUALITY = 70

        /**
         * 치수와 포맷 둘 다 그대로여도 되는지 판정한다. 어느 한쪽이라도 바뀌어야 다시 굽는다 —
         * 이미 JPEG 이고 상한 이하인 배경을 다시 구우면 손실만 더해진다.
         *
         * @param fileSize 올릴 파일의 치수. 누끼면 여백을 걷어낸 알맹이다
         * @param sourceLongSide 누끼를 오려낸 사진 전체의 긴 변. 배경은 보지 않는다
         */
        fun of(
            fileSize: UploadImageSize,
            imageType: ImageType,
            sourceFormat: UploadImageFormat,
            sourceLongSide: SourceLongSide?,
        ): UploadImagePlan {
            val targetSize = targetSizeOf(fileSize, imageType, sourceLongSide)
            val targetFormat = uploadFormatOf(imageType, sourceFormat)

            if (targetSize == fileSize && targetFormat == sourceFormat) return Passthrough

            return Reencode(
                targetSize = targetSize,
                sampleSize = sampleSizeOf(fileSize, targetSize),
                format = targetFormat,
            )
        }

        private fun targetSizeOf(
            fileSize: UploadImageSize,
            imageType: ImageType,
            sourceLongSide: SourceLongSide?,
        ): UploadImageSize = when (imageType) {
            ImageType.BACKGROUND -> scaledSize(fileSize, BACKGROUND_LONG_SIDE_LIMIT)
            // 방어선을 겹치는 이유는 배율 갈래가 아니라 그것을 건너뛴 갈래들 때문이다
            ImageType.NUKKI -> scaledSize(scaledBySource(fileSize, sourceLongSide), NUKKI_LONG_SIDE_LIMIT)
        }

        /**
         * 원본이 [NUKKI_SOURCE_LONG_SIDE] 였다면 이 알맹이가 가졌을 크기. 결과가 너무 잘아지면
         * [NUKKI_MIN_LONG_SIDE] 까지 되돌리되, **되돌림의 상한이 `fileSize` 자신이라 확대가 아니다.**
         *
         * 하한을 입력이 아니라 결과에 거는 이유는 `specs/…-topping-upload-source-scaled.md` 「하한」.
         */
        private fun scaledBySource(
            fileSize: UploadImageSize,
            sourceLongSide: SourceLongSide?,
        ): UploadImageSize {
            if (sourceLongSide == null || sourceLongSide.px <= NUKKI_SOURCE_LONG_SIDE) return fileSize

            val fileLongSide = maxOf(fileSize.width, fileSize.height)
            val scaledLongSide = fileLongSide.toDouble() * NUKKI_SOURCE_LONG_SIDE / sourceLongSide.px
            val targetLongSide = scaledLongSide
                .coerceAtLeast(NUKKI_MIN_LONG_SIDE.toDouble())
                .coerceAtMost(fileLongSide.toDouble())
            if (targetLongSide >= fileLongSide) return fileSize

            val ratio = targetLongSide / fileLongSide
            return UploadImageSize(
                width = (fileSize.width * ratio).roundToInt().coerceAtLeast(1),
                height = (fileSize.height * ratio).roundToInt().coerceAtLeast(1),
            )
        }

        /** 배경은 캔버스를 덮는 불투명 이미지라 알파를 버려도 잃는 것이 없다 */
        private fun uploadFormatOf(
            imageType: ImageType,
            sourceFormat: UploadImageFormat,
        ): UploadImageFormat = when (imageType) {
            ImageType.NUKKI -> sourceFormat
            ImageType.BACKGROUND -> UploadImageFormat.JPEG
        }

        /** 확대는 정보를 늘리지 않으면서 바이트만 키운다 */
        private fun scaledSize(
            fileSize: UploadImageSize,
            longSideLimit: Int,
        ): UploadImageSize {
            val longSide = maxOf(fileSize.width, fileSize.height)
            if (longSide <= longSideLimit) return fileSize

            val ratio = longSideLimit.toDouble() / longSide
            return UploadImageSize(
                width = (fileSize.width * ratio).roundToInt().coerceAtLeast(1),
                height = (fileSize.height * ratio).roundToInt().coerceAtLeast(1),
            )
        }

        /**
         * 목표보다 작아지지 않는 선까지만 2 의 거듭제곱으로 줄인다. 넘겨서 줄이면 뒤에서 확대하게 되고,
         * `BitmapFactory` 는 2 의 거듭제곱이 아닌 값을 그 아래 거듭제곱으로 내림한다.
         */
        private fun sampleSizeOf(
            fileSize: UploadImageSize,
            targetSize: UploadImageSize,
        ): Int {
            var sampleSize = 1
            while (fileSize.width / (sampleSize * 2) >= targetSize.width &&
                fileSize.height / (sampleSize * 2) >= targetSize.height
            ) {
                sampleSize *= 2
            }
            return sampleSize
        }
    }
```

파일 상단 import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`를 더한다.

- [ ] **Step 4: 전처리기 계약과 구현에 인자를 얹는다**

`UploadImagePreprocessor.kt`:

```kotlin
/** 업로드 직전에 이미지를 서버로 보낼 형태로 맞춘다 */
interface UploadImagePreprocessor {
    /**
     * @param sourceLongSide 모르면 null 이다 — 그때는 잘린 판 상한만 걸린다
     */
    suspend fun prepare(
        file: File,
        imageType: ImageType,
        sourceLongSide: SourceLongSide?,
    ): Result<PreparedUploadImage>
}
```

`UploadImagePreprocessorImpl.kt`의 `prepare` 시그니처에 같은 인자를 더하고, `UploadImagePlan.of` 호출을 고친다.

```kotlin
    override suspend fun prepare(
        file: File,
        imageType: ImageType,
        sourceLongSide: SourceLongSide?,
    ): Result<PreparedUploadImage> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFormat = UploadImageFormat.ofExtension(file.extension)
                ?: throw UnsupportedImageException("서버가 받지 않는 확장자다 - ${file.extension}")
            val fileSize = decodeSize(file)

            when (val plan = UploadImagePlan.of(fileSize, imageType, sourceFormat, sourceLongSide)) {
                UploadImagePlan.Passthrough -> {
                    PreparedUploadImage(file = file, format = sourceFormat, isTemporary = false)
                }

                is UploadImagePlan.Reencode -> {
                    val reencoded = writeReencoded(file, fileSize, plan)
                    sourceLogger.i {
                        "업로드 이미지를 줄였다 - ${fileSize.width}x${fileSize.height} ${file.length()}B " +
                            "→ ${reencoded.size.width}x${reencoded.size.height} ${reencoded.file.length()}B " +
                            "(${plan.format.contentType}, 회전 ${reencoded.rotationDegrees}도, " +
                            "원본 긴 변 ${sourceLongSide?.px ?: "모름"}, " +
                            "배율 ${maxOf(plan.targetSize.width, plan.targetSize.height).toDouble() /
                                maxOf(fileSize.width, fileSize.height)})"
                    }
                    PreparedUploadImage(file = reencoded.file, format = plan.format, isTemporary = true)
                }
            }
        }
    }
```

`writeReencoded`·`decodeDownscaled`의 `sourceSize` 파라미터 이름도 `fileSize`로 함께 바꾼다. 두 함수 안의 `sourceSize` 참조를 전부 치환하되 **로직은 건드리지 않는다.**

파일 상단 import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`를 더한다.

- [ ] **Step 5: 호출부를 임시로 `null`로 맞춘다**

`ImageUploadRepositoryImpl.kt`의 `prepare` 호출을 고친다. 실제 값 배선은 Task 5에서 한다.

```kotlin
        val prepared = uploadImagePreprocessor
            .prepare(file = file, imageType = imageType, sourceLongSide = null)
            .getOrElse { return Result.failure(it.toAppError()) }
```

- [ ] **Step 6: 깨진 스텁을 고친다**

Step 5가 `prepare` 호출을 바꾸는 순간 `ImageUploadRepositoryImplTest`의 스텁 다섯이 컴파일에서 깨진다.

```kotlin
coEvery { uploadImagePreprocessor.prepare(any(), any()) } returns ...
```

전부 인자 셋으로 넓힌다.

```kotlin
coEvery { uploadImagePreprocessor.prepare(any(), any(), any()) } returns ...
```

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

`UploadImagePlanTest`만 돌리면 위 스텁 파손을 놓친다. `:data` 유닛 전체를 돌린다.

```bash
./gradlew :data:testDebugUnitTest
```

기대: PASS. `UploadImagePlanTest`는 기존 8건 + 신규 8건이다.

- [ ] **Step 8: 모듈 전체를 빌드한다**

```bash
./gradlew :data:compileDebugKotlin :domain:compileDebugKotlin
```

기대: BUILD SUCCESSFUL.

- [ ] **Step 9: 커밋 — 사용자가 요청했을 때만**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/image/SourceLongSide.kt \
        data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt \
        data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessor.kt \
        data/src/main/java/com/teamyg/parfait/data/utils/image/UploadImagePreprocessorImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt \
        data/src/test/java/com/teamyg/parfait/data/model/image/UploadImagePlanTest.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt
git commit -m "feat: 누끼 업로드 목표 치수를 원본 긴 변으로 정한다"
```

---

## Task 2: 초안이 원본 긴 변을 들고 다니게 한다

초안 스키마에 값을 얹고 저장·복원을 덮는다. 이 태스크가 끝나도 값을 채우는 곳은 없다 — 모든 호출부가 `null`을 넘긴다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModel.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `SourceLongSide` (Task 1)
- Produces:
  - `ToppingDraft.sourceLongSide: SourceLongSide?`
  - `ToppingDraftRepository.record(subjectImagePath: String, cutoutImagePath: String?, borderColorArgb: Int?, borderWidthDp: Float?, sourceLongSide: SourceLongSide?): Boolean`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ToppingDraftLocalDataSourceImplTest.kt`는 `FakePreferencesDataStore`를 쓰고 `filledDraft` 픽스처 하나로 왕복을 덮는다. 그 픽스처에 새 필드를 얹으면 기존 `save_thenRead_roundTripsEveryField`가 그대로 회귀가 된다 — 그 테스트의 주석이 이미 "값 클래스 둘과 널 넷을 거쳐 오므로 매퍼가 뒤집혀도 컴파일러가 막지 못한다"고 이유를 적고 있다.

```kotlin
    private val filledDraft = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 4,
        subjectImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/subject.png",
        cutoutImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/cutout.png",
        borderColorArgb = 0xFFFF6B6B.toInt(),
        borderWidthDp = 4f,
        sourceLongSide = SourceLongSide(4032),
    )
```

그리고 구버전 호환 케이스 하나를 새로 더한다. 이 저장소는 `Json { ignoreUnknownKeys = true }`로 디코딩하고 새 필드에 기본값이 있으므로 옛 JSON이 살아 돌아와야 한다.

```kotlin
    @Test
    fun read_legacyJsonWithoutSourceLongSide_keepsDraftAndNullsTheField() = runTest {
        // Given 이 필드가 생기기 전에 저장된 JSON 이 남아 있다
        val legacyJson = """{"groupId":1,"parfaitId":2,"nextPositionZ":4,""" +
            """"subjectImagePath":"/cache/segmentation/subject.png"}"""
        dataStore.putRaw(ToppingDraftLocalDataSourceImpl.TOPPING_DRAFT_KEY_NAME, legacyJson)

        // When 읽는다
        val restored = dataSource.draft.first()

        // Then 초안을 통째로 버리지 않고 이 필드만 비운다 - 흐름 도중 앱을 껐다 켠 사용자를 잃지 않는다
        assertEquals("/cache/segmentation/subject.png", restored?.subjectImagePath)
        assertNull(restored?.sourceLongSide)
    }
```

`putRaw`는 `FakePreferencesDataStore`가 이미 가진 헬퍼이고, 같은 파일의 `draft_storedFormatIsUnreadable_isNull`이 그 방식을 쓴다. import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`만 더하면 된다 — `assertNull`은 이미 있다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*ToppingDraftLocalDataSourceImplTest*"
```

기대: 컴파일 실패. `ToppingDraft` 에 `sourceLongSide` 가 없다.

- [ ] **Step 3: VO와 저장 형태에 필드를 더한다**

`ToppingDraft.kt`:

```kotlin
/**
 * @param cutoutImagePath 재편집 시작 마스크. 좌표계를 지켜야 해 트리밍하지 않는다.
 * @param sourceLongSide 알맹이를 오려낸 사진 전체의 긴 변. 업로드 배율의 분모다
 */
data class ToppingDraft(
    val groupId: GroupId,
    val parfaitId: ParfaitId,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    val sourceLongSide: SourceLongSide? = null,
)
```

기존 `@param subjectImagePath` 줄은 그대로 둔다. import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`를 더한다.

`ToppingDraftEntity.kt`:

```kotlin
@Serializable
internal data class ToppingDraftEntity(
    val groupId: Long,
    val parfaitId: Long,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    val sourceLongSide: Int? = null,
)

internal fun ToppingDraft.toEntity(): ToppingDraftEntity = ToppingDraftEntity(
    groupId = groupId.value,
    parfaitId = parfaitId.value,
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
    sourceLongSide = sourceLongSide?.px,
)

internal fun ToppingDraftEntity.toVO(): ToppingDraft = ToppingDraft(
    groupId = GroupId(groupId),
    parfaitId = ParfaitId(parfaitId),
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
    sourceLongSide = sourceLongSide?.let(::SourceLongSide),
)
```

import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`를 더한다.

- [ ] **Step 4: `record` 계약과 구현을 고친다**

`ToppingDraftRepository.kt`의 `record`에 인자를 더한다.

```kotlin
    suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
        sourceLongSide: SourceLongSide?,
    ): Boolean
```

`ToppingDraftRepositoryImpl.kt`:

```kotlin
    override suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
        sourceLongSide: SourceLongSide?,
    ): Boolean {
        val current = toppingDraftLocalDataSource.draft.first() ?: return false

        toppingDraftLocalDataSource.save(
            current.copy(
                subjectImagePath = subjectImagePath,
                cutoutImagePath = cutoutImagePath,
                borderColorArgb = borderColorArgb,
                borderWidthDp = borderWidthDp,
                sourceLongSide = sourceLongSide,
            ),
        )
        return true
    }
```

두 파일 모두 import에 `SourceLongSide`를 더한다.

- [ ] **Step 5: 호출부 넷을 임시로 `null`로 맞춘다**

`SegmentationViewModel.kt`의 두 `record` 호출과 `SegmentationConfirmViewModel.kt`의 두 `record` 호출에 `sourceLongSide = null`을 더한다. 실제 값은 Task 3·4에서 채운다.

- [ ] **Step 6: 기존 테스트를 넓힌다**

세 파일이 깨진다.

- `SegmentationViewModelTest.kt`·`SegmentationConfirmViewModelTest.kt` — `record(any(), any(), any(), any())`를 전부 `record(any(), any(), any(), any(), any())`로 바꾼다. 위치 인자로 단언하는 곳(`record(REUSED_PATH, null, null, null)`)은 `record(REUSED_PATH, null, null, null, null)`로 바꾼다.
- `ToppingDraftRepositoryImplTest.kt` — **mockk 스텁이 아니라 실구현체를 직접 부르는** 호출 넷이 있다. 각 호출에 `sourceLongSide = null`을 더한다. 이 파일은 초안 왕복이 아니라 `record`의 계약(초안이 없으면 `false`)을 보는 곳이라 값 자체를 단언할 필요는 없다.

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest
```

기대: 둘 다 PASS. `:data` 를 통째로 도는 이유는 `ToppingDraftRepositoryImplTest` 파손을 놓치지 않기 위해서다.

- [ ] **Step 8: 커밋 — 사용자가 요청했을 때만**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt \
        domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt \
        data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt \
        data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt \
        feature/segmentation/impl/src
git commit -m "feat: 토핑 초안이 원본 긴 변을 들고 다니게 한다"
```

---

## Task 3: 세그멘테이션이 원본 긴 변을 채운다

자동 누끼와 "편집 없이 사용" 두 경로가 실제 값을 초안에 적는다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationResult.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `SourceLongSide` (Task 1), `ToppingDraftRepository.record(..., sourceLongSide)` (Task 2)
- Produces: `SegmentationResult.sourceLongSide: SourceLongSide`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationViewModelTest.kt`에 더한다. 기존 테스트가 `SegmentationResult`를 어떤 헬퍼로 세우는지 먼저 읽고 그 방식에 맞춘다.

```kotlin
    @Test
    fun selectCandidate_recordsSourceLongSideFromResult() = runTest {
        // Given 누끼 저장이 원본 긴 변을 함께 돌려준다
        coEvery { persistSubject(any()) } returns Result.success(
            SegmentationResult(
                subjectImagePath = "/cache/canvas.png",
                trimmedSubjectImagePath = "/cache/trimmed.png",
                sourceLongSide = SourceLongSide(4032),
            ),
        )
        coEvery { toppingDraftRepository.record(any(), any(), any(), any(), any()) } returns true

        // When 후보를 고른다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 그 값이 초안에 실린다
        coVerify {
            toppingDraftRepository.record(
                subjectImagePath = "/cache/trimmed.png",
                cutoutImagePath = "/cache/canvas.png",
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = SourceLongSide(4032),
            )
        }
    }
```

그리고 「편집 없이 사용」 경로도 덮는다. 그 경로는 값을 조립하는 방식이 달라(캐스팅이 실패하면 `null`이 흐른다) 검증이 가장 필요한 자리다.

```kotlin
    @Test
    fun useOriginal_recordsSourceLongSideFromOriginBitmap() = runTest {
        // Given 원본 판을 그대로 토핑 재료로 쓴다
        coEvery { toppingDraftRepository.record(any(), any(), any(), any(), any()) } returns true

        // When 편집 없이 사용을 고른다
        viewModel.processIntent(SegmentationIntent.ClickUseOriginal)
        advanceUntilIdle()

        // Then 원본이 곧 알맹이라 그 비트맵의 긴 변이 실린다
        coVerify {
            toppingDraftRepository.record(
                subjectImagePath = any(),
                cutoutImagePath = any(),
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = SourceLongSide(ORIGIN_LONG_SIDE),
            )
        }
    }
```

인텐트 이름과 원본 비트맵 픽스처는 같은 파일의 기존 「편집 없이 사용」 테스트에서 그대로 가져온다. `ORIGIN_LONG_SIDE`는 그 픽스처 비트맵의 긴 변이다.

후보를 상태에 올려 두는 준비 단계는 같은 파일의 기존 후보 선택 테스트를 그대로 복사한다. `persistSubject`는 이 파일이 세운 mockk 프로퍼티 이름이다 — UseCase 타입 이름이 아니다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"
```

기대: 컴파일 실패. `SegmentationResult` 에 `sourceLongSide` 가 없다.

- [ ] **Step 3: 산출물 모델에 필드를 더한다**

`SegmentationResult.kt`:

```kotlin
package com.teamyg.parfait.domain.model

import com.teamyg.parfait.domain.model.image.SourceLongSide

data class SegmentationResult(
    /** 원본과 같은 캔버스 크기의 객체 이미지. 수동 편집 화면이 원본과 픽셀 단위로 맞춰 그리는 데 쓴다 */
    val subjectImagePath: String,
    /** 투명한 여백을 걷어내 객체 크기만 남긴 이미지. 미리보기·배치처럼 실제 보이는 크기가 필요할 때 쓴다 */
    val trimmedSubjectImagePath: String,
    /** 위 둘을 오려낸 사진 전체의 긴 변. 업로드 배율의 분모다 */
    val sourceLongSide: SourceLongSide,
)
```

- [ ] **Step 4: `persistSubject`가 값을 채우게 한다**

`ImageSegmentationRepositoryImpl.kt`의 `persistSubject` 안, `SegmentationResult`를 만드는 자리를 고친다.

```kotlin
                Result.success(
                    SegmentationResult(
                        subjectImagePath = subjectFile.absolutePath,
                        trimmedSubjectImagePath = trimmedFile.absolutePath,
                        sourceLongSide = SourceLongSide(
                            maxOf(candidate.canvasWidth, candidate.canvasHeight),
                        ),
                    ),
```

`subjectFile`·`trimmedFile` 변수명은 현재 코드의 것을 그대로 쓴다. import에 `SourceLongSide`를 더한다.

- [ ] **Step 5: 두 경로가 값을 적게 한다**

`SegmentationViewModel.kt`의 자동 누끼 경로(`persistSubjectUseCase` 성공 갈래)에서 `record` 호출을 고친다.

```kotlin
                        toppingDraftRepository.record(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
                            sourceLongSide = result.sourceLongSide,
                        )
```

`useOriginal()` 경로는 원본 판을 그대로 쓰므로 그 비트맵의 긴 변이 곧 원본 긴 변이다. `saveBitmapUseCase` 호출 앞에 값을 구해 둔다.

```kotlin
            // 이 경로는 원본이 곧 알맹이라 사진 전체의 긴 변이 비트맵의 긴 변이다
            val sourceLongSide = (originBitmapWrapper as? AndroidBitmap)
                ?.getRawData()
                ?.let { SourceLongSide(maxOf(it.width, it.height)) }

            val path = saveBitmapUseCase(originBitmapWrapper).getOrElse {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
                return@launch
            }

            val recorded = runSuspendCatching {
                toppingDraftRepository.record(
                    subjectImagePath = path,
                    cutoutImagePath = path,
                    borderColorArgb = null,
                    borderWidthDp = null,
                    sourceLongSide = sourceLongSide,
                )
            }.getOrDefault(false)
```

⚠️ `BitmapWrapper` 인터페이스에는 `width`·`height`가 **없다**(본문이 비어 있는 인터페이스다). 그래서 `AndroidBitmap`으로 캐스팅해 `getRawData()`로 실제 비트맵을 꺼내야 한다. `AndroidBitmap`은 이 파일이 이미 import하고 있고 `loadCandidates()`가 같은 패턴을 쓰고 있으니 그대로 따른다. 캐스팅이 실패하면 `null`이 흘러가고 방어선이 받는다.

`import`에 `SourceLongSide`를 더한다. `AndroidBitmap`은 이미 있다.

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest :data:testDebugUnitTest
```

기대: PASS.

- [ ] **Step 7: 커밋 — 사용자가 요청했을 때만**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/SegmentationResult.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt \
        feature/segmentation/impl/src
git commit -m "feat: 세그멘테이션 산출물에 원본 긴 변을 싣는다"
```

---

## Task 4: 수동 편집 경로가 값을 잇는다

C-104 편집을 거친 알맹이도 같은 배율을 받아야 한다. 편집 결과의 cutout이 원본 크기 판이므로 그 긴 변이 곧 원본 긴 변이다.

**Files:**
- Modify: `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api/NavKeyToppingEdit.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModelTest.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: `SourceLongSide` (Task 1), `record(..., sourceLongSide)` (Task 2)
- Produces: `ToppingEditResult.sourceLongSide: SourceLongSide`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`SegmentationConfirmViewModelTest.kt`에 더한다.

```kotlin
    @Test
    fun onEditResult_recordsSourceLongSideFromResult() = runTest {
        // Given 편집 결과가 원본 긴 변을 함께 돌려준다
        coEvery { toppingDraftRepository.record(any(), any(), any(), any(), any()) } returns true

        // When 편집 결과를 받는다
        viewModel.processIntent(
            SegmentationConfirmIntent.OnEditResult(
                ToppingEditResult(
                    subjectImagePath = "/cache/edited-trimmed.png",
                    cutoutImagePath = "/cache/edited-canvas.png",
                    borderLayers = emptyList(),
                    sourceLongSide = SourceLongSide(3024),
                ),
            ),
        )
        advanceUntilIdle()

        // Then 그 값이 초안에 실린다
        coVerify {
            toppingDraftRepository.record(
                subjectImagePath = "/cache/edited-trimmed.png",
                cutoutImagePath = "/cache/edited-canvas.png",
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = SourceLongSide(3024),
            )
        }
    }

```

재사용 진입은 **새 테스트를 만들지 않는다.** 그 경로는 인텐트가 아니라 `init` 블록에서 일어나고, `SegmentationConfirmIntent`에는 진입 인텐트 자체가 없다(`OnEditResult`와 `OnConfirmTutorial` 둘뿐이다). 대신 이미 그것을 검증하는 기존 단언 셋을 고친다.

```kotlin
            toppingDraftRepository.record(REUSED_PATH, null, null, null, null)
```

마지막 `null`이 재사용 진입의 `sourceLongSide`다. 그 알맹이를 오려낸 사진의 치수를 알 방법이 없어 배율을 지어내지 않는다. Task 2에서 위치 인자를 다섯으로 넓힐 때 이미 같은 모양이 되므로, 이 태스크에서는 **그 단언이 여전히 살아 있는지 확인만 한다.**

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationConfirmViewModelTest*"
```

기대: 컴파일 실패. `ToppingEditResult` 에 `sourceLongSide` 가 없다.

- [ ] **Step 3: 결과 타입에 필드를 더한다**

`NavKeyToppingEdit.kt`의 `ToppingEditResult`:

```kotlin
/**
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 * @param sourceLongSide 원본 사진이 남아 있지 않은 진입에서는 null 이다
 */
data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
    val sourceLongSide: SourceLongSide?,
)
```

import에 `com.teamyg.parfait.domain.model.image.SourceLongSide`를 더한다.

- [ ] **Step 4: 편집 화면이 값을 싣게 한다**

`ToppingEditViewModel.kt`의 `completeEdit()`에서 결과를 만드는 자리를 고친다. `cutout`은 원본 크기 판이므로 그 치수를 그대로 쓴다.

```kotlin
            // borderOnly 진입의 cutout 은 사진이 아니라 되살린 알맹이라 배율의 분모가 못 된다.
            // 그 밖의 진입에서는 cutout 이 원본 좌표계를 유지한 판이라 긴 변이 그대로 쓰인다
            val sourceLongSide = if (current.isBorderOnly) {
                null
            } else {
                SourceLongSide(maxOf(cutout.width, cutout.height))
            }
```

`cutout.recycle()`이 불리기 **전에** 이 줄을 두어야 한다. 재활용된 비트맵은 치수를 읽을 수 없다. `trimmedCutout` 계산 직후, `saveBitmapUseCase` 블록 앞이 안전한 자리다.

⚠️ **`borderOnly` 갈래를 반드시 가른다.** 이 화면은 두 방향에서 열린다. 하나는 사진에서 딴 누끼를 다듬는 경우이고, 다른 하나는 최근 목록에서 되살린 알맹이의 테두리만 고치는 경우다. 뒤엣것은 `cutout`이 사진이 아니라 알맹이 자신이라, 그 긴 변을 분모로 쓰면 배율이 항상 1에 가까워져 규칙이 무력해진다. 상태의 `isBorderOnly`가 두 경우를 이미 가르고 있다.

그리고 결과를 만들 때 실어 보낸다.

```kotlin
            postSideEffect(
                ToppingEditEffect.EditCompleted(
                    ToppingEditResult(
                        subjectImagePath = subjectPath,
                        cutoutImagePath = cutoutPath,
                        borderLayers = current.borderLayers,
                        sourceLongSide = sourceLongSide,
                    ),
                ),
            )
```

import에 `SourceLongSide`를 더한다.

- [ ] **Step 5: 확인 화면이 값을 초안에 넘기게 한다**

`SegmentationConfirmViewModel.kt`의 `record(result: ToppingEditResult)`를 고친다.

```kotlin
            val recorded = toppingDraftRepository.record(
                subjectImagePath = result.subjectImagePath,
                cutoutImagePath = result.cutoutImagePath,
                borderColorArgb = border?.colorArgb,
                borderWidthDp = border?.widthDp,
                sourceLongSide = result.sourceLongSide,
            )
```

같은 파일의 재사용 진입 갈래(`isReuseEntry`)는 **`null`을 그대로 둔다.** 이유는 "이미 축소된 파일이라서"가 **아니다** — 최근 목록에 남는 것은 업로드 전 원본 해상도 판이다(`addRecentImageUseCase(source = imagePath)`가 초안의 `subjectImagePath`를 그대로 넘긴다). 진짜 이유는 그 알맹이를 오려낸 **사진의 치수를 알 방법이 없다**는 것이다. 그 자리에 이유를 한 줄 남긴다.

```kotlin
                    val recorded = toppingDraftRepository.record(
                        subjectImagePath = subjectImagePath,
                        cutoutImagePath = null,
                        borderColorArgb = null,
                        borderWidthDp = null,
                        // 최근 목록에서 되살린 알맹이는 세그멘테이션을 타지 않아 오려낸 사진의
                        // 치수를 알 방법이 없다. 배율을 지어내지 않고 방어선에만 맡긴다
                        sourceLongSide = null,
                    )
```

- [ ] **Step 6: 다른 모듈의 깨진 생성부를 고친다**

`ToppingEditResult`에 기본값을 두지 않으므로, 그것을 만드는 테스트가 전부 깨진다. `CanvasBGEditViewModelTest.kt`에 생성부가 둘 있다. 각각에 `sourceLongSide = null`을 더한다. 그 파일이 다루는 것은 캔버스에 이미 놓인 토핑의 `borderOnly` 편집이라, 값이 널인 것이 그 경로의 실제 모습이다.

`ToppingEditResult`를 **읽기만** 하는 `CanvasBGEditViewModel.handleOnToppingEditResult`와 `CanvasBGEditRoute`는 고칠 것이 없다.

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest
```

기대: 둘 다 PASS.

- [ ] **Step 8: 커밋 — 사용자가 요청했을 때만**

```bash
git add feature/segmentation/api/src feature/segmentation/impl/src feature/groups/canvas/impl/src
git commit -m "feat: 수동 편집 결과에도 원본 긴 변을 잇는다"
```

---

## Task 5: 업로드 경계까지 배선한다

초안에 실린 값이 실제로 전처리기까지 도달하게 한다. 이 태스크가 끝나면 규칙이 처음으로 동작한다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageUploadRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImpl.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/UploadImageUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModelTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/ImageUploadRepositoryImplTest.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/image/UploadImageUseCaseTest.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCaseTest.kt`

**Interfaces:**
- Consumes: `ToppingDraft.sourceLongSide` (Task 2), `UploadImagePreprocessor.prepare(..., sourceLongSide)` (Task 1)
- Produces: 없음(마지막 소비자)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasToppingPlaceViewModelTest.kt`에 더한다. 초안 더블을 세우는 방식은 같은 파일의 기존 테스트를 따른다.

```kotlin
    @Test
    fun confirm_passesDraftSourceLongSideToUseCase() = runTest {
        // Given 초안이 원본 긴 변을 들고 있다
        every { toppingDraftRepository.draft } returns flowOf(
            draft().copy(sourceLongSide = SourceLongSide(4032)),
        )

        // When 배치를 확정한다
        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 그 값이 업로드까지 내려간다
        coVerify {
            addToppingUseCase(
                groupId = any(),
                parfaitId = any(),
                filePath = any(),
                transform = any(),
                border = any(),
                sourceLongSide = SourceLongSide(4032),
            )
        }
    }
```

`draft(...)`는 이 파일에 이미 있는 초안 헬퍼다. 그 헬퍼에 `sourceLongSide` 파라미터를 새로 뚫지 말고 `copy`로 얹는다 — 헬퍼 시그니처를 넓히면 이 값을 안 보는 기존 테스트까지 흔들린다. 캔버스 크기·토핑 크기가 준비되어야 확정이 진행되므로 기존 확정 테스트의 준비 단계를 그대로 복사한다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"
```

기대: 컴파일 실패. `addToppingUseCase` 에 `sourceLongSide` 인자가 없다.

- [ ] **Step 3: 업로드 계약에 인자를 더한다**

`ImageUploadRepository.kt`:

```kotlin
    /**
     * @param filePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
     * @param sourceLongSide 모르면 null 이다 — 그때는 잘린 판 상한만 걸린다
     */
    suspend fun upload(
        filePath: String,
        imageType: ImageType,
        sourceLongSide: SourceLongSide?,
    ): Result<ImageId>
```

기본값을 두지 않는다. 새 호출부가 이 값을 빠뜨리면 컴파일에서 드러나야 한다.

`ImageUploadRepositoryImpl.kt`의 `upload`에 같은 인자를 더하고 전처리기에 넘긴다.

```kotlin
        val prepared = uploadImagePreprocessor
            .prepare(file = file, imageType = imageType, sourceLongSide = sourceLongSide)
            .getOrElse { return Result.failure(it.toAppError()) }
```

두 파일 모두 import에 `SourceLongSide`를 더한다.

- [ ] **Step 4: 배경 경로가 `null`을 명시하게 한다**

`UploadImageUseCase.kt`:

```kotlin
        // 배경은 원본 배율을 쓰지 않는다
        // (`specs/2026-09-09-topping-upload-source-scaled.md` 「동작 / 상태」)
        return imageUploadRepository.upload(
            filePath = filePath,
            imageType = imageType,
            sourceLongSide = null,
        )
```

- [ ] **Step 5: 토핑 UseCase가 값을 나르게 한다**

`AddToppingUseCase.kt`의 `invoke`에 인자를 더하고 그대로 넘긴다.

```kotlin
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        filePath: String,
        transform: ToppingTransform,
        border: ToppingBorder,
        sourceLongSide: SourceLongSide?,
    ): Result<PlacedToppingVO> {
        val imageId = imageUploadRepository
            .upload(filePath = filePath, imageType = ImageType.NUKKI, sourceLongSide = sourceLongSide)
            .getOrElse { throwable -> return Result.failure(throwable) }
```

import에 `SourceLongSide`를 더한다.

- [ ] **Step 6: 배치 화면이 초안 값을 읽어 넘기게 한다**

`CanvasToppingPlaceViewModel.kt`에서 초안을 구독하는 자리(`toppingImagePath = draft?.subjectImagePath`가 있는 곳)에 값을 함께 담는다. UI 상태에 `toppingSourceLongSide: SourceLongSide? = null` 필드를 더하고 같은 자리에서 채운다.

```kotlin
                        toppingImagePath = draft?.subjectImagePath,
                        toppingSourceLongSide = draft?.sourceLongSide,
```

확정 시 UseCase로 넘긴다.

```kotlin
                addToppingUseCase(
                    groupId = groupId,
                    parfaitId = parfaitId,
                    filePath = imagePath,
                    transform = transform,
                    border = border,
                    sourceLongSide = current.toppingSourceLongSide,
                )
```

`current`는 그 함수가 이미 들고 있는 상태 스냅샷이다. 새로 읽지 않는다 — 확정 중 초안이 바뀌면 경로와 배율이 어긋난다.

import에 `SourceLongSide`를 더한다.

- [ ] **Step 7: 깨진 테스트 넷을 고친다**

`upload`와 `AddToppingUseCase.invoke`의 시그니처가 넓어져 네 파일이 깨진다. 전부 **스텁·호출에 인자 하나를 더하는 기계적 수정**이다.

| 파일 | 무엇을 고치나 |
|---|---|
| `ImageUploadRepositoryImplTest.kt` | `repository.upload(filePath = …, imageType = …)` 호출 열여섯에 `sourceLongSide = null`을 더한다 |
| `UploadImageUseCaseTest.kt` | `imageUploadRepository.upload(any(), any())` 스텁·검증 다섯을 `upload(any(), any(), any())`로 넓힌다 |
| `AddToppingUseCaseTest.kt` | `upload` 스텁 넷을 넓히고, UseCase를 직접 부르는 헬퍼에 `sourceLongSide = null`을 더한다 |
| `CanvasToppingPlaceViewModelTest.kt` | `addToppingUseCase(...)` 스텁·검증에 `sourceLongSide = any()`를 더한다 |

`ImageUploadRepositoryImplTest`가 `upload`에 `null`을 넘겨도 되는 이유는 그 파일이 보는 것이 발급·전송·확인 3단계의 순서와 실패 전파이지 축소 배율이 아니어서다. 배율은 `UploadImagePlanTest`가 순수 함수로 덮는다.

- [ ] **Step 8: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest :domain:testDebugUnitTest :data:testDebugUnitTest
```

기대: 전부 PASS.

- [ ] **Step 9: 전체 유닛 테스트를 돌린다**

```bash
./gradlew testDebugUnitTest
```

기대: BUILD SUCCESSFUL.

- [ ] **Step 10: 커밋 — 사용자가 요청했을 때만**

```bash
git add domain/src data/src feature/groups/canvas/impl/src
git commit -m "feat: 초안의 원본 긴 변을 업로드 경계까지 잇는다"
```

---

## Task 6: 문서 포인터를 맞춘다

코드에 남은 「iOS 와 맞춘 값」 근거를 걷고 실측 절차를 남긴다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt`

**Interfaces:**
- Consumes: Task 1의 상수 블록
- Produces: 없음

- [ ] **Step 1: 누끼 상한 주석에서 iOS 근거를 걷는다**

Task 1에서 이미 새 주석을 넣었다면 이 단계는 확인만 한다. `UploadImagePlan.kt`에 아래 문장이 **남아 있지 않아야** 한다.

```
iOS 와 맞춘 값이라 한쪽만 바꾸면 같은 캔버스가 기기별로 다른 화질이 된다
```

누끼 상수 셋은 `specs/2026-09-09-topping-upload-source-scaled.md`와 `adr/0032-android-own-topping-upload-scale.md`를 가리키고, `JPEG_QUALITY`만 배경 한정으로 iOS 정합을 언급한다.

```bash
grep -n "iOS" data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt
```

기대: `JPEG_QUALITY` 주석 한 줄만 걸린다.

- [ ] **Step 2: 실기기에서 로그를 확인한다**

앱을 설치하고 토핑을 하나 올린 뒤 로그를 본다.

```bash
./gradlew :app:installDebug
adb logcat -s ParfaitSource | grep "업로드 이미지를 줄였다"
```

기대: 축소 전후 치수·바이트와 원본 긴 변이 한 줄에 찍힌다. 스펙 「배경」의 3건과 비교할 실측치를 여기서 얻는다.

- [ ] **Step 3: 커밋 — 사용자가 요청했을 때만**

```bash
git add data/src/main/java/com/teamyg/parfait/data/model/image/UploadImagePlan.kt
git commit -m "docs: 누끼 상한의 근거를 ADR 로 옮긴다"
```

---

## 완료 조건

- `./gradlew testDebugUnitTest`가 통과한다.
- `UploadImagePlanTest`가 배율·방어선·하한 되돌림·하한 단조성·하한이 확대를 안 만듦·확대금지·망가진 입력·배경 무영향을 덮는다.
- 필드가 없는 옛 초안 JSON을 읽어도 초안을 통째로 버리지 않는다.
- 실기기 로그에 원본 긴 변, 적용 배율, 축소 전후 치수·바이트가 한 줄에 찍힌다.
- `UploadImagePlan.kt`에 누끼 상한의 iOS 근거가 남아 있지 않다.
