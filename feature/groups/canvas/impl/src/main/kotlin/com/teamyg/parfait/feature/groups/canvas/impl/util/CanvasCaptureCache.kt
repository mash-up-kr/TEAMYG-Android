package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

private const val CAPTURE_DIR_NAME = "canvas_capture"
private const val CAPTURE_FILE_NAME = "canvas_preview.png"

/** PNG 는 무손실이라 이 값을 보지 않지만, [Bitmap.compress] 가 인자를 요구한다 */
private const val PNG_QUALITY = 100

/**
 * 캡처한 캔버스를 캐시에 PNG 로 굽고 그 파일을 돌려준다.
 *
 * 이름을 매번 새로 짓지 않는 이유: 이 이미지는 미리보기 한 번을 위해서만 살아 있으면 되고,
 * 이름을 고정해야 저장을 그만둔 캡처가 캐시에 쌓이지 않는다.
 *
 * 디스크를 만지므로 호출부가 IO 디스패처를 잡아야 한다.
 */
internal fun Bitmap.writeToCanvasCaptureCache(context: Context): Result<File> = runCatching {
    val directory = File(context.cacheDir, CAPTURE_DIR_NAME).apply { mkdirs() }

    File(directory, CAPTURE_FILE_NAME).also { file ->
        file.outputStream().use { output -> compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output) }
    }
}

/**
 * 미리보기가 돌려준 캡처를 다시 읽는다.
 *
 * 캔버스를 한 번 더 캡처하지 않는 이유: 사용자가 보고 확정한 그림과 갤러리에 남는 그림이
 * 같아야 한다.
 *
 * 디스크를 만지므로 호출부가 IO 디스패처를 잡아야 한다.
 */
internal fun readCanvasCaptureCache(path: String): Result<Bitmap> = runCatching {
    // 파일이 사라졌거나 PNG 로 읽히지 않으면 decodeFile 은 예외 없이 null 을 준다
    checkNotNull(BitmapFactory.decodeFile(path)) { "캡처한 캔버스를 읽지 못했다: $path" }
}
