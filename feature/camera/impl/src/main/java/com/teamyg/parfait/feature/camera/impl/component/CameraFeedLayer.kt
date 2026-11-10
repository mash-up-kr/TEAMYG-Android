package com.teamyg.parfait.feature.camera.impl.component

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.model.CameraTransition
import kotlin.math.PI
import kotlin.math.sin

private val IS_BLUR_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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
private val VIEWFINDER_COLOR = YGAtomicColors.Gray.Gray500

/**
 * 카메라 피드를 한 번 기록해 두 번 그린다 — 뷰파인더 밖은 흐리게, 안은 원본으로.
 * 블러가 지원되지 않는 버전에서는 스크림만 적용한다.
 */
@Composable
internal fun CameraFeedLayer(
    viewfinderRect: () -> Rect?,
    transition: CameraTransition?,
    onFeedRectChange: (Rect) -> Unit,
    onTapViewfinder: (Offset) -> Unit,
    onPinchZoomStart: () -> Unit,
    onPinchZoom: (scale: Float) -> Unit,
    onPinchZoomEnd: () -> Unit,
    modifier: Modifier = Modifier,
    feed: @Composable () -> Unit,
) {
    val contentLayer = rememberGraphicsLayer()
    val blurLayer = rememberGraphicsLayer()
    val currentOnTapViewfinder by rememberUpdatedState(onTapViewfinder)
    val currentOnPinchZoomStart by rememberUpdatedState(onPinchZoomStart)
    val currentOnPinchZoom by rememberUpdatedState(onPinchZoom)
    val currentOnPinchZoomEnd by rememberUpdatedState(onPinchZoomEnd)

    Box(
        modifier = modifier
            .onLayoutRectChanged(debounceMillis = 0) { onFeedRectChange(it.boundsInRoot.toRect()) }
            .viewfinderScrim(
                contentLayer = contentLayer,
                blurLayer = blurLayer,
                viewfinderRect = viewfinderRect,
            ).pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (viewfinderRect()?.contains(offset) == true) currentOnTapViewfinder(offset)
                }
            }.pointerInput(Unit) {
                detectPinchZoom(
                    onPinchZoomStart = { currentOnPinchZoomStart() },
                    onPinchZoom = { scale -> currentOnPinchZoom(scale) },
                    onPinchZoomEnd = { currentOnPinchZoomEnd() },
                )
            },
    ) {
        feed()
        CameraTransitionCover(
            transition = transition,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun Modifier.viewfinderScrim(
    contentLayer: GraphicsLayer,
    blurLayer: GraphicsLayer,
    viewfinderRect: () -> Rect?,
): Modifier = drawWithCache {
    val radius = BLUR_RADIUS.toPx()
    if (IS_BLUR_SUPPORTED) {
        blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
    }
    val borderStroke = Stroke(width = VIEWFINDER_BORDER_WIDTH.toPx())
    val cornerLength = VIEWFINDER_CORNER_LENGTH.toPx()
    val cornerWidth = VIEWFINDER_CORNER_WIDTH.toPx()
    val framePadding = VIEWFINDER_FRAME_PADDING.toPx()

    onDrawWithContent {
        contentLayer.record { this@onDrawWithContent.drawContent() }

        if (IS_BLUR_SUPPORTED) {
            blurLayer.record { drawLayer(contentLayer) }
            drawLayer(blurLayer)
        } else {
            drawLayer(contentLayer)
        }

        drawRect(color = YGAtomicColors.Transparency.Black25)

        val rect = viewfinderRect() ?: return@onDrawWithContent
        clipRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
        ) {
            drawLayer(contentLayer)
        }
        drawRect(
            color = VIEWFINDER_COLOR,
            topLeft = rect.topLeft,
            size = rect.size,
            style = borderStroke,
        )
        drawViewfinderCorners(
            frame = rect.deflate(framePadding),
            cornerLength = cornerLength,
            cornerWidth = cornerWidth,
        )
    }
}

private fun DrawScope.drawViewfinderCorners(
    frame: Rect,
    cornerLength: Float,
    cornerWidth: Float,
) {
    val topLeft = Offset(frame.left, frame.top)
    val topRight = Offset(frame.right, frame.top)
    val bottomLeft = Offset(frame.left, frame.bottom)
    val bottomRight = Offset(frame.right, frame.bottom)
    val right = Offset(cornerLength, 0f)
    val down = Offset(0f, cornerLength)
    drawLine(VIEWFINDER_COLOR, topLeft, topLeft + right, cornerWidth)
    drawLine(VIEWFINDER_COLOR, topLeft, topLeft + down, cornerWidth)
    drawLine(VIEWFINDER_COLOR, topRight - right, topRight, cornerWidth)
    drawLine(VIEWFINDER_COLOR, topRight, topRight + down, cornerWidth)
    drawLine(VIEWFINDER_COLOR, bottomLeft, bottomLeft + right, cornerWidth)
    drawLine(VIEWFINDER_COLOR, bottomLeft - down, bottomLeft, cornerWidth)
    drawLine(VIEWFINDER_COLOR, bottomRight - right, bottomRight, cornerWidth)
    drawLine(VIEWFINDER_COLOR, bottomRight - down, bottomRight, cornerWidth)
}

/**
 * 카메라를 다시 여는 동안 검정 화면 대신 직전 프레임을 흐리게 덮는다. 블러는 API 31 미만에서 빠진다.
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
                    // 앞 절반은 0→90°, 뒤 절반은 -90→0°. 180°까지 돌리면 넘긴 뒤 프레임이 거꾸로 남는다
                    rotationX = if (progress < 0.5f) progress * 180f else progress * 180f - 180f
                    val scale = 1f - (1f - TRANSITION_FLIP_MIN_SCALE) * sin(progress * PI.toFloat())
                    scaleX = scale
                    scaleY = scale
                    cameraDistance = TRANSITION_CAMERA_DISTANCE * density
                }.blur(radius = TRANSITION_BLUR_RADIUS, edgeTreatment = BlurredEdgeTreatment.Rectangle),
        )
    }
}

private class CameraFeedLayerPreviewParameterProvider : NamedPreviewParameterProvider<CameraTransition?>() {
    override val entries = listOf(
        "idle" to null,
        "transition" to CameraTransition(frame = ImageBitmap(width = 9, height = 16), isFlip = false),
    )
}

@YGPreview
@Composable
private fun PreviewCameraFeedLayer(
    @PreviewParameter(CameraFeedLayerPreviewParameterProvider::class) transition: CameraTransition?,
) = PreviewBox {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val inset = with(LocalDensity.current) { 24.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val viewfinderRect = Rect(
            left = inset,
            top = inset * 4,
            right = width - inset,
            bottom = height - inset * 6,
        )

        CameraFeedLayer(
            viewfinderRect = { viewfinderRect },
            transition = transition,
            onFeedRectChange = {},
            onTapViewfinder = {},
            onPinchZoomStart = {},
            onPinchZoom = {},
            onPinchZoomEnd = {},
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(YGAtomicColors.Gray.White, YGAtomicColors.Gray.Gray500),
                        ),
                    ),
            )
        }
    }
}
