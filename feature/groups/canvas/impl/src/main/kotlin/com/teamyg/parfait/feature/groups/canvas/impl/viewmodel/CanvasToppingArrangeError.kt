package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.canvas.impl.R

/**
 * 토핑 배치 수정의 서버 요청이 되돌아온 사유. 화면에는 토스트 한 줄로만 나가므로 사용자가
 * 다르게 굴 수 있는 만큼만 가른다 — 다시 시도하면 될 일인가([NETWORK]), 그 밖인가([TOPPING_SAVE_UNKNOWN]).
 *
 * [NETWORK] 만 요청을 가리지 않고 함께 쓴다 — 문구가 무엇을 하다 끊겼든 같은 말이면 된다.
 */
enum class CanvasToppingArrangeError {
    NETWORK,

    TOPPING_SAVE_UNKNOWN,
}

@Composable
internal fun CanvasToppingArrangeError.toStringResource(): String = when (this) {
    CanvasToppingArrangeError.NETWORK -> stringResource(R.string.canvas_bg_edit_save_error_network)

    CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN ->
        stringResource(R.string.canvas_bg_edit_topping_save_error_unknown)
}
