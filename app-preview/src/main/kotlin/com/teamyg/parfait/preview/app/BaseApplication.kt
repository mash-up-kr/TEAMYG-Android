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
    // 프리뷰 앱도 본 앱과 같은 로더를 써야 이미지가 뜨는 모습이 갈리지 않는다
    override fun newImageLoader(context: PlatformContext): ImageLoader = newParfaitImageLoader(context)
}
