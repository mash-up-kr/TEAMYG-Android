package com.teamyg.parfait.feature.intro.impl.model

import java.net.URI

private const val PLAY_HOST = "play.google.com"

/**
 * 스토어 열기를 시도할 순서대로 URI 후보를 돌려준다.
 *
 * 서버가 준 [storeUrl]은 Play 스토어를 가리킬 때만 맨 앞에 둔다. 임의 호스트로 나가는 길을 막기 위해서다.
 */
internal fun storeLinkCandidates(
    storeUrl: String?,
    packageName: String,
): List<String> {
    val market = "market://details?id=$packageName"
    val web = "https://$PLAY_HOST/store/apps/details?id=$packageName"
    val server = storeUrl?.takeIf(::isPlayStoreUrl)
    return listOfNotNull(server, market, web).distinct()
}

private fun isPlayStoreUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme == "market" || (uri.scheme == "https" && uri.host == PLAY_HOST)
}
