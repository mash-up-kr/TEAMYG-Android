package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGroupTagChip
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox

private const val TOPPING_FRAME_SIZE = 160
private const val TOPPING_IMAGE_SIZE = 96

@Composable
internal fun ToppingGroup(
    name: String,
    uploadTime: String,
    imageUrl: String,
    chipColorType: YGColorChipType,
    templateType: ToppingTemplateType,
    groupType: ToppingGroupType,
    modifier: Modifier = Modifier,
    frameSize: Dp = TOPPING_FRAME_SIZE.dp,
    imageSize: Dp = TOPPING_IMAGE_SIZE.dp,
) {

    Box(modifier = modifier.size(frameSize)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    translationX = groupType.translationX.toFloat()
                    translationY = groupType.translationY.toFloat()
                },
        ) {
            ToppingContent(
                imageUrl = imageUrl,
                templateType = templateType,
                modifier = Modifier
                    .size(imageSize)
                    .graphicsLayer {
                        rotationZ = -groupType.imageRotate.toFloat()
                    },
            )
            YGGroupTagChip(
                name = name,
                time = uploadTime,
                colorType = chipColorType,
            )
        }
    }
}

@Composable
private fun ToppingContent(
    imageUrl: String,
    templateType: ToppingTemplateType,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(color = Color.Cyan),
        ) {
            Text("Sample Image")
        }

    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            placeholder = painterResource(templateType.resId),
            modifier = modifier,
        )
    }
}

private data class ToppingPreviewData(
    val groupType: ToppingGroupType,
)

private class YGColorChipPreviewParameterProvider : PreviewParameterProvider<ToppingPreviewData> {
    override val values = sequenceOf(
        ToppingPreviewData(groupType = ToppingGroupType.Left1),
        ToppingPreviewData(groupType = ToppingGroupType.Left2),
        ToppingPreviewData(groupType = ToppingGroupType.Left3),
        ToppingPreviewData(groupType = ToppingGroupType.Right1),
        ToppingPreviewData(groupType = ToppingGroupType.Right2),
        ToppingPreviewData(groupType = ToppingGroupType.Right3),
    )
}

@Preview
@Composable
private fun ToppingGroupPreview(
    @PreviewParameter(YGColorChipPreviewParameterProvider::class)
    param: ToppingPreviewData,
) = PreviewBox {
    ToppingGroup(
        imageUrl = "",
        templateType = ToppingTemplateType.Type1,
        groupType = param.groupType,
        name = "매쉬업",
        uploadTime = "3분전",
        chipColorType = YGColorChipType.NametagChip1,
    )
}
