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
 * (`adr/0035-canvas-video-onscreen-capture-encoding.md`).
 *
 * ⚠️ **이 인스턴스를 생성한 스레드에서만 [draw]·[close] 를 호출해야 한다.** EGL 컨텍스트가 그
 * 스레드에 묶여 있어, 다른 스레드에서 부르면 예외 없이 그리기가 조용히 실패한다
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

    private var isClosed = false

    init {
        var createdDisplay = EGL14.EGL_NO_DISPLAY
        var createdContext = EGL14.EGL_NO_CONTEXT
        var createdSurface = EGL14.EGL_NO_SURFACE

        try {
            createdDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(createdDisplay != EGL14.EGL_NO_DISPLAY) { "EGL 디스플레이를 얻지 못했다" }

            val version = IntArray(2)
            check(EGL14.eglInitialize(createdDisplay, version, 0, version, 1)) { "EGL 초기화에 실패했다" }

            val config = chooseConfig(createdDisplay)
            createdContext = EGL14.eglCreateContext(
                createdDisplay,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
            check(createdContext != EGL14.EGL_NO_CONTEXT) { "EGL 컨텍스트를 만들지 못했다" }

            createdSurface = EGL14.eglCreateWindowSurface(
                createdDisplay,
                config,
                surface,
                intArrayOf(EGL14.EGL_NONE),
                0,
            )
            check(createdSurface != EGL14.EGL_NO_SURFACE) { "EGL 윈도우 표면을 만들지 못했다" }

            check(EGL14.eglMakeCurrent(createdDisplay, createdSurface, createdSurface, createdContext)) {
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
        } catch (throwable: Throwable) {
            // 생성자가 던지면 close() 를 부를 인스턴스가 없다. 여기서 놓으면 EGL 객체가 샌다
            releaseEgl(createdDisplay, createdSurface, createdContext)
            throw throwable
        }

        display = createdDisplay
        context = createdContext
        eglSurface = createdSurface
    }

    /**
     * [bitmap] 을 표면 전체에 그리고 프레임을 제출한다.
     *
     * ⚠️ 소비자가 받아가기 전에 여러 번 몰아 부르면 앞 프레임이 조용히 유실될 수 있다(`BufferQueue`
     * 소비자 쪽 동작). 한 장 그릴 때마다 소비자가 가져가게 한다.
     *
     * ⚠️ `Config.HARDWARE` 비트맵은 `ARGB_8888` 로 복사해 올린다. `GLUtils.texImage2D` 가 HARDWARE 를
     * 거부하는데 `GraphicsLayer.toImageBitmap()` 은 API 28 이상에서 그것을 돌려준다 — **이 복사를
     * 지우면 녹화가 항상 실패한다**(스펙 `canvas-topping-timelapse-video` 「주의」).
     *
     * @param presentationTimeNanos 코덱이 프레임 타임스탬프로 읽는다. 없으면 재생 속도가 벽시계에
     *   끌려간다
     */
    fun draw(
        bitmap: Bitmap,
        presentationTimeNanos: Long,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 호출부의 bitmap 은 호출부가 recycle 한다. 여기서 만든 사본만 회수한다
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

        releaseEgl(display, eglSurface, context)
    }

    private fun releaseEgl(
        display: EGLDisplay,
        surface: EGLSurface,
        context: EGLContext,
    ) {
        if (display == EGL14.EGL_NO_DISPLAY) return

        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT,
        )
        if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(display)
    }

    private fun chooseConfig(display: EGLDisplay): EGLConfig {
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
