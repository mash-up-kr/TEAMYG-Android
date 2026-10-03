package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.data.model.image.MaskedAlpha
import com.teamyg.parfait.data.model.image.SegmentationMaskSpec
import java.nio.FloatBuffer

/**
 * 전경 신뢰도를 알파로 사상한다. 이진 컷 대신 램프를 쓰는 것은 경계 한두 픽셀을 부드럽게 하기
 * 위해서다.
 *
 * 변환은 버림이다. 종전 상수가 "이 값을 넘는" 신뢰도만 객체로 봤고, 버림이면
 * `알파 > 127 ⇔ 신뢰도 ≥ 0.35 + 128 × 0.3 / 255`(대략 0.5006)라 그 경계가 거의 그대로 옮겨진다.
 * 반올림으로 바꾸면 0.5가 전경이 되어 판정이 뒤집힌다.
 */
internal fun confidenceToAlpha(confidence: Float): Int {
    val floor = SegmentationMaskSpec.RAMP_FLOOR
    val ceiling = SegmentationMaskSpec.RAMP_CEILING
    if (confidence <= floor) return 0
    if (confidence >= ceiling) return SegmentationMaskSpec.FULLY_OPAQUE

    return (SegmentationMaskSpec.FULLY_OPAQUE * (confidence - floor) / (ceiling - floor)).toInt()
}

/** 검출 공간에서 돈다 */
internal fun confidenceToAlphaArray(
    mask: FloatBuffer,
    width: Int,
    height: Int,
): ByteArray {
    val alpha = ByteArray(width * height)
    for (index in alpha.indices) alpha[index] = confidenceToAlpha(mask[index]).toByte()

    return alpha
}

/**
 * 알파 후처리. 원본 공간에서 돈다 — [guidance] 가 원본을 읽기 때문이다.
 *
 * [alpha] 를 제자리에서 지운다. 되돌림에 쓸 알파는 부르기 전에 사본을 떠 둔다.
 */
internal suspend fun postProcessMaskedAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? {
    val result = postProcessAlpha(alpha, width, height, options, guidance = guidance) ?: return null

    return MaskedAlpha(alpha = alpha, result = result)
}

/**
 * 검출 공간과 원본 공간이 같을 때만 쓴다 — 알파를 다른 치수로 옮기지 않는다.
 *
 * @param mask 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 */
internal suspend fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? = postProcessMaskedAlpha(
    confidenceToAlphaArray(mask, width, height),
    width,
    height,
    options,
    guidance,
)
