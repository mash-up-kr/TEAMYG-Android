---
id: segmentation-preprocessing
title: 세그멘테이션 입력 전처리 구현 계획 (14 Task, 4단계 스파이크 게이트)
status: in-progress
type: work-order
created: 2026-08-23
updated: 2026-09-17
platforms: android
owner: android
related_adr: ADR-0012, ADR-0011, ADR-0014
related_spec: segmentation-preprocessing, segmentation-mask-postprocessing
related_code: ImageSegmentationRepositoryImpl#decodeImage, ContentResolver.kt#decodeUriToBitmap, ContentResolver.kt#rotatedToUpright, ExifOrientation.kt#exifOrientationToDegrees, CameraPreviewComponent.kt, CameraCrop.kt#saveViewfinderCapture, RecentImageRepositoryImpl#extensionOf, FileCameraCacheLocalDataSourceImpl#createFile
archived_reason:
tags: [plan, parfait]
---

# 세그멘테이션 입력 전처리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 세그멘테이션이 보는 픽셀의 품질을 올려 누끼가 대상을 더 잘 얻게 한다.

**Architecture:** 손실이 처음 생기는 촬영 지점(`ImageCapture`)을 먼저 고치고, 디코드 지점
(`decodeUriToBitmap`·`decodeImage`)에 입력 정규화를 놓는다. 정규화의 판단은 순수 함수로 빼서 기기 없이
검증하고, 실제 픽셀 효과는 **버리는 스파이크에서 미리 만들어 본 뒤 고정 사진 세트로 판정한다.**
판정을 통과한 것만 정식으로 구현한다.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX 1.6.1, ML Kit Subject Segmentation, Hilt,
`androidx.exifinterface`(신규), kotlin.test + MockK

**Spec:** [`parfait/specs/2026-08-23-segmentation-preprocessing.md`](../specs/2026-08-23-segmentation-preprocessing.md)

> ✅ **1단계(Task 1~4)가 develop 에 들어갔다** — PR #349 `feature/segmentation-preprocessing`
> (`a5e8a760`, 2026-08-24 머지). 커밋 다섯·9파일 **삽입 177줄·삭제 6줄**이고 계획이 지정한 커밋
> 메시지 넷이 그대로 남았다. **2단계 이후(Task 5~14)는 전부 미착수다** — 스파이크·사진 세트 측정이
> 사람 손을 필요로 하고, 4단계는 그 판정을 통과한 것만 한다. 그래서 이 계획은 아카이브로 가지 않고
> active 에 남는다.
>
> **as-built 이탈 둘**(둘 다 Task 3, 후속 커밋 `be657892`가 고쳤다):
> ① `readExifDegrees` 의 KDoc 근거가 `ImageDecoder.createSource` 가 스트림을 소비한다는 것에서
> **`MediaStore.Images.Media.getBitmap` 이 EXIF 를 적용하지 않는다**는 것으로 바뀌었다 — 이 함수는
> API 28 미만 갈래에서만 불리므로 `ImageDecoder` 는 애초에 그 경로에 없었다.
> ② `openInputStream` 이 `null` 을 주는 갈래에 경고 로그가 붙었다(계획은 조용히 `0` 이었다).
> 두 이탈 모두 계약을 넓히거나 좁히지 않는다.

## Global Constraints

- **작업 저장소는 `TJYG-Android`다**(이 계획 문서가 있는 repo와 다르다).
  **베이스는 `develop`이다(2026-08-24 정정).** 초판은 `feature/c103-multi-subject-ui` 팁
  (`ab196483`) 위에 스택으로 쌓게 했다 — 측정이 C-103 다중 후보 화면과 면적 1% 필터를 필요로
  하는데 그 둘이 당시 `develop`에 없었기 때문이다. **PR #342(`34bf1939`)가 그 스택을 통째로
  머지해 전제가 사라졌으므로**, 이 계획은 `develop`에서 새 브랜치를 판다. 확대의 효과가 가장
  크게 드러날 자리가 후보 개수와 면적 필터라는 근거 자체는 그대로 유효하다.
- **커밋만 하고 push·PR은 하지 않는다.** 사용자가 명시적으로 요청할 때까지 리모트로 내보내지 않는다.
- 매 태스크 끝에 `./gradlew test ktlintCheck :app:assembleDebug`가 통과해야 한다.
  (예외: Task 5 스파이크. 그 태스크는 버릴 코드라 검증을 요구하지 않는다.)
- **주석 규약**: 코드가 이미 말하는 것은 쓰지 않는다. `@return`·`@param`은 타입·이름이 말하지 못할
  때만 쓴다. **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다 — 근거는 문서 포인터로 적는다).
  아키텍처 결정은 코드가 아니라 문서에 쓰고 코드에는 포인터 한 줄만 둔다.
  주석 분량은 그 코드의 **어려움**에 비례해야지 **중요함**에 비례하면 안 된다.
- 테스트는 `kotlin.test`(`@Test`·`assertEquals`·`assertNull`)를 쓴다. 이름은
  `함수명_조건_기대`이고 본문에 `// Given` `// When` `// Then` 주석을 단다.
- **변환 자체를 위한 단독 테스트를 만들지 않는다.** 판단이 든 순수 함수만 덮는다.
- **이 저장소에 Robolectric이 없다.** `Bitmap`·`ImageDecoder`·`ExifInterface`·CameraX 빌더가 걸린
  코드는 JVM 유닛으로 못 덮는다. 그런 태스크는 **왜 테스트가 없는지를 본문에 적는다.**
- **기존 파일을 "전문"으로 덮어쓰지 않는다.** 항상 추가·치환 지시로 고친다. 이 저장소는 전문
  덮어쓰기로 남의 함수를 지운 이력이 있다.
- `minSdk`는 26이다. API 분기를 지울 수 없다.
- 세그멘테이션 입력 이미지 하한은 **짧은 변 512px**이다(ML Kit Android 가이드
  "Tips to improve performance").

---

## 파일 구성

| 파일 | 책임 | 단계 |
|---|---|---|
| `feature/camera/impl/.../component/CameraPreviewComponent.kt` | `ImageCapture` 촬영 품질 | 1단계 |
| `gradle/libs.versions.toml` | `androidx.exifinterface` 추가 | 1단계 |
| `core/util/android/build.gradle.kts` | 위 의존성 결선 | 1단계 |
| `core/util/android/.../extension/ExifOrientation.kt` (신설) | EXIF 상수를 각도로 (순수 함수) | 1단계 |
| `core/util/android/src/test/.../extension/ExifOrientationTest.kt` (신설) | 위 함수의 유닛 | 1단계 |
| `core/util/android/.../Logger.kt` (신설) | 모듈 로거 | 1단계 |
| `core/util/android/.../extension/ContentResolver.kt` | 회전 보정 결선 | 1단계 |
| `data/.../repository/image/RecentImageRepositoryImpl.kt` | `SOURCE` 확장자를 바이트로 판정 | 1단계 |
| `data/src/test/.../repository/image/RecentImageRepositoryImplTest.kt` | 위 판정의 유닛 | 1단계 |
| `data/.../repository/image/SegmentationInputNormalizer.kt` (신설) | 확대 치수·하한·픽셀 상한 | 4단계 |
| `data/src/test/.../repository/image/SegmentationInputNormalizerTest.kt` (신설) | 위 함수의 유닛 | 4단계 |
| `domain/.../repository/image/ImageSegmentationRepository.kt` | 계약 KDoc 확장 | 4단계 |
| `data/.../repository/image/ImageSegmentationRepositoryImpl.kt` | 확대 결선 | 4단계 |
| `domain/.../model/camera/CameraCacheFormat.kt` (신설) | 캐시 파일 포맷 어휘 | 4단계 |
| `domain/.../usecase/camera/CreateCameraCacheFileUseCase.kt` | 포맷 인자(기본 JPEG) | 4단계 |
| `domain/.../repository/camera/CameraCacheFileRepository.kt` | 위 인자 통과 | 4단계 |
| `data/.../repository/camera/CameraCacheFileRepositoryImpl.kt` | 위 인자 통과 | 4단계 |
| `data/.../source/file/local/FileCameraCacheLocalDataSource(+Impl).kt` | 포맷별 확장자 | 4단계 |
| `feature/camera/impl/.../util/CameraCrop.kt` | 포맷별 압축 | 4단계 |
| `feature/camera/impl/build.gradle.kts` | 유닛 테스트 소스셋 신설 | 4단계 |
| `feature/camera/impl/.../viewmodel/CustomCameraViewModel.kt` | 포맷 분기·촬영 중 상태 | 4단계 |
| `feature/camera/impl/src/test/.../viewmodel/CustomCameraViewModelTest.kt` (신설) | 연타 가드 유닛 | 4단계 |
| `feature/camera/impl/.../route/CustomCameraRoute.kt` | `returnResultOnly`로 포맷 결정 | 4단계 |
| `feature/camera/impl/.../screen/CustomCameraScreen.kt` | 셔터 비활성 전달 | 4단계 |
| `feature/camera/impl/.../component/CameraControlComponent.kt` | 셔터 비활성 중계 | 4단계 |
| `core/designsystem/.../component/ygcamerashutter/YGCameraShutter.kt` | `enabled` 파라미터 신설 | 4단계 |

