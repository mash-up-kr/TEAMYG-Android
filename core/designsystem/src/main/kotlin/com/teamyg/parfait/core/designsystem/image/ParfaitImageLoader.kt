package com.teamyg.parfait.core.designsystem.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
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

/**
 * [reloadKey] 를 올리면 캐시를 건너뛰고 다시 받아 온다. 같은 url 로 다시 그리기만 하면
 * 실패한 요청이 그대로 재사용되고, 디스크에 앉은 깨진 바이트를 읽으면 몇 번을 눌러도
 * 같은 실패다.
 */
@Composable
fun rememberReloadableImageRequest(
    url: String?,
    reloadKey: Int,
): ImageRequest {
    val context = LocalPlatformContext.current

    return remember(url, reloadKey) {
        ImageRequest
            .Builder(context)
            .data(url)
            .apply {
                if (reloadKey > 0) {
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }.build()
    }
}
