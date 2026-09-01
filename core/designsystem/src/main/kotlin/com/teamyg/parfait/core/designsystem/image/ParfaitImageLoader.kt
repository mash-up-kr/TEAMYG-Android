package com.teamyg.parfait.core.designsystem.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.crossfade

/**
 * 앱 전체가 공유하는 이미지 로더. 원격 이미지가 투명한 자리에서 완성본으로 튀지 않고
 * 페이드로 들어오게 한다.
 *
 * 메모리 캐시에서 나온 이미지는 Coil 이 페이드를 스스로 건너뛴다 — 이미 받아 둔 그림까지
 * 매번 흐려졌다 나타나지는 않는다.
 *
 * 이 함수를 `Application` 이 `SingletonImageLoader.Factory` 로 물어 준다. `app` 과
 * `app-preview` 가 각자 Application 을 갖고 있어, 설정이 갈리지 않도록 만드는 자리는
 * 여기 하나로 둔다.
 */
fun newParfaitImageLoader(context: PlatformContext): ImageLoader = ImageLoader
    .Builder(context)
    .crossfade(true)
    .build()
