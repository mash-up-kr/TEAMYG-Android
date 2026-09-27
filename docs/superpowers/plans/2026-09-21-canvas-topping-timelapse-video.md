# 캔버스 토핑 타임랩스 동영상 저장 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 캔버스에 토핑이 쌓인 순서대로 하나씩 팝인하는 무음 mp4를 만들어 기기 갤러리에 저장한다.

**Architecture:** 프레임은 새로 그리지 않는다. 기존 `CanvasToppingLayer`를 녹화용으로 한 벌 더 띄워
프레임마다 `GraphicsLayer.toImageBitmap()`으로 읽고, `MediaCodec` + EGL 입력 표면 + `MediaMuxer`로
직접 mp4를 쓴다. 외부 의존성은 더하지 않는다. 순서·타이밍 계산은 순수 Kotlin으로 떼어 JVM에서
검증하고, 기기 의존은 인코더 한 조각에 가둔다.

**Tech Stack:** Kotlin, Jetpack Compose, `MediaCodec`/`MediaMuxer`/EGL14/GLES20(플랫폼 API),
Hilt, mockk, Turbine, AndroidJUnit4

**Spec:** [`../specs/2026-09-21-canvas-topping-timelapse-video.md`](../specs/2026-09-21-canvas-topping-timelapse-video.md)

## 진행 상태

**Task 1~4 완료, Task 5~8 미착수.** 화면 아래 조각(타임라인·인코더·갤러리 영상 저장)만 들어왔고
화면 결선은 아직 없다(OQ-P-411). Task 5~8을 구현한 사본은 로컬 브랜치
`feature/video-4-topping-layer-reveal`·`feature/video-5-wiring`에 있다(develop 미머지).

as-built 이탈 — 아래 Task 본문의 코드 블록은 계획 당시 그대로이고, 현재 코드와 다음이 다르다.

- **Task 1**: 타임라인 수치 상수는 `util/ToppingVideoTimeline.kt` 최상위가 아니라
  `model/VideoTimelineOptions` object에 있다. `CANVAS_VIDEO_FRAMES_PER_SECOND`는
  `VideoTimelineOptions.FRAMES_PER_SECOND`로 바뀌었고, `ToppingVideoFrame`은 `model/`로 옮겨졌다.
  `POP_OVERSHOOT`는 `0.08f`가 아니라 easeOutBack 계수 `1.5f`다 — 정점이 스펙의 1.08이 되는 값이다.
- **Task 2**: `BitmapSurfaceWriter.draw`가 `Config.HARDWARE` 비트맵을 `ARGB_8888`로 복사해
  업로드한다. 계획에 없던 변경이고 없으면 API 28 이상에서 녹화가 항상 실패한다(스펙 「주의」).
- **Task 3**: `Mp4VideoEncoder`가 전용 단일 스레드를 소유하고 EOS 대기에 상한을 둔다. 상수는
  파일 최상위가 아니라 `private companion object`에 있다.
- **Task 2·3·4**: 주석은 [`code-conventions.md`](../../code-conventions.md)에 맞춰 줄였다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 베이스 브랜치는 `develop`. 워크트리를 만들지 않고 본
  체크아웃에서 브랜치를 판다.
- **커밋하지 않는다.** 사용자가 따로 요청하지 않는 한 각 Task는 커밋 없이 끝낸다. 각 Task의
  마지막 단계는 커밋이 아니라 테스트 통과 확인이다.
- **외부 의존성을 추가하지 않는다.** Media3·ffmpeg·`camera-video` 어느 것도 넣지 않는다
  (ADR-0034).
- **기존 파일을 전문으로 덮어쓰지 않는다.** 추가·치환 지시만 따른다.
- `minSdk` 26, `targetSdk` 36, `compileSdk` 37.
- **코드 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md) 규약을 따른다.** 무엇을 하는지가 아니라 **왜
  그렇게 했는지**를 적는다. 코드를 읽으면 아는 사실을 옮겨 쓰지 않는다. 주석은 한국어다.
- **매퍼 단독 테스트(`XxxVOMapperTest`)와 DTO 직렬화 테스트를 만들지 않는다.** 판단이 든 변환은
  DataSource·Repository 테스트의 케이스로 덮는다.
- **라인번호를 문서에 적지 않는다.** 근거는 파일명과 심볼명으로 쓴다.
- 출력 영상 규격: `video/avc`, **720 × 1280(세로)**, 30fps, 6 Mbps, 키프레임 간격 1초, 무음.
- 타임라인: 도입 15프레임 + 토핑당 12프레임(팝인 8 + 유지 4) + 마무리 30프레임.
- 저장 위치 `Movies/Parfait`, MIME `video/mp4`, 파일명 `parfait_<epochMillis>.mp4`.
- 등장 순서는 **`ToppingTransform.positionZ` 오름차순**이다. `createdAt`을 쓰지 않는다.

---

## 파일 구성

**신규**

| 파일 | 책임 |
|------|------|
| `feature/groups/canvas/impl/.../util/ToppingVideoTimeline.kt` | 토핑 개수 → 프레임 목록. 순수 Kotlin |
| `feature/groups/canvas/impl/src/test/.../util/ToppingVideoTimelineTest.kt` | 위 JVM 테스트 |
| `core/util/android/.../video/BitmapSurfaceWriter.kt` | EGL 초기화 + 비트맵을 `Surface`에 그리는 통로 |
| `core/util/android/src/androidTest/.../video/BitmapSurfaceWriterTest.kt` | `ImageReader`로 픽셀 회수해 검증 |
| `core/util/android/.../video/Mp4VideoEncoder.kt` | `MediaCodec` + `MediaMuxer`로 mp4 쓰기 |
| `core/util/android/src/androidTest/.../video/Mp4VideoEncoderTest.kt` | `MediaMetadataRetriever`로 산출물 검증 |
| `domain/.../usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt` | 영상 갤러리 저장 UseCase |
| `data/src/test/.../repository/gallery/GalleryRepositoryImplTest.kt` | 영상 저장 경로 유닛 테스트 |
| `feature/groups/canvas/impl/.../component/CanvasVideoRecordLayer.kt` | 녹화용 오프스크린 토핑 레이어 |
| `feature/groups/canvas/impl/.../util/CanvasVideoRecorder.kt` | 프레임 루프 |

**수정**

| 파일 | 변경 |
|------|------|
| `feature/groups/canvas/api/.../NavKeyCanvasImageSave.kt` | `CanvasImageSaveResult` → `CanvasSaveResult` sealed, NavKey에 `toppingCount` 추가 |
| `feature/groups/canvas/impl/.../screen/CanvasImageSaveScreen.kt` | 하단 버튼 2개, 영상 버튼 비활성 조건 |
| `feature/groups/canvas/impl/.../route/CanvasImageSaveRoute.kt` | 두 콜백을 각 결과로 발행 |
| `feature/groups/canvas/impl/.../route/CanvasMainRoute.kt` | 결과 분기, 권한 슬롯 sealed화, 녹화 결선 |
| `feature/groups/canvas/impl/.../screen/CanvasMainScreen.kt` | 녹화 레이어 배치, Canvas-Area dp 폭 보고 |
| `feature/groups/canvas/impl/.../viewmodel/CanvasMainViewModel.kt` | 영상 인텐트·이펙트·상태 |
| `feature/groups/canvas/impl/src/main/res/values/strings.xml` | 버튼·토스트 문구 |
| `data/.../utils/GalleryMediaProvider.kt` | `insertPendingVideo` |
| `data/.../repository/gallery/GalleryRepositoryImpl.kt` | `saveVideoToGallery` |
| `domain/.../repository/gallery/GalleryRepository.kt` | `saveVideoToGallery` 선언 |
| `core/util/android/build.gradle.kts` | 없음 (플랫폼 API만 쓴다) |

---

### Task 1: 타임라인 계산 (`ToppingVideoTimeline`)

