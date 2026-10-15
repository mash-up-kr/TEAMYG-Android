package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.computeToppingButtonPoints
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingCenter
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingImageSize
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingLongSide
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

internal data class EditableToppingDrawEntry(
    val topping: EditableTopping,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val center: DpOffset,
    val size: DpSize,
    val drawnBorderWidthDp: Float,
)

/** 배치와 크기만 잰다. 거리판은 호출부가 따로 띄워 넘긴다 */
@Composable
internal fun rememberEditableToppingDrawEntries(
    toppings: List<EditableTopping>,
    canvasWidth: Dp,
    canvasHeight: Dp,
): List<EditableToppingDrawEntry> = toppings.map { topping ->
    key(topping.parfaitImageId) {
        val painter = rememberAsyncImagePainter(model = topping.imageUrl)
        val painterState by painter.state.collectAsState()
        val intrinsicSize = painter.intrinsicSize

        val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            0f
        }

        EditableToppingDrawEntry(
            topping = topping,
            painter = painter,
            center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.positionX,
                positionY = topping.positionY,
            ),
            size = toppingImageSize(
                longSide = toppingLongSide(canvasWidth, topping.scale),
                aspectRatio = aspectRatio,
            ),
            // 테두리를 그리지 않는 상태에서는 판정도 넓히지 않는다 — 그리지 않은 링만큼 부풀면
            // 판정이 외형과 어긋난다
            drawnBorderWidthDp = topping.border
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.widthDp
                ?: 0f,
        )
    }
}

internal data class EditableToppingHitEntry(
    val draw: EditableToppingDrawEntry,
    val target: ToppingHitTarget,
) {
    val topping: EditableTopping get() = draw.topping
}

/**
 * @param outlines [EditableTopping.imageUrl] 로 찾는다
 */
@Composable
internal fun rememberEditableToppingHitEntries(
    drawEntries: List<EditableToppingDrawEntry>,
    outlines: Map<String, ToppingOutline>,
): List<EditableToppingHitEntry> {
    val density = LocalDensity.current

    return drawEntries.map { entry ->
        EditableToppingHitEntry(
            draw = entry,
            target = with(density) {
                ToppingHitTarget(
                    centerXPx = entry.center.x.toPx(),
                    centerYPx = entry.center.y.toPx(),
                    imageWidthPx = entry.size.width.toPx(),
                    imageHeightPx = entry.size.height.toPx(),
                    rotationDegrees = entry.topping.rotationDegrees,
                    borderWidthPx = entry.drawnBorderWidthDp.dp.toPx(),
                    outline = outlines[entry.topping.imageUrl],
                )
            },
        )
    }
}

/**
 * 캔버스 미리보기 박스 안, 저장된 배치([EditableTopping.positionX]/[positionY])대로 겹쳐 그리는
 * 이미지. 캔버스 메인([CanvasToppingLayer])과 같은 규칙을 써야 편집한 그대로 돌아간다.
 *
 * 선택 시 보이는 스트로크·버튼은 이 이미지와 함께 돌지 않아야 해서 [ToppingFocusDecoration]에서
 * 별도로 그린다.
 *
 * Box 가 이미지보다 [EditableToppingDrawEntry.drawnBorderWidthDp]만큼 크고 그만큼 안쪽으로 덜어낸다.
 * [YGToppingCutoutImage]가 거리판으로 만든 띠는 그 폭만큼 상자 밖으로 나가는데, `alpha`가
 * 1 미만이면 오프스크린 버퍼가 생겨 레이어 밖으로 나간 부분이 잘리기 때문이다.
 *
 * @param onClick null 이면 접근성 클릭도 붙지 않는다 — 실제로 누를 수 없는 화면에서 버튼으로
 *   읽히면 안 된다.
 * @param centerOverride 패널이 열린 동안 그릴 자리. null 이면 저장된 배치대로 그린다
 */
@Composable
internal fun EditableToppingImage(
    entry: EditableToppingDrawEntry,
    outline: ToppingOutline?,
    alpha: Float,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    centerOverride: DpOffset? = null,
) {
    val painterState by entry.painter.state.collectAsState()
    val border = entry.topping.border
    val description = stringResource(R.string.canvas_topping_content_description)
    val outlineInset = entry.drawnBorderWidthDp.dp

    Box(
        modifier = modifier
            .testTag("editable_topping_${entry.topping.parfaitImageId}")
            .centeredAt(centerOverride ?: entry.center)
            .requiredSize(entry.size + DpSize(outlineInset * 2, outlineInset * 2))
            .graphicsLayer(
                rotationZ = entry.topping.rotationDegrees,
                alpha = alpha,
            ).let { base ->
                if (onClick == null) {
                    base
                } else {
                    // 판정은 입력 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
                    base.semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = description
                        onClick {
                            onClick()
                            true
                        }
                    }
                }
            },
    ) {
        YGToppingCutoutImage(
            painter = entry.painter,
            // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
            borderColor = border
                ?.let { Color(it.colorArgb) }
                ?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = (border?.widthDp ?: 0f).dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(outlineInset),
            outline = outline,
        )
    }
}

/**
 * 선택된 토핑의 점선 선택 박스와 좌상단 삭제 버튼.
 *
 * @param center 선택 박스를 두는 자리. 이미지를 그린 자리와 같아야 한다
 * @param showActionButtons 제스처 중에는 `false`. 포인터 대상은 down 시점에 정해지므로 첫 down부터
 *   버튼을 빼야 두 번째 손가락을 버튼이 가로채지 않는다.
 */
@Composable
internal fun ToppingFocusDecoration(
    entry: EditableToppingHitEntry,
    center: DpOffset,
    onClickDelete: () -> Unit,
    showActionButtons: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizeAfterScale = with(density) {
        DpSize(entry.target.imageWidthPx.toDp(), entry.target.imageHeightPx.toDp())
    }
    val buttonPoints = computeToppingButtonPoints(
        center = center,
        sizeAfterScale = sizeAfterScale,
        rotationDegrees = entry.topping.rotationDegrees,
    )

    Box(modifier = modifier) {
        ToppingSelectionStroke(
            center = center,
            sizeAfterScale = sizeAfterScale,
            rotationDegrees = entry.topping.rotationDegrees,
        )
        if (showActionButtons) {
            YGCircleButton(
                iconResource = DesignSystemR.drawable.ic_close,
                type = YGCircleButtonType.Small,
                contentDescription = stringResource(R.string.canvas_bg_edit_topping_delete),
                onClick = onClickDelete,
                modifier = Modifier.centeredAt(buttonPoints.topLeft),
            )
        }
    }
}
