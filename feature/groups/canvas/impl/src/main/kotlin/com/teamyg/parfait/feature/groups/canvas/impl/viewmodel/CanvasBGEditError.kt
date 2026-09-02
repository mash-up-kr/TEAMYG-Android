package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.canvas.impl.R

/**
 * 편집 화면의 서버 요청이 되돌아온 사유. 화면에는 토스트 한 줄로만 나가므로 갈래도 사용자가
 * 실제로 다르게 굴 수 있는 만큼만 둔다 — 다시 시도하면 될 일인가([NETWORK]), 이 사진 자체가
 * 안 되는 것인가([UNSUPPORTED_IMAGE]), 그 밖인가(`*_UNKNOWN`).
 *
 * [NETWORK] 만 요청을 가리지 않고 함께 쓴다 — 문구가 무엇을 하다 끊겼든 같은 말이면 된다.
 */
enum class CanvasBGEditError {
    NETWORK,

    /** 서버가 받지 않는 형식이거나 사진을 읽지 못했다 — 다시 눌러도 같은 결과다 */
    UNSUPPORTED_IMAGE,

    BACKGROUND_SAVE_UNKNOWN,

    TOPPING_SAVE_UNKNOWN,

    TOPPING_DELETE_UNKNOWN,
}

@Composable
internal fun CanvasBGEditError.toStringResource(): String = when (this) {
    CanvasBGEditError.NETWORK -> stringResource(R.string.canvas_bg_edit_save_error_network)
    CanvasBGEditError.UNSUPPORTED_IMAGE -> stringResource(R.string.canvas_bg_edit_save_error_unsupported_image)
    CanvasBGEditError.BACKGROUND_SAVE_UNKNOWN -> stringResource(R.string.canvas_bg_edit_save_error_unknown)
    CanvasBGEditError.TOPPING_SAVE_UNKNOWN -> stringResource(R.string.canvas_bg_edit_topping_save_error_unknown)
    CanvasBGEditError.TOPPING_DELETE_UNKNOWN -> stringResource(R.string.canvas_bg_edit_topping_delete_error_unknown)
}