토핑 개수를 프레임 목록으로 바꾸는 순수 함수다. 안드로이드 의존이 없어 전부 JVM에서 검증된다.
이 Task만으로는 화면에 아무 변화가 없지만, 프레임 수와 팝인 진행도의 정의가 여기서 고정되므로
뒤의 모든 Task가 이 계약을 본다.

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimeline.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimelineTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `const val CANVAS_VIDEO_FRAMES_PER_SECOND: Int = 30`
  - `data class ToppingVideoFrame(val visibleCount: Int, val popProgress: Float)`
  - `fun toppingVideoFrames(toppingCount: Int): List<ToppingVideoFrame>`
  - `fun toppingPopScale(popProgress: Float): Float`
  - `fun toppingPopAlpha(popProgress: Float): Float`

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToppingVideoTimelineTest {
    @Test
    fun toppingVideoFrames_noToppings_isIntroPlusOutroOnly() {
        // When 토핑이 없다
        val frames = toppingVideoFrames(toppingCount = 0)

        // Then 도입 15 + 마무리 30 프레임만 남는다
        assertEquals(45, frames.size)
        assertTrue(frames.all { it.visibleCount == 0 })
    }

    @Test
    fun toppingVideoFrames_threeToppings_countsIntroToppingsAndOutro() {
        // When 토핑 3개
        val frames = toppingVideoFrames(toppingCount = 3)

        // Then 15 + 12*3 + 30
        assertEquals(81, frames.size)
    }

    @Test
    fun toppingVideoFrames_introFrames_showNothing() {
        // When 토핑 1개
        val frames = toppingVideoFrames(toppingCount = 1)

        // Then 앞 15프레임은 배경만이다
        assertTrue(frames.take(15).all { it.visibleCount == 0 && it.popProgress == 0f })
    }

    @Test
    fun toppingVideoFrames_lastFrame_showsEveryToppingSettled() {
        // When 토핑 4개
        val frames = toppingVideoFrames(toppingCount = 4)

        // Then 마지막 프레임은 전부 보이고 팝인이 끝나 있다 — 이미지 저장물과 같은 장면이어야 한다
        assertEquals(4, frames.last().visibleCount)
        assertEquals(1f, frames.last().popProgress, 0f)
    }

    @Test
    fun toppingVideoFrames_firstFrameOfEachTopping_startsPopFromZero() {
        // When 토핑 2개
        val frames = toppingVideoFrames(toppingCount = 2)

        // Then 각 토핑의 첫 프레임에서 그 토핑이 막 나타난다
        assertEquals(1, frames[15].visibleCount)
        assertEquals(0f, frames[15].popProgress, 0f)
        assertEquals(2, frames[27].visibleCount)
        assertEquals(0f, frames[27].popProgress, 0f)
    }

    @Test
    fun toppingVideoFrames_holdFrames_keepPopFinished() {
        // When 토핑 1개
        val frames = toppingVideoFrames(toppingCount = 1)

        // Then 팝인 8프레임 뒤 유지 4프레임은 진행도가 1이다
        assertTrue(frames.subList(23, 27).all { it.popProgress == 1f })
    }

    @Test
    fun toppingPopScale_atStart_isZero() {
        assertEquals(0f, toppingPopScale(0f), 0.0001f)
    }

    @Test
    fun toppingPopScale_atEnd_isOne() {
        assertEquals(1f, toppingPopScale(1f), 0.0001f)
    }

    @Test
    fun toppingPopScale_beforeEnd_overshootsAboveOne() {
        // Then 중간에 1을 넘겨야 튀어 오르는 인상이 난다
        assertTrue(toppingPopScale(0.6f) > 1f)
    }

    @Test
    fun toppingPopAlpha_halfway_isAlreadyOpaque() {
        // Then 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 선다
        assertEquals(1f, toppingPopAlpha(0.5f), 0.0001f)
    }

    @Test
    fun toppingPopAlpha_atStart_isTransparent() {
        assertEquals(0f, toppingPopAlpha(0f), 0.0001f)
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingVideoTimelineTest*"`
Expected: 컴파일 실패 — `Unresolved reference: toppingVideoFrames`

- [x] **Step 3: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

/** 프레임 인덱스로 진행하므로 기기 성능이 결과를 바꾸지 않는다 */
const val CANVAS_VIDEO_FRAMES_PER_SECOND: Int = 30

private const val INTRO_FRAMES = 15
private const val POP_FRAMES = 8
private const val HOLD_FRAMES = 4
private const val OUTRO_FRAMES = 30

/** 팝인이 1을 넘겨 되돌아오는 정도. A-001 스플래시의 spring 팝인과 결을 맞춘 값이다 */
private const val POP_OVERSHOOT = 0.08f

/** 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 서야 튀어 오르는 인상이 난다 */
private const val ALPHA_COMPLETION_POINT = 0.5f

/**
 * 한 프레임의 상태.
 *
 * @param visibleCount 이 프레임에 그려지는 토핑 개수. 등장 순서대로 앞에서 센다
 * @param popProgress 마지막으로 등장한 토핑의 팝인 진행도(0~1). [visibleCount] 가 0이면 0이다
 */
data class ToppingVideoFrame(
    val visibleCount: Int,
    val popProgress: Float,
)

/**
 * 토핑 [toppingCount] 개가 하나씩 등장하는 영상의 전체 프레임을 만든다.
 *
 * 마지막 프레임은 **모든 토핑이 팝인을 끝낸 상태**다. 이미지 저장물과 같은 장면이어야 하기 때문이다.
 */
fun toppingVideoFrames(toppingCount: Int): List<ToppingVideoFrame> {
    val frames = ArrayList<ToppingVideoFrame>(
        INTRO_FRAMES + (POP_FRAMES + HOLD_FRAMES) * toppingCount + OUTRO_FRAMES,
    )

    repeat(INTRO_FRAMES) { frames += ToppingVideoFrame(visibleCount = 0, popProgress = 0f) }

    for (index in 0 until toppingCount) {
        val visibleCount = index + 1
        repeat(POP_FRAMES) { step ->
            frames += ToppingVideoFrame(
                visibleCount = visibleCount,
                popProgress = step.toFloat() / POP_FRAMES,
            )
        }
        repeat(HOLD_FRAMES) { frames += ToppingVideoFrame(visibleCount = visibleCount, popProgress = 1f) }
    }

    repeat(OUTRO_FRAMES) { frames += ToppingVideoFrame(visibleCount = toppingCount, popProgress = 1f) }

    return frames
}

/**
 * 팝인 진행도를 배율로 옮긴다. 1을 넘겼다 돌아오는 곡선이라 튀어 오르는 인상이 난다.
 *
 * 0에서 정확히 0, 1에서 정확히 1이어야 한다 — 끝값이 어긋나면 마지막 프레임이 이미지 저장물과
 * 다른 크기로 앉는다.
 */
fun toppingPopScale(popProgress: Float): Float {
    val clamped = popProgress.coerceIn(0f, 1f)
    // sin(pi * t) 는 양 끝이 0이고 가운데가 1이라, 오버슈트 양을 그대로 실어 더하면 끝값이 보존된다
    val overshoot = POP_OVERSHOOT * kotlin.math.sin(Math.PI * clamped).toFloat()
    return clamped + overshoot
}

/** 알파는 [ALPHA_COMPLETION_POINT] 에서 이미 1이다 */
fun toppingPopAlpha(popProgress: Float): Float =
    (popProgress.coerceIn(0f, 1f) / ALPHA_COMPLETION_POINT).coerceAtMost(1f)
```

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingVideoTimelineTest*"`
Expected: PASS (11건)

- [x] **Step 5: ktlint를 통과시킨다**

Run: `./gradlew :feature:groups:canvas:impl:ktlintCheck`
Expected: PASS

---

### Task 2: 비트맵을 Surface에 그리는 EGL 통로 (`BitmapSurfaceWriter`)

`MediaCodec`의 입력 표면에는 `Canvas`로 그릴 수 없다. GL로 그려야 한다. 이 Task는 인코더와
무관하게 **"비트맵 한 장을 임의의 `Surface`에 그린다"**만 만든다. 소비자를 `ImageReader`로 두면
픽셀을 회수해 검증할 수 있어, 인코더가 끼기 전에 이 통로가 맞는지 먼저 확정된다.

**Files:**
- Create: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/video/BitmapSurfaceWriter.kt`
- Test: `core/util/android/src/androidTest/kotlin/com/teamyg/parfait/core/util/android/video/BitmapSurfaceWriterTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `class BitmapSurfaceWriter(surface: Surface) : AutoCloseable`
  - `fun BitmapSurfaceWriter.draw(bitmap: Bitmap, presentationTimeNanos: Long)`
  - `fun BitmapSurfaceWriter.close()`

- [x] **Step 1: 실패하는 계측 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.core.util.android.video

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.media.ImageReader
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class BitmapSurfaceWriterTest {
    private val width = 64
    private val height = 64

    @Test
    fun draw_solidBitmap_paintsThatColorOnSurface() {
        // Given RGBA_8888 을 받는 소비자와, 한 색으로 채운 비트맵
        val reader = ImageReader.newInstance(width, height, ImageFormat.FLEX_RGBA_8888, 2)
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(255, 0, 0)) }

        // When 그 비트맵을 표면에 그린다
        BitmapSurfaceWriter(reader.surface).use { writer ->
            writer.draw(bitmap = bitmap, presentationTimeNanos = 0L)
        }

        // Then 소비자가 받은 프레임의 가운데 픽셀이 그 색이다
        val image = reader.acquireNextImage()
        assertNotNull(image)
        val plane = image.planes[0]
        val buffer = plane.buffer
        val offset = (height / 2) * plane.rowStride + (width / 2) * plane.pixelStride
        assertEquals(255, buffer.get(offset).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 1).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 2).toInt() and 0xFF)

        image.close()
        reader.close()
        bitmap.recycle()
    }

    @Test
    fun draw_twoBitmaps_deliversTwoFrames() {
        // Given 소비자와 서로 다른 두 비트맵
        val reader = ImageReader.newInstance(width, height, ImageFormat.FLEX_RGBA_8888, 3)
        val first = createBitmap(width, height).apply { eraseColor(Color.rgb(255, 0, 0)) }
        val second = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 255, 0)) }

        // When 두 번 그린다
        BitmapSurfaceWriter(reader.surface).use { writer ->
            writer.draw(bitmap = first, presentationTimeNanos = 0L)
            writer.draw(bitmap = second, presentationTimeNanos = 33_333_333L)
        }

        // Then 프레임이 두 장 온다
        val firstImage = reader.acquireNextImage()
        assertNotNull(firstImage)
        firstImage.close()
        val secondImage = reader.acquireNextImage()
        assertNotNull(secondImage)
        secondImage.close()

        reader.close()
        first.recycle()
        second.recycle()
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :core:util:android:connectedDebugAndroidTest --tests "*BitmapSurfaceWriterTest*"`
Expected: 컴파일 실패 — `Unresolved reference: BitmapSurfaceWriter`

> 기기나 에뮬레이터가 붙어 있어야 한다. `adb devices`로 먼저 확인한다.

- [x] **Step 3: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.core.util.android.video

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * `MediaCodec` 입력 표면에는 `Surface.lockCanvas` 가 통하지 않는다 — 그 표면의 소비자가
 * 코덱이라 GL 생산자를 기대한다. 그래서 비트맵을 텍스처로 올려 사각형 하나에 입히는
 * 최소한의 GL 경로를 여기 한 벌만 둔다.
 *
 * 색 공간 변환은 GPU가 맡는다. 직접 YUV로 바꾸면 기기마다 갈리는 컬러 포맷을 앱이 떠안는다
 * (`adr/0034-canvas-video-onscreen-capture-encoding.md`).
 */
class BitmapSurfaceWriter(
    surface: Surface,
) : AutoCloseable {
    private val display: EGLDisplay
    private val context: EGLContext
    private val eglSurface: EGLSurface
    private val program: Int
    private val textureId: Int
    private val positionHandle: Int
    private val texCoordHandle: Int

    init {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "EGL 디스플레이를 얻지 못했다" }

        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "EGL 초기화에 실패했다" }

        val config = chooseConfig()
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0,
        )
        check(context != EGL14.EGL_NO_CONTEXT) { "EGL 컨텍스트를 만들지 못했다" }

        eglSurface = EGL14.eglCreateWindowSurface(
            display,
            config,
            surface,
            intArrayOf(EGL14.EGL_NONE),
            0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "EGL 윈도우 표면을 만들지 못했다" }

        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
            "EGL 컨텍스트를 현재로 만들지 못했다"
        }

        program = buildProgram()
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    /**
     * [bitmap] 을 표면 전체에 그리고 프레임을 제출한다.
     *
     * [presentationTimeNanos] 는 `eglPresentationTimeANDROID` 로 표면에 실린다 — 코덱이 이 값을
     * 프레임 타임스탬프로 읽으므로, 이 값이 없으면 영상의 재생 속도가 벽시계에 끌려간다.
     */
    fun draw(
        bitmap: Bitmap,
        presentationTimeNanos: Long,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        EGL14.eglPresentationTimeANDROID(display, eglSurface, presentationTimeNanos)
        check(EGL14.eglSwapBuffers(display, eglSurface)) { "프레임을 제출하지 못했다" }
    }

    override fun close() {
        if (display == EGL14.EGL_NO_DISPLAY) return

        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT,
        )
        EGL14.eglDestroySurface(display, eglSurface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(display)
    }

    private fun chooseConfig(): EGLConfig {
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            // 코덱이 소비할 표면이라는 표시. 이 플래그가 없으면 기기에 따라 인코딩이 조용히 깨진다
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        check(
            EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, configCount, 0) &&
                configCount[0] > 0,
        ) { "쓸 수 있는 EGL 설정이 없다" }

        return requireNotNull(configs[0])
    }

    private fun buildProgram(): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        check(linked[0] == GLES20.GL_TRUE) {
            "셰이더 프로그램 링크에 실패했다 - ${GLES20.glGetProgramInfoLog(program)}"
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return program
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        check(compiled[0] == GLES20.GL_TRUE) {
            "셰이더 컴파일에 실패했다 - ${GLES20.glGetShaderInfoLog(shader)}"
        }

        return shader
    }

    private companion object {
        /** `EGL_RECORDABLE_ANDROID`. EGL14 상수에 없어 직접 적는다 */
        const val EGL_RECORDABLE_ANDROID = 0x3142

        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        val vertexBuffer = floatBufferOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )

        /**
         * 세로가 뒤집혀 있다. 비트맵은 위에서 아래로 줄이 쌓이는데 GL 텍스처 좌표는 아래에서
         * 위로 올라가므로, 여기서 한 번 뒤집지 않으면 영상이 거꾸로 나온다.
         */
        val texCoordBuffer = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f,
        )

        fun floatBufferOf(vararg values: Float) = ByteBuffer
            .allocateDirect(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }
}
```

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :core:util:android:connectedDebugAndroidTest --tests "*BitmapSurfaceWriterTest*"`
Expected: PASS (2건)

> 가운데 픽셀이 빨강이 아니라면 `texCoordBuffer`의 상하 반전을 먼저 본다. 단색 비트맵은 반전을
> 드러내지 않으므로, 실패하면 위 절반만 칠한 비트맵으로 임시 확인한다.

- [x] **Step 5: ktlint를 통과시킨다**

Run: `./gradlew :core:util:android:ktlintCheck`
Expected: PASS

---

### Task 3: mp4 인코더 (`Mp4VideoEncoder`)

Task 2의 통로에 `MediaCodec`과 `MediaMuxer`를 붙여 실제 mp4를 쓴다. 도메인과 화면을 모르는
조각이므로 비트맵 시퀀스만 받는다.

**Files:**
- Create: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/video/Mp4VideoEncoder.kt`
- Test: `core/util/android/src/androidTest/kotlin/com/teamyg/parfait/core/util/android/video/Mp4VideoEncoderTest.kt`

