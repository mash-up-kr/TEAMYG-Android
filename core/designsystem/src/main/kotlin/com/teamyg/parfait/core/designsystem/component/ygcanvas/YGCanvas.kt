package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenu
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuAction
import com.teamyg.parfait.core.designsystem.component.ygcanvasmenu.YGCanvasMenuItem
import com.teamyg.parfait.core.designsystem.component.ygcanvasdateselect.YGCanvasDateSelectButton
import com.teamyg.parfait.core.designsystem.component.ygskeleton.YGSkeleton
import com.teamyg.parfait.core.designsystem.shape.canvasCutCornerShape
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * Canvas-Area 종횡비. 캔버스를 그리는 화면과 그 위 좌표를 계산하는 화면이 **같은 값**을 써야
 * 저장된 배치가 맞는 자리에 얹힌다 — 값이 갈리면 모든 토핑의 세로 위치가 조금씩 밀리고
 * 컴파일은 깨지지 않는다.
 */
const val CANVAS_AREA_ASPECT_RATIO = 9f / 16f

@Composable
fun YGCanvas(
    date: String,
    day: String,
    onDateSelectClick: () -> Unit,
    /** 없으면(`null`) [YGCanvasMenu] 가 [editAction] 만 전체 너비로 보여준다 */
    addAction: YGCanvasMenuAction?,
    editAction: YGCanvasMenuAction,
    modifier: Modifier = Modifier,
    /** 미설정이면 null — 그때는 흰 바탕이 깔린다 */
    background: YGCanvasBackground? = null,
    isDimmed: Boolean = false,
    onDimClick: () -> Unit = {},
    isMenuExpanded: Boolean = false,
    /** 토핑이 하나도 없는 캔버스. [background] 까지 미설정일 때만 [emptyMessage] 안내판이 덮는다 */
    isEmpty: Boolean = false,
    isCalendarVisible: Boolean = false,
    expandedItems: List<YGCanvasMenuItem> = emptyList(),
    emptyMessage: String = "",
    isSaveVisible: Boolean = false,
    onClickSave: () -> Unit = {},
    saveContentDescription: String? = null,
    calendarContent: @Composable () -> Unit = {},
    /**
     * 넘기면 배경+토핑(테두리·모서리 컷 모양, 빈 캔버스 문구, 날짜 버튼 같은 프레임 UI는 제외)을
     * 그릴 때마다 이 레이어에도 함께 기록해, 호출부가 나중에 [GraphicsLayer.toImageBitmap]으로
     * 캡처할 수 있게 한다.
     */
    captureGraphicsLayer: GraphicsLayer? = null,
    /** 캔버스 프레임의 진짜 상단 테두리 선에 위쪽이 맞춰지는 오버레이 자리(토스트·얼럿 등)다 */
    overlayContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = canvasCutCornerShape()
    val defaultHorizontalPadding = YGTheme.layout.padding.padding7
    val minVerticalGap = YGTheme.layout.padding.padding5
    val menuHeight = SizeTokens.Size44.getDp()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val metrics = calculateCanvasLayoutMetrics(
            defaultHorizontalPadding = defaultHorizontalPadding,
            minVerticalGap = minVerticalGap,
            menuHeight = menuHeight,
        )

        Box(
            modifier = Modifier
                .padding(start = metrics.horizontalPadding, top = metrics.verticalGap)
                .width(metrics.canvasWidth)
                .height(metrics.canvasAreaHeight + menuHeight - 1.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(-1.dp)) {
                CanvasArea(
                    shape = shape,
                    background = background,
                    isEmpty = isEmpty,
                    emptyMessage = emptyMessage,
                    content = content,
                    captureGraphicsLayer = captureGraphicsLayer,
                    dateSelect = {
                        if (isCalendarVisible.not()) {
                            YGCanvasDateSelectButton(
                                date = date,
                                day = day,
                                onClick = onDateSelectClick,
                                isSaveVisible = isSaveVisible,
                                onClickSave = onClickSave,
                                saveContentDescription = saveContentDescription,
                            )
                        }
                    },
                )
                if (isMenuExpanded) {
                    Spacer(modifier = Modifier.height(menuHeight))
                } else {
                    YGCanvasMenu(
                        addAction = addAction,
                        editAction = editAction,
                    )
                }
            }

            if (isDimmed) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(color = YGAtomicColors.Transparency.Black25)
                        .clickableYGNoRipple(
                            onClick = onDimClick,
                        ),
                )
            }

            if (isCalendarVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 달력은 딤 위에 겹쳐 있을 뿐이라 항목 사이 빈 자리를 누르면 뒤의 딤이 받아 달력이 닫힌다
                        .pointerInput(Unit) { detectTapGestures { /* 달력 안에서 소비 */ } },
                ) {
                    YGCanvasDateSelectButton(
                        date = date,
                        day = day,
                        onClick = onDateSelectClick,
                        isSaveVisible = isSaveVisible,
                        onClickSave = onClickSave,
                        saveContentDescription = saveContentDescription,
                    )
                    calendarContent()
                }
            }

            if (isMenuExpanded) {
                YGCanvasMenu(
                    addAction = addAction,
                    editAction = editAction,
                    isExpanded = true,
                    expandedItems = expandedItems,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // metrics.verticalGap 은 캔버스 프레임(테두리 포함)의 실제 상단 위치라, 여기서 그대로
        // 써야 오버레이 위쪽이 캔버스 상단 테두리 선에 정확히 걸린다
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = metrics.verticalGap + 1.dp),
        ) {
            overlayContent()
        }
    }
}

