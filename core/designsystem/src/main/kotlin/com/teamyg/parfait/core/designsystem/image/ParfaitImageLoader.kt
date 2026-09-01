package com.teamyg.parfait.core.designsystem.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade

/**
 * 앱 전체가 공유하는 이미지 로더. 원격 이미지가 투명한 자리에서 완성본으로 튀지 않고
 * 페이드로 들어오게 한다.
 *
 * Application 마다 따로 만들면 설정이 갈리므로 만드는 자리는 여기 하나로 둔다.
 */
fun newParfaitImageLoader(context: PlatformContext): ImageLoader = ImageLoader
    .Builder(context)
    .crossfade(true)
    .build()
