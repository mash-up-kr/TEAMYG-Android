package com.teamyg.parfait.feature.camera.impl.component

import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

private val BLUR_RADIUS = 4.dp

/**
 * 단일 카메라 피드를 한 번 기록해 두 번 그린다.
 * 1. 전체를 블러로 그리고 2. 딤 스크림을 얹은 뒤 3. 뷰파인더 영역만 원본으로 복원한다.
 * 블러가 지원되지 않는 버전에서는 스크림만 적용한다.
 */
@Composable
internal fun CameraFeedLayer(
    previewView: PreviewView,
    camera: Camera?,
    viewfinderRect: () -> Rect?,
    onFeedRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    isBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
) {
    val contentLayer = rememberGraphicsLayer()
    val blurLayer = rememberGraphicsLayer()

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates -> onFeedRectChange(coordinates.boundsInRoot()) }
            .drawWithContent {
                contentLayer.record { this@drawWithContent.drawContent() }

                if (isBlurSupported) {
                    val radius = BLUR_RADIUS.toPx()
                    blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    blurLayer.record { drawLayer(contentLayer) }
                    drawLayer(blurLayer)
                } else {
                    drawLayer(contentLayer)
                }

                drawRect(color = YGAtomicColors.Transparency.Black25)

                viewfinderRect()?.let { rect ->
                    clipRect(
                        left = rect.left,
                        top = rect.top,
                        right = rect.right,
                        bottom = rect.bottom,
                    ) {
                        drawLayer(contentLayer)
                    }
                }
            }.pointerInput(camera, previewView) {
                detectTapGestures { offset ->
                    val focusTarget = camera ?: return@detectTapGestures
                    if (viewfinderRect()?.contains(offset) != true) return@detectTapGestures

                    val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                    val action = FocusMeteringAction.Builder(point).build()

                    focusTarget.cameraControl.startFocusAndMetering(action)
                }
            },
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
