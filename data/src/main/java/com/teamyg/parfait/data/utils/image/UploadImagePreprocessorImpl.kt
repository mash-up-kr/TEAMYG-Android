package com.teamyg.parfait.data.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import com.teamyg.parfait.core.util.android.extension.readExifDegrees
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.PreparedUploadImage
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.model.image.UploadImagePlan
import com.teamyg.parfait.data.model.image.UploadImageSize
import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSourceImpl
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.SourceLongSide
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadImagePreprocessorImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : UploadImagePreprocessor {
    init {
        sourceLogger.i { "UploadImagePreprocessorImpl::init" }
    }

    // 원본이 캐시 밖(최근 알맹이는 filesDir)일 수 있어 그 옆에 쓰지 않는다 — 고아가 남아도
    // 캐시면 OS 가 회수한다
    private val uploadCacheDir: File by lazy {
        File(context.cacheDir, ImageFileLocalDataSourceImpl.UPLOAD_DIR_NAME)
    }

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

    private fun decodeSize(file: File): UploadImageSize {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw UnsupportedImageException("이미지 크기를 읽지 못했다 - ${file.name}")
        }
        return UploadImageSize(width = options.outWidth, height = options.outHeight)
    }

    /**
     * 축소 → 회전 → 합성 순서와 그 근거는 `specs/2026-09-08-upload-image-downscale.md` 의
     * 「EXIF 회전」·「메모리」 절에 있다. 순서를 바꾸면 원본 해상도 판이 두 장 동시에 산다.
     *
     * 구운 치수를 함께 돌려주는 이유: 호출부가 로그를 남기려고 파일을 다시 디코드하면
     * 그 예외로 이미 멀쩡히 구운 업로드가 실패한다.
     */
    private fun writeReencoded(
        source: File,
        fileSize: UploadImageSize,
        plan: UploadImagePlan.Reencode,
    ): ReencodedImage {
        val degrees = source.readExifDegrees()

        val decoded = decodeDownscaled(source, fileSize, plan)

        // 가운데 갈래가 방어선이다 — 한 축이라도 target 에 못 미치면 맞추지 않고 둔다.
        // 맞추는 순간 그건 확대이고, 확대 금지가 이 경로의 불변식이다
        val scaled = when {
            decoded.width == plan.targetSize.width && decoded.height == plan.targetSize.height -> decoded

            decoded.width < plan.targetSize.width || decoded.height < plan.targetSize.height -> decoded

            else ->
                Bitmap
                    .createScaledBitmap(decoded, plan.targetSize.width, plan.targetSize.height, true)
                    .also { if (it !== decoded) decoded.recycle() }
        }

        val upright = rotateToUpright(scaled, degrees)

        // JPEG 에는 알파가 없다. 합성하지 않으면 투명한 자리가 검게 앉는다
        val encodable = if (plan.format == UploadImageFormat.JPEG && upright.hasAlpha()) {
            flattenOnWhite(upright).also { if (it !== upright) upright.recycle() }
        } else {
            upright
        }
        val outputSize = UploadImageSize(width = encodable.width, height = encodable.height)

        uploadCacheDir.mkdirs()
        val target = File(uploadCacheDir, "${UUID.randomUUID()}.${plan.format.extension}")
        try {
            target.outputStream().use { output ->
                // compress 는 던지지 않고 false 를 준다 — 안 보면 잘린 파일이 그대로 올라간다
                check(encodable.compress(plan.format.compressFormat, UploadImagePlan.JPEG_QUALITY, output)) {
                    "축소본을 굽지 못했다 - ${target.name}"
                }
            }
        } catch (throwable: Throwable) {
            target.delete()
            throw throwable
        } finally {
            encodable.recycle()
        }

        return ReencodedImage(file = target, size = outputSize, rotationDegrees = degrees)
    }

    /**
     * `inSampleSize` 는 2 의 거듭제곱 계단이라 목표보다 훨씬 큰 판이 남을 수 있다 — 배경의
     * 2049~4095 구간이 `sampleSize` 1 에 걸려 원본 해상도 그대로다. 밀도 비로 디코드 시점에
     * 목표까지 마저 줄인다(근거는 `specs/2026-09-08-upload-image-downscale.md` 「메모리」).
     *
     * 기준 축은 **덜 줄여도 되는 쪽**이다. 그 축은 정확히 목표가 되고 남는 축은 반올림이
     * 어느 쪽으로 가든 목표 이상이라, 뒤따르는 `createScaledBitmap` 이 확대로 돌 수 없다.
     *
     * 샘플링 후 치수를 `outWidth` 로 읽지 않는 이유: 그 문서가 "스케일을 반영하지 않은 입력
     * 너비"라고 못박고 있어 실제 동작이 어느 쪽이든 기댈 수 없다. 정수 나눗셈으로 대신한다.
     */
    private fun decodeDownscaled(
        source: File,
        fileSize: UploadImageSize,
        plan: UploadImagePlan.Reencode,
    ): Bitmap {
        val sampled = UploadImageSize(
            width = fileSize.width / plan.sampleSize,
            height = fileSize.height / plan.sampleSize,
        )
        val target = plan.targetSize

        // 두 비율 비교를 실수 나눗셈 없이 교차곱으로 한다
        val useWidthAsBasis = target.width.toLong() * sampled.height >= target.height.toLong() * sampled.width

        val options = BitmapFactory.Options().apply {
            inSampleSize = plan.sampleSize
            inDensity = if (useWidthAsBasis) sampled.width else sampled.height
            inTargetDensity = if (useWidthAsBasis) target.width else target.height
            inScaled = true
        }
        return BitmapFactory
            .decodeFile(source.absolutePath, options)
            ?.also {
                // 지우지 말 것: BitmapFactory 가 inTargetDensity 를 결과 density 로 남기고
                // 그 값이 뒤까지 전파된다. flattenOnWhite 의 흰 판은 기기 기본 density 라,
                // 둘이 다르면 Canvas.drawBitmap 이 두 density 비율로 그림을 자동 축소한다
                it.density = Bitmap.DENSITY_NONE
            }
            ?: throw UnsupportedImageException("이미지를 디코드하지 못했다 - ${source.name}")
    }

    private fun rotateToUpright(
        bitmap: Bitmap,
        degrees: Int,
    ): Bitmap {
        if (degrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flattened
    }

    private data class ReencodedImage(
        val file: File,
        val size: UploadImageSize,
        val rotationDegrees: Int,
    )
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
