package com.teamyg.gallery.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.gallery.impl.screen.SystemGalleryPickerScreen
import com.teamyg.navigation.Navigator

@Composable
internal fun SystemGalleryPickerRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    SystemGalleryPickerScreen(
        modifier = modifier,
        onClickConfirm = {
            resultEventBus.sendResult("content://placeholder/gallery/picked.jpg")
            navigator.onBack()
        },
    )
}
