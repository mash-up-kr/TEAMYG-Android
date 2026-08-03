package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarCanvas
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun YGTopBarPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewSection("YGTopBarBack") {
                    YGTopBarBack(onIconClick = {})
                }
            }
            item {
                PreviewSection("YGTopBarDetail") {
                    YGTopBarDetail(
                        title = "상세 화면",
                        onIconClick = {},
                    )
                }
            }
            item {
                PreviewSection("YGTopBarEmpty") {
                    YGTopBarEmpty(
                        date = "December 31",
                        day = "Wed",
                        onIconClick = {},
                        windowInsets = WindowInsets(0),
                    )
                }
            }
            item {
                PreviewSection("YGTopBarDefault") {
                    YGTopBarEmpty(
                        date = "December 31",
                        day = "Wed",
                        onIconClick = {},
                        windowInsets = WindowInsets(0),
                        rightContent = {
                            YGChipButton(
                                text = "그룹 추가하기",
                                colors = YGChipButtonColorsDefaults.GrayOutline,
                                onClick = {},
                                startIconResource = R.drawable.ic_plus,
                            )
                        },
                    )
                }
            }
            item {
                PreviewSection("YGTopBarEmpty + backdrop blur") {
                    val hazeState = rememberHazeState()
                    Box {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .hazeSource(state = hazeState),
                        ) {
                            repeat(6) { index ->
                                Text(
                                    text = "배경 콘텐츠 줄 $index — 블러가 걸리면 흐려진다",
                                    style = YGTheme.typography.body.b02R,
                                    color = YGAtomicColors.Gray.Gray800,
                                )
                            }
                        }
                        YGTopBarEmpty(
                            date = "December 31",
                            day = "Wed",
                            onIconClick = {},
                            hazeState = hazeState,
                            windowInsets = WindowInsets(0),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                PreviewSection("YGTopBarCanvas") {
                    YGTopBarCanvas(
                        title = "그룹이름",
                        onBackClick = {},
                        onMenuClick = {},
                        memberContent = { MemberListSample() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberListSample() {
    val members = listOf(
        YGColorChipType.NametagChip5 to "김",
        YGColorChipType.NametagChip4 to "이",
        YGColorChipType.NametagChip1 to "박",
        YGColorChipType.NametagChip2 to "최",
        YGColorChipType.NametagChip12 to "정",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        members.forEach { (type, name) ->
            YGNametagChip(
                colorChipType = type,
                userFirstName = name,
                chip = YGNametagChipStyle.Style28,
            )
        }
        YGNametagChip(
            colorChipType = YGColorChipType.NametagChipPlus,
            userFirstName = "+7",
            chip = YGNametagChipStyle.Style28,
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGTopBarPreviewScreen() = PreviewBox {
    YGTopBarPreviewScreen(
        onBack = {},
    )
}
