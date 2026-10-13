package com.teamyg.parfait.data.repository.deeplink

import com.teamyg.parfait.data.installer.deeplink.AppLinkReferrerGateway
import com.teamyg.parfait.data.installer.deeplink.ReferrerLookup
import com.teamyg.parfait.data.source.deeplink.local.AppLinkReferrerLocalDataSource
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import com.teamyg.parfait.domain.repository.deeplink.AppLinkReferrerRepository
import java.net.URLDecoder
import javax.inject.Inject

class AppLinkReferrerRepositoryImpl
@Inject
constructor(
    private val gateway: AppLinkReferrerGateway,
    private val localDataSource: AppLinkReferrerLocalDataSource,
) : AppLinkReferrerRepository {
    override suspend fun consumeDeepLinkOnce(): AppLinkDeepLink? {
        if (localDataSource.hasChecked()) return null

        // 일시 실패는 "확인함"으로 남기지 않는다 — 남기면 한 번의 실패로 복원 기회가 영구히 사라진다.
        return when (val lookup = gateway.getReferrer()) {
            is ReferrerLookup.Found -> {
                localDataSource.markChecked()
                val inviteCode = referrerQueryParameter(
                    lookup.referrer,
                    key = AppLinkDeepLink.OpenGroupInvite.INVITE_CODE_QUERY_KEY,
                )
                AppLinkDeepLink.OpenGroupInvite.parse(inviteCode)
            }

            ReferrerLookup.NotAvailable -> {
                localDataSource.markChecked()
                null
            }

            ReferrerLookup.TransientFailure -> null
        }
    }

    /**
     * referrer 는 `Uri` 가 아니라 쿼리스트링 조각(`inviteCode=ABC123&utm_source=...`)만 온다 —
     * [android.net.Uri] 가 기대하는 스킴·호스트가 없어 직접 나눠 읽는다.
     */
    private fun referrerQueryParameter(
        referrer: String,
        key: String,
    ): String? = referrer
        .split("&")
        .map { it.split("=", limit = 2) }
        .firstOrNull { it.getOrNull(0) == key }
        ?.getOrNull(1)
        ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
}
