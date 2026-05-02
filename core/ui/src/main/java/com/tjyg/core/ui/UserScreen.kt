package com.tjyg.core.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()){
    val state = viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit){
        viewModel.effect.collect { effect ->
            when(effect){
                is UserSideEffect.showToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if(state.value.isLoading){
        Text(
            modifier = Modifier.height(100.dp),
            text = "로딩중입니다~"
        )
    }else{
        Column{
            Spacer(modifier = Modifier.height(100.dp))
            Button(
                onClick = {viewModel.processIntent(UserIntent.LoadUser)}){
                Text(
                    text = "불러오기",
                )
            }
            Text(
                modifier = Modifier.height(100.dp),
                text = state.value.userName
            )
        }
    }
}
