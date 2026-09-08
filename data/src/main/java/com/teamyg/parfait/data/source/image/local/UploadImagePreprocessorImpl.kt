package com.teamyg.parfait.data.source.image.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.UPLOAD_JPEG_QUALITY
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.model.image.UploadImagePlan
import com.teamyg.parfait.data.model.image.UploadImageSize
import com.teamyg.parfait.data.model.image.planUploadImage
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.image.ImageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadImagePreprocessorImpl
@Inject
constructor() : UploadImagePreprocessor {
    override suspend fun prepare(
        file: File,
        imageType: ImageType,
    ): Result<PreparedUploadImage> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFormat = UploadImageFormat.ofExtension(file.extension)
                ?: throw UnsupportedImageException("서버가 받지 않는 확장자다 - ${file.extension}")
            val sourceSize = decodeSize(file)

            when (val plan = planUploadImage(sourceSize, imageType, sourceFormat)) {
                UploadImagePlan.Passthrough -> {
                    PreparedUploadImage(file = file, format = sourceFormat, isTemporary = false)
                }

                is UploadImagePlan.Reencode -> {
                    val reencoded = writeReencoded(file, plan)
                    sourceLogger.i {
                        "업로드 이미지를 줄였다 - ${sourceSize.width}x${sourceSize.height} ${file.length()}B " +
                            "→ ${plan.targetSize.width}x${plan.targetSize.height} ${reencoded.length()}B " +
                            "(${plan.format.contentType})"
                    }
                    PreparedUploadImage(file = reencoded, format = plan.format, isTemporary = true)
                }
            }
        }
    }

    private fun decodeSize(file: File): UploadImageSize {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw UnsupportedImageException("이미지 크기를 읽지 못했다 - ${file.name}")
        }
        return UploadImageSize(width = options.outWidth, height = options.outHeight)
    }

    /**
     * 다 쓴 판을 그때그때 놓아주는 이유: 원본 해상도 비트맵 둘이 동시에 살아 있으면 줄이려다
     * OOM 이 난다.
     */
    private fun writeReencoded(
        source: File,
        plan: UploadImagePlan.Reencode,
    ): File {
        val options = BitmapFactory.Options().apply { inSampleSize = plan.sampleSize }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
            ?: throw UnsupportedImageException("이미지를 디코드하지 못했다 - ${source.name}")

        val scaled = if (decoded.width == plan.targetSize.width && decoded.height == plan.targetSize.height) {
            decoded
        } else {
            Bitmap
                .createScaledBitmap(decoded, plan.targetSize.width, plan.targetSize.height, true)
                .also { if (it !== decoded) decoded.recycle() }
        }

        // JPEG 에는 알파가 없다. 합성하지 않으면 투명한 자리가 검게 앉는다
        val encodable = if (plan.format == UploadImageFormat.JPEG && scaled.hasAlpha()) {
            flattenOnWhite(scaled).also { if (it !== scaled) scaled.recycle() }
        } else {
            scaled
        }

        val target = File(source.parentFile, "${UUID.randomUUID()}.${plan.format.extension}")
        try {
            target.outputStream().use { output ->
                // compress 는 던지지 않고 false 를 준다 — 안 보면 잘린 파일이 그대로 올라간다
                check(encodable.compress(plan.format.compressFormat, UPLOAD_JPEG_QUALITY, output)) {
                    "축소본을 굽지 못했다 - ${target.name}"
                }
            }
        } catch (throwable: Throwable) {
            target.delete()
            throw throwable
        } finally {
            encodable.recycle()
        }

        return target
    }

    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flattened
    }
}

/**
 * 이 매핑을 [UploadImageFormat] 안에 두지 않는 이유: 그 열거형은 JVM 유닛이 그대로 읽는데,
 * `Bitmap.CompressFormat` 은 단위 테스트용 android.jar 에서 실제 값을 보장하지 않는다.
 */
private val UploadImageFormat.compressFormat: Bitmap.CompressFormat
    get() = when (this) {
        UploadImageFormat.PNG -> Bitmap.CompressFormat.PNG
        UploadImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
    }
