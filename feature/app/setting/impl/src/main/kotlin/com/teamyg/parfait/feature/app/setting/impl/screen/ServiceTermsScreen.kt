package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.NotionWebView

@Composable
internal fun ServiceTermsScreen(
    url: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        YGTopBarDetail(
            title = stringResource(R.string.setting_item_service_terms),
            onIconClick = onClickBack,
            modifier = Modifier.fillMaxWidth(),
        )
        NotionWebView(
            url = url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@YGPreview
@Composable
private fun PreviewServiceTermsScreen() = PreviewBox {
    ServiceTermsScreen(
        url = "",
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