**Interfaces:**
- Consumes: `BitmapSurfaceWriter(surface)`, `BitmapSurfaceWriter.draw(bitmap, presentationTimeNanos)`
- Produces:
  - `class Mp4VideoEncoder(outputFile: File, width: Int, height: Int, framesPerSecond: Int) : AutoCloseable`
  - `fun Mp4VideoEncoder.encodeFrame(bitmap: Bitmap)`
  - `fun Mp4VideoEncoder.finish()`

- [x] **Step 1: 실패하는 계측 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.core.util.android.video

import android.graphics.Color
import android.media.MediaMetadataRetriever
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@LargeTest
class Mp4VideoEncoderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val width = 720
    private val height = 1280
    private val framesPerSecond = 30

    @Test
    fun finish_thirtyFrames_writesOneSecondPlayableMp4() {
        // Given 출력 파일과 한 색으로 채운 프레임
        val output = File(context.cacheDir, "encoder_test_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 0, 255)) }

        // When 30프레임을 인코딩한다
        Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        ).use { encoder ->
            repeat(framesPerSecond) { encoder.encodeFrame(bitmap) }
            encoder.finish()
        }

        // Then 열리는 mp4 가 나오고 길이가 1초에 가깝다
        assertTrue(output.exists())
        assertTrue(output.length() > 0L)

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(output.absolutePath)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLong()
        val videoWidth = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toInt()
        val videoHeight = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toInt()
        retriever.release()

        assertEquals(width, videoWidth)
        assertEquals(height, videoHeight)
        assertTrue("길이가 $durationMs ms 다", durationMs != null && durationMs in 800L..1400L)

        output.delete()
        bitmap.recycle()
    }

    @Test
    fun close_withoutFinish_leavesNoPartialFileBehind() {
        // Given 출력 파일
        val output = File(context.cacheDir, "encoder_abort_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 0, 255)) }

        // When finish 없이 닫는다 (인코딩 중 실패 상황)
        Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        ).use { encoder ->
            encoder.encodeFrame(bitmap)
        }

        // Then 재생할 수 없는 파일을 남기지 않는다
        assertTrue("중단한 산출물이 남았다", !output.exists() || output.length() == 0L)

        output.delete()
        bitmap.recycle()
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :core:util:android:connectedDebugAndroidTest --tests "*Mp4VideoEncoderTest*"`
Expected: 컴파일 실패 — `Unresolved reference: Mp4VideoEncoder`

- [x] **Step 3: 최소 구현을 쓴다**

```kotlin
package com.teamyg.parfait.core.util.android.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
import java.io.File

private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
private const val BIT_RATE = 6_000_000
private const val I_FRAME_INTERVAL_SECONDS = 1
private const val DEQUEUE_TIMEOUT_MICROS = 10_000L

/**
 * 비트맵 시퀀스를 무음 mp4 로 쓴다.
 *
 * 프레임 타임스탬프를 벽시계가 아니라 **프레임 번호 ÷ [framesPerSecond]** 로 계산한다 —
 * 인코딩이 느린 기기에서도 재생 속도가 같아야 한다.
 *
 * [finish] 를 부르지 않고 닫으면 **산출물을 지운다.** 헤더가 덜 쓰인 mp4 는 재생할 수 없는데,
 * 남겨 두면 호출부가 그것을 갤러리에 올릴 수 있다.
 */
class Mp4VideoEncoder(
    private val outputFile: File,
    width: Int,
    height: Int,
    private val framesPerSecond: Int,
) : AutoCloseable {
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
    private val muxer: MediaMuxer
    private val writer: BitmapSurfaceWriter
    private val bufferInfo = MediaCodec.BufferInfo()

    private var trackIndex = -1
    private var isMuxerStarted = false
    private var frameIndex = 0
    private var isFinished = false

    init {
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, framesPerSecond)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        writer = BitmapSurfaceWriter(codec.createInputSurface())
        codec.start()

        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    /** [bitmap] 을 다음 프레임으로 넣는다. 호출 순서가 곧 재생 순서다 */
    fun encodeFrame(bitmap: Bitmap) {
        check(!isFinished) { "이미 마감한 인코더에 프레임을 넣었다" }

        val presentationTimeNanos = frameIndex.toLong() * 1_000_000_000L / framesPerSecond
        writer.draw(bitmap = bitmap, presentationTimeNanos = presentationTimeNanos)
        frameIndex++

        drainCodec(endOfStream = false)
    }

    /** 남은 출력을 모두 받아 쓰고 컨테이너를 닫는다. 이 호출이 끝나야 재생 가능한 파일이 된다 */
    fun finish() {
        check(!isFinished) { "인코더를 두 번 마감했다" }

        codec.signalEndOfInputStream()
        drainCodec(endOfStream = true)
        isFinished = true
    }

    override fun close() {
        runCatching { writer.close() }
        runCatching {
            codec.stop()
            codec.release()
        }
        runCatching {
            if (isMuxerStarted) muxer.stop()
            muxer.release()
        }

        if (!isFinished && outputFile.exists()) {
            // 헤더가 덜 쓰인 mp4 는 재생되지 않는다. 호출부가 그것을 갤러리에 올리지 못하게 지운다
            val deleted = outputFile.delete()
            if (!deleted) {
                coreUtilAndroidLogger.w { "중단한 mp4 를 지우지 못했다 - path: ${outputFile.absolutePath}" }
            }
        }
    }

    /**
     * 코덱이 내놓은 출력을 muxer 로 옮긴다.
     *
     * @param endOfStream 참이면 EOS 플래그를 받을 때까지 기다린다. 거짓이면 지금 나온 것만 가져간다 —
     *   프레임마다 기다리면 인코더 파이프라인이 직렬화돼 느려진다
     */
    private fun drainCodec(endOfStream: Boolean) {
        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_MICROS)

            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }

                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!isMuxerStarted) { "출력 포맷이 두 번 바뀌었다" }
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    isMuxerStarted = true
                }

                outputIndex >= 0 -> {
                    val buffer = requireNotNull(codec.getOutputBuffer(outputIndex))

                    // CODEC_CONFIG 은 addTrack 이 이미 포맷으로 받아 간 값이라 트랙에 쓰면 중복이다
                    val isCodecConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!isCodecConfig && bufferInfo.size > 0) {
                        check(isMuxerStarted) { "트랙을 열기 전에 샘플이 나왔다" }
                        buffer.position(bufferInfo.offset)
                        buffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
}
```

- [x] **Step 4: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :core:util:android:connectedDebugAndroidTest --tests "*Mp4VideoEncoderTest*"`
Expected: PASS (2건)

