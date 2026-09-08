package com.teamyg.parfait.data.source.image.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import com.teamyg.parfait.core.util.android.extension.readExifDegrees
import com.teamyg.parfait.data.model.exception.UnsupportedImageException
import com.teamyg.parfait.data.model.image.UPLOAD_JPEG_QUALITY
import com.teamyg.parfait.data.model.image.UploadImageFormat
import com.teamyg.parfait.data.model.image.UploadImagePlan
import com.teamyg.parfait.data.model.image.UploadImageSize
import com.teamyg.parfait.data.model.image.planUploadImage
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.image.ImageType
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

    // 호출부(NUKKI·배경 재사용 경로 포함)마다 원본이 캐시·내부저장소·서명 디렉터리로 제각각이라,
    // 축소본은 그 위치를 따라가지 않고 항상 앱 캐시에 쓴다 — 취소·실패로 고아가 남아도 OS 가
    // 회수한다. 같은 업로드 캐시 디렉터리를 ImageFileLocalDataSourceImpl 과 공유한다.
    private val uploadCacheDir: File by lazy {
        File(context.cacheDir, ImageFileLocalDataSourceImpl.UPLOAD_DIR_NAME)
    }

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
                            "→ ${reencoded.size.width}x${reencoded.size.height} ${reencoded.file.length()}B " +
                            "(${plan.format.contentType}, 회전 ${reencoded.rotationDegrees}도)"
                    }
                    PreparedUploadImage(file = reencoded.file, format = plan.format, isTemporary = true)
                }
            }
        }
    }

    private fun decodeSize(
        file: File,
        sampleSize: Int = 1,
    ): UploadImageSize {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = sampleSize
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw UnsupportedImageException("이미지 크기를 읽지 못했다 - ${file.name}")
        }
        return UploadImageSize(width = options.outWidth, height = options.outHeight)
    }

    /**
     * 다 쓴 판을 그때그때 놓아주는 이유: 원본 해상도 비트맵 둘이 동시에 살아 있으면 줄이려다
     * OOM 이 난다. 그래서 EXIF 회전도 축소 **다음에** 굽는다 — 원본 해상도 판을 먼저 돌리면
     * 그 순간 원본 해상도 판이 두 장(회전 전·후) 동시에 산다. 축소본을 돌리면 90·270 도에서도
     * 가로세로가 그 축소본 그대로 뒤집혀 나와 `plan.targetSize` 를 따로 맞바꿀 필요가 없다.
     * 출력에는 태그를 쓰지 않는다 — 재인코딩된 파일에 원본 EXIF 가 그대로 남는다는 보장이
     * 없어서다.
     *
     * 축소본은 언제나 앱 캐시(`uploadCacheDir`)에 쓴다 — `source` 옆(`source.parentFile`)에
     * 쓰면, 원본이 캐시가 아닌 곳(최근 사용 알맹이 재사용 경로의 `filesDir/recent_images/`
     * 등)에 있을 때 취소·실패로 남는 고아 파일이 영구 저장소에 눌러앉는다.
     *
     * 실제로 구운 치수를 [ReencodedImage.size] 로 함께 돌려주는 이유: 호출부가 로그를 남기려고
     * 파일을 다시 디코드하면, 그 디코드가 던지는 예외로 이미 멀쩡히 구운 업로드가 실패할 수
     * 있다. `encodable` 이 이미 메모리에 들고 있는 치수를 recycle 전에 그대로 읽어 돌려준다.
     */
    private fun writeReencoded(
        source: File,
        plan: UploadImagePlan.Reencode,
    ): ReencodedImage {
        val degrees = source.readExifDegrees()

        val decoded = decodeDownscaled(source, plan)

        val scaled = if (decoded.width == plan.targetSize.width && decoded.height == plan.targetSize.height) {
            decoded
        } else {
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

        return ReencodedImage(file = target, size = outputSize, rotationDegrees = degrees)
    }

    /**
     * `inSampleSize` 만 쓰면 2 의 거듭제곱 계단에서 멈춰(`plan.sampleSize` 는 "목표보다
     * 작아지지 않는 선까지만" 줄인다) 목표보다 훨씬 큰 판이 힙에 올라갈 수 있다 — 배경(긴 변
     * 상한 2048)의 2049~4095 구간은 `sampleSize` 가 1 에 걸려 원본 해상도 그대로다.
     * `inDensity`/`inTargetDensity` 로 디코드 시점에 목표까지 마저 줄인다.
     *
     * 두 축 중 **비율이 더 큰(덜 줄여도 되는) 축을 기준**으로 스케일을 건다. 그 축은
     * `inTargetDensity`/`inDensity` 가 정확히 `target` 값이라 결과도 정확히 같다. 남는 축은
     * 기준 축의 비율이 그쪽이 필요로 하는 비율보다 크거나 같아 실수 연산으로는 항상 목표
     * 이상이고, 정수 반올림·내림 어느 쪽이든 실수값이 정수 target 이상이면 결과도 target
     * 이상이다(반올림은 내림보다 크거나 같고, 내림도 정수 target 이상인 실수를 내리면
     * target 이하로 내려가지 않는다). 그래서 이 함수의 결과는 두 축 모두 [plan.targetSize]
     * 이상임이 보장되고, 뒤따르는 `createScaledBitmap` 은 줄이기만 하지 키우지 않는다.
     */
    private fun decodeDownscaled(
        source: File,
        plan: UploadImagePlan.Reencode,
    ): Bitmap {
        val sampled = decodeSize(source, plan.sampleSize)
        val target = plan.targetSize

        // 교차곱으로 비교한다 — target.width / sampled.width 와 target.height / sampled.height
        // 중 어느 쪽이 더 큰(덜 줄여도 되는) 비율인지, 실수 나눗셈 없이 정수로 판정한다
        val useWidthAsBasis = target.width.toLong() * sampled.height >= target.height.toLong() * sampled.width

        val options = BitmapFactory.Options().apply {
            inSampleSize = plan.sampleSize
            inDensity = if (useWidthAsBasis) sampled.width else sampled.height
            inTargetDensity = if (useWidthAsBasis) target.width else target.height
            inScaled = true
        }
        return BitmapFactory.decodeFile(source.absolutePath, options)
            ?: throw UnsupportedImageException("이미지를 디코드하지 못했다 - ${source.name}")
    }

    /** `core.util.android` 의 `ContentResolver.rotatedToUpright` 와 같은 규약이다 */
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

    /** [writeReencoded] 가 구운 파일과, 로깅에 쓸 실제 출력 치수·적용한 회전 각도 */
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
