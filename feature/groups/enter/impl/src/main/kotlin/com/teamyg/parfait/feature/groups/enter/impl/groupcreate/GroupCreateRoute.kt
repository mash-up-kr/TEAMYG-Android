package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.navigation.Navigator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupCreateRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupCreateViewModel = hiltViewModel(),
) {
    GroupCreateScreen(
    )
}
