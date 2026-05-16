package com.teamyg.login.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

@Composable
fun LoginRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "login",
            modifier = Modifier.clickable {
                navigator.goTo(NavKeyGroupHome(groupId = 1231))
            }
        )
    }
}