**단계 구조가 이 계획의 핵심이다.**

| 단계 | 태스크 | 성격 |
|---|---|---|
| 1단계 | Task 1~4 | 근거가 확정된 것. 무조건 한다 |
| 2단계 | Task 5 | **버리는 스파이크.** 측정 대상을 만들기만 한다 |
| 3단계 | Task 6~8 | 사람이 하는 측정과 판정. Task 8은 회전 확장(독립 조건부) |
| 4단계 | Task 9~13 | 판정을 통과한 것만 정식 구현 |
| 마무리 | Task 14 | 실기기 회귀와 문서 |

---

# 1단계 — 근거가 확정된 것

## Task 1: 촬영 품질을 올린다

**Files:**
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/component/CameraPreviewComponent.kt`

**Interfaces:**
- Consumes: 없음
- Produces: 없음(설정 변경). 뒤 태스크가 의존하는 심볼이 없다.

**테스트가 없는 이유**: CameraX 빌더 설정이고 이 저장소에 Robolectric이 없다. 검증은 Task 6 사진 세트가 한다.

- [x] **Step 1: 빌더에 품질 설정을 넣는다**

`imageCapture` 생성부(현재 `ImageCapture.Builder().build()`)를 바꾼다.

```kotlin
val imageCapture: ImageCapture = ImageCapture
    .Builder()
    // 근거: parfait/specs/2026-08-23-segmentation-preprocessing.md 「1. 손실이 처음 생기는 자리」
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
    .setJpegQuality(MAX_JPEG_QUALITY)
    .build()
```

파일 하단 상수 자리에 더한다. `CAPTURE_MODE_MAXIMIZE_QUALITY`의 기본 품질이 이미 100이지만,
기본값에 기대지 않고 명시한다.

```kotlin
private const val MAX_JPEG_QUALITY = 100
```

- [x] **Step 2: 전체 검사**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 3: 커밋**

```bash
git add feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/component/CameraPreviewComponent.kt
git commit -m "fix: 촬영을 최대 품질로 받는다"
```

---

## Task 2: EXIF orientation 을 각도로 바꾸는 순수 함수

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `core/util/android/build.gradle.kts`
- Create: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ExifOrientation.kt`
- Test: `core/util/android/src/test/kotlin/com/teamyg/parfait/core/util/android/extension/ExifOrientationTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `internal fun exifOrientationToDegrees(orientation: Int): Int` —
  `com.teamyg.parfait.core.util.android.extension` 패키지. Task 3이 부른다.

같은 모듈 유닛 테스트가 `internal`을 본다. 이 모듈에 선례는 없지만 `data` 모듈의
`SegmentationMask.kt` 의 `internal` 함수(당시 `maskSubjectPixels`, 현재 `maskSubjectAlpha`)가 같은
컨벤션 플러그인 조합에서 그렇게 동작한다.

- [x] **Step 1: 의존성을 추가한다**

`gradle/libs.versions.toml`의 `[versions]` 절 `#Android` 부근에 넣는다.

```toml
exifinterface = "1.4.2"
```

`[libraries]` 절 `#Android` 부근, `androidx-core-ktx` 아래에 넣는다.

```toml
androidx-exifinterface = { group = "androidx.exifinterface", name = "exifinterface", version.ref = "exifinterface" }
```

`core/util/android/build.gradle.kts`의 `dependencies` 블록에 한 줄 더한다.

```kotlin
implementation(libs.androidx.exifinterface)
```

- [x] **Step 2: 실패하는 테스트를 쓴다**

`ExifOrientationTest.kt`를 만든다.

```kotlin
package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface
import kotlin.test.Test
import kotlin.test.assertEquals

class ExifOrientationTest {
    @Test
    fun exifOrientationToDegrees_rotateTags_mapToTheirAngles() {
        // Given 회전만 나타내는 태그 셋
        // Then 각각의 각도가 된다
        assertEquals(90, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, exifOrientationToDegrees(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun exifOrientationToDegrees_normalOrUndefined_isZero() {
        // Given 돌릴 필요가 없거나 태그가 없는 경우
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_UNDEFINED))
    }

    @Test
    fun exifOrientationToDegrees_mirrorTags_areZero() {
        // Given 좌우 반전이 섞인 태그 넷
        // Then 0 이다. 반전을 적용하면 뒤집힌 누끼가 나오고 정확도에는 기여하지 않는다
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(0, exifOrientationToDegrees(ExifInterface.ORIENTATION_TRANSVERSE))
    }

    @Test
    fun exifOrientationToDegrees_unknownValue_isZero() {
        // 깨진 파일이 범위 밖 값을 주는 일이 있다
        assertEquals(0, exifOrientationToDegrees(-1))
        assertEquals(0, exifOrientationToDegrees(99))
    }
}
```

- [x] **Step 3: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :core:util:android:testDebugUnitTest`
Expected: 컴파일 실패 — `Unresolved reference: exifOrientationToDegrees`

- [x] **Step 4: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.core.util.android.extension

import androidx.exifinterface.media.ExifInterface

/**
 * EXIF orientation 태그를 시계 방향 회전 각도로 바꾼다.
 *
 * 미러링(`FLIP_*`·`TRANSPOSE`·`TRANSVERSE`)이 0 인 이유는
 * `parfait/specs/2026-08-23-segmentation-preprocessing.md` 「범위 제외」에 있다.
 */
internal fun exifOrientationToDegrees(orientation: Int): Int = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}
```

- [x] **Step 5: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :core:util:android:testDebugUnitTest`
Expected: PASS

- [x] **Step 6: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add gradle/libs.versions.toml core/util/android/build.gradle.kts \
  core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ExifOrientation.kt \
  core/util/android/src/test/kotlin/com/teamyg/parfait/core/util/android/extension/ExifOrientationTest.kt
git commit -m "feat: EXIF orientation 을 회전 각도로 바꾼다"
```

---

## Task 3: API 28 미만 디코드에 회전 보정을 결선한다