- [x] **Step 5: ktlint를 통과시킨다**

Run: `./gradlew :core:util:android:ktlintCheck`
Expected: PASS

---

### Task 4: 갤러리 영상 저장 슬라이스

인코딩이 끝난 mp4 파일을 갤러리에 등록한다. 이미지 경로의 `IS_PENDING` 2단 커밋 구조를 그대로
따르되, 입력이 비트맵이 아니라 파일이라 스트림을 복사한다.

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/utils/GalleryMediaProvider.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/gallery/GalleryRepositoryImpl.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/gallery/GalleryRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt`
- Create: `data/src/test/java/com/teamyg/parfait/data/repository/gallery/GalleryRepositoryImplTest.kt`

**Interfaces:**
- Consumes: 없음 (Task 1~3과 독립)
- Produces:
  - `GalleryMediaProvider.insertPendingVideo(displayName: String): Uri?`
  - `GalleryMediaProvider.finalizePendingVideo(uri: Uri)`
  - `GalleryRepository.saveVideoToGallery(videoFilePath: String, displayName: String): Result<Unit>`
  - `SaveCanvasVideoToGalleryUseCase.invoke(videoFilePath: String, displayName: String): Result<Unit>`

- [x] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.data.repository.gallery

import android.net.Uri
import com.teamyg.parfait.data.utils.GalleryMediaProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File

class GalleryRepositoryImplTest {
    private val galleryMediaProvider: GalleryMediaProvider = mockk(relaxed = true)
    private val uri: Uri = mockk(relaxed = true)

    private val repository = GalleryRepositoryImpl(galleryMediaProvider = galleryMediaProvider)

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun givenVideoFile(bytes: ByteArray = byteArrayOf(1, 2, 3)): File =
        File.createTempFile("canvas_video", ".mp4").apply {
            writeBytes(bytes)
            deleteOnExit()
        }

    @Test
    fun saveVideoToGallery_streamOpens_copiesBytesAndFinalizes() = runTest {
        // Given 등록이 되고 출력 스트림이 열린다
        val file = givenVideoFile(byteArrayOf(7, 8, 9))
        val output = ByteArrayOutputStream()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns output

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 파일 내용이 그대로 들어가고 대기 표시가 내려간다
        assertTrue(result.isSuccess)
        assertTrue(output.toByteArray().contentEquals(byteArrayOf(7, 8, 9)))
        verify(exactly = 1) { galleryMediaProvider.finalizePendingVideo(uri) }
    }

    @Test
    fun saveVideoToGallery_insertFails_returnsFailureWithoutDeleting() = runTest {
        // Given 등록 자체가 실패한다
        val file = givenVideoFile()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns null

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 지울 대상이 없으므로 삭제를 부르지 않는다
        assertTrue(result.isFailure)
        verify(exactly = 0) { galleryMediaProvider.deleteImage(any()) }
    }

    @Test
    fun saveVideoToGallery_outputStreamNull_deletesRegistration() = runTest {
        // Given 등록은 됐지만 스트림이 안 열린다
        val file = givenVideoFile()
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns null

        // When 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = file.absolutePath,
            displayName = "parfait_1.mp4",
        )

        // Then 빈 항목이 갤러리에 남지 않게 등록을 되돌린다
        assertTrue(result.isFailure)
        verify(exactly = 1) { galleryMediaProvider.deleteImage(uri) }
        verify(exactly = 0) { galleryMediaProvider.finalizePendingVideo(uri) }
    }

    @Test
    fun saveVideoToGallery_missingFile_returnsFailureAndDeletesRegistration() = runTest {
        // Given 등록은 됐는데 원본 파일이 없다
        every { galleryMediaProvider.insertPendingVideo(any()) } returns uri
        every { galleryMediaProvider.openOutputStream(uri) } returns ByteArrayOutputStream()

        // When 없는 경로로 저장한다
        val result = repository.saveVideoToGallery(
            videoFilePath = "/does/not/exist.mp4",
            displayName = "parfait_1.mp4",
        )

        // Then 실패로 접고 등록을 되돌린다
        assertTrue(result.isFailure)
        verify(exactly = 1) { galleryMediaProvider.deleteImage(uri) }
    }
}
```

- [x] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*GalleryRepositoryImplTest*"`
Expected: 컴파일 실패 — `Unresolved reference: insertPendingVideo`

- [x] **Step 3: `GalleryMediaProvider`에 영상 등록을 더한다**

`GalleryMediaProvider.kt` 상단 상수 옆에 더한다.

```kotlin
private const val VIDEO_MIME_TYPE = "video/mp4"
```

`insertPendingImage` 아래에 더한다. **기존 함수들은 손대지 않는다.**

```kotlin
    /**
     * 영상 컬렉션에 새 항목을 등록한다. 구조는 [insertPendingImage] 와 같고 갈리는 것은 컬렉션·MIME·
     * 저장 디렉토리뿐이다 — 이미지와 영상은 서로 다른 MediaStore 컬렉션이라 한 함수로 합칠 수 없다.
     */
    fun insertPendingVideo(displayName: String): Uri? {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SAVE_SUBDIRECTORY")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        return context.contentResolver?.insert(collection, values)
    }

    /** [insertPendingVideo] 로 걸어 둔 IS_PENDING 표시를 내려, 갤러리 앱에 실제로 보이게 한다. */
    fun finalizePendingVideo(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val values = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
        context.contentResolver?.update(uri, values, null, null)
    }
```

- [x] **Step 4: `GalleryRepository`에 선언을 더한다**

`GalleryRepository.kt`의 `saveImageToGallery` 아래에 더한다.

```kotlin
    /**
     * [videoFilePath] 의 mp4 를 기기 갤러리에 새 영상으로 저장한다.
     *
     * 비트맵이 아니라 경로를 받는 이유: 영상은 인코더가 이미 파일로 완성해 둔다. 바이트를 메모리로
     * 올려 나르면 수십 MB 를 한 번 더 들고 있게 된다.
     */
    suspend fun saveVideoToGallery(
        videoFilePath: String,
        displayName: String,
    ): Result<Unit>
```

- [x] **Step 5: `GalleryRepositoryImpl`에 구현을 더한다**

`saveImageToGallery` 아래에 더한다. import에 `java.io.File`을 추가한다.

```kotlin
    /**
     * [saveImageToGallery] 와 같은 IS_PENDING 2단 커밋이다. 다른 점은 압축하지 않고 완성된 파일을
     * 그대로 복사하는 것뿐이다.
     */
    override suspend fun saveVideoToGallery(
        videoFilePath: String,
        displayName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runSuspendCatching {
            val source = File(videoFilePath)
            val uri = galleryMediaProvider.insertPendingVideo(displayName)
                ?: error("갤러리에 영상을 등록하지 못했다")

            try {
                galleryMediaProvider.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("갤러리 영상의 출력 스트림을 열지 못했다")

                galleryMediaProvider.finalizePendingVideo(uri)
            } catch (throwable: Throwable) {
                galleryMediaProvider.deleteImage(uri)
                throw throwable
            }
        }
    }
```

- [x] **Step 6: UseCase를 만든다**

```kotlin
package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

/**
 * 인코딩이 끝난 캔버스 타임랩스 mp4 를 기기 갤러리에 저장한다.
 *
 * 녹화와 인코딩은 화면 계층 책임이라(컴포지션 캡처다) 여기서는 완성된 파일만 받는다 —
 * [SaveCanvasToGalleryUseCase] 가 이미 만들어진 비트맵만 받는 것과 같은 경계다.
 */
class SaveCanvasVideoToGalleryUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "SaveCanvasVideoToGalleryUseCase::init" }
    }

    suspend operator fun invoke(
        videoFilePath: String,
        displayName: String,
    ): Result<Unit> = galleryRepository
        .saveVideoToGallery(videoFilePath = videoFilePath, displayName = displayName)
        .onFailure { throwable ->
            useCaseLogger.e(throwable) { "SaveCanvasVideoToGalleryUseCase - 갤러리 저장 실패" }
        }
}
```

- [x] **Step 7: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*GalleryRepositoryImplTest*"`
Expected: PASS (4건)

- [x] **Step 8: 모듈을 컴파일하고 ktlint를 통과시킨다**

Run: `./gradlew :domain:compileDebugKotlin :data:compileDebugKotlin :domain:ktlintCheck :data:ktlintCheck`
Expected: PASS

---

### Task 5: 저장 결과 타입을 sealed로 가르고 NavKey에 토핑 개수를 실는다

