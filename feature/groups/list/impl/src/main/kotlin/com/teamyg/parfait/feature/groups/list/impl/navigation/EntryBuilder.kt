package com.teamyg.parfait.feature.groups.list.impl.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.GroupListRoute

fun EntryProviderScope<NavKey>.featureGroupListEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupList> {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = YGAtomicColors.Gray.White),
        ) {
            Image(
                painter = painterResource(R.drawable.group_list_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            GroupListRoute(
                navigator = navigator,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
