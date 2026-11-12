package com.teamyg.parfait.core.designsystem.utils.preview

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview

/**
 * [UI_SNAPSHOT_PREVIEW_GROUP] 을 단 변형만 프리뷰 스크린샷 비교 대상이다(ADR-0037).
 */
@Preview(
    name = "phone",
    group = UI_SNAPSHOT_PREVIEW_GROUP,
    device = "spec:width=411dp,height=891dp,navigation=buttons",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "phone-night",
    device = "spec:width=411dp,height=891dp,navigation=buttons",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "foldable",
    device = "spec:width=673dp,height=841dp,navigation=buttons",
    showBackground = true,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "foldable-night",
    device = "spec:width=673dp,height=841dp,navigation=buttons",
    showBackground = true,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
@Preview(
    name = "foldable-fold",
    device = "spec:width=336dp,height=841dp,navigation=buttons",
    showBackground = true,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "foldable-fold-night",
    device = "spec:width=336dp,height=841dp,navigation=buttons",
    showBackground = true,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES,
)
annotation class YGPreview