미리보기 화면이 두 갈래의 저장을 알릴 수 있어야 한다. 그리고 **토핑 0건일 때 동영상 버튼을
비활성화하려면 미리보기 화면이 토핑 개수를 알아야 한다** — 지금 NavKey에는 `imagePath`와 `date`만
있어서 알 길이 없다. 스펙이 이 전달 경로를 빠뜨렸으므로 여기서 함께 채운다.

⚠️ **이 Task 도중에는 `:feature:groups:canvas:impl` 컴파일이 깨진다.** 타입을 바꾸면 두 Route가
동시에 고쳐져야 한다. Step 2와 Step 3은 한 덩이다.

**Files:**
- Modify: `feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/NavKeyCanvasImageSave.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `sealed interface CanvasSaveResult`, `CanvasSaveResult.Image(val imagePath: String)`, `CanvasSaveResult.Video`
  - `NavKeyCanvasImageSave(val imagePath: String, val date: String, val toppingCount: Int)`
  - 결과 키 `CANVAS_IMAGE_SAVE_RESULT_KEY`는 이름과 값 그대로 유지한다

- [ ] **Step 1: api 모듈의 타입을 바꾼다**

`NavKeyCanvasImageSave.kt`에서 `data class NavKeyCanvasImageSave`와 `CanvasImageSaveResult`를
아래로 치환한다. `CANVAS_IMAGE_SAVE_RESULT_KEY` 선언은 그대로 둔다.

```kotlin
/**
 * 갤러리에 무엇이 저장될지 먼저 보여 주는 미리보기 화면.
 *
 * 캡처한 비트맵을 키에 실을 수 없어(NavKey 는 직렬화돼 오간다) 캔버스 메인이 캐시에 PNG 로
 * 굽고 그 자리만 넘긴다.
 *
 * @param imagePath 캡처한 캔버스 PNG 의 경로. 미리보기가 닫히면 쓸모가 없어지는 캐시 파일이다
 * @param date 어느 날의 캔버스인지. `LocalDate.toString()`(ISO-8601) 형태 — 이 모듈은
 *  kotlinx-datetime 을 쓰지 않아 문자열로 나르고, 받는 쪽이 `LocalDate.parse` 로 되돌린다
 * @param toppingCount 그 캔버스의 토핑 개수. 동영상 저장은 쌓을 것이 있어야 성립하므로 0이면
 *  그 버튼을 비활성화한다 — 미리보기는 평면 PNG 만 받아 토핑을 셀 수 없어 여기로 실어 나른다
 */
@Serializable
data class NavKeyCanvasImageSave(
    val imagePath: String,
    val date: String,
    val toppingCount: Int,
) : NavKey

/**
 * 미리보기에서 저장을 확정했다는 결과.
 *
 * 저장 자체를 여기서 하지 않는 이유: 결과 토스트가 뜨는 자리는 캔버스 메인이다. 미리보기가
 * 저장까지 하고 나면 알림만 남기고 사라지는 화면이 되어, 실패했을 때 알릴 곳이 없다.
 *
 * 종류를 플래그가 아니라 sealed 로 가르는 이유: [Video] 는 [Image.imagePath] 를 쓰지 않는다.
 * 한 클래스에 두면 아무도 읽지 않는 값이 결과에 실린다.
 */
sealed interface CanvasSaveResult {
    /** @param imagePath 저장할 이미지의 경로. 넘겨받은 [NavKeyCanvasImageSave.imagePath] 를 그대로 돌려준다 */
    data class Image(val imagePath: String) : CanvasSaveResult

    /**
     * 캔버스 메인이 토핑 배치 순서대로 프레임을 찍어 동영상을 만든다. 경로를 싣지 않는 이유는
     * 평면 PNG 가 아니라 **토핑 목록**에서 다시 그리기 때문이다.
     */
    data object Video : CanvasSaveResult
}
```

- [ ] **Step 2: 미리보기 Route가 두 결과를 발행하게 고친다**

`CanvasImageSaveRoute.kt`의 import에서 `CanvasImageSaveResult`를 `CanvasSaveResult`로 바꾸고,
`CanvasImageSaveScreen` 호출을 아래로 치환한다.

```kotlin
        CanvasImageSaveScreen(
            bitmap = capturedBitmap,
            fallbackImagePath = navKey.imagePath,
            date = LocalDate.parse(navKey.date),
            isVideoSaveEnabled = navKey.toppingCount > 0,
            onClickClose = { navigator.onBack() },
            onClickSaveImage = {
                resultEventBus.sendResult(
                    CANVAS_IMAGE_SAVE_RESULT_KEY,
                    CanvasSaveResult.Image(imagePath = navKey.imagePath),
                )
                navigator.onBack()
            },
            onClickSaveVideo = {
                resultEventBus.sendResult(
                    CANVAS_IMAGE_SAVE_RESULT_KEY,
                    CanvasSaveResult.Video,
                )
                navigator.onBack()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
```

- [ ] **Step 3: 캔버스 메인이 두 결과를 갈라 받게 고친다**

`CanvasMainRoute.kt`에서 `ResultEffect<CanvasImageSaveResult>` 블록을 아래로 치환한다. import도
`CanvasSaveResult`로 바꾼다. `saveWithPermission`은 아직 비트맵만 받는다 — Task 7에서 일반화한다.

```kotlin
    // 미리보기에서 저장을 확정하고 돌아왔다. 이미지는 보여 준 그림 그대로 남겨야 하므로 캔버스를
    // 다시 캡처하지 않고 미리보기가 쓰던 파일을 읽는다. 동영상은 평면 PNG 로는 만들 수 없어
    // ViewModel 이 든 토핑 목록에서 다시 그린다
    ResultEffect<CanvasSaveResult>(resultKey = CANVAS_IMAGE_SAVE_RESULT_KEY) { result ->
        when (result) {
            is CanvasSaveResult.Image -> scope.launch {
                withContext(Dispatchers.IO) { readCanvasCaptureCache(result.imagePath) }
                    .onSuccess(saveWithPermission)
                    .onFailure { toastPolicy.showError(gallerySaveFailureMessage) }
            }

            is CanvasSaveResult.Video ->
                viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)
        }
    }
```

- [ ] **Step 4: 미리보기로 넘어가는 자리에 토핑 개수를 싣는다**

`CanvasMainRoute.kt`의 `RequestCanvasCaptureForPreview` 분기에서 `NavKeyCanvasImageSave` 생성을
아래로 치환한다.

```kotlin
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                    toppingCount = viewModel.state.value.toppings.size,
                                ),
                            )
```

- [ ] **Step 5: 컴파일이 통과하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:api:compileDebugKotlin`
Expected: PASS

> `:feature:groups:canvas:impl`은 Task 6에서 `CanvasImageSaveScreen`의 파라미터를 바꾸고
> Task 7에서 `OnClickSaveVideoToGallery` 인텐트를 만들 때까지 깨진 채다. 예상된 상태다.

---

### Task 6: 미리보기 화면에 동영상 버튼을 더한다

하단 버튼이 하나에서 둘로 늘어난다. 이미지 저장이 주 동작이므로 Primary를 유지하고, 동영상은
그 아래 보조 버튼으로 둔다. 토핑이 0건이면 동영상 버튼을 비활성화한다.

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasImageSaveScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `NavKeyCanvasImageSave.toppingCount` (Task 5)
- Produces:
  - `CanvasImageSaveScreen(bitmap, fallbackImagePath, date, isVideoSaveEnabled, onClickClose, onClickSaveImage, onClickSaveVideo, modifier)`

- [ ] **Step 1: 문구를 더한다**

`strings.xml`의 `canvas_image_save_confirm` 아래에 더한다.

```xml
    <string name="canvas_image_save_video">토핑 쌓이는 영상으로 저장</string>
    <string name="canvas_main_gallery_video_save_success">%1$d월 %2$d일의 캔버스 영상이 갤러리에 저장됐어요</string>
    <string name="canvas_main_gallery_video_save_failure">영상 저장에 실패했어요. 나중에 다시 시도해 주세요.</string>
```

- [ ] **Step 2: 화면 시그니처와 하단 버튼 영역을 고친다**

`CanvasImageSaveScreen`의 파라미터에서 `onClickSave`를 지우고 셋을 더한다.

```kotlin
internal fun CanvasImageSaveScreen(
    bitmap: ImageBitmap?,
    fallbackImagePath: String,
    date: LocalDate,
    isVideoSaveEnabled: Boolean,
    onClickClose: () -> Unit,
    onClickSaveImage: () -> Unit,
    onClickSaveVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

하단의 `Box { YGButton(...) }` 블록을 아래로 치환한다.

```kotlin
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding7,
                ),
        ) {
            YGButton(
                text = stringResource(R.string.canvas_image_save_confirm),
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = true,
                onClick = onClickSaveImage,
            )

            // 쌓일 것이 없으면 영상이 성립하지 않는다 — 토핑 0건에서는 누를 수 없다
            YGButton(
                text = stringResource(R.string.canvas_image_save_video),
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = isVideoSaveEnabled,
                onClick = onClickSaveVideo,
            )
        }
```

> `YGButtonType.Medium.Secondary`가 없으면 `YGButtonType`의 실제 하위 타입을 확인해 보조 스타일에
> 해당하는 것으로 바꾼다. 디자인 시스템에 없는 타입을 새로 만들지 않는다.

- [ ] **Step 3: 프리뷰를 고친다**

`PreviewCanvasImageSaveScreen`의 호출을 아래로 치환한다.

```kotlin
    CanvasImageSaveScreen(
        bitmap = null,
        fallbackImagePath = "",
        date = date,
        isVideoSaveEnabled = true,
        onClickClose = {},
        onClickSaveImage = {},
        onClickSaveVideo = {},
        modifier = Modifier.fillMaxSize(),
    )
