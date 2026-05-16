package com.teamyg.grouphome.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureGroupHomeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupHome> {
        GroupHomeRoute(navigator)
    }
}
