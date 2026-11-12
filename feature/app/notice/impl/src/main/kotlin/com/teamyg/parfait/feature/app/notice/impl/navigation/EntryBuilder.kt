package com.teamyg.parfait.feature.app.notice.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeDetail
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeList
import com.teamyg.parfait.feature.app.notice.impl.route.NoticeDetailRoute
import com.teamyg.parfait.feature.app.notice.impl.route.NoticeListRoute

fun EntryProviderScope<NavKey>.featureAppNoticeEntryBuilder(navigator: Navigator) {
    entry<NavKeyNoticeList> {
        NoticeListRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeyNoticeDetail> { key ->
        NoticeDetailRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
