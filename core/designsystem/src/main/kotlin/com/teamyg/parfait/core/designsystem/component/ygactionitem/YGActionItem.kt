package com.teamyg.parfait.core.designsystem.component.ygactionitem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * @param enabled `false` 면 클릭을 받지 않는다. 요청이 나가 있는 동안 같은 동작이 다시
 *   실행되는 것을 막으려는 용도다. **색은 바뀌지 않는다** — 비활성 색이 디자인시스템에
 *   정의돼 있지 않아 여기서 임의로 정하지 않는다.
 */
@Composable
fun YGActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val contentColor = if (isPressed) YGAtomicColors.Gray.Gray700 else YGAtomicColors.Gray.Gray500

    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
            .padding(
                vertical = YGTheme.layout.padding.padding5,
                horizontal = YGTheme.layout.padding.padding6,
            ),
    ) {
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = contentColor,
        )
    }
}

@YGPreview
@Composable
fun YGActionItemPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
    ) {
        YGActionItem(
            text = "그룹 나가기",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        YGActionItem(
            text = "새 그룹 만들기",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
