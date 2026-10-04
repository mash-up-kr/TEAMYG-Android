package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import com.teamyg.parfait.core.designsystem.component.ygfloatingbar.YGFloatingBarBackTitleClose
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.util.DEFAULT_TOPPING_BORDER_WIDTH_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.groups.canvas.impl.R

/**
 * 토핑 배치 화면의 뼈대.
 *
 * 캔버스 영역은 자르지 않는다. 토핑의 점선 선택 박스는 캔버스 밖으로 나가도 보여야 한다.
 * 캔버스 내용을 자르는 것은 [canvas] 안에서 호출부가 한다.
 *
 * @param panel 캔버스 영역 위에 겹친다. 자리는 호출부가 `align` 으로 정한다.
 * @param toast 캔버스 윗변에 맞춰 화면 폭 전체로 겹친다. 스캐폴드의 토스트 자리(상태바 바로 아래)는
 *   헤더를 덮어서, 헤더 아래에 띄워야 하는 화면이 토스트 호스트를 여기에 둔다
 */
@Composable
internal fun ToppingArrangeLayout(
    header: @Composable () -> Unit,
    onClickConfirm: () -> Unit,
    panel: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    toast: @Composable () -> Unit = {},
    canvas: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var layoutWidthPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size -> layoutWidthPx = size.width },
    ) {
        header()

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            // fillMaxWidth() 를 붙이지 않는다. 최소 폭이 고정되면 aspectRatio 가 높이 쪽으로 줄이지 못해
            // 캔버스가 위아래로 넘치고, 뒤에 그려지는 확정 버튼이 패널을 덮는다
            Box(modifier = Modifier.aspectRatio(CANVAS_AREA_ASPECT_RATIO)) {
                canvas()
                panel()

                // 캔버스는 좌우 여백 안쪽에 있고 토스트는 화면 폭이다. 부모보다 넓은 requiredWidth 는
                // 가운데로 놓이고, 캔버스가 화면 가운데라 양쪽으로 같은 만큼 나간다
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .requiredWidth(with(density) { layoutWidthPx.toDp() }),
                ) {
                    toast()
                }
            }
        }

        YGButton(
            text = stringResource(R.string.canvas_topping_arrange_confirm),
            buttonType = YGButtonType.Large,
            isEnabled = true,
            onClick = onClickConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YGTheme.layout.padding.padding7)
                .padding(bottom = YGTheme.layout.padding.padding1),
        )
    }
}

@YGPreview
@Composable
private fun ToppingArrangeLayoutPreview() = PreviewBox {
    ToppingArrangeLayout(
        header = {
            YGFloatingBarBackTitleClose(
                title = stringResource(R.string.canvas_topping_place_title),
                onBackClick = {},
                onCloseClick = {},
            )
        },
        onClickConfirm = {},
        panel = {
            ToppingBorderPanel(
                isOpen = false,
                selectedColorArgb = null,
                widthDp = DEFAULT_TOPPING_BORDER_WIDTH_DP,
                widthRange = TOPPING_BORDER_WIDTH_RANGE_DP,
                onClickToggle = {},
                onSelectColor = {},
                onChangeWidth = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(YGAtomicColors.Gray.Gray200),
        )
    }
}
