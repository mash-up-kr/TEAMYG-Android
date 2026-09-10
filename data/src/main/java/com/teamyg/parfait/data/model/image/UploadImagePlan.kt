package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.SourceLongSide
import kotlin.math.roundToInt

sealed interface UploadImagePlan {
    data object Passthrough : UploadImagePlan

    /** @param sampleSize 디코드 단계에서 미리 줄일 배수 */
    data class Reencode(
        val targetSize: UploadImageSize,
        val sampleSize: Int,
        val format: UploadImageFormat,
    ) : UploadImagePlan

    companion object {
        /**
         * 누끼 배율의 분자. "원본이 이 크기였다면" 을 기준으로 잘린 판을 줄여, 알맹이의 절대
         * 크기가 아니라 **프레임 안에서 차지하는 비율**을 보존한다.
         *
         * 값의 근거는 `specs/2026-09-09-topping-upload-source-scaled.md`,
         * iOS 를 따르지 않는 근거는 `adr/0032-android-own-topping-upload-scale.md`.
         */
        private const val NUKKI_SOURCE_LONG_SIDE = 1280

        /** 원본 긴 변을 모를 때 걸리는 방어선. 배율 갈래를 지나면 결과가 이미 이 값 이하다 */
        private const val NUKKI_LONG_SIDE_LIMIT = 1280

        /** 축소 **결과**의 하한. 근거는 `specs/2026-09-09-topping-upload-source-scaled.md` 「하한」 */
        private const val NUKKI_MIN_LONG_SIDE = 256

        private const val BACKGROUND_LONG_SIDE_LIMIT = 2048

        /**
         * PNG 는 무손실이라 이 값을 보지 않는다. 배경 한정으로 iOS 와 맞춘 값이다
         * (근거는 `specs/archive/2026-09-08-upload-image-downscale.md` 「결정 표」).
         */
        const val JPEG_QUALITY = 70

        /**
         * 치수와 포맷 둘 다 그대로여도 되는지 판정한다. 어느 한쪽이라도 바뀌어야 다시 굽는다 —
         * 이미 JPEG 이고 상한 이하인 배경을 다시 구우면 손실만 더해진다.
         *
         * @param fileSize 올릴 파일의 치수. 누끼면 여백을 걷어낸 알맹이다
         */
        fun of(
            fileSize: UploadImageSize,
            imageType: ImageType,
            sourceFormat: UploadImageFormat,
            sourceLongSide: SourceLongSide?,
        ): UploadImagePlan {
            val targetSize = targetSizeOf(fileSize, imageType, sourceLongSide)
            val targetFormat = uploadFormatOf(imageType, sourceFormat)

            if (targetSize == fileSize && targetFormat == sourceFormat) return Passthrough

            return Reencode(
                targetSize = targetSize,
                sampleSize = sampleSizeOf(fileSize, targetSize),
                format = targetFormat,
            )
        }

        private fun targetSizeOf(
            fileSize: UploadImageSize,
            imageType: ImageType,
            sourceLongSide: SourceLongSide?,
        ): UploadImageSize = when (imageType) {
            ImageType.BACKGROUND -> scaledSize(fileSize, BACKGROUND_LONG_SIDE_LIMIT)

            // 방어선을 겹치는 이유는 배율 갈래가 아니라 그것을 건너뛴 갈래들 때문이다
            ImageType.NUKKI -> scaledSize(scaledBySource(fileSize, sourceLongSide), NUKKI_LONG_SIDE_LIMIT)
        }

        /**
         * 원본이 [NUKKI_SOURCE_LONG_SIDE] 였다면 이 알맹이가 가졌을 크기. 결과가 너무 잘아지면
         * [NUKKI_MIN_LONG_SIDE] 까지 되돌리되, **되돌림의 상한이 `fileSize` 자신이라 확대가 아니다.**
         *
         * 하한을 입력이 아니라 결과에 거는 이유는 `specs/2026-09-09-topping-upload-source-scaled.md` 「하한」.
         */
        private fun scaledBySource(
            fileSize: UploadImageSize,
            sourceLongSide: SourceLongSide?,
        ): UploadImageSize {
            if (sourceLongSide == null || sourceLongSide.px <= NUKKI_SOURCE_LONG_SIDE) return fileSize

            val fileLongSide = maxOf(fileSize.width, fileSize.height)
            val scaledLongSide = fileLongSide.toDouble() * NUKKI_SOURCE_LONG_SIDE / sourceLongSide.px
            val targetLongSide = scaledLongSide
                .coerceAtLeast(NUKKI_MIN_LONG_SIDE.toDouble())
                .coerceAtMost(fileLongSide.toDouble())
            if (targetLongSide >= fileLongSide) return fileSize

            val ratio = targetLongSide / fileLongSide
            return UploadImageSize(
                width = (fileSize.width * ratio).roundToInt().coerceAtLeast(1),
                height = (fileSize.height * ratio).roundToInt().coerceAtLeast(1),
            )
        }

        /** 로그용 — 실제 축소 판정에는 쓰이지 않는다 */
        fun ruleScaleOf(sourceLongSide: SourceLongSide): Double = NUKKI_SOURCE_LONG_SIDE.toDouble() / sourceLongSide.px

        /** 배경은 캔버스를 덮는 불투명 이미지라 알파를 버려도 잃는 것이 없다 */
        private fun uploadFormatOf(
            imageType: ImageType,
            sourceFormat: UploadImageFormat,
        ): UploadImageFormat = when (imageType) {
            ImageType.NUKKI -> sourceFormat
            ImageType.BACKGROUND -> UploadImageFormat.JPEG
        }

        /** 확대는 정보를 늘리지 않으면서 바이트만 키운다 */
        private fun scaledSize(
            fileSize: UploadImageSize,
            longSideLimit: Int,
        ): UploadImageSize {
            val longSide = maxOf(fileSize.width, fileSize.height)
            if (longSide <= longSideLimit) return fileSize

            val ratio = longSideLimit.toDouble() / longSide
            return UploadImageSize(
                width = (fileSize.width * ratio).roundToInt().coerceAtLeast(1),
                height = (fileSize.height * ratio).roundToInt().coerceAtLeast(1),
            )
        }

        /**
         * 목표보다 작아지지 않는 선까지만 2 의 거듭제곱으로 줄인다. 넘겨서 줄이면 뒤에서 확대하게 되고,
         * `BitmapFactory` 는 2 의 거듭제곱이 아닌 값을 그 아래 거듭제곱으로 내림한다.
         */
        private fun sampleSizeOf(
            fileSize: UploadImageSize,
            targetSize: UploadImageSize,
        ): Int {
            var sampleSize = 1
            while (fileSize.width / (sampleSize * 2) >= targetSize.width &&
                fileSize.height / (sampleSize * 2) >= targetSize.height
            ) {
                sampleSize *= 2
            }
            return sampleSize
        }
    }
}