private data class CanvasLayoutMetrics(
    val canvasWidth: Dp,
    val canvasAreaHeight: Dp,
    val verticalGap: Dp,
    val horizontalPadding: Dp,
)

/**
 * Canvas-Area만 16:9 비율을 유지하고 Canvas-Menu는 고정 높이로 더해진다.
 * 상하 gap은 항상 동일한 값을 유지하며 최소 gap([minVerticalGap])을 보장하고,
 * 세로 공간이 부족하면 캔버스를 축소해 좌우 패딩이 [defaultHorizontalPadding]보다 커진다.
 */
private fun BoxWithConstraintsScope.calculateCanvasLayoutMetrics(
    defaultHorizontalPadding: Dp,
    minVerticalGap: Dp,
    menuHeight: Dp,
): CanvasLayoutMetrics {
    val defaultWidth = maxWidth - defaultHorizontalPadding * 2
    val defaultAreaHeight = defaultWidth / CANVAS_AREA_ASPECT_RATIO
    val defaultTotalHeight = defaultAreaHeight + menuHeight

    if (!constraints.hasBoundedHeight) {
        return CanvasLayoutMetrics(defaultWidth, defaultAreaHeight, minVerticalGap, defaultHorizontalPadding)
    }

    val availableHeight = maxHeight - minVerticalGap * 2
    if (defaultTotalHeight <= availableHeight) {
        return CanvasLayoutMetrics(
            canvasWidth = defaultWidth,
            canvasAreaHeight = defaultAreaHeight,
            verticalGap = (maxHeight - defaultTotalHeight) / 2,
            horizontalPadding = defaultHorizontalPadding,
        )
    }

    val areaHeight = availableHeight - menuHeight
    val width = areaHeight * CANVAS_AREA_ASPECT_RATIO
    return CanvasLayoutMetrics(
        canvasWidth = width,
        canvasAreaHeight = areaHeight,
        verticalGap = minVerticalGap,
        horizontalPadding = (maxWidth - width) / 2,
    )
}

@Composable
private fun CanvasArea(
    shape: Shape,
    background: YGCanvasBackground?,
    isEmpty: Boolean,
    emptyMessage: String,
    dateSelect: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
    captureGraphicsLayer: GraphicsLayer? = null,
) {
    // 캡처 대상은 배경+토핑뿐이다 — 테두리·모서리 컷 모양, 빈 캔버스 안내 문구, 날짜 버튼은
    // 프레임(UI 크롬)이라 갤러리에 저장되는 이미지에는 안 들어가야 한다. 그래서 그 셋을 그리는
    // 바깥 Box가 아니라, 배경+토핑만 담는 안쪽 Box에 캡처 레이어를 건다.
    val captureModifier = if (captureGraphicsLayer != null) {
        Modifier.drawWithContent {
            captureGraphicsLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureGraphicsLayer)
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
            .clip(shape)
            .border(
                width = 1.dp,
                color = YGAtomicColors.Gray.Gray500,
                shape = shape,
            ),
    ) {
        Box(modifier = captureModifier.matchParentSize()) {
            when (background) {
                // 배경을 안 고른 캔버스에도 토핑은 흰 바탕 위에 깔린다
                null -> Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = YGAtomicColors.Gray.White),
                )

                is YGCanvasBackground.Solid -> Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = background.color),
                )

                // 로딩 자리에 움직이는 스켈레톤을 깔려면 Painter 가 아니라 컴포저블 슬롯이
                // 필요해 SubcomposeAsyncImage 를 쓴다. 배경은 캔버스마다 한 장이라 서브컴포지션
                // 비용이 목록처럼 쌓이지 않는다
                is YGCanvasBackground.Image -> SubcomposeAsyncImage(
                    model = background.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = { YGSkeleton(modifier = Modifier.matchParentSize()) },
                    modifier = Modifier.matchParentSize(),
                )
            }

            content()
        }

        // 배경도 토핑도 없을 때만 회색 안내판이 덮는다 — 배경이 정해지는 순간 안내는 사라지고
        // 고른 배경이 그대로 보여야 한다
        if (isEmpty && background == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = YGAtomicColors.Gray.Gray100),
            ) {
                Text(
                    text = emptyMessage,
                    style = YGTheme.typography.caption.c01M,
                    color = YGAtomicColors.Gray.Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = YGTheme.layout.padding.padding6),
                )
            }
        }

        dateSelect()
    }
}

@YGPreview
@Composable
private fun YGCanvasPreview() = PreviewBox {
    YGCanvas(
        date = "May 20",
        day = "(Wed)",
        onDateSelectClick = {},
        addAction = YGCanvasMenuAction(
            text = "토핑 추가",
            iconResource = R.drawable.ic_plus,
            onClick = {},
        ),
        editAction = YGCanvasMenuAction(
            text = "캔버스 편집",
            iconResource = R.drawable.ic_caret_right,
            onClick = {},
        ),
        isEmpty = true,
        emptyMessage = "아직 캔버스가 비어 있어요\n첫번째 토핑을 올려 캔버스를 채워보세요",
    )
}
