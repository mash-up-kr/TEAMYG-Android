package com.teamyg.parfait.data.provider

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val CANVAS_VIDEO_FRAME_RATE = 30

/**
 * [Transformer] 는 호출 스레드에 Looper 가 있어야 해서 [Dispatchers.Main] 에서 돌린다.
 * 이미지 시퀀스는 [EditedMediaItem.durationUs]·[EditedMediaItem.frameRate] 로 노출 시간을
 * 정하고, [Transformer] 가 그 시간만큼의 프레임을 만들어 이어 붙인다.
 */
@UnstableApi
class CanvasVideoEncoderImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : CanvasVideoEncoder {
    override suspend fun encode(
        frames: List<File>,
        frameDurationMs: Long,
        outputFile: File,
    ): Result<Unit> = runSuspendCatching {
        val editedItems = frames.map { frame ->
            EditedMediaItem
                .Builder(MediaItem.fromUri(frame.toUri()))
                .setDurationUs(frameDurationMs.milliseconds.inWholeMicroseconds)
                .setFrameRate(CANVAS_VIDEO_FRAME_RATE)
                .build()
        }
        val composition = Composition
            .Builder(EditedMediaItemSequence.Builder(editedItems).build())
            .build()

        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val transformer = Transformer
                    .Builder(context)
                    .addListener(
                        object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult,
                            ) {
                                if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                if (continuation.isActive) continuation.resumeWith(Result.failure(exportException))
                            }
                        },
                    ).build()

                transformer.start(composition, outputFile.absolutePath)
                continuation.invokeOnCancellation { transformer.cancel() }
            }
        }
    }
}