```

- [ ] **Step 4: 컴파일과 ktlint를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: `OnClickSaveVideoToGallery` 미정의로만 실패한다. 다른 오류가 나면 이 Task 안에서 고친다.

---

### Task 7: ViewModel의 영상 저장 경로

녹화 시작 요청, 녹화 중 상태, 완성된 파일의 갤러리 저장, 결과 알림을 ViewModel에 붙인다. 녹화
자체는 화면이 하므로 ViewModel은 상태와 저장만 든다.

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: `SaveCanvasVideoToGalleryUseCase.invoke(videoFilePath, displayName)` (Task 4)
- Produces:
  - `CanvasMainIntent.OnClickSaveVideoToGallery`
  - `CanvasMainIntent.SaveRecordedVideo(val videoFilePath: String)`
  - `CanvasMainIntent.CanvasVideoRecordFailed`
  - `CanvasMainEffect.StartCanvasVideoRecording`
  - `CanvasMainUiState.isRecordingVideo: Boolean`
  - `CanvasMainEffect.ShowGallerySaveResult(isSuccess, date, isVideo)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasMainViewModelTest.kt`에 더한다. 기존 테스트의 `createViewModel` 헬퍼 이름과 인자는 파일을
열어 그대로 따른다. `saveCanvasVideoToGallery` mock을 생성자 인자에 추가해야 한다.

```kotlin
    @Test
    fun onClickSaveVideoToGallery_always_startsRecordingAndRaisesFlag() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑이 있는 오늘 캔버스
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            // When 동영상 저장을 누른다
            viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)

            // Then 화면에 녹화를 요청하고 녹화 중 표시를 세운다
            assertEquals(CanvasMainEffect.StartCanvasVideoRecording, awaitItem())
            assertTrue(viewModel.state.value.isRecordingVideo)
        }
    }

    @Test
    fun saveRecordedVideo_useCaseSucceeds_reportsVideoSuccessAndClearsFlag() = runTest(mainDispatcherRule.dispatcher) {
        // Given 갤러리 저장이 성공한다
        coEvery { saveCanvasVideoToGallery(any(), any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val today = viewModel.state.value.today

        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)
            assertEquals(CanvasMainEffect.StartCanvasVideoRecording, awaitItem())

            // When 녹화가 끝난 파일을 넘긴다
            viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(videoFilePath = "/cache/a.mp4"))
            advanceUntilIdle()

            // Then 영상 성공으로 알리고 녹화 중 표시를 내린다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = true, date = today, isVideo = true),
                awaitItem(),
            )
            assertFalse(viewModel.state.value.isRecordingVideo)
        }
    }

    @Test
    fun saveRecordedVideo_useCaseFails_reportsVideoFailureAndClearsFlag() = runTest(mainDispatcherRule.dispatcher) {
        // Given 갤러리 저장이 실패한다
        coEvery { saveCanvasVideoToGallery(any(), any()) } returns Result.failure(RuntimeException("저장 실패"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        val today = viewModel.state.value.today

        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)
            assertEquals(CanvasMainEffect.StartCanvasVideoRecording, awaitItem())

            // When 녹화가 끝난 파일을 넘긴다
            viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(videoFilePath = "/cache/a.mp4"))
            advanceUntilIdle()

            // Then 영상 실패로 알린다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = false, date = today, isVideo = true),
                awaitItem(),
            )
            assertFalse(viewModel.state.value.isRecordingVideo)
        }
    }

    @Test
    fun canvasVideoRecordFailed_always_reportsVideoFailureWithoutCallingUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 녹화 도중 실패한다
        val viewModel = createViewModel()
        advanceUntilIdle()
        val today = viewModel.state.value.today

        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)
            assertEquals(CanvasMainEffect.StartCanvasVideoRecording, awaitItem())

            // When 화면이 실패를 알린다
            viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
            advanceUntilIdle()

            // Then 갤러리 저장을 부르지 않고 실패만 알린다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = false, date = today, isVideo = true),
                awaitItem(),
            )
            assertFalse(viewModel.state.value.isRecordingVideo)
            coVerify(exactly = 0) { saveCanvasVideoToGallery(any(), any()) }
        }
    }

    @Test
    fun saveCapturedCanvas_stillImagePath_reportsWithVideoFlagFalse() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미지 저장이 성공한다
        coEvery { saveCanvasToGallery(any(), any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val today = viewModel.state.value.today

        viewModel.effect.test {
            // When 캡처한 비트맵을 저장한다
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(mockk(relaxed = true)))
            advanceUntilIdle()

            // Then 기존 이미지 경로는 isVideo 가 거짓이다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = true, date = today, isVideo = false),
                awaitItem(),
            )
        }
    }
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*"`
Expected: 컴파일 실패 — `Unresolved reference: OnClickSaveVideoToGallery`

> ⚠️ `ShowGallerySaveResult`에 인자가 하나 늘어나므로 **기존 갤러리 저장 테스트 2건도 함께
> 고쳐야** 컴파일된다. 기존 단언에 `isVideo = false`를 더한다.

- [ ] **Step 3: 상태·이펙트·인텐트를 더한다**

`CanvasMainUiState`의 `tutorialStep` 아래에 더한다.

```kotlin
    /**
     * 캔버스 타임랩스 영상을 만드는 중. 참인 동안 녹화용 레이어가 컴포지션에 들어가고 로딩
     * 오버레이가 화면을 덮는다.
     *
     * 진행률을 두지 않는 이유는 취소 경로를 만들지 않기로 확정했기 때문이다 — 불린 하나면 족하다.
     */
    val isRecordingVideo: Boolean = false,
```

`CanvasMainEffect`의 `ShowGallerySaveResult`를 아래로 치환하고 새 이펙트를 더한다.

```kotlin
    /**
     * 캔버스 타임랩스 영상을 찍어 달라는 요청.
     *
     * 프레임 캡처는 컴포지션을 읽는 일이라 화면만 할 수 있다 — [RequestCanvasCaptureForPreview] 와
     * 같은 이유로 ViewModel 은 요청만 보낸다. 화면은 끝나면 [CanvasMainIntent.SaveRecordedVideo] 로,
     * 실패하면 [CanvasMainIntent.CanvasVideoRecordFailed] 로 돌아온다.
     */
    data object StartCanvasVideoRecording : CanvasMainEffect

    /** @param isVideo 이미지와 영상의 토스트 문구가 갈린다. 저장 경로 자체는 같은 사건이라 이펙트를 나누지 않는다 */
    data class ShowGallerySaveResult(
        val isSuccess: Boolean,
        val date: LocalDate,
        val isVideo: Boolean,
    ) : CanvasMainEffect
```

`CanvasMainIntent`의 `SaveCapturedCanvas` 아래에 더한다.

```kotlin
    /** 미리보기에서 동영상 저장을 확정하고 돌아왔다 */
    data object OnClickSaveVideoToGallery : CanvasMainIntent

    /** 화면이 녹화를 마쳐 캐시에 mp4 를 완성했다 */
    data class SaveRecordedVideo(val videoFilePath: String) : CanvasMainIntent

    /** 녹화나 인코딩이 실패했다. 저장할 파일이 없다 */
    data object CanvasVideoRecordFailed : CanvasMainIntent
```

- [ ] **Step 4: 인텐트 분기와 핸들러를 더한다**

`processIntent`의 `SaveCapturedCanvas` 분기 아래에 더한다.

```kotlin
            is CanvasMainIntent.OnClickSaveVideoToGallery -> handleClickSaveVideoToGallery()

            is CanvasMainIntent.SaveRecordedVideo -> handleSaveRecordedVideo(intent.videoFilePath)

            is CanvasMainIntent.CanvasVideoRecordFailed -> handleCanvasVideoRecordFailed()
```

`handleSaveCapturedCanvas` 아래에 더하고, 같은 함수 안의 `ShowGallerySaveResult` 두 곳에
`isVideo = false`를 더한다.

```kotlin
    private fun handleClickSaveVideoToGallery() {
        updateState { copy(isRecordingVideo = true) }
        postSideEffect(effect = CanvasMainEffect.StartCanvasVideoRecording)
    }

    private fun handleSaveRecordedVideo(videoFilePath: String) {
        val date = state.value.selectedDate

        launch(key = SAVE_CANVAS_VIDEO_TO_GALLERY_KEY) {
            val displayName = "parfait_${System.currentTimeMillis()}.mp4"

            val result = saveCanvasVideoToGalleryUseCase(
                videoFilePath = videoFilePath,
                displayName = displayName,
            )

            // 성공이든 실패든 녹화는 끝났다 — 오버레이를 먼저 걷지 않으면 토스트가 딤 아래에 깔린다
            updateState { copy(isRecordingVideo = false) }
            postSideEffect(
                effect = CanvasMainEffect.ShowGallerySaveResult(
                    isSuccess = result.isSuccess,
                    date = date,
                    isVideo = true,
                ),
            )
        }
    }

    private fun handleCanvasVideoRecordFailed() {
        val date = state.value.selectedDate

        updateState { copy(isRecordingVideo = false) }
        postSideEffect(
            effect = CanvasMainEffect.ShowGallerySaveResult(isSuccess = false, date = date, isVideo = true),
        )
    }
```

`companion object`의 `SAVE_CANVAS_TO_GALLERY_KEY` 아래에 더한다.

```kotlin
        const val SAVE_CANVAS_VIDEO_TO_GALLERY_KEY = "saveCanvasVideoToGallery"
```

생성자에 UseCase를 더한다. import도 함께 추가한다.

```kotlin
    private val saveCanvasVideoToGalleryUseCase: SaveCanvasVideoToGalleryUseCase,
```

- [ ] **Step 5: 테스트가 통과하는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*"`
Expected: PASS (기존 전량 + 신규 5건)

- [ ] **Step 6: ktlint를 통과시킨다**

Run: `./gradlew :feature:groups:canvas:impl:ktlintCheck`
Expected: PASS

---

