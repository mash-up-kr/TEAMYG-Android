package com.teamyg.parfait.core.ui.preview

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "phone",
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
