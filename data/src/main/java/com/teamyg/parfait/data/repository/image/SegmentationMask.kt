package com.teamyg.parfait.data.repository.image

import java.nio.FloatBuffer

/** 이 신뢰도 이하는 완전히 투명하다 */
private const val RAMP_FLOOR = 0.35f

/** 이 신뢰도 이상은 완전히 불투명하다 */
private const val RAMP_CEILING = 0.65f

private const val FULLY_OPAQUE = 255

/**
 * 전경 신뢰도를 알파로 사상한다. 이진 컷 대신 램프를 쓰는 것은 경계 한두 픽셀을 부드럽게 하기
 * 위해서다.
 *
 * ⚠️ 변환은 **버림**이다. 종전 상수가 "이 값을 **넘는**" 신뢰도만 객체로 봤고, 버림이면
 * `알파 > 127 ⇔ 신뢰도 ≥ 0.35 + 128 × 0.3 / 255`(대략 0.5006)라 그 경계가 거의 그대로 옮겨진다.
 * 반올림으로 바꾸면 0.5가 전경이 되어 판정이 뒤집힌다.
 */
internal fun confidenceToAlpha(confidence: Float): Int {
    if (confidence <= RAMP_FLOOR) return 0
    if (confidence >= RAMP_CEILING) return FULLY_OPAQUE

    return (FULLY_OPAQUE * (confidence - RAMP_FLOOR) / (RAMP_CEILING - RAMP_FLOOR)).toInt()
}

internal class MaskedAlpha(
    val alpha: ByteArray,
    val result: AlphaPostProcessResult,
)

/**
 * 전경 신뢰도 마스크에서 후처리까지 끝낸 알파를 만든다.
 *
 * `Bitmap` 을 받지도 돌려주지도 않는 것은 이 판단을 기기 없이 검증하기 위해서다. 호출부는 돌려받은
 * [AlphaPostProcessResult.bounds] 영역만 원본에서 읽어 판을 만들면 된다 — 원본 크기 픽셀 배열을
 * 거칠 이유가 없다.
 *
 * @param mask 픽셀별 전경 신뢰도. 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 * @return 남은 알파가 없으면 `null`
 */
internal suspend fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
): MaskedAlpha? {
    val alpha = ByteArray(width * height)
    for (index in alpha.indices) {
        alpha[index] = confidenceToAlpha(mask[index]).toByte()
    }

    val result = postProcessAlpha(alpha, width, height, options) ?: return null

    return MaskedAlpha(alpha = alpha, result = result)
}
