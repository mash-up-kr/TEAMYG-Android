package com.teamyg.grouphome.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

@Composable
fun GroupHomeRoute(
    navigator: Navigator,
    key: NavKeyGroupHome,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "group home // groupId: ${key.groupId}",
        )
    }
}
