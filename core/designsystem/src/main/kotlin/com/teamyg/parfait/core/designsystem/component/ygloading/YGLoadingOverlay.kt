package com.teamyg.parfait.core.designsystem.component.ygloading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

const val YG_LOADING_OVERLAY_TEST_TAG = "yg_loading_overlay"

/**
 * 로딩 중 화면 위에 덮는 오버레이. Dim 과 인디케이터를 그리고 그 아래 컨텐츠의
 * 터치를 삼킨다.
 *
 * 기본이 화면 전체를 덮는 크기다 — [modifier] 를 비워 호출해도 스피너 크기로
 * 쭈그러들지 않는다. 크기를 더 좁히거나 넓히고 싶으면 [modifier] 로 덧붙인다.
 *
 * ⚠️ 임시 구현이다 — 로딩 UI 디자인이 아직 정해지지 않았다. Dim 농도·인디케이터 모양·
 * 문구 유무 전부 확정 전 자리 채움이고, 디자인이 나오면 이 파일만 고친다.
 * 다른 곳에 로딩 UI 를 복제하지 마라 — 그러면 고칠 곳이 늘어난다.
 *
 * 터치 차단에 `clickable` 이 아니라 [pointerInput] 을 쓰는 이유: `clickable` 은 클릭
 * 시맨틱과 접근성 액션을 붙여 TalkBack 이 이 오버레이를 버튼으로 읽는다. 여기서 필요한
 * 것은 "누를 수 있는 것"이 아니라 "지나갈 수 없는 것"이다.
 */
@Composable
fun YGLoadingOverlay(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.yg_loading_overlay_description)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .testTag(YG_LOADING_OVERLAY_TEST_TAG)
            .background(YGAtomicColors.Transparency.Black25)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        CircularProgressIndicator(color = YGAtomicColors.Cherry.Cherry100)
    }
}

@YGPreview
@Composable
private fun YGLoadingOverlayPreview() = PreviewBox {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "가려질 컨텐츠",
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray900,
        )
        YGLoadingOverlay()
    }
}
