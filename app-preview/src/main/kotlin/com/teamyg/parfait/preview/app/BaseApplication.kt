package com.teamyg.parfait.preview.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.teamyg.parfait.core.designsystem.image.newParfaitImageLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader = newParfaitImageLoader(context)
}
