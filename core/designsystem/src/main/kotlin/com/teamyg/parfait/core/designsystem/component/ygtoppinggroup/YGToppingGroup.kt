package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChip
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
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
        // clip 은 이미지가 프레임을 넘어 인접 셀을 덮는 것을 막는 방어선이다.
        // rotate 보다 안쪽이어야 한다. 바깥이면 회전 오버행까지 잘려 배치 변형이 깨진다
        val imageModifier = Modifier
            .size(SizeTokens.Size96.getDp())
            .offset(
                x = type.imageOffset.x,
                y = type.imageOffset.y,
            ).rotate(type.rotation)
            .clip(RectangleShape)

        when (image) {
            is YGToppingImage.Remote -> RemoteToppingImage(
                image = image,
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

@Composable
private fun RemoteToppingImage(
    image: YGToppingImage.Remote,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val sizePx = with(LocalDensity.current) { SizeTokens.Size96.getDp().roundToPx() }

    // 크기를 안 주면 painter 는 원본 해상도로 디코딩한다
    val request = remember(image.url, sizePx) {
        ImageRequest
            .Builder(context)
            .data(image.url)
            .size(sizePx)
            .build()
    }
    // 원격 이미지는 배경이 지워진 누끼라, Crop 으로 긴 변을 잘라 내면 피사체가 사라진다
    val painter = rememberAsyncImagePainter(model = request, contentScale = ContentScale.Fit)
    val painterState by painter.state.collectAsState()

    if (painterState is AsyncImagePainter.State.Error) {
        Image(
            painter = painterResource(TOPPING_ERROR_DRAWABLE),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        val border = image.border
        val borderWidth = border?.width ?: 0.dp

        YGToppingCutoutImage(
            painter = painter,
            // 알맹이가 뜨기 전에 띠를 깔면 실루엣 모양 색 덩어리만 보인다
            borderColor = border?.color?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = borderWidth,
            // 띠가 알맹이 밖으로 굵기만큼 나가므로 clip 안쪽에서 굵기만큼 덜어 낸다
            modifier = modifier.padding(borderWidth),
            outline = border?.outline,
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
