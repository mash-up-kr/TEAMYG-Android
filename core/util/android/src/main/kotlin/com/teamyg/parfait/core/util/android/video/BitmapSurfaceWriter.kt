package com.teamyg.parfait.core.util.android.video

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
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
 * (`adr/0033-canvas-video-onscreen-capture-encoding.md`).
 *
 * **스레드 종속**: EGL 컨텍스트는 그것을 `eglMakeCurrent` 로 현재로 만든 스레드에 묶인다.
 * 이 클래스는 생성자(`init`)에서 그 스레드에 컨텍스트를 current 로 만들고 이후 다시 바꾸지
 * 않으므로, **이 인스턴스를 생성한 스레드에서만 [draw]·[close] 를 호출해야 한다.** 다른
 * 스레드(예: 인코더가 별도 디스패처에서 프레임을 넣는 경우)에서 부르면 예외 없이 그리기가
 * 조용히 실패한다 — GL 호출이 "current 컨텍스트가 없다"는 이유로 무시되기 때문이다.
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

    // display 등을 val 로 유지하고(생성 후 불변인 EGL 핸들이라는 의미가 분명해진다) close() 의
    // 중복 호출 방지는 별도 플래그로 가른다. display 를 var 로 두고 EGL_NO_DISPLAY 로 되돌리는
    // 방식도 가능하지만, "핸들이 유효한지"와 "이미 정리했는지"를 하나의 값에 겹쳐 표현하면
    // 읽는 쪽에서 헷갈린다.
    private var isClosed = false

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
     *
     * **소비자가 매번 즉시 받아가야 한다.** 소비자가 이전 프레임을 받기 전에 [draw] 를 여러 번
     * 몰아서 부르면, 아직 소비되지 않은 이전 프레임이 조용히 유실될 수 있다 — 실제로 이
     * 프로젝트가 검증에 쓴 에뮬레이터(Pixel_7_API_36)에서 `ImageReader.acquireNextImage()` 로
     * 재현했고, GL 을 전혀 쓰지 않는 순수 `Surface.lockCanvas`/`unlockCanvasAndPost` 만으로도
     * 똑같이 재현돼 이 클래스의 버그가 아니라 그 기기의 `BufferQueue` 소비자 쪽 동작임을
     * 확인했다. 프레임을 미리 여러 장 그려두지 말고, 한 장 그릴 때마다 소비자가 가져가게 한다.
     *
     * **[bitmap] 이 `Config.HARDWARE` 면 업로드 직전에 `ARGB_8888` 로 복사한다.**
     * `GLUtils.texImage2D` 는 HARDWARE 비트맵을 받으면 `IllegalArgumentException("invalid
     * Bitmap format")` 을 던진다(`GLUtils.java` 네이티브 구현이 하드웨어 버퍼를 CPU 에서 읽는
     * 경로를 지원하지 않는다). 그런데 이 클래스를 실제로 부르는 [CanvasVideoRecorder] 는 매
     * 프레임 `GraphicsLayer.toImageBitmap()` 으로 비트맵을 얻는데, Compose(`ui-graphics-android`,
     * `LayerSnapshot.android.kt` 의 `LayerSnapshotV28`) 는 API 28(P) 이상에서 이 호출이 내부적으로
     * `Bitmap.createBitmap(Picture)` 를 타 **항상** HARDWARE 비트맵을 돌려준다 — 즉 이 변환은
     * 예외적인 경로가 아니라 실기기에서 [draw] 가 받는 **기본값**이다. 변환을 생략하면 이
     * 앱이 지원하는 사실상 모든 기기(API 28+)에서 녹화 첫 프레임부터 실패한다.
     * 이 복사는 프레임마다 전체 해상도(예: 720x1280) 비트맵 하나를 그대로 다시 뜨는 비용이 든다
     * — 작지 않지만, 없으면 기능 자체가 죽으므로 "불필요한 낭비"로 보고 지우면 안 된다.
     * `HARDWARE` 가 아닌 비트맵(기존 계측 테스트가 쓰는 소프트웨어 `ARGB_8888` 등)은 원래도
     * `texImage2D` 가 그대로 받아들이므로 이 경로를 타지 않아 비용이 붙지 않는다.
     */
    fun draw(
        bitmap: Bitmap,
        presentationTimeNanos: Long,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 호출부가 넘긴 bitmap 의 수명은 이 함수가 책임지지 않는다(호출부가 recycle 한다) —
        // 여기서 새로 만든 임시 사본만 이 함수가 직접 회수한다
        val uploadBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        try {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, uploadBitmap, 0)
        } finally {
            if (uploadBitmap !== bitmap) uploadBitmap.recycle()
        }

        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        EGLExt.eglPresentationTimeANDROID(display, eglSurface, presentationTimeNanos)
        check(EGL14.eglSwapBuffers(display, eglSurface)) { "프레임을 제출하지 못했다" }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true

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
            -1f,
            -1f,
            1f,
            -1f,
            -1f,
            1f,
            1f,
            1f,
        )

        /**
         * 세로가 뒤집혀 있다. 비트맵은 위에서 아래로 줄이 쌓이는데 GL 텍스처 좌표는 아래에서
         * 위로 올라가므로, 여기서 한 번 뒤집지 않으면 영상이 거꾸로 나온다.
         */
        val texCoordBuffer = floatBufferOf(
            0f,
            1f,
            1f,
            1f,
            0f,
            0f,
            1f,
            0f,
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
