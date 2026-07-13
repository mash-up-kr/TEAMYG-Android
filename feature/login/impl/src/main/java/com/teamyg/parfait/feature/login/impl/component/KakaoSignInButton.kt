package com.teamyg.parfait.feature.login.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.KakaoDesignGuideColors
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
        shape = RoundedCornerShape(50.dp),
        colors = buttonColors,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = 15.dp,
            vertical = 12.dp,
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.icon_logo_kakao),
            contentDescription = "kakao_login",
            colorFilter = ColorFilter.tint(KakaoDesignGuideColors.SignSymbolColor),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(text = "카카오 로그인")

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
