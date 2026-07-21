package com.teamyg.parfait.feature.common.terms.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.common.terms.impl.R
import com.teamyg.parfait.feature.common.terms.impl.component.NotionWebView

@Composable
internal fun PrivacyPolicyScreen(
    url: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        YGTopBarDetail(
            title = stringResource(R.string.terms_privacy_title),
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
private fun PreviewPrivacyPolicyScreen() = PreviewBox {
    PrivacyPolicyScreen(
        url = "",
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
