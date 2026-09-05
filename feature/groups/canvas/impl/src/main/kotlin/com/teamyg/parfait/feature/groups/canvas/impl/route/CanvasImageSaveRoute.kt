package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_IMAGE_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasImageSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasImageSaveScreen
import kotlinx.datetime.LocalDate

/**
 * 미리보기만 하는 화면이라 ViewModel 이 없다 — 보여 줄 것은 넘겨받은 이미지가 전부이고,
 * 갤러리 저장은 결과를 받는 캔버스 메인의 몫이다.
 */
@Composable
internal fun CanvasImageSaveRoute(
    navKey: NavKeyCanvasImageSave,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        CanvasImageSaveScreen(
            imagePath = navKey.imagePath,
            date = LocalDate.parse(navKey.date),
            onClickClose = { navigator.onBack() },
            onClickSave = {
                resultEventBus.sendResult(
                    CANVAS_IMAGE_SAVE_RESULT_KEY,
                    CanvasImageSaveResult(imagePath = navKey.imagePath),
                )
                navigator.onBack()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
