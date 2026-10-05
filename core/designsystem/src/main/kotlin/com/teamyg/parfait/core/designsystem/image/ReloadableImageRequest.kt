package com.teamyg.parfait.core.designsystem.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

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
