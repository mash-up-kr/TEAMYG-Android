package com.teamyg.parfait.core.ui.component.modal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.ui.R as CoreR

/** 뒤로 가기나 바깥 터치로 닫히지 않는다 */
@Composable
fun YGModalForceUpdate(
    onQuit: () -> Unit,
    onUpdate: () -> Unit,
) {
    YGModalAppUpdate(
        secondaryText = stringResource(CoreR.string.yg_modal_force_update_quit),
        onSecondaryClick = onQuit,
        onUpdate = onUpdate,
    )
}

/** 별도 시안이 없어 강제 업데이트 시안에서 왼쪽 버튼만 바꿨다 */
@Composable
fun YGModalOptionalUpdate(
    onLater: () -> Unit,
    onUpdate: () -> Unit,
) {
    YGModalAppUpdate(
        secondaryText = stringResource(CoreR.string.yg_modal_optional_update_later),
        onSecondaryClick = onLater,
        onUpdate = onUpdate,
    )
}

@Composable
private fun YGModalAppUpdate(
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    onUpdate: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(CoreR.string.yg_modal_app_update_title),
        body = stringResource(CoreR.string.yg_modal_app_update_body),
        iconRes = DesignSystemR.drawable.ic_warning_round,
        secondaryText = secondaryText,
        onSecondaryClick = onSecondaryClick,
        primaryText = stringResource(CoreR.string.yg_modal_app_update_confirm),
        onPrimaryClick = onUpdate,
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    )
}

@YGPreview
@Composable
private fun YGModalForceUpdatePreview() = PreviewBox {
    YGModalForceUpdate(onQuit = {}, onUpdate = {})
}

@YGPreview
@Composable
private fun YGModalOptionalUpdatePreview() = PreviewBox {
    YGModalOptionalUpdate(onLater = {}, onUpdate = {})
}