**Files:**
- Create: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/Logger.kt`
- Modify: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ContentResolver.kt`

**Interfaces:**
- Consumes: `exifOrientationToDegrees(orientation: Int): Int` (Task 2)
- Produces: `fun ContentResolver.decodeUriToBitmap(uri: Uri): Bitmap` — 시그니처는 그대로이고 계약만
  넓어진다. Task 10이 이 함수 위에 확대를 얹는다.

**테스트가 없는 이유**: `Bitmap`·`ImageDecoder`·`ExifInterface`가 걸려 JVM에서 못 돈다. 이 저장소에
Robolectric이 없다. 판단은 Task 2의 순수 함수가 덮었고 픽셀 효과는 Task 6 사진 세트가 본다.

⚠️ **API 28 이상은 이 태스크에서 건드리지 않는다.** `ImageDecoder`가 EXIF를 자동 적용하는지 공식
문서로 확인하지 못했다(OQ-P-280). 이미 적용된 판을 또 돌리면 두 번 돌아간다. 판정 못 한 상태의
기본값은 "보정하지 않음"이다. 넓힐지는 Task 8이 정한다.

- [x] **Step 1: 모듈 로거를 만든다**

`data/src/main/java/com/teamyg/parfait/data/utils/Logger.kt`와 같은 형태다.

```kotlin
package com.teamyg.parfait.core.util.android

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

internal val coreUtilAndroidLogger: Logger by lazy {
    Loggers.create(tag = "CoreUtilAndroid")
}
```

- [x] **Step 2: `ContentResolver.kt` 를 고친다 (전문 교체 금지)**

⚠️ **이 파일에는 `decodeUriToBitmap` 말고 `readBytes`도 있다.** 소비자가
`FileRecentImageLocalDataSourceImpl`과 `ImageFileLocalDataSourceImpl` 둘이다.
**`readBytes`를 지우면 `:data` 컴파일이 깨진다.** 아래 세 가지만 한다.

**(a) 임포트를 더한다.**

```kotlin
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
```

**(b) `decodeUriToBitmap`의 `else` 갈래 끝에 호출을 붙인다.** `if` 갈래(API 28 이상)는 손대지 않는다.

```kotlin
} else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media
        .getBitmap(this, uri)
        .rotatedToUpright(this, uri)
}
```

**(c) 파일 하단에 비공개 함수 둘을 더한다.** `readBytes` 아래여도 되고 위여도 된다.

```kotlin
/**
 * EXIF 회전을 픽셀에 적용한다.
 *
 * 왜 API 28 이상에서는 부르지 않는지는
 * `parfait/synthesis/open-questions.md` OQ-P-280 에 있다.
 */
private fun Bitmap.rotatedToUpright(
    resolver: ContentResolver,
    uri: Uri,
): Bitmap {
    val degrees = resolver.readExifDegrees(uri)
    if (degrees == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)

    // 각도가 0 이 아니라 언제나 새 인스턴스다. 전체 해상도 판 둘이 함께 살지 않게 여기서 닫는다
    recycle()

    return rotated
}

/**
 * [ImageDecoder.createSource] 가 스트림을 소비하므로 EXIF 는 uri 를 한 번 더 열어서 읽는다.
 *
 * 못 읽으면 0 이다 — 태그가 깨진 것과 이미지를 못 연 것은 다른 사건이라 디코드를 실패시키지
 * 않는다. 다만 이 갈래가 상시 참이 되면 보정이 조용히 무효가 되므로 남긴다.
 */
private fun ContentResolver.readExifDegrees(uri: Uri): Int = try {
    openInputStream(uri).use { input ->
        if (input == null) {
            0
        } else {
            exifOrientationToDegrees(
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                ),
            )
        }
    }
} catch (throwable: Exception) {
    coreUtilAndroidLogger.w(throwable) { "EXIF 를 읽지 못해 회전 보정을 건너뛴다 - uri: $uri" }
    0
}
```

`Logger.w(throwable: Throwable? = null, tag: String? = null, message: () -> String)`이 실제
시그니처다. 위 호출이 그대로 컴파일된다.

- [x] **Step 3: `readBytes` 가 그대로 있는지 확인한다**

Run: `grep -n "fun ContentResolver.readBytes" core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ContentResolver.kt`
Expected: 한 줄이 나온다. 안 나오면 지운 것이므로 되살린다.

- [x] **Step 4: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/Logger.kt \
  core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ContentResolver.kt
git commit -m "fix: API 28 미만에서 누운 사진을 세워서 디코드한다"
```

---

## Task 4: 최근 이미지 확장자를 이름이 아니라 바이트로 정한다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `UploadImageFormat.ofBytes(bytes: ByteArray): UploadImageFormat?`,
  `UploadImageFormat.extension: String` — 같은 `:data` 모듈의 `data.model.image` 패키지에 있다.
- Produces: 없음(내부 판정 변경).

> 📌 **스펙은 이 항목을 "PNG 채택 시"로 분류했으나 1단계로 올린다.** 지금도 참인 결함이다.
> `SegmentationViewModel`이 세그멘테이션 진입마다 원본을 `RecentImageKind.SOURCE`로 기록하는데
> **사용자가 갤러리에서 고른 PNG도 그 경로를 탄다.** 확장자가 `"jpg"`로 못박혀 있어 PNG 바이트가
> `.jpg` 이름으로 앉고, C-301 배경 편집이 최근 목록에서 그것을 고르면
> `ImageFileLocalDataSourceImpl#formatOf`가 확장자에서 유도된 MIME을 바이트 스니핑보다 먼저 믿어
> `image/jpeg`로 업로드된다.

⚠️ **이 변경은 앞으로 저장되는 것만 고친다.** 파일명이 `sha256 + "." + extension`이라 같은 사진이
`.jpg`와 `.png` 두 이름으로 남을 수 있고, 최근 목록의 중복 판정 키가 uri라 **두 칸을 먹는다.**
마이그레이션은 하지 않는다 — 목록 상한이 9칸이라 곧 밀려난다. 이 감수를 OQ-P-283으로 남긴다.

- [x] **Step 1: 실패하는 테스트를 쓴다**

기존 `store_withAbsolutePath_readsThroughFilePathAndKeepsPngExtension` 아래에 더한다.
`File`·`assertEquals`는 이미 import되어 있다.

```kotlin
@Test
fun store_sourceIsActuallyPng_namesItPngNotJpg() = runTest {
    // Given 사용자가 갤러리에서 고른 PNG. SOURCE 경로로 들어온다
    val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    val target = File(dir, "abc.png")
    every { fileDataSource.readBytes("content://media/2") } returns bytes
    every { fileDataSource.getTargetFile(bytes, "png") } returns target
    every { fileDataSource.getUriStringForFile(target) } returns "content://recent/abc.png"

    // When 최근 이미지로 저장한다
    val stored = repository().storeRecentImageInInternalStorage(
        source = "content://media/2",
        kind = RecentImageKind.SOURCE,
    )

    // Then 내용대로 png 다. jpg 로 굳으면 배경으로 다시 골랐을 때
    // 확장자에서 유도된 image/jpeg 로 올라간다
    verify { fileDataSource.getTargetFile(bytes, "png") }
    assertEquals("content://recent/abc.png", stored)
}

@Test
fun store_sourceFormatIsUnknown_fallsBackToJpg() = runTest {
    // Given 앞머리가 PNG 도 JPEG 도 아닌 바이트
    val bytes = byteArrayOf(0x47, 0x49, 0x46, 0x38)
    val target = File(dir, "def.jpg")
    every { fileDataSource.readBytes("content://media/3") } returns bytes
    every { fileDataSource.getTargetFile(bytes, "jpg") } returns target
    every { fileDataSource.getUriStringForFile(target) } returns "content://recent/def.jpg"

    // When 최근 이미지로 저장한다
    repository().storeRecentImageInInternalStorage(
        source = "content://media/3",
        kind = RecentImageKind.SOURCE,
    )

    // Then 판정 실패가 저장 실패가 되지는 않는다. 종전 동작을 폴백으로 둔다
    verify { fileDataSource.getTargetFile(bytes, "jpg") }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*RecentImageRepositoryImplTest*'`
