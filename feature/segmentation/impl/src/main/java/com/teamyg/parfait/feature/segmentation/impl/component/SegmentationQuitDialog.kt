package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 토핑 만들기를 접을지 묻는다. 바깥 탭·뒤로가기는 「계속 편집」과 같다 */
@Composable
internal fun SegmentationQuitDialog(
    onConfirmQuit: () -> Unit,
    onDismiss: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(R.string.segmentation_quit_dialog_title),
        body = stringResource(R.string.segmentation_quit_dialog_body),
        iconRes = DesignSystemR.drawable.ic_warning_round,
        secondaryText = stringResource(R.string.segmentation_quit_dialog_confirm),
        onSecondaryClick = onConfirmQuit,
        primaryText = stringResource(R.string.segmentation_quit_dialog_cancel),
        onPrimaryClick = onDismiss,
        onDismissRequest = onDismiss,
    )
}

@YGPreview
@Composable
private fun PreviewSegmentationQuitDialog() = PreviewBox {
    SegmentationQuitDialog(onConfirmQuit = {}, onDismiss = {})
}
