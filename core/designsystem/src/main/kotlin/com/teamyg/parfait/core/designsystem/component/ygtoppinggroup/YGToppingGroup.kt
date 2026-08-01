package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChip
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Topping-Group
 */
@Composable
fun YGToppingGroup(
    image: YGToppingImage,
    name: String,
    timestamp: String,
    chipType: YGGrouptagChipType,
    type: YGToppingGroupType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(SizeTokens.Size160.getDp()),
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier = Modifier
            .size(SizeTokens.Size96.getDp())
            .offset(
                x = type.imageOffset.x,
                y = type.imageOffset.y,
            ).rotate(type.rotation)
            .clip(RectangleShape)

        when (image) {
            is YGToppingImage.Remote -> AsyncImage(
                model = image.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(TOPPING_ERROR_DRAWABLE),
                modifier = imageModifier,
            )

            is YGToppingImage.Template -> Image(
                painter = painterResource(image.type.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )

            YGToppingImage.Error -> Image(
                painter = painterResource(TOPPING_ERROR_DRAWABLE),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
        }

        YGGrouptagChip(
            name = name,
            timestamp = timestamp,
            type = chipType,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .offset(
                    x = type.chipOffset.x,
                    y = type.chipOffset.y,
                ),
        )
    }
}

@YGPreview
@Composable
private fun YGToppingGroupPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        YGToppingGroup(
            image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
            name = "잠탈감금",
            timestamp = "3분전",
            chipType = YGGrouptagChipType.TYPE_1_2,
            type = YGToppingGroupType.TYPE_1_LEFT,
        )
        YGToppingGroup(
            image = YGToppingImage.Error,
            name = "팀장은연경이",
            timestamp = "3분전",
            chipType = YGGrouptagChipType.TYPE_5_6,
            type = YGToppingGroupType.TYPE_2_RIGHT,
        )
    }
}