Expected: `store_sourceIsActuallyPng_namesItPngNotJpg` FAIL —
`getTargetFile(bytes, "png")`가 안 불렸다는 MockK 검증 실패

- [x] **Step 3: 구현을 고친다**

`kind.fileExtension()`을 쓰던 자리를 바꾼다.

```kotlin
val target: File = fileRecentImageLocalDataSource.getTargetFile(bytes, extensionOf(kind, bytes))
```

`fileExtension()`을 지우고 아래로 바꾼다.

```kotlin
/**
 * 알맹이는 언제나 투명 PNG 라 종류로 정해지지만, 원본은 사용자가 고른 파일이라 내용을 봐야 한다.
 * 이름이 거짓이면 업로드가 content type 을 잘못 정한다.
 */
private fun extensionOf(
    kind: RecentImageKind,
    bytes: ByteArray,
): String = when (kind) {
    RecentImageKind.SOURCE -> UploadImageFormat.ofBytes(bytes)?.extension ?: UploadImageFormat.JPEG.extension
    RecentImageKind.CUTOUT -> UploadImageFormat.PNG.extension
}
```

`import com.teamyg.parfait.data.model.image.UploadImageFormat`를 더한다.

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*RecentImageRepositoryImplTest*'`
Expected: PASS. **기존 케이스는 손대지 않는다** — SOURCE 케이스의 바이트가 `byteArrayOf(1, 2, 3)`이라
`ofBytes`가 `null`을 주고 폴백 `"jpg"`로 그대로 통과한다.

- [x] **Step 5: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt \
  data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt
git commit -m "fix: 최근 원본의 확장자를 이름이 아니라 내용으로 정한다"
```

---

# 2단계 — 버리는 스파이크

## Task 5: 측정 대상을 만든다 (커밋하지 않는다)

**Files:**
- 4단계 Task 9~13이 만들 코드를 **임시로** 얹는다. 목록은 그 태스크들을 보라.

**Interfaces:**
- Consumes: Task 1~4의 결과물
- Produces: 측정 가능한 빌드. **정식 산출물이 아니다.**

측정하려면 측정 대상이 있어야 하는데, 무엇을 정식으로 넣을지는 측정이 정한다. 그 순환을
**버리는 스파이크**로 끊는다.

- [ ] **Step 1: 스파이크 브랜치를 딴다**

```bash
git checkout -b spike/segmentation-preprocessing
```

- [ ] **Step 2: 4단계 코드를 얹는다**

Task 9·10(확대)과 Task 11·12·13(PNG)의 **구현만** 옮겨 적는다.
**테스트·KDoc·ktlint·커밋을 요구하지 않는다.** 이 브랜치는 버린다. 여기서 다듬으면 4단계를 두 번 한다.

⚠️ **PNG를 재려면 Task 11·12·13이 모두 필요하다.** Task 11까지만 하면 아무도 PNG를 안 넘긴다.

- [ ] **Step 3: 두 빌드를 준비한다**

- **A 빌드**: 1단계 팁(스파이크 없음)
- **B 빌드**: 스파이크 브랜치

Task 6이 둘을 비교한다. 확대만 켠 빌드와 PNG만 켠 빌드를 따로 만들 수 있으면 더 좋으나, 두
항목의 판정이 서로 독립이므로 **한 번에 켜 놓고 각 사진이 어느 항목을 판정하는지로 가른다.**
512 미만 사진은 확대를, 경계 복잡 사진은 PNG를 판정한다.

- [ ] **Step 4: 스파이크임을 확인한다**

Run: `git log --oneline -1`
Expected: 커밋이 없거나(작업 트리에만 있음) 스파이크 커밋 하나. **이 브랜치는 어느 경우에도
머지하지 않는다.**

---

# 3단계 — 측정과 판정

## Task 6: 고정 사진 세트로 판정한다

**Files:**
- 코드 변경 없음.

**Interfaces:**
- Consumes: Task 5의 A·B 빌드
- Produces: 4단계 실행 범위를 정하는 판정 결과

⚠️ **이 태스크는 사람이 실기기에서 해야 한다. 서브에이전트가 대신할 수 없다.**

- [ ] **Step 1: 사진 여섯 장을 준비한다**

| 사진 | 판정 대상 |
|---|---|
| EXIF 회전 태그가 붙은 세로 사진 | 회전 정합(Task 8이 쓴다) |
| 짧은 변 512 미만 이미지 | 확대의 효과(Task 9·10) |
| 머리카락·털·잎사귀처럼 경계가 복잡한 피사체 | 촬영 품질과 저장 포맷(Task 11~13) |
| 피사체가 뷰파인더 변에 걸친 촬영 | 이번엔 안 고친다. 기준선만 |
| 저대비·역광 | 이번엔 안 고친다. 기준선만 |
| 다중 피사체 | 기존 다중 후보 회귀 |

**회전 사진은 태그가 실제로 붙어 있는지 먼저 확인한다.** 많은 카메라 앱이 픽셀에 회전을 굽고
태그는 정상으로 쓴다. 그런 사진으로 시험하면 두 API 대역 모두 문제없다고 나오고 아무것도 배우지
못한다.

Run: `exiftool <파일>`로 Orientation 값이 6 또는 8인지 확인한다.

- [ ] **Step 2: 회전을 판정한다**

API 27 에뮬레이터와 API 28 이상 기기에서 각각 회전 사진을 갤러리 경로로 넣고 C-103 후보 화면을 본다.

- API 27에서 세워져 나오면 Task 3이 동작한 것이다.
- **API 28 이상에서도 누워 나오면** Task 8을 실행한다. 세워져 나오면 Task 8을 건너뛴다.

- [ ] **Step 3: 손실을 가른다**

같은 피사체를 같은 구도로 촬영해 최종 누끼를 나란히 둔다.

1. Task 1 이전 빌드 (기준선)
2. A 빌드 (촬영 품질만 상향, 저장은 JPEG 95 그대로)
3. B 빌드 (촬영 품질 상향 + PNG 저장)

2번과 3번의 차이가 눈에 보이면 PNG를 채택한다. **확신이 안 서면 버린다** — 스펙의 기본값이
그쪽이고, 버린 기록도 결과다.

함께 잰다 — PNG 저장 시간, 파일 크기, 셔터에서 확인 화면까지의 공백.

- [ ] **Step 4: 확대를 판정한다**

짧은 변 512 미만 사진을 갤러리 경로로 넣고 A·B 빌드의 후보 화면을 비교한다. **후보 개수와 박스
위치**를 본다(면적 1% 필터가 걸러 내던 것이 살아나는지가 가장 큰 신호다).

같은 사진으로 **배치까지 끝내서** 캔버스에 박힌 토핑 크기가 A·B에서 달라지는지도 본다(OQ-P-282).

여기서도 확신이 안 서면 버린다.

- [ ] **Step 5: 결과를 적는다**

`parfait/specs/2026-08-23-segmentation-preprocessing.md`의 근거 등급 표에 판정 결과를 채운다.
OQ-P-278·280·282의 상태도 갱신한다.

- [ ] **Step 6: 스파이크를 버린다**

```bash
git checkout <1단계 팁 브랜치>
git branch -D spike/segmentation-preprocessing
```

---

## Task 7: 판정을 실행 범위로 옮긴다

**Files:**
- 코드 변경 없음. 이 계획 문서의 체크박스를 정리한다.

- [ ] **Step 1: 조합에 따라 실행할 태스크를 정한다**

| 확대 | PNG | 실행 |
|---|---|---|
| 채택 | 채택 | Task 9·10·11·12·13 |
| 채택 | 기각 | Task 9·10만 |
| 기각 | 채택 | Task 11·12·13만 |
| 기각 | 기각 | 없음. Task 14로 간다 |

**Task 8(회전 범위 확장)은 이 표와 독립이다.** Task 6 Step 2가 따로 정한다.

- [ ] **Step 2: 버리기로 한 태스크에 표시를 남긴다**

실행하지 않는 태스크의 제목 옆에 `[버림 — 근거: Task 6 Step N 판정]`을 적는다. 계획 문서가
그대로 실행 기록이 된다.

---

## Task 8: 회전 보정을 전 버전으로 넓힌다 (조건부)

**Files:**
- Modify: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ContentResolver.kt`

