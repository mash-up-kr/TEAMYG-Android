package com.teamyg.parfait.feature.groups.canvas.impl.component

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
 * 토핑 배치 화면의 뼈대. 머리글, 캔버스 영역, 하단 확정 버튼을 세로로 쌓는다.
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CANVAS_AREA_ASPECT_RATIO),
            ) {
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
