package com.teamyg.parfait.core.designsystem.component.ygtutorial

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * 화면 전체를 덮는 튜토리얼 한 장. 딤은 시스템바 밑까지 이어지도록 인셋을 받지 않는다.
 *
 * [imageResource] 는 알파 없는 풀스크린 목업이다 — 강조할 자리만 뚫린 오버레이가 아니라,
 * 딤까지 구워진 화면 한 장이 통째로 실제 화면을 덮는다. 그래서 시스템바를 뺀 자리에만 그린다.
 * 이미지가 앱 상단바부터 시작하므로 상태바까지 끌어올리면 실제 화면보다 위로 밀려 어긋난다.
 *
 * 딤 자체가 클릭을 삼킨다. 튜토리얼이 떠 있는 동안 그 아래 화면이 눌리면, 사용자는 자기가
 * 무엇을 건드렸는지 모른 채 다른 화면에 가 있게 된다.
 *
 * 설명 카드만 시스템바 인셋을 받는다 — 딤은 화면 끝까지 이어지고 글자만 시스템바를 피한다.
 */
@Composable
fun YGTutorialOverlay(
    @DrawableRes imageResource: Int,
    title: String,
    description: String,
    onClickButton: () -> Unit,
    modifier: Modifier = Modifier,
    progress: YGTutorialProgress? = null,
    placement: YGTutorialBoxPlacement = YGTutorialBoxPlacement.Top,
) {
    val isBottomPlacement = placement == YGTutorialBoxPlacement.Bottom

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Transparency.Black50)
            .clickableYGNoRipple(onClick = {}),
    ) {
        Image(
            painter = painterResource(id = imageResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        )

        YGTutorialBox(
            title = title,
            description = description,
            onClickButton = onClickButton,
            progress = progress,
            modifier = Modifier
                .align(if (isBottomPlacement) Alignment.BottomCenter else Alignment.TopCenter)
                .windowInsetsPadding(
                    if (isBottomPlacement) WindowInsets.navigationBars else WindowInsets.statusBars,
                ).padding(
                    top = if (isBottomPlacement) 0.dp else YGTheme.layout.padding.padding6,
                    bottom = if (isBottomPlacement) YGTheme.layout.padding.padding1 else 0.dp,
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                ).fillMaxWidth(),
        )
    }
}
