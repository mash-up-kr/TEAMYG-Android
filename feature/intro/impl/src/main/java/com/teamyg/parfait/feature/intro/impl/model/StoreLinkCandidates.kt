package com.teamyg.parfait.feature.intro.impl.model

import com.teamyg.parfait.core.util.jvm.extension.toJavaUriOrNull

/** 패키지를 지정하지 않으면 시스템 선택창이 원스토어·갤럭시 스토어 같은 Play 밖 스토어를 함께 보여준다. */
internal const val PLAY_STORE_PACKAGE = "com.android.vending"

private const val PLAY_HOST = "play.google.com"
private const val SCHEME_MARKET = "market"
private const val SCHEME_HTTPS = "https"

/**
 * 서버가 준 [storeUrl]은 `market`·`https`일 때만 맨 앞에 둔다. Play 가 아닌 `https`는 공지·안내 페이지다.
 * `http`·`intent` 같은 다른 스킴은 다른 앱을 띄우거나 평문으로 나갈 수 있어 버린다.
 */
internal fun storeLinkCandidates(
    storeUrl: String?,
    packageName: String,
): List<UpdateLink> {
    val market = "$SCHEME_MARKET://details?id=$packageName"
    val web = "$SCHEME_HTTPS://$PLAY_HOST/store/apps/details?id=$packageName"
    val server = storeUrl?.takeIf(::isAllowedServerUrl)
    return listOfNotNull(server, market, web)
        .distinct()
        .map { UpdateLink(uri = it, opensInBrowser = it.toJavaUriOrNull()?.scheme == SCHEME_HTTPS) }
}

private fun isAllowedServerUrl(url: String): Boolean {
    val uri = url.toJavaUriOrNull() ?: return false
    return uri.scheme == SCHEME_MARKET || (uri.scheme == SCHEME_HTTPS && !uri.host.isNullOrBlank())
}
