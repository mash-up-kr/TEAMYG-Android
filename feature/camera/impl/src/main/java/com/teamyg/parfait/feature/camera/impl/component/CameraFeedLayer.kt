package com.teamyg.parfait.feature.camera.impl.component

import android.os.Build
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.camera.impl.model.CameraTransition
import kotlin.math.PI
import kotlin.math.sin

private val BLUR_RADIUS = 4.dp
private val TRANSITION_BLUR_RADIUS = 24.dp
private const val TRANSITION_FLIP_MILLIS = 300
private const val TRANSITION_FADE_MILLIS = 150
private const val TRANSITION_CAMERA_DISTANCE = 10f
private const val TRANSITION_FLIP_MIN_SCALE = 0.6f
private val VIEWFINDER_BORDER_WIDTH = 1.dp
private val VIEWFINDER_CORNER_LENGTH = 18.dp
private val VIEWFINDER_CORNER_WIDTH = 2.dp
private val VIEWFINDER_FRAME_PADDING = 10.dp

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
    transition: CameraTransition?,
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
                    drawRect(
                        color = YGAtomicColors.Gray.Gray500,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = VIEWFINDER_BORDER_WIDTH.toPx()),
                    )

                    val cornerLength = VIEWFINDER_CORNER_LENGTH.toPx()
                    val cornerWidth = VIEWFINDER_CORNER_WIDTH.toPx()
                    val frameRect = rect.deflate(VIEWFINDER_FRAME_PADDING.toPx())

                    listOf(
                        // topLeft
                        Offset(frameRect.left, frameRect.top) to Offset(frameRect.left + cornerLength, frameRect.top),
                        Offset(frameRect.left, frameRect.top) to Offset(frameRect.left, frameRect.top + cornerLength),
                        // topRight
                        Offset(frameRect.right - cornerLength, frameRect.top) to Offset(frameRect.right, frameRect.top),
                        Offset(frameRect.right, frameRect.top) to Offset(frameRect.right, frameRect.top + cornerLength),
                        // bottomLeft
                        Offset(frameRect.left, frameRect.bottom) to
                            Offset(frameRect.left + cornerLength, frameRect.bottom),
                        Offset(frameRect.left, frameRect.bottom - cornerLength) to
                            Offset(frameRect.left, frameRect.bottom),
                        // bottomRight
                        Offset(frameRect.right - cornerLength, frameRect.bottom) to
                            Offset(frameRect.right, frameRect.bottom),
                        Offset(frameRect.right, frameRect.bottom - cornerLength) to
                            Offset(frameRect.right, frameRect.bottom),
                    ).forEach { (start, end) ->
                        drawLine(
                            color = YGAtomicColors.Gray.Gray500,
                            start = start,
                            end = end,
                            strokeWidth = cornerWidth,
                        )
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
        CameraTransitionCover(
            transition = transition,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 카메라를 다시 여는 동안 검정 화면 대신 직전 프레임을 흐리게 덮는다. 전면/후면 전환이면 위아래로 넘긴다.
 * 블러는 API 31 미만에서 빠지고 피드 레이어의 딤만 남는다.
 */
@Composable
private fun CameraTransitionCover(
    transition: CameraTransition?,
    modifier: Modifier = Modifier,
) {
    // 걷히는 동안(페이드아웃) 그릴 덮개를 붙들어 둔다
    var shown by remember { mutableStateOf(transition) }
    if (transition != null) shown = transition

    AnimatedVisibility(
        visible = transition != null,
        modifier = modifier,
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = tween(durationMillis = TRANSITION_FADE_MILLIS)),
    ) {
        val cover = shown ?: return@AnimatedVisibility
        // 0→1 진행도. 앞 절반은 0→90°로 넘기고 뒤 절반은 -90→0°로 돌려 세워, 다 넘어간 뒤 프레임이 거꾸로 남지 않는다
        val flipProgress = remember(cover) { Animatable(0f) }
        LaunchedEffect(cover) {
            if (cover.isFlip) {
                flipProgress.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = TRANSITION_FLIP_MILLIS))
            }
        }

        Image(
            bitmap = cover.frame,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = flipProgress.value
                    rotationX = if (progress < 0.5f) progress * 180f else progress * 180f - 180f
                    // 넘어가는 동안 카드처럼 작아졌다 돌아와야 원근이 산다(기본 카메라)
                    val scale = 1f - (1f - TRANSITION_FLIP_MIN_SCALE) * sin(progress * PI.toFloat())
                    scaleX = scale
                    scaleY = scale
                    cameraDistance = TRANSITION_CAMERA_DISTANCE * density
                }.blur(radius = TRANSITION_BLUR_RADIUS, edgeTreatment = BlurredEdgeTreatment.Rectangle),
        )
    }
}
