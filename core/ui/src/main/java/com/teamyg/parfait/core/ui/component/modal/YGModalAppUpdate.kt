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
    YGModalAppUpdate(
        secondaryText = stringResource(CoreR.string.yg_modal_force_update_quit),
        onSecondaryClick = onQuit,
        onUpdate = onUpdate,
    )
}

/**
 * 강제 업데이트와 문구·아이콘이 같고 왼쪽 버튼만 다르다. 별도 시안이 없어 강제 업데이트 시안을 따른다.
 * 닫는 길이 [onLater]·[onUpdate] 버튼뿐인 것도 같다 — 다음 화면으로 넘어가는 계기를 버튼 하나로 둔다.
 */
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
