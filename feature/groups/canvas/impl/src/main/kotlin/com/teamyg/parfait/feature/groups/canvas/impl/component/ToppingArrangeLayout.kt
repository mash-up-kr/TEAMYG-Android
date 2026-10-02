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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 */
@Composable
internal fun ToppingArrangeLayout(
    header: @Composable () -> Unit,
    onClickConfirm: () -> Unit,
    panel: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    canvas: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
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
