package com.teamyg.parfait.core.designsystem.component.ygtopbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGTopBarBack(
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_caret_left,
        contentDescription = "뒤로가기",
        onIconClick = onIconClick,
        modifier = modifier,
    )
}

@Composable
fun YGTopBarDetail(
    title: String,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_caret_left,
        contentDescription = "뒤로가기",
        onIconClick = onIconClick,
        modifier = modifier,
        titleContent = {
            Text(
                text = title,
                style = YGTheme.typography.body.b01R,
                color = YGAtomicColors.Gray.Gray800,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
fun YGTopBarEmpty(
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_hamburger,
        contentDescription = "메뉴",
        onIconClick = onIconClick,
        modifier = modifier,
        titleContent = {
            Image(
                painter = painterResource(R.drawable.ic_plus), // todo : parfait logo 로 변경 예정
                contentDescription = null,
            )
        },
    )
}

@Composable
fun YGTopBarDefault(
    onIconClick: () -> Unit,
    onChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_hamburger,
        contentDescription = "메뉴",
        onIconClick = onIconClick,
        modifier = modifier,
        titleContent = {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_plus), // todo : parfait logo 로 변경 예정
                    contentDescription = null,
                )
            }
            YGChipButton(
                text = "새 그룹",
                colors = YGChipButtonColorsDefaults.CherryBackgroundPressed,
                onClick = onChipClick,
                startIconResource = R.drawable.ic_plus,
            )
        },
    )
}

@Composable
private fun YGTopBarContent(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable RowScope.() -> Unit = { },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                top = YGTheme.layout.padding.padding3,
                end = YGTheme.layout.padding.padding7,
                bottom = YGTheme.layout.padding.padding3,
                start = YGTheme.layout.padding.padding3,
            ),
    ) {
        YGIconButton(
            iconResource = iconResource,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = contentDescription,
            onClick = onIconClick,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            content = titleContent,
        )
    }
}

@YGPreview
@Composable
fun YGTopBarPreview() = PreviewBox {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        YGTopBarBack(onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarDetail(title = "그룹이름", onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarEmpty(onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarDefault(onChipClick = { }, onIconClick = { }, modifier = Modifier.fillMaxWidth())
    }
}