**Interfaces:**
- Consumes: `rotatedToUpright`·`readExifDegrees` (Task 3)
- Produces: 없음

> **게이트**: Task 6 Step 2에서 **API 28 이상에서도 사진이 누워 나온 경우에만** 한다.
> 세워져 나왔으면 이 태스크를 건너뛰고 OQ-P-280을 "API 28 이상은 `ImageDecoder`가 적용한다"로 닫는다.
> **확대·PNG 게이트와 무관하다.**

- [ ] **Step 1: API 분기를 걷는다**

`decodeUriToBitmap`의 `if` 갈래 결과에도 `.rotatedToUpright(this, uri)`를 붙인다. `ExifOrientationTest`는
그대로 통과한다.

- [ ] **Step 2: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/ContentResolver.kt
git commit -m "fix: 모든 API 대역에서 누운 사진을 세운다"
```

- [ ] **Step 3: 미결을 닫는다**

`parfait/synthesis/open-questions.md`의 OQ-P-280에 관찰 결과와 처분을 적는다.

---

# 4단계 — 판정이 통과한 것만

## Task 9: 확대 치수를 계산하는 순수 함수

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationInputNormalizer.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationInputNormalizerTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `internal data class ScaledSize(val width: Int, val height: Int)`,
  `internal fun computeUpscaleTarget(width: Int, height: Int): ScaledSize?` —
  `data.repository.image` 패키지. Task 10이 부른다.

Task 5 스파이크에서 이미 써 본 코드를 정식으로 다시 넣는 자리다. 테스트와 KDoc이 여기서 붙는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SegmentationInputNormalizerTest {
    @Test
    fun computeUpscaleTarget_shortSideBelowTheFloor_scalesUpKeepingRatio() {
        // Given 짧은 변이 400 인 가로 사진
        val target = computeUpscaleTarget(width = 600, height = 400)

        // Then 짧은 변이 512 가 되고 비율이 유지된다
        assertEquals(ScaledSize(width = 768, height = 512), target)
    }

    @Test
    fun computeUpscaleTarget_shortSideIsExactlyTheFloor_isNull() {
        // Then 하한을 만족하면 아무것도 하지 않는다. null 이 "확대 없음"이다
        assertNull(computeUpscaleTarget(width = 1024, height = 512))
    }

    @Test
    fun computeUpscaleTarget_shortSideAboveTheFloor_isNull() {
        assertNull(computeUpscaleTarget(width = 513, height = 513))
    }

    @Test
    fun computeUpscaleTarget_shortSideJustBelowTheFloor_scalesUp() {
        // Given 511 은 경계 바로 아래다
        val target = computeUpscaleTarget(width = 511, height = 511)

        // Then 올림해서 하한을 밑돌지 않게 한다
        assertEquals(ScaledSize(width = 512, height = 512), target)
    }

    @Test
    fun computeUpscaleTarget_extremeAspectRatio_isNullBecauseOfThePixelCap() {
        // Given 짧은 변만 보면 5.12 배로 키워야 하는 파노라마 조각.
        // 그대로 키우면 512 × 10240 = 5,242,880 픽셀이다
        val target = computeUpscaleTarget(width = 2000, height = 100)

        // Then 확대하지 않는다. 확대해서 죽는 것보다 확대를 안 하는 편이 낫다
        assertNull(target)
    }

    @Test
    fun computeUpscaleTarget_nonPositiveDimension_isNull() {
        assertNull(computeUpscaleTarget(width = 0, height = 400))
        assertNull(computeUpscaleTarget(width = 400, height = -1))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SegmentationInputNormalizerTest*'`
Expected: 컴파일 실패 — `Unresolved reference: computeUpscaleTarget`

- [ ] **Step 3: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.data.repository.image

import kotlin.math.ceil

/** ML Kit 가이드가 정확도 하한으로 제시하는 짧은 변 치수 */
internal const val MIN_SEGMENTATION_SIDE = 512

/**
 * 확대 후 총 픽셀 상한. 약 16MB(ARGB) 어치다.
 *
 * 짧은 변만 보고 키우면 극단 종횡비에서 긴 변이 폭증한다
 * (`parfait/synthesis/open-questions.md` OQ-P-228 의 피크 위에 얹힌다).
 */
internal const val MAX_UPSCALED_PIXELS = 4_000_000L

internal data class ScaledSize(val width: Int, val height: Int)

/**
 * @return 확대가 필요 없거나 확대하면 [MAX_UPSCALED_PIXELS] 를 넘으면 `null`
 */
internal fun computeUpscaleTarget(
    width: Int,
    height: Int,
): ScaledSize? {
    if (width <= 0 || height <= 0) return null

    val shortSide = minOf(width, height)
    if (shortSide >= MIN_SEGMENTATION_SIDE) return null

    val ratio = MIN_SEGMENTATION_SIDE.toDouble() / shortSide
    // 올림한다. 내림하면 짧은 변이 511 로 떨어져 하한을 다시 밑돈다
    val scaledWidth = ceil(width * ratio).toInt()
    val scaledHeight = ceil(height * ratio).toInt()

    if (scaledWidth.toLong() * scaledHeight > MAX_UPSCALED_PIXELS) return null

    return ScaledSize(width = scaledWidth, height = scaledHeight)
}
```

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests '*SegmentationInputNormalizerTest*'`
Expected: PASS

- [ ] **Step 5: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/SegmentationInputNormalizer.kt \
  data/src/test/java/com/teamyg/parfait/data/repository/image/SegmentationInputNormalizerTest.kt
git commit -m "feat: 세그멘테이션 입력 확대 치수를 계산한다"
```

---

## Task 10: 디코드에 확대를 결선한다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt`

**Interfaces:**
- Consumes: `computeUpscaleTarget`·`ScaledSize` (Task 9), `decodeUriToBitmap` (Task 3)
- Produces: 없음(계약 문구만 넓어진다).

**테스트가 없는 이유**: `Bitmap`이 걸려 JVM에서 못 돈다. 판단은 Task 9가 덮었다.
`SegmentationViewModelTest`는 `DecodeImageUseCase`를 목으로 두므로 영향받지 않는다.

- [ ] **Step 1: 계약 KDoc 을 넓힌다 (기존 문장을 지우지 않는다)**

⚠️ `ImageSegmentationRepository.decodeImage`에 **이미 KDoc 이 있다.** 실패 처리 계약이 적혀 있으므로
지우지 말고 앞에 한 문단을 더한다.

