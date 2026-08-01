package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlert
import com.teamyg.parfait.core.designsystem.component.ygalert.YGAlertHost
import com.teamyg.parfait.core.designsystem.component.ygalert.rememberYGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val YG_ALERT_SAMPLE_TITLE = "Title"
private const val YG_ALERT_SAMPLE_SUB = "Sub"
private const val YG_ALERT_SAMPLE_BUTTON = "Text"

@Composable
internal fun YGAlertPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alertPolicy = rememberYGAlertPolicy()

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarBack(onIconClick = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    PreviewSection("with button") {
                        YGAlert(
                            title = YG_ALERT_SAMPLE_TITLE,
                            sub = YG_ALERT_SAMPLE_SUB,
                            buttonText = YG_ALERT_SAMPLE_BUTTON,
                            onButtonClick = {},
                        )
                    }
                }
                item {
                    PreviewSection("without button") {
                        YGAlert(
                            title = YG_ALERT_SAMPLE_TITLE,
                            sub = YG_ALERT_SAMPLE_SUB,
                        )
                    }
                }
                item {
                    PreviewSection("show: with button") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = {
                                alertPolicy.show(
                                    title = YG_ALERT_SAMPLE_TITLE,
                                    sub = YG_ALERT_SAMPLE_SUB,
                                    buttonText = YG_ALERT_SAMPLE_BUTTON,
                                    onButtonClick = {
                                        alertPolicy.show(
                                            title = "clicked",
                                            sub = YG_ALERT_SAMPLE_SUB,
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
                item {
                    PreviewSection("show: without button") {
                        YGButton(
                            text = "띄우기",
                            buttonType = YGButtonType.Medium.Primary,
                            isEnabled = true,
                            onClick = {
                                alertPolicy.show(
                                    title = YG_ALERT_SAMPLE_TITLE,
                                    sub = YG_ALERT_SAMPLE_SUB,
                                )
                            },
                        )
                    }
                }
            }
        }
        YGAlertHost(
            policy = alertPolicy,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGAlertPreviewScreen() = PreviewBox {
    YGAlertPreviewScreen(
        onBack = {},
    )
}
