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
 * 비트맵을 GL 텍스처로 올려 `Surface` 에 그린다. `MediaCodec` 입력 표면은 GL 생산자를 기대해
 * `Surface.lockCanvas` 가 통하지 않는다. 색 공간 변환도 GPU 가 맡는다.
 *
 * **생성한 스레드에서만 [draw]·[close] 를 불러야 한다.** EGL 컨텍스트가 그 스레드에 묶여,
 * 다른 스레드에서는 예외 없이 그리기가 조용히 실패한다.
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
     * [presentationTimeNanos] 가 코덱의 프레임 타임스탬프가 된다.
     *
     * **소비자가 매번 즉시 받아가야 한다.** 몰아서 그리면 기기에 따라 소비 전 프레임이 조용히
     * 유실된다(`BufferQueue` 소비자 쪽 동작이라 여기서 못 고친다).
     *
     * ⚠️ **`Config.HARDWARE` 사본 변환을 지우지 않는다.** `GLUtils.texImage2D` 는 HARDWARE 비트맵을
     * 거부하는데, Compose `GraphicsLayer.toImageBitmap()` 은 API 28+ 에서 HARDWARE 를 돌려준다.
     * 프레임마다 복사 비용이 들지만 없으면 녹화가 항상 실패한다.
     */
    fun draw(
        bitmap: Bitmap,
        presentationTimeNanos: Long,
    ) {
        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

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

        /** 세로가 뒤집혀 있다. 비트맵은 위→아래, GL 텍스처 좌표는 아래→위라 안 뒤집으면 영상이 거꾸로 나온다 */
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