```kotlin
/**
 * 세그멘테이션 입력 규격으로 정규화해 디코드한다 — 방향을 세우고 짧은 변 하한을 맞춘다.
 * 색공간과 `Bitmap.Config` 는 건드리지 않는다(OQ-P-281).
 *
 * (기존 KDoc 본문을 여기에 그대로 이어 붙인다 — 실패 처리 계약을 지우지 않는다)
 */
suspend fun decodeImage(uri: String): BitmapWrapper
```

- [ ] **Step 2: 확대를 적용한다**

`decodeImage`를 바꾸고 비공개 확장을 더한다.

```kotlin
override suspend fun decodeImage(uri: String): BitmapWrapper {
    val bitmap: Bitmap = context.contentResolver.decodeUriToBitmap(uri.toUri())

    return bitmap.upscaledForSegmentation().toAndroidBitmap()
}

private fun Bitmap.upscaledForSegmentation(): Bitmap {
    val target = computeUpscaleTarget(width, height) ?: return this

    // androidx.core.graphics.scale - filter 기본값이 true 라 인자를 다시 주지 않는다
    val scaled = scale(target.width, target.height)

    // 목표 치수는 언제나 원본보다 커서 새 인스턴스가 나온다. 원본을 여기서 닫는다
    recycle()

    return scaled
}
```

> **2026-09-17 정정** — 초판은 `Bitmap.createScaledBitmap(this, ..., true)` 를 적었다. PR #504(develop
> `924cb5802`)가 대체 가능한 호출부를 모두 `androidx.core.graphics` 확장으로 옮겼으므로
> (`UploadImagePreprocessorImpl`·`SegmentationRecoveryNormalizer`·`ImageSegmentationRepositoryImpl`),
> 이 Task 를 초판대로 쓰면 그 라운드가 걷어낸 관용구를 되살린다. `scale` 은 내부에서
> `createScaledBitmap` 을 그대로 부르므로 identity 반환 조건(`plans/README.md` 가 적은
> `!isMutable`)과 회수 규칙은 달라지지 않는다.

- [ ] **Step 3: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add data/src/main/java/com/teamyg/parfait/data/repository/image/ImageSegmentationRepositoryImpl.kt \
  domain/src/main/java/com/teamyg/parfait/domain/repository/image/ImageSegmentationRepository.kt
git commit -m "feat: 하한 미만 이미지를 세그멘테이션 규격으로 키워서 디코드한다"
```

---

## Task 11: 캐시 파일 포맷을 어휘로 만든다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/camera/CameraCacheFormat.kt`
  (부모 디렉토리 `model/camera/`가 없다. 만든다)
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/camera/CreateCameraCacheFileUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/camera/CameraCacheFileRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/camera/CameraCacheFileRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/file/local/FileCameraCacheLocalDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/file/local/FileCameraCacheLocalDataSourceImpl.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `enum class CameraCacheFormat { JPEG, PNG }`(`domain.model.camera`),
  `CreateCameraCacheFileUseCase.invoke(format: CameraCacheFormat = CameraCacheFormat.JPEG): File`.
  Task 12·13이 쓴다.

**소비자 0인 계층을 먼저 만든다.** 이 저장소의 확립된 관례다(c103 PR1의 `persistSubject`, c106 PR1·PR2).
리뷰 초점을 계층 관통 하나로 좁힌다. **동작은 안 바뀐다** — 아무도 PNG를 안 넘긴다.

**테스트가 없는 이유**: 확장자 매핑은 판단이 든 변환이 아니다(저장소 규약: 변환 단독 테스트를 만들지 않는다).

기본값 JPEG가 핵심이다. `CreateCameraCacheUriUseCase`가 인자 없이 파일 use case를 부르므로
**시스템 카메라 경로는 한 글자도 안 바뀐다.**

- [ ] **Step 1: 포맷 어휘를 만든다**

```kotlin
package com.teamyg.parfait.domain.model.camera

/** 카메라가 캐시에 남기는 파일의 형식. 확장자 매핑은 data 가 한다 */
enum class CameraCacheFormat {
    JPEG,
    PNG,
}
```

- [ ] **Step 2: 계층을 따라 인자를 흘린다**

`CreateCameraCacheFileUseCase.invoke`에 `format: CameraCacheFormat = CameraCacheFormat.JPEG`를 더하고
`cameraCacheFileRepository.createCameraCacheFile(format)`로 넘긴다. 나머지 본문은 그대로 둔다.

`CameraCacheFileRepository.createCameraCacheFile`, `CameraCacheFileRepositoryImpl`,
`FileCameraCacheLocalDataSource.createFile`에 같은 인자를 더한다.
**`CreateCameraCacheUriUseCase`는 건드리지 않는다.**

`FileCameraCacheLocalDataSourceImpl`은 시그니처만 바꾸고 **본문과 애노테이션을 유지한다.**

⚠️ `override fun createFile` 위의 `@OptIn(FormatStringsInDatetimeFormats::class)`를 **지우지 않는다.**
본문의 `byUnicodePattern`이 error 수준 opt-in을 요구해서, 빠지면 `:data` 컴파일이 깨진다.

```kotlin
@OptIn(FormatStringsInDatetimeFormats::class)
override fun createFile(format: CameraCacheFormat): File {
    val timestamp = Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(LocalDateTime.Format { byUnicodePattern(FILE_NAME_PATTERN) })

    return File(
        dir,
        "IMG_$timestamp.${format.extension}",
    )
}

/** 파일명이 곧 업로드 content type 의 단서라 형식과 확장자가 어긋나면 안 된다 */
private val CameraCacheFormat.extension: String
    get() = when (this) {
        CameraCacheFormat.JPEG -> "jpg"
        CameraCacheFormat.PNG -> "png"
    }
```

- [ ] **Step 3: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/camera/CameraCacheFormat.kt \
  domain/src/main/java/com/teamyg/parfait/domain/usecase/camera/CreateCameraCacheFileUseCase.kt \
  domain/src/main/java/com/teamyg/parfait/domain/repository/camera/CameraCacheFileRepository.kt \
  data/src/main/java/com/teamyg/parfait/data/repository/camera/CameraCacheFileRepositoryImpl.kt \
  data/src/main/java/com/teamyg/parfait/data/source/file/local/FileCameraCacheLocalDataSource.kt \
  data/src/main/java/com/teamyg/parfait/data/source/file/local/FileCameraCacheLocalDataSourceImpl.kt
git commit -m "feat: 카메라 캐시 파일 포맷을 고를 수 있게 한다"
```

---

## Task 12: 촬영 저장이 포맷을 받아 압축한다

**Files:**
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/util/CameraCrop.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt`

**Interfaces:**
- Consumes: `CameraCacheFormat` (Task 11)
- Produces: `saveViewfinderCapture(captured, rotationDegrees, viewfinderRect, feedRect, isFrontFacing, format, file)`
  — Task 13이 `format` 인자의 값만 바꾼다.

`feature/camera/impl`은 컨벤션 플러그인이 `:domain`을 주므로 `CameraCacheFormat`이 보인다.

**동작은 안 바뀐다.** 호출부가 JPEG를 넘기고, **JPEG 품질도 95 그대로 유지한다.**
품질을 100으로 올리면 Task 6의 3벌 비교에서 "촬영 품질 상향의 기여"와 "저장 품질 상향의 기여"가
섞인다. 이 라운드의 가설은 첫 세대 손실이고 두 번째 세대 JPEG 품질이 아니다.

**테스트가 없는 이유**: `feature/camera/impl`에 유닛 테스트 소스셋이 없고(Task 13이 만든다),
`CompressFormat` 매핑은 판단이 든 변환이 아니다.

