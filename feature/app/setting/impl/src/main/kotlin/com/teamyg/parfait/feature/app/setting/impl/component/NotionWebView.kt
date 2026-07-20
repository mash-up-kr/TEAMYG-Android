package com.teamyg.parfait.feature.app.setting.impl.component

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.app.setting.impl.R

@Composable
internal fun NotionWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        NotionWebViewPreviewPlaceholder(url = url, modifier = modifier)
        return
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(modifier.clipToBounds()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?,
                        ) {
                            loading = true
                            error = false
                        }

                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            loading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                error = true
                                loading = false
                            }
                        }
                    }
                    webViewRef = this
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (error) {
            NotionWebViewError(
                onRetry = {
                    error = false
                    loading = true
                    webViewRef?.reload()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun NotionWebViewPreviewPlaceholder(
    url: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.Gray100),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        ) {
            Text(
                text = "Notion WebView",
                style = YGTheme.typography.body.b01R,
                color = YGAtomicColors.Gray.Gray500,
            )
            Text(
                text = url,
                style = YGTheme.typography.caption.c01R,
                color = YGAtomicColors.Gray.Gray400,
            )
        }
    }
}

@Composable
private fun NotionWebViewError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.setting_webview_error_message),
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray500,
        )
        Text(
            text = stringResource(R.string.setting_webview_retry),
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Cherry.Cherry600,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}
