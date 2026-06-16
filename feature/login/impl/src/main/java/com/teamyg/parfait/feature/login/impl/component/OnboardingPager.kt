package com.teamyg.parfait.feature.login.impl.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview
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
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        Spacer(modifier = Modifier.height(44.dp))

        PagerIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (page.painterResourceId) {
            null -> Box(
                modifier = Modifier
                    .size(225.dp)
                    .clip(RectangleShape)
                    .background(Color(0xFFEBEBEB)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "[ 일러스트 ]",
                    color = Color(0xFF404040),
                    fontSize = 18.sp,
                )
            }

            else -> {
                // TODO impl
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = page.title,
            color = Color(0xFF333333),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (1.2).em,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            color = Color(0xFF737373),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = (1.4).em,
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

            Box(
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