- [ ] **Step 1: 압축을 포맷에 맞춘다**

`JPEG_QUALITY` 상수는 **지우지 않고 그대로 쓴다.** 다만 그 상수가 `computeCropRect`의 KDoc과 함수
선언 사이에 끼어 있으므로, KDoc 바로 아래로 자리를 옮겨 KDoc이 제 함수에 붙게 한다.

`saveViewfinderCapture`에 파라미터를 더한다.

```kotlin
internal fun saveViewfinderCapture(
    captured: Bitmap,
    rotationDegrees: Int,
    viewfinderRect: Rect?,
    feedRect: Rect?,
    isFrontFacing: Boolean,
    format: CameraCacheFormat,
    file: File,
) {
    val rotated = captured.rotate(rotationDegrees)
    val cropRect = if (viewfinderRect != null && feedRect != null) {
        computeCropRect(
            viewfinderRect = viewfinderRect,
            feedRect = feedRect,
            imageSize = IntSize(rotated.width, rotated.height),
            isFrontFacing = isFrontFacing,
        )
    } else {
        null
    }

    file.outputStream().use { output ->
        // PNG 는 무손실이라 품질 인자를 무시한다
        rotated.crop(cropRect).compress(format.compressFormat, JPEG_QUALITY, output)
    }
}

private val CameraCacheFormat.compressFormat: Bitmap.CompressFormat
    get() = when (this) {
        CameraCacheFormat.JPEG -> Bitmap.CompressFormat.JPEG
        CameraCacheFormat.PNG -> Bitmap.CompressFormat.PNG
    }
```

- [ ] **Step 2: 호출부를 JPEG 고정으로 맞춘다**

`CustomCameraRoute.kt`의 `saveViewfinderCapture` 호출에 `format = CameraCacheFormat.JPEG`를 더한다.
**저장 블록의 나머지는 손대지 않는다** — 뒤따르는 `viewModel.processIntent(...)` 호출이 그대로 있어야 한다.

- [ ] **Step 3: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/util/CameraCrop.kt \
  feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt
git commit -m "refactor: 촬영 저장이 포맷을 받아 압축한다"
```

---

## Task 13: 토핑 경로만 무손실로 가르고 셔터를 잠근다

**Files:**
- Modify: `feature/camera/impl/build.gradle.kts`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/viewmodel/CustomCameraViewModel.kt`
- Create: `feature/camera/impl/src/test/java/com/teamyg/parfait/feature/camera/impl/viewmodel/CustomCameraViewModelTest.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/route/CustomCameraRoute.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/screen/CustomCameraScreen.kt`
- Modify: `feature/camera/impl/src/main/java/com/teamyg/parfait/feature/camera/impl/component/CameraControlComponent.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcamerashutter/YGCameraShutter.kt`

**Interfaces:**
- Consumes: `CameraCacheFormat` (Task 11), `saveViewfinderCapture(..., format, file)` (Task 12)
- Produces: `CustomCameraIntent.OnClickShutter(format: CameraCacheFormat)`(기존 `data object`를
  `data class`로 바꾼다), `CustomCameraState.isCapturing: Boolean`,
  `YGCameraShutter(onClick, modifier, enabled, interactionSource)`

커스텀 카메라의 소비자가 둘이다. 토핑 만들기와 **C-301 배경 촬영**(`returnResultOnly = true`)이다.
뒤엣것은 누끼를 안 타고 곧장 서버로 올라가므로 무손실의 이득이 0이고 업로드 용량만 커진다.

셔터 잠금이 함께 들어가는 이유는 **PNG 인코딩이 셔터와 확인 화면 사이에 끼기 때문**이다.
근거는 `parfait/specs/2026-08-23-segmentation-preprocessing.md` 「설계 1」에 있다.

⚠️ **비활성 시각 표현은 이 라운드에서 정하지 않는다.** 디자인 근거가 없다. 클릭만 막는다.

- [ ] **Step 1: 테스트 소스셋을 연다**

`feature/camera/impl/build.gradle.kts`의 `plugins` 블록에 더한다.

```kotlin
alias(libs.plugins.parfait.test.unit)
```

이 모듈에는 유닛 테스트가 한 건도 없었다. 연타 가드는 순수 ViewModel 로직이라 JVM으로 덮을 수 있고,
깨지면 조용히 열려서 실기기에서도 재현이 까다롭다.

- [ ] **Step 2: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.feature.camera.impl.viewmodel

import com.teamyg.parfait.domain.model.camera.CameraCacheFormat
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheFileUseCase
import com.teamyg.parfait.domain.usecase.camera.CreateCameraCacheUriUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test

class CustomCameraViewModelTest {
    private val createFile: CreateCameraCacheFileUseCase = mockk()
    private val createUri: CreateCameraCacheUriUseCase = mockk()

    private fun viewModel() = CustomCameraViewModel(
        createCameraCacheFileUseCase = createFile,
        createCameraCacheUriUseCase = createUri,
    )

    @Test
    fun onClickShutter_pressedTwiceBeforeSaving_createsOnlyOneFile() = runTest {
        // Given 저장이 아직 안 끝난 상태
        every { createFile(CameraCacheFormat.PNG) } returns File("IMG_1.png")
        val sut = viewModel()

        // When 셔터를 두 번 누른다
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))

        // Then 파일은 한 번만 만든다. 파일명이 초 단위라 두 번 만들면 서로를 덮는다
        verify(exactly = 1) { createFile(CameraCacheFormat.PNG) }
    }

    @Test
    fun onClickShutter_afterCaptureSaved_capturesAgain() = runTest {
        // Given 한 번 찍고 저장까지 끝난 상태
        val file = File("IMG_1.png")
        every { createFile(CameraCacheFormat.PNG) } returns file
        every { createUri(file = file) } returns "content://camera/IMG_1.png"
        val sut = viewModel()
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))
        sut.processIntent(CustomCameraIntent.OnCaptureSaved(file))

        // When 다시 누른다
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))

        // Then 잠금이 풀려 있다
        verify(exactly = 2) { createFile(CameraCacheFormat.PNG) }
    }

    @Test
    fun onCaptureFailed_unlocksTheShutter() = runTest {
        // Given 찍었는데 저장이 실패한 상태
        every { createFile(CameraCacheFormat.PNG) } returns File("IMG_1.png")
        val sut = viewModel()
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))
        sut.processIntent(CustomCameraIntent.OnCaptureFailed)

        // When 다시 누른다
        sut.processIntent(CustomCameraIntent.OnClickShutter(CameraCacheFormat.PNG))

        // Then 실패가 셔터를 영영 잠그지 않는다
        verify(exactly = 2) { createFile(CameraCacheFormat.PNG) }
    }
}
```

> `CustomCameraIntent.OnCaptureSaved`·`OnCaptureFailed`의 실제 생성 형태를
> `CustomCameraViewModel.kt`에서 확인하고 맞춘다. `runTest`가 필요 없으면 뺀다.

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:camera:impl:testDebugUnitTest`
Expected: 컴파일 실패 — `OnClickShutter`가 인자를 안 받는다

- [ ] **Step 4: 의도와 상태를 바꾼다**

`CustomCameraViewModel.kt`:

```kotlin
data class OnClickShutter(val format: CameraCacheFormat) : CustomCameraIntent
```

`CustomCameraState`에 `val isCapturing: Boolean = false,`를 더한다.

