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
import com.teamyg.parfait.data.utils.providerLogger
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
 *
 * 이미지 하나의 길이는 [MediaItem.Builder.setImageDurationMs] 로 정한다 — 이게 없으면
 * `DefaultAssetLoaderFactory` 가 고정 길이 이미지로 인식하지 못하고 일반 미디어 취급하는
 * `ExoPlayerAssetLoader` 로 흘려보내는데, 정지 이미지 트랙에는 재생 길이가 없어
 * `IllegalArgumentException("Could not retrieve required duration")` 로 실패한다.
 * [EditedMediaItem.Builder.setDurationUs]·[EditedMediaItem.Builder.setFrameRate] 는 그
 * 길이를 합성 타임라인에 얼마나 반영할지(트림·프레임 수)를 정하는 것이라 별개로 필요하다.
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
            val mediaItem = MediaItem
                .Builder()
                .setUri(frame.toUri())
                .setImageDurationMs(frameDurationMs)
                .build()

            EditedMediaItem
                .Builder(mediaItem)
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
                                providerLogger.e(exportException) {
                                    "CanvasVideoEncoderImpl - 인코딩 실패: ${exportException.errorCodeName}"
                                }
                                if (continuation.isActive) continuation.resumeWith(Result.failure(exportException))
                            }
                        },
                    ).build()

                transformer.start(composition, outputFile.absolutePath)
                continuation.invokeOnCancellation { transformer.cancel() }
            }
        }
    }.onFailure { throwable ->
        providerLogger.e(throwable) { "CanvasVideoEncoderImpl::encode - 실패" }
    }
}