### Task 8: 녹화 레이어와 프레임 루프를 결선한다

마지막 조각이다. 녹화용 오프스크린 레이어를 컴포지션에 넣고, 프레임 루프를 돌려 인코더에
밀어넣고, 권한 흐름을 일반화한다. 컴포지션 캡처라 자동 테스트로 덮을 수 없다 — 실기기 확인으로
마감한다.

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasVideoRecordLayer.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasVideoRecorder.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`

**Interfaces:**
- Consumes: `toppingVideoFrames`, `toppingPopScale`, `toppingPopAlpha`, `CANVAS_VIDEO_FRAMES_PER_SECOND`
  (Task 1) · `Mp4VideoEncoder` (Task 3) · `CanvasMainEffect.StartCanvasVideoRecording`,
  `CanvasMainIntent.SaveRecordedVideo`, `CanvasMainIntent.CanvasVideoRecordFailed`,
  `CanvasMainUiState.isRecordingVideo` (Task 7)
- Produces: 없음 (마지막 Task)

- [ ] **Step 1: `CanvasToppingLayer`에 등장 진행도를 주입할 파라미터를 더한다**

기본값을 두어 기존 호출부 세 곳은 한 줄도 바뀌지 않는다.

`CanvasToppingLayer`의 파라미터 목록에 더한다.

```kotlin
    /**
     * 그릴 토핑 개수. 앞에서부터 이만큼만 그린다. `null` 이면 전부 그린다.
     *
     * 타임랩스 녹화가 프레임마다 이 값을 올려 토핑이 하나씩 나타나게 만든다.
     */
    visibleToppingCount: Int? = null,
    /** [visibleToppingCount] 의 마지막 토핑에만 적용하는 팝인 진행도(0~1) */
    lastToppingPopProgress: Float = 1f,
```

`entries`를 만든 직후에 자르는 줄을 더한다.

```kotlin
        // 녹화가 프레임마다 개수를 올린다. 자르기만 하면 되는 이유는 목록이 이미 positionZ
        // 오름차순, 즉 배치 순서라서다
        val visibleEntries = visibleToppingCount
            ?.let { count -> entries.take(count) }
            ?: entries
```

`entries.forEach`를 `visibleEntries.forEach`로 바꾸고, 그 안에서 마지막 항목에만 팝인을 입힌다.

```kotlin
            visibleEntries.forEachIndexed { index, entry ->
                if (entry.topping.parfaitImageId != spotlightedToppingId) {
                    val isLast = index == visibleEntries.lastIndex
                    val popProgress = if (isLast) lastToppingPopProgress else 1f

                    CanvasTopping(
                        entry = entry,
                        canvasWidth = areaWidth,
                        canvasHeight = areaHeight,
                        onClick = { onClickTopping(entry.topping) },
                        clickable = hitTestEnabled,
                        popProgress = popProgress,
                    )
                }
            }
```

`CanvasTopping`에 파라미터를 더하고, `graphicsLayer` 블록에서 배율과 알파를 입힌다.

```kotlin
    popProgress: Float = 1f,
```

```kotlin
            .graphicsLayer {
                rotationZ = transform.rotation.toFloat()
                // 테두리 띠가 상자 밖으로 나가는데 alpha < 1 이면 그만큼 잘린다 — 진행 중에만 건다
                if (popProgress < 1f) {
                    val pop = toppingPopScale(popProgress)
                    scaleX = pop
                    scaleY = pop
                    alpha = toppingPopAlpha(popProgress)
                }
            }
```

> ⚠️ `hitTestEnabled && toppingsVisible` 블록의 `entries`도 `visibleEntries`로 바꿔야 한다.
> 안 보이는 토핑이 눌리면 안 된다.

- [ ] **Step 2: 녹화용 레이어를 만든다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState

/**
 * 타임랩스 녹화만을 위해 토핑 레이어를 한 벌 더 띄운다. **화면에는 나타나지 않는다** —
 * `drawContent` 를 레이어에 기록만 하고 `drawLayer` 를 부르지 않는다.
 *
 * 사용자에게 보이는 캔버스를 프레임마다 흔들지 않으려고 따로 띄운다. 그쪽을 쓰면 녹화 중
 * 캔버스가 깜빡이고, 스포트라이트·판정 같은 화면 상태가 영상에 섞인다.
 *
 * ⚠️ **[canvasWidth]·[canvasHeight] 는 화면의 Canvas-Area 와 같은 dp 여야 한다.** 토핑 테두리
 * 굵기가 화면 기준 dp 고정이라(ADR-0025), dp 폭이 달라지면 캔버스 폭에 대한 굵기 비율이 달라져
 * 영상의 테두리가 이미지 저장물과 다른 두께로 앉는다.
 *
 * 출력 픽셀 크기는 [Density] 를 덮어써서 고정한다 — dp 기하는 화면과 같게 두고 픽셀만 옮기는
 * 유일한 방법이다(`adr/0033-canvas-video-onscreen-capture-encoding.md`).
 *
 * @param targetWidthPx 산출물의 가로 픽셀. [canvasWidth] 와 함께 밀도를 정한다
 */
@Composable
internal fun CanvasVideoRecordLayer(
    toppings: List<CanvasToppingVO>,
    visibleToppingCount: Int,
    lastToppingPopProgress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    targetWidthPx: Int,
    captureLayer: GraphicsLayer,
    onLoadStateChange: (CanvasLoadState) -> Unit,
) {
    val recordDensity = Density(density = targetWidthPx / canvasWidth.value)

    CompositionLocalProvider(LocalDensity provides recordDensity) {
        Box(
            modifier = Modifier
                // 부모 constraints 로 clamp 되면 해상도가 조용히 줄어든다
                .requiredSize(width = canvasWidth, height = canvasHeight)
                .drawWithContent {
                    // drawLayer 를 부르지 않는다 — 기록만 하고 화면에는 내보내지 않는다
                    captureLayer.record { this@drawWithContent.drawContent() }
                },
        ) {
            CanvasToppingLayer(
                toppings = toppings,
                spotlightedToppingId = null,
                onClickTopping = {},
                onClickSpotlightDim = {},
                hitTestEnabled = false,
                // 하나씩 등장하는 것이 이 영상의 전부다. 한꺼번에 드러내는 빗장을 걸면 안 된다
                revealTogether = false,
                visibleToppingCount = visibleToppingCount,
                lastToppingPopProgress = lastToppingPopProgress,
                onLoadStateChange = onLoadStateChange,
                modifier = Modifier.requiredSize(width = canvasWidth, height = canvasHeight),
            )
        }
    }
}
```

- [ ] **Step 3: 프레임 루프를 만든다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.teamyg.parfait.core.util.android.video.Mp4VideoEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 산출물 가로 픽셀. 캔버스 영역이 세로 9:16 이라 높이는 이 값의 16/9 다 */
const val CANVAS_VIDEO_WIDTH_PX = 720
const val CANVAS_VIDEO_HEIGHT_PX = 1280

private const val VIDEO_CACHE_FILE_NAME = "canvas_timelapse.mp4"

/**
 * 캔버스 타임랩스 mp4 를 캐시에 만든다.
 *
 * 프레임 하나를 만드는 일은 **상태를 한 칸 전진시키고 그려질 때까지 기다린 뒤 레이어를 읽는
 * 것**이다. 그 세 단계가 화면 계층에 묶여 있어 이 함수는 호출부에서 받은 [advanceFrame] 으로
 * 앞의 둘을 위임하고, 읽기와 인코딩만 맡는다.
 *
 * 벽시계를 보지 않는다 — 프레임 번호가 타임스탬프를 정하므로 느린 기기에서도 재생 속도가 같다.
 *
 * @param advanceFrame 프레임 인덱스를 받아 그 상태가 실제로 **그려질 때까지** 대기한다.
 *   호출부가 `withFrameNanos` 로 프레임 경계를 기다려야 한다
 * @return 완성된 mp4 파일. 실패하면 `Result.failure` 이고 부분 파일은 남지 않는다
 */
suspend fun recordCanvasVideo(
    context: Context,
    frameCount: Int,
    captureLayer: GraphicsLayer,
    advanceFrame: suspend (frameIndex: Int) -> Unit,
): Result<File> = runCatching {
    val output = File(context.cacheDir, VIDEO_CACHE_FILE_NAME)

    Mp4VideoEncoder(
        outputFile = output,
        width = CANVAS_VIDEO_WIDTH_PX,
        height = CANVAS_VIDEO_HEIGHT_PX,
        framesPerSecond = CANVAS_VIDEO_FRAMES_PER_SECOND,
    ).use { encoder ->
        for (frameIndex in 0 until frameCount) {
            advanceFrame(frameIndex)

            val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
            // 인코딩은 CPU·코덱 일이라 메인 스레드에서 돌리면 다음 프레임의 그리기를 막는다
            withContext(Dispatchers.Default) { encoder.encodeFrame(bitmap) }
            bitmap.recycle()
        }

        encoder.finish()
    }

    output
}
```

- [ ] **Step 4: 화면에 녹화 레이어를 넣고 Canvas-Area dp 폭을 보고한다**

`CanvasMainScreen`에 파라미터를 더한다.

```kotlin
    recordLayer: GraphicsLayer,
    recordVisibleToppingCount: Int,
    recordLastToppingPopProgress: Float,
    isRecordingVideo: Boolean,
    onCanvasAreaSizeChange: (width: Dp, height: Dp) -> Unit,
