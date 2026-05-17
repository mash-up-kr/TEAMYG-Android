package com.teamyg.login.impl

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

@Composable
fun LoginRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    ResultEffect<String> { returnText ->
        Toast.makeText(context, returnText, Toast.LENGTH_LONG).show()
    }

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
