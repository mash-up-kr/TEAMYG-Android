package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState

/**
 * 테두리 탭의 내용. 잘라낸 결과에 두를 테두리를 다듬는 화면이다.
 *
 * 아직 자리만 잡아둔 상태다. 영역 탭과 달리 마스크를 바꾸지 않고 결과 이미지 바깥에 선을 두르는
 * 작업이라, 굵기와 색을 고르는 컨트롤과 그 값을 반영한 미리보기가 들어올 자리다.
 */
// Todo : 테두리 굵기/색 편집 구현
@Composable
internal fun ToppingBorderEditScreen(
    state: ToppingEditState,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.topping_edit_border_preparing))
    }
}

@YGPreview
@Composable
private fun PreviewToppingBorderEditScreen() = PreviewBox {
    ToppingBorderEditScreen(
        state = ToppingEditState(),
        modifier = Modifier.fillMaxSize(),
    )
}
