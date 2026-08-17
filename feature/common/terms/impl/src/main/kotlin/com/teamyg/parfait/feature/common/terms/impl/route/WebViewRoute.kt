package com.teamyg.parfait.feature.common.terms.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.common.terms.impl.screen.WebViewScreen

/**
 * ViewModel 이 없는 이유: 이 화면의 상태는 목적지에 실려 온 [title]·[url] 이 전부고, 부를 API 도
 * 없다. 뒤로 가기만 남는데 그것을 상태로 감쌀 이유가 없다.
 */
@Composable
internal fun WebViewRoute(
    title: String,
    url: String,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    WebViewScreen(
        title = title,
        url = url,
        onClickBack = navigator::onBack,
        modifier = modifier,
    )
}
