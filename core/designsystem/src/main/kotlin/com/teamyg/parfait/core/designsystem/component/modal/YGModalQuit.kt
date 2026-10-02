package com.teamyg.parfait.core.designsystem.component.modal

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/** 사진을 새로 쌓는 흐름을 그만둘지 묻는다 */
@Composable
fun YGModalQuitAdd(
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) = YGModalQuit(
    titleRes = R.string.yg_modal_quit_add_title,
    continueRes = R.string.yg_modal_quit_continue_add,
    onConfirmQuit = onConfirmQuit,
    onDismiss = onDismiss,
)

/** 사진 편집을 그만둘지 묻는다 */
@Composable
fun YGModalQuitEdit(
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) = YGModalQuit(
    titleRes = R.string.yg_modal_quit_edit_title,
    continueRes = R.string.yg_modal_quit_continue_edit,
    onConfirmQuit = onConfirmQuit,
    onDismiss = onDismiss,
)

/** 배경 변경을 그만둘지 묻는다 */
@Composable
fun YGModalQuitBackground(
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) = YGModalQuit(
    titleRes = R.string.yg_modal_quit_background_title,
    continueRes = R.string.yg_modal_quit_continue_edit,
    onConfirmQuit = onConfirmQuit,
    onDismiss = onDismiss,
)

/** 바깥을 누르거나 뒤로 가면 [onDismiss] 다. 그만두는 길은 버튼 하나뿐이다 */
@Composable
private fun YGModalQuit(
    @StringRes titleRes: Int,
    @StringRes continueRes: Int,
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(titleRes),
        body = stringResource(R.string.yg_modal_quit_body),
        iconRes = R.drawable.ic_warning_round,
        secondaryText = stringResource(R.string.yg_modal_quit_confirm),
        onSecondaryClick = onConfirmQuit,
        primaryText = stringResource(continueRes),
        onPrimaryClick = onDismiss,
        onDismissRequest = onDismiss,
    )
}

@YGPreview
@Composable
private fun YGModalQuitAddPreview() = PreviewBox {
    YGModalQuitAdd(onConfirmQuit = {}, onDismiss = {})
}

@YGPreview
@Composable
private fun YGModalQuitEditPreview() = PreviewBox {
    YGModalQuitEdit(onConfirmQuit = {}, onDismiss = {})
}

@YGPreview
@Composable
private fun YGModalQuitBackgroundPreview() = PreviewBox {
    YGModalQuitBackground(onConfirmQuit = {}, onDismiss = {})
}
