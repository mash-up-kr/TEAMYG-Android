package com.teamyg.parfait.core.designsystem.component.ygdimoverlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * 화면 위에 덮는 Dim 판. 그 아래 컨텐츠의 터치를 삼킨다.
 *
 * 터치 차단에 `clickable` 이 아니라 [pointerInput] 을 쓰는 이유: `clickable` 은 클릭
 * 시맨틱과 접근성 액션을 붙여 TalkBack 이 이 판을 버튼으로 읽는다. 여기서 필요한
 * 것은 "누를 수 있는 것"이 아니라 "지나갈 수 없는 것"이다. [content] 안의 버튼은
 * 자식이 포인터를 먼저 받으므로 그대로 눌린다.
 *
 * @param contentDescription 판 전체를 한 덩어리로 읽힐 문구. **누를 것이 있으면 넘기지
 *   않는다** — 자식을 병합해 버리면 버튼이 개별 액션을 잃는다
 */
@Composable
fun YGDimOverlay(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(YGAtomicColors.Transparency.Black75)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }.then(
                if (contentDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics(mergeDescendants = true) {
                        this.contentDescription = contentDescription
                    }
                },
            ),
        content = content,
    )
}
