package com.teamyg.parfait.core.designsystem.component.ygtopbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.teamyg.parfait.core.designsystem.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
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
    date: String,
    day: String,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    windowInsets: WindowInsets = YGTopBarDefaults.windowInsets,
    rightContent: @Composable () -> Unit = {},
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_hamburger,
        contentDescription = "메뉴",
        onIconClick = onIconClick,
        modifier = modifier
            .ygTopBarBackdrop(hazeState = hazeState)
            .windowInsetsPadding(windowInsets),
        titleContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = date,
                    style = YGTheme.typography.body.b01R,
                    color = YGAtomicColors.Gray.Gray800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "($day)",
                    style = YGTheme.typography.body.b01R,
                    color = YGAtomicColors.Gray.Gray300,
                    maxLines = 1,
                )
            }
            rightContent()
        },
    )
}

@Composable
private fun Modifier.ygTopBarBackdrop(hazeState: HazeState?): Modifier = if (hazeState == null) {
    this.drawBehind { drawRect(color = YGAtomicColors.Transparency.White75) }
} else {
    this.hazeEffect(state = hazeState) {
        blurRadius = YGTopBarDefaults.BackdropBlurRadius
        backgroundColor = YGAtomicColors.Gray.White
        tints = listOf(HazeTint(YGAtomicColors.Transparency.White75))
    }
}

@Composable
fun YGTopBarCanvas(
    title: String,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    memberContent: @Composable RowScope.() -> Unit = { },
) {
    YGTopBarContent(
        iconResource = R.drawable.ic_caret_left,
        contentDescription = "뒤로가기",
        onIconClick = onBackClick,
        modifier = modifier,
        contentPadding = PaddingValues(YGTheme.layout.padding.padding3),
        titleContent = {
            Text(
                text = title,
                style = YGTheme.typography.body.b01R,
                color = YGAtomicColors.Gray.Gray800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            memberContent()
        },
        trailingContent = {
            YGIconButton(
                iconResource = R.drawable.ic_hamburger,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = "메뉴",
                onClick = onMenuClick,
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
    contentPadding: PaddingValues = PaddingValues(
        start = YGTheme.layout.padding.padding3,
        top = YGTheme.layout.padding.padding3,
        end = YGTheme.layout.padding.padding7,
        bottom = YGTheme.layout.padding.padding3,
    ),
    titleContent: @Composable RowScope.() -> Unit = { },
    trailingContent: @Composable () -> Unit = { },
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(contentPadding),
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
        trailingContent()
    }
}

@YGPreview
@Composable
private fun YGTopBarPreview() = PreviewBox {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        YGTopBarBack(onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarDetail(title = "그룹이름", onIconClick = { }, modifier = Modifier.fillMaxWidth())
        YGTopBarEmpty(
            date = "December 31",
            day = "Wed",
            onIconClick = { },
            modifier = Modifier.fillMaxWidth(),
        )
        YGTopBarEmpty(
            date = "December 31",
            day = "Wed",
            onIconClick = {},
            rightContent = {
                YGChipButton(
                    text = "그룹 추가하기",
                    colors = YGChipButtonColorsDefaults.GrayOutline,
                    onClick = {},
                    startIconResource = R.drawable.ic_plus,
                )
            },
        )
        YGTopBarEmpty(
            date = "December 31, 2026 (아주 긴 날짜 문자열)",
            day = "Wed",
            onIconClick = {},
            modifier = Modifier.fillMaxWidth(),
            rightContent = {
                YGChipButton(
                    text = "그룹 추가하기",
                    colors = YGChipButtonColorsDefaults.GrayOutline,
                    onClick = {},
                    startIconResource = R.drawable.ic_plus,
                )
            },
        )
        YGTopBarCanvas(
            title = "그룹이름",
            onBackClick = { },
            onMenuClick = { },
            modifier = Modifier.fillMaxWidth(),
            memberContent = {
                YGNametagChip(
                    colorChipType = YGColorChipType.NametagChip5,
                    userFirstName = "김",
                    chip = YGNametagChipStyle.Style28,
                )
            },
        )
        YGTopBarCanvas(
            title = "아주아주긴그룹이름입니다정말로",
            onBackClick = { },
            onMenuClick = { },
            modifier = Modifier.fillMaxWidth(),
            memberContent = {
                YGNametagChip(
                    colorChipType = YGColorChipType.NametagChip5,
                    userFirstName = "김",
                    chip = YGNametagChipStyle.Style28,
                )
            },
        )
    }
}
