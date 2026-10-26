package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPopAlpha
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingPopScale

/**
 * 저장된 배치대로 캔버스에 놓인 텍스트 하나. 캔버스 메인과 토핑 정리 화면이 함께 쓴다 — 둘의 박스와
 * 판정이 같아야 정리한 그대로 돌아간다.
 *
 * 위치는 이미지와 같이 박스 **중심**이다. 정렬은 저장되지 않아 가운데로 그린다.
 *
 * @param onBoxSizeChange scale 을 곱하기 전 박스 크기. 판정이 이 값에 scale 을 곱해 쓴다
 * @param onClick null 이면 접근성 클릭도 붙지 않는다 — 누를 수 없는 자리에서 버튼으로 읽히면 안 된다
 * @param popProgress 타임랩스 팝인 진행도(0~1). 1 이면 효과가 없다
 */
@Composable
internal fun PlacedTextTopping(
    text: String,
    color: TextToppingColor,
    type: TextToppingType,
    designFontSize: Float,
    center: DpOffset,
    scale: Float,
    rotationDegrees: Float,
    canvasWidth: Dp,
    onBoxSizeChange: (IntSize) -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    popProgress: Float = 1f,
) {
    TextToppingContent(
        text = text,
        color = color,
        type = type,
        align = TextAlign.Center,
        designFontSize = designFontSize,
        canvasWidth = canvasWidth,
        modifier = modifier
            .centeredAt(center)
            // graphicsLayer 의 배율은 잰 크기에 들어가지 않는다 — 그 앞에서 재야 scale 전 크기다
            .onSizeChanged(onBoxSizeChange)
            .graphicsLayer {
                val pop = if (popProgress < 1f) toppingPopScale(popProgress) else 1f
                scaleX = scale * pop
                scaleY = scale * pop
                rotationZ = rotationDegrees
                if (popProgress < 1f) alpha = toppingPopAlpha(popProgress)
            }.then(
                // 글자는 BasicText 가 읽어 주므로 설명을 따로 달지 않는다
                if (onClick != null) {
                    Modifier.semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick {
                            onClick()
                            true
                        }
                    }
                } else {
                    Modifier
                },
            ),
    )
}

@YGPreview
@Composable
private fun PreviewPlacedTextTopping() = PreviewBox {
    PlacedTextTopping(
        text = "오늘의 파르페",
        color = TextToppingColor.Black,
        type = TextToppingType.FILL,
        designFontSize = 18f,
        center = DpOffset(160.dp, 80.dp),
        scale = 1.2f,
        rotationDegrees = -10f,
        canvasWidth = 320.dp,
        onBoxSizeChange = {},
        onClick = null,
    )
}
