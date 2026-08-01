package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationViewModel

@Composable
internal fun SegmentationRoute(
    navigator: Navigator,
    key: NavKeySegmentation,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationViewModel, SegmentationViewModel.Factory>(
        creationCallback = { factory -> factory.create(key.sourceImageUri) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // 실패는 SegmentationErrorScreen 이 보여주므로 여기서 화면을 벗어나지 않는다
                is SegmentationEffect.SegmentationFailed -> {
                    Toast.makeText(context, "객체 감지 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SegmentationScreen(
        state = state,
        modifier = modifier,
        onClickBack = { navigator.onBack() },
        // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
        onClickSubject = {
            state.subjectImagePath?.let { path ->
                navigator.goTo(NavKeySegmentationConfirm(subjectImagePath = path))
            }
        },
    )
}
