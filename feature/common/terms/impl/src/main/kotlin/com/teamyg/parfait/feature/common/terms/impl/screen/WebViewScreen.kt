package com.teamyg.parfait.feature.common.terms.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.common.terms.impl.component.NotionWebView

@Composable
internal fun WebViewScreen(
    title: String,
    url: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        YGTopBarDetail(
            title = title,
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

private class WebViewPreviewParameterProvider : PreviewParameterProvider<Pair<String, String>> {
    override val values = sequenceOf(
        "서비스 이용약관" to "https://example.com/terms-of-service",
        "개인정보 처리 방침" to "https://example.com/privacy-policy",
    )
}

@YGPreview
@Composable
private fun PreviewWebViewScreen(
    @PreviewParameter(WebViewPreviewParameterProvider::class) titleToUrl: Pair<String, String>,
) = PreviewBox {
    WebViewScreen(
        title = titleToUrl.first,
        url = titleToUrl.second,
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
