package com.teamyg.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped

@ActivityRetainedScoped
class Navigator(initialNavigationKey: NavKey) {

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(initialNavigationKey)

    fun goTo(destination: NavKey){
        backStack.add(destination)
    }
    fun onBack() {
        backStack.removeLastOrNull()
    }
}
