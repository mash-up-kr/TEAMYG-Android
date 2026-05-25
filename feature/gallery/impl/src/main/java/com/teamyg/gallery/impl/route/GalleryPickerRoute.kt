package com.teamyg.gallery.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.gallery.impl.screen.GalleryPickerScreen
import com.teamyg.navigation.Navigator

@Composable
internal fun GalleryPickerRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current

    GalleryPickerScreen(
        modifier = modifier,
        onClickConfirm = {
            resultEventBus.sendResult("content://placeholder/gallery/picked.jpg")
            navigator.onBack()
        },
    )
}
