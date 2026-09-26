package com.teamyg.parfait.feature.groups.list.impl.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingBorder
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingTemplate
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder

// TODO(토핑 템플릿): 정책은 그룹 생성 시 6종 중 하나를 무작위로 골라 고정하는 것이다.
//  서버가 그 값을 내려주면 groupId 파생을 걷어낸다
private val TOPPING_TEMPLATES = YGToppingTemplate.entries

/**
 * 오늘 캔버스에 토핑이 없는 그룹은 조회 실패([YGToppingImage.Error])와 다른 상태라 템플릿을 띄운다.
 * ⚠️ 어제까지 토핑이 있던 그룹도 여기 걸린다 — 서버가 오늘 것만 내려주기 때문이고, 그것을 템플릿으로
 * 그리는 것이 맞는지는 아직 결정 전이다(OQ-P-336, api/parfait-group.md).
 * 목록 순서가 바뀌어도 같은 그림이 걸리도록 index 가 아니라 groupId 로 고른다.
 */
internal fun MyParfaitGroupVO.toToppingImage(outlines: Map<String, ToppingOutline>): YGToppingImage {
    val url = recentImageUrl
        ?: return YGToppingImage.Template(TOPPING_TEMPLATES[groupId.value.mod(TOPPING_TEMPLATES.size)])

    val border = drawableBorder()?.let { (color, width) ->
        YGToppingBorder(color = color, width = width.dp, outline = outlines[url])
    }

    return YGToppingImage.Remote(url = url, border = border)
}

/** 거리판은 비싸서 테두리를 실제로 그릴 토핑만 뜬다 */
internal fun List<MyParfaitGroupVO>.borderedImageUrls(): List<String> = mapNotNull { group ->
    group.recentImageUrl?.takeIf { group.drawableBorder() != null }
}

private fun MyParfaitGroupVO.drawableBorder(): Pair<Color, Double>? {
    val solid = recentImageBorder as? ToppingBorder.Solid ?: return null
    val color = solid.color.toColorOrNull() ?: return null
    return color to solid.width
}
