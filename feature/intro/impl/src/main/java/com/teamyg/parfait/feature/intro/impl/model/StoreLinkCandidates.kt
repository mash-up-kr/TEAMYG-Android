package com.teamyg.parfait.feature.intro.impl.model

import java.net.URI

private const val PLAY_HOST = "play.google.com"

/**
 * 업데이트 버튼이 열기를 시도할 순서대로 URI 후보를 돌려준다.
 *
 * 서버가 준 [storeUrl]은 `market` 이거나 `https` 일 때만 맨 앞에 둔다. Play 가 아닌 `https` 는 공지·안내
 * 페이지로 보내는 경우다. `http`·`intent` 같은 다른 스킴은 다른 앱을 띄우거나 평문으로 나갈 수 있어 버린다.
 */
internal fun storeLinkCandidates(
    storeUrl: String?,
    packageName: String,
): List<String> {
    val market = "market://details?id=$packageName"
    val web = "https://$PLAY_HOST/store/apps/details?id=$packageName"
    val server = storeUrl?.takeIf(::isAllowedServerUrl)
    return listOfNotNull(server, market, web).distinct()
}

private fun isAllowedServerUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return uri.scheme == "market" || (uri.scheme == "https" && !uri.host.isNullOrBlank())
}
