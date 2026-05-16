package com.teamyg.login.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.login.api.NavKeyLoginHome
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureLoginEntryBuilder(navigator: Navigator) {
    entry<NavKeyLoginHome> {
        LoginRoute(navigator)
    }
}
