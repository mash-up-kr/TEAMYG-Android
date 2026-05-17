package com.teamyg.grouphome.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

@Composable
fun GroupHomeRoute(
    navigator: Navigator,
    key: NavKeyGroupHome,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current
    var returnText by remember { mutableStateOf("") }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = "group home // groupId: ${key.groupId}",
        )
        TextField(
            value = returnText,
            onValueChange = { returnText = it },
        )
        Button(
            onClick = {
                resultEventBus.sendResult(returnText)
                navigator.onBack()
            },
        ) {
            Text("return with value")
        }
    }
}
