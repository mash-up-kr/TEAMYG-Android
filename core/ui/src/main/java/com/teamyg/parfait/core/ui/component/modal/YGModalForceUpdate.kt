package com.teamyg.parfait.core.ui.component.modal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.ui.R as CoreR

/** 뒤로 가기나 바깥 터치로 닫히지 않는다. 벗어나는 길은 [onQuit]과 [onUpdate] 두 버튼뿐이다 */
@Composable
fun YGModalForceUpdate(
    onQuit: () -> Unit,
    onUpdate: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(CoreR.string.yg_modal_force_update_title),
        body = stringResource(CoreR.string.yg_modal_force_update_body),
        iconRes = DesignSystemR.drawable.ic_warning_round,
        secondaryText = stringResource(CoreR.string.yg_modal_force_update_quit),
        onSecondaryClick = onQuit,
        primaryText = stringResource(CoreR.string.yg_modal_force_update_confirm),
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