```kotlin
private fun handleOnClickShutter(intent: CustomCameraIntent.OnClickShutter) {
    // 저장이 끝나야 화면이 넘어간다. 그 사이의 재입력을 여기서 막는다
    // (근거: parfait/specs/2026-08-23-segmentation-preprocessing.md 「설계 1」)
    if (state.value.isCapturing) return

    updateState { copy(isCapturing = true) }
    postSideEffect(CustomCameraEffect.CaptureImage(file = createCameraCacheFileUseCase(intent.format)))
}

private fun handleOnCaptureSaved(intent: CustomCameraIntent.OnCaptureSaved) {
    updateState { copy(isCapturing = false) }

    val uri = createCameraCacheUriUseCase(file = intent.file)
    postSideEffect(CustomCameraEffect.NavigateToConfirm(uri = uri))
}

private fun handleOnCaptureFailed() {
    updateState { copy(isCapturing = false) }
    postSideEffect(CustomCameraEffect.CaptureFailed)
}
```

`processIntent`의 분기를 `is CustomCameraIntent.OnClickShutter -> handleOnClickShutter(intent)`로 바꾼다.

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:camera:impl:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 셔터 컴포넌트에 `enabled` 를 뚫는다**

`YGCameraShutter`에 `enabled: Boolean = true`를 더하고 내부 `clickableYGNoRipple`에 넘긴다.
그 modifier는 이미 `enabled`를 받는다. 기본값이 `true`라 기존 Preview 호출부는 안 깨진다.

`CameraControlComponent`에도 같은 파라미터를 더해 중계하고, `CustomCameraScreen`이
`state.isCapturing`을 읽어 `enabled = !state.isCapturing`으로 내려보낸다.
중간의 `CameraContent`도 파라미터를 하나 더 받는다.

- [ ] **Step 7: Route 가 포맷을 정하고 실패 시 파일을 지운다**

`CustomCameraRoute.kt`에 포맷을 한 번 계산한다.

```kotlin
// 배경 촬영은 누끼를 안 타고 곧장 업로드된다. 무손실의 이득이 없고 용량만 커진다
val cacheFormat = if (returnResultOnly) CameraCacheFormat.JPEG else CameraCacheFormat.PNG
```

셔터 결선을 바꾼다.

```kotlin
onClickShutter = { viewModel.processIntent(CustomCameraIntent.OnClickShutter(cacheFormat)) },
```

Task 12에서 넣은 `format = CameraCacheFormat.JPEG`를 `format = cacheFormat`으로 바꾼다.

저장 블록에 삭제를 더한다. ⚠️ **뒤따르는 `viewModel.processIntent(...)` 호출을 지우지 않는다.**
`file.delete()`를 그 **앞**에 둔다.

```kotlin
val saved = runCatching { /* 기존 saveViewfinderCapture 호출 그대로 */ }.isSuccess

// 쓰다 끊긴 파일이 남는다. 이 디렉토리를 지우는 코드가 없다(OQ-P-279)
if (!saved) {
    withContext(Dispatchers.IO) { file.delete() }
}

// 기존 processIntent 호출을 여기 그대로 둔다
```

- [ ] **Step 8: 전체 검사와 커밋**

Run: `./gradlew test ktlintCheck :app:assembleDebug`

```bash
git add feature/camera/impl/ \
  core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcamerashutter/YGCameraShutter.kt
git commit -m "feat: 토핑 촬영만 무손실로 남기고 저장 중 셔터를 잠근다"
```

---

# 마무리

## Task 14: 실기기 확인과 문서 갱신

**Files:**
- Modify: `parfait/specs/2026-08-23-segmentation-preprocessing.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/plans/README.md`
- Modify: `parfait/synthesis/open-questions.md`

⚠️ 문서는 `TJYG-Android`가 아니라 **이 계획이 있는 repo**에 있다. 브랜치를 따로 딴다.

- [ ] **Step 1: 회귀를 확인한다**

- 토핑 만들기 전체 흐름이 통과하는가
- **배경 촬영이 여전히 JPEG로 저장되고 업로드가 성공하는가**
- 최근 이미지 목록에서 고른 배경이 올라가는가(Task 4)
- **Task 4 이전에 저장해 둔 PNG 원본을 다시 골랐을 때 목록이 어떻게 되는가**(OQ-P-283)
- 셔터 연타가 파일을 덮지 않는가(Task 13을 했다면)

- [ ] **Step 2: 스펙에 as-built 절을 더한다**

근거 등급 표의 각 항목이 **들어갔는지 버려졌는지**를 적는다. 버린 것은 이유와 측정 결과를 함께
적는다. **버린 기록이 다음 사람에게는 결과다.**

- [ ] **Step 3: 미결을 갱신한다**

OQ-P-278(확대 판정) · OQ-P-279(PNG 크기 증가분) · OQ-P-280(회전 관찰) · OQ-P-282(배치 크기) ·
OQ-P-283(최근 이미지 중복).

- [ ] **Step 4: 인덱스와 커밋**

스펙 `status`를 `implemented`로 바꿔 `specs/archive/`로, 계획을 `plans/archive/`로 옮기고 두
README를 갱신한다.

```bash
git add parfait/
git commit -m "docs: 세그멘테이션 전처리 as-built 를 기록한다"
```

---

## 자체 점검

**스펙 커버리지**

| 스펙 항목 | 태스크 |
|---|---|
| `ImageCapture` 촬영 품질 상향 | Task 1 |
| API 26·27 EXIF 회전 보정 | Task 2·3 |
| API 28 이상 추가 보정(조건부) | **Task 8**(Task 6 Step 2가 게이트) |
| 짧은 변 512 하한(조건부) | Task 9·10 |
| 확대 총 픽셀 상한 | Task 9 |
| 촬영 저장 PNG(조건부) | Task 11·12·13 |
| 배경 촬영 경로 분리 | Task 13 Step 7 |
| 최근 이미지 확장자 바이트 판정 | Task 4 (스펙보다 앞당겼다) |
| 촬영 중 상태·셔터 비활성 | Task 13 Step 4·6 |
| PNG 저장 실패 시 파일 삭제 | Task 13 Step 7 |
| 회전 전 판 회수 | Task 3 Step 2 |
| EXIF URI 재개방과 실패 로그 | Task 3 Step 2 |
| `decodeImage` KDoc 경계 명시 | Task 10 Step 1 |
| 순수 함수 2종 JVM 테스트 | Task 2·9 |
| 연타 가드 JVM 테스트 | Task 13 Step 1~5 |
| 고정 사진 세트 6장 | Task 6 |
| 손실 3벌 비교 | Task 6 Step 3 |
| 배치까지 끝내는 확인 | Task 6 Step 4 |

**스펙이 범위 밖으로 둔 것은 태스크가 없다** — 다운샘플, 뷰파인더 크롭, 색공간·`Bitmap.Config`·HEIC,
대비 정규화, 후처리, EXIF 미러링, 갤러리 원본 손실, `SystemCameraRoute` 저장 포맷.

**타입 일관성**

- `CameraCacheFormat` — Task 11이 정의하고 12·13이 쓴다.
- `computeUpscaleTarget`·`ScaledSize` — Task 9가 정의하고 10이 쓴다.
- `exifOrientationToDegrees` — Task 2가 정의하고 3·8이 쓴다.
- `CustomCameraIntent.OnClickShutter` — Task 13에서 `data object`에서 `data class`로 바뀐다.
  값으로 쓰는 자리가 선언·`processIntent` 분기·Route 호출 셋뿐이고 전부 Task 13이 고친다.
- `saveViewfinderCapture` — Task 12가 `format` 파라미터를 더하고 같은 태스크에서 호출부를 맞춘다.
  Task 13이 그 인자의 값만 바꾼다.
- `YGCameraShutter` — Task 13이 `enabled: Boolean = true`를 더한다. 기본값이 있어 기존 호출부는
  안 깨진다.
