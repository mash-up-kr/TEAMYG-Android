package com.teamyg.parfait.feature.login.impl.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.login.impl.model.OnboardingPage
import com.teamyg.parfait.feature.login.impl.model.OnboardingPagesPreviewParameterProvider

@Composable
internal fun OnboardingPager(
    pages: List<OnboardingPage>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index ->
            OnboardingPageContent(page = pages[index], modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(page.painterResourceId),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(30.dp)) // 30.dp가 gap 없음

        Text(
            text = page.description,
            style = YGTheme.typography.caption.c01R,
            color = YGAtomicColors.Gray.Gray300,
        )
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF1A1A1A),
    inactiveColor: Color = Color(0xFFD9D9D9),
    dotSize: Dp = 8.dp,
    spacing: Dp = 8.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val color by animateColorAsState(
                targetValue = if (index == currentPage) activeColor else inactiveColor,
                label = "indicatorColor",
            )

            Spacer(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewOnboardingPager(
    @PreviewParameter(OnboardingPagesPreviewParameterProvider::class) pages: List<OnboardingPage>,
) = PreviewBox {
    OnboardingPager(
        pages = pages,
        modifier = Modifier.fillMaxSize(),
    )
}
