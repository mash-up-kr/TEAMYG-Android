package com.teamyg.parfait.feature.login.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.KakaoDesignGuideColors
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.login.impl.R

@Composable
internal fun KakaoSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val buttonColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = KakaoDesignGuideColors.SignContainerColor,
        contentColor = KakaoDesignGuideColors.SignLabelColor,
        disabledContainerColor = KakaoDesignGuideColors.SignContainerColor,
        disabledContentColor = KakaoDesignGuideColors.SignLabelColor,
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = YGTheme.shapes.radius.none,
        colors = buttonColors,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = YGTheme.layout.padding.padding5,
            vertical = YGTheme.layout.padding.padding3,
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.icon_logo_kakao),
            contentDescription = stringResource(R.string.login_kakao_content_description),
            colorFilter = ColorFilter.tint(KakaoDesignGuideColors.SignSymbolColor),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.login_kakao_button_text),
            style = YGTheme.typography.body.b01SB,
            color = YGAtomicColors.Gray.Black,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@YGPreview
@Composable
private fun PreviewKakaoSignInButton() = PreviewBox {
    KakaoSignInButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