```

`CanvasToppingLayer`를 담은 `Box` 안에 더한다. 그 `Box`가 Canvas-Area와 같은 크기다.

```kotlin
            val density = LocalDensity.current

            // 녹화 레이어가 같은 dp 폭을 써야 테두리 굵기 비율이 이미지 저장물과 같아진다
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .onSizeChanged { size ->
                        with(density) {
                            onCanvasAreaSizeChange(size.width.toDp(), size.height.toDp())
                        }
                    },
            )

            if (isRecordingVideo) {
                CanvasVideoRecordLayer(
                    toppings = canvasState.toppings,
                    visibleToppingCount = recordVisibleToppingCount,
                    lastToppingPopProgress = recordLastToppingPopProgress,
                    canvasWidth = recordedCanvasWidth,
                    canvasHeight = recordedCanvasHeight,
                    targetWidthPx = CANVAS_VIDEO_WIDTH_PX,
                    captureLayer = recordLayer,
                    onLoadStateChange = { recordToppingState = it },
                )
            }
```

> 이 Step의 정확한 배치는 `CanvasMainScreen`의 실제 레이아웃을 열어 맞춘다. 지켜야 할 조건은
> 둘이다. 녹화 레이어가 **Canvas-Area와 같은 dp 크기**를 받는 것, 그리고 화면에 보이는
> `CanvasToppingLayer`의 파라미터가 **하나도 바뀌지 않는** 것이다.

- [ ] **Step 5: Route에서 녹화를 돌리고 권한 흐름을 일반화한다**

`CanvasMainRoute.kt`의 `pendingGalleryBitmap`과 런처, `saveWithPermission`을 아래로 치환한다.

```kotlin
    // WRITE_EXTERNAL_STORAGE 요청은 Activity 가 있어야만 가능해, 저장할 것을 여기서 들고 있다가
    // 승인이 오면 그때 ViewModel 로 넘긴다(API 29+ 는 애초에 필요 없어 안 걸린다).
    // 이미지는 비트맵, 영상은 파일 경로라 한 타입으로는 못 담는다
    var pendingGallerySave by remember { mutableStateOf<PendingGallerySave?>(null) }
    val galleryWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingGallerySave
        pendingGallerySave = null
        when {
            pending == null -> Unit

            !granted -> {
                viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                    .takeIf { pending is PendingGallerySave.Video }
                    ?: toastPolicy.showError(gallerySaveFailureMessage)
            }

            pending is PendingGallerySave.Image ->
                viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(pending.bitmap))

            pending is PendingGallerySave.Video ->
                viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(pending.filePath))
        }
    }

    val saveWithPermission: (PendingGallerySave) -> Unit = { pending ->
        if (GalleryWritePermissionManager.hasPermission(context)) {
            when (pending) {
                is PendingGallerySave.Image ->
                    viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(pending.bitmap))

                is PendingGallerySave.Video ->
                    viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(pending.filePath))
            }
        } else {
            pendingGallerySave = pending
            galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
        }
    }
```

파일 하단에 타입을 더한다.

```kotlin
/** 권한 승인을 기다리는 저장 대상. 이미지와 영상이 서로 다른 것을 나르므로 갈라 둔다 */
private sealed interface PendingGallerySave {
    data class Image(val bitmap: Bitmap) : PendingGallerySave

    data class Video(val filePath: String) : PendingGallerySave
}
```

`Image` 결과 분기의 `.onSuccess(saveWithPermission)`을 아래로 바꾼다.

```kotlin
                    .onSuccess { bitmap -> saveWithPermission(PendingGallerySave.Image(bitmap)) }
```

녹화 상태와 이펙트 처리를 더한다.

```kotlin
    val recordLayer = rememberGraphicsLayer()
    var recordVisibleToppingCount by remember { mutableIntStateOf(0) }
    var recordLastToppingPopProgress by remember { mutableFloatStateOf(1f) }
    var recordToppingState by remember { mutableStateOf(CanvasLoadState.Loading) }
    var canvasAreaWidth by remember { mutableStateOf(0.dp) }
    var canvasAreaHeight by remember { mutableStateOf(0.dp) }
```

`effect.collect`의 `when`에 분기를 더한다.

```kotlin
                is CanvasMainEffect.StartCanvasVideoRecording -> {
                    val frames = toppingVideoFrames(
                        toppingCount = viewModel.state.value.toppings.size,
                    )

                    // 첫 프레임을 빈 그림으로 찍지 않는다 — 토핑 이미지가 다 모여야 시작한다
                    snapshotFlow { recordToppingState }
                        .first { it != CanvasLoadState.Loading }

                    if (recordToppingState == CanvasLoadState.Failed) {
                        viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                    } else {
                        recordCanvasVideo(
                            context = context,
                            frameCount = frames.size,
                            captureLayer = recordLayer,
                        ) { frameIndex ->
                            val frame = frames[frameIndex]
                            recordVisibleToppingCount = frame.visibleCount
                            recordLastToppingPopProgress = frame.popProgress
                            // 상태를 세운 뒤 실제로 그려진 프레임을 기다린다. 이 대기가 없으면
                            // 직전 상태를 두 번 찍는다
                            withFrameNanos { }
                        }.onSuccess { file ->
                            saveWithPermission(PendingGallerySave.Video(file.absolutePath))
                        }.onFailure {
                            viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                        }
                    }
                }
```

`ShowGallerySaveResult` 분기에서 문구를 갈라야 한다. 기존 분기를 찾아 `effect.isVideo`로 문구를
고른다.

```kotlin
                is CanvasMainEffect.ShowGallerySaveResult -> {
                    if (effect.isSuccess) {
                        val format = if (effect.isVideo) {
                            gallerySaveVideoSuccessFormat
                        } else {
                            gallerySaveSuccessFormat
                        }
                        toastPolicy.show(format.format(effect.date.monthNumber, effect.date.dayOfMonth))
                    } else {
                        toastPolicy.showError(
                            if (effect.isVideo) gallerySaveVideoFailureMessage else gallerySaveFailureMessage,
                        )
                    }
                }
```

> 기존 성공·실패 분기의 실제 호출 형태(`toastPolicy.show` 인자, `date` 포맷)는 파일을 열어 그대로
> 따른다. 위는 문구를 어디서 가르는지만 보여 준다.

문구 변수를 더한다.

```kotlin
    val gallerySaveVideoSuccessFormat = stringResource(R.string.canvas_main_gallery_video_save_success)
    val gallerySaveVideoFailureMessage = stringResource(R.string.canvas_main_gallery_video_save_failure)
```

`CanvasMainScreen` 호출에 인자를 더한다.

```kotlin
                recordLayer = recordLayer,
                recordVisibleToppingCount = recordVisibleToppingCount,
                recordLastToppingPopProgress = recordLastToppingPopProgress,
                isRecordingVideo = canvasState.isRecordingVideo,
                onCanvasAreaSizeChange = { width, height ->
                    canvasAreaWidth = width
                    canvasAreaHeight = height
                },
```

- [ ] **Step 6: 로딩 오버레이를 덮는다**

`CanvasMainRoute`의 스캐폴드 안, 튜토리얼 오버레이 옆에 더한다.

```kotlin
        // 녹화 중에는 화면을 덮는다. 오버레이가 클릭을 삼켜 별도 차단 장치가 필요 없다
        if (canvasState.isRecordingVideo) {
            YGLoadingOverlay()
        }
```

- [ ] **Step 7: 전체 빌드와 테스트를 돌린다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: PASS

Run: `./gradlew testDebugUnitTest`
Expected: PASS (기존 전량 + 이 계획이 더한 유닛)

Run: `./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 8: 실기기로 확인한다 (사용자 판정)**

저장소에서 확인할 수 없는 항목이다. 아래 일곱 갈래를 사람이 본다.

1. **오늘 캔버스, 토핑 3개** — 저장 → 미리보기 → 동영상 저장. 갤러리 `Movies/Parfait`에 mp4가
   생기고 재생된다. 토핑이 배치 순서대로 하나씩 팝인한다.
2. **마지막 프레임 대조** — 같은 캔버스를 이미지로도 저장해, 영상 마지막 프레임과 **테두리 굵기와
   토핑 위치가 같은지** 본다. 이것이 ADR-0033이 진 가장 큰 위험이다.
3. **지난(마감된) 캔버스** — 달력에서 지난 날을 골라 같은 흐름을 태운다.
4. **토핑 0건** — 동영상 버튼이 비활성이다.
5. **녹화 중 화면** — 로딩 오버레이가 덮여 있고, 그 아래 캔버스가 깜빡이지 않는다.
6. **토핑 다수(10개 이상)** — 생성 시간이 견딜 만한지, 영상 길이가 `0.5 + 0.4N + 1.0`초에 맞는지.
7. **회전·방향** — 영상이 뒤집히거나 좌우가 바뀌지 않았다. `BitmapSurfaceWriter`의 텍스처 좌표
   반전이 맞는지 여기서 드러난다.

---

## 자기 점검 결과

**스펙 대조** — 스펙의 모든 절이 Task에 대응한다. 다만 **스펙에 없던 전달 경로 하나를 계획이
채웠다**: 미리보기 화면이 토핑 개수를 몰라 "토핑 0건이면 버튼 비활성"을 실행할 수 없었다.
`NavKeyCanvasImageSave.toppingCount`를 Task 5에서 더한다. 스펙 본문도 같이 고쳐야 한다.

**미해결로 남기는 것** — 버튼 레이블·토스트 문구는 Task 6에서 임시 문구를 넣었다. 디자인이 오면
`strings.xml`만 고치면 된다. `YGButtonType`의 보조 스타일 이름은 실제 타입을 확인해 맞춘다.

**타입 일관성** — `CanvasSaveResult`(Task 5) → `OnClickSaveVideoToGallery`(Task 7) →
`StartCanvasVideoRecording`(Task 7) → `recordCanvasVideo`(Task 8)의 이름이 Task 사이에서 일치한다.
`ShowGallerySaveResult`의 `isVideo` 인자 추가가 기존 테스트 2건을 깨뜨리는 것을 Task 7 Step 2에
적었다.
