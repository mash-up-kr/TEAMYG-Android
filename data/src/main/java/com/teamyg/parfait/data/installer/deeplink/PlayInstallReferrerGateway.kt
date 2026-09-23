package com.teamyg.parfait.data.installer.deeplink

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.teamyg.parfait.data.utils.repositoryLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class PlayInstallReferrerGateway
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : AppLinkReferrerGateway {
    override suspend fun getReferrer(): String? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()

        // endConnection 을 두 경로(정상 응답, 코루틴 취소) 모두에서 호출해야 서비스 바인딩이 샌다.
        continuation.invokeOnCancellation { client.endConnection() }

        client.startConnection(
            object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    val referrer = if (responseCode == InstallReferrerResponse.OK) {
                        runCatching { client.installReferrer.installReferrer }
                            .onFailure { repositoryLogger.w(it) { "[APP-LINK] Install Referrer 조회가 실패했다" } }
                            .getOrNull()
                    } else {
                        repositoryLogger.i { "[APP-LINK] Install Referrer 응답 코드 $responseCode" }
                        null
                    }

                    client.endConnection()
                    if (continuation.isActive) continuation.resume(referrer)
                }

                override fun onInstallReferrerServiceDisconnected() {
                    // 여기선 재시도하지 않는다 — 다음 콜드 스타트에서 다시 시도된다.
                }
            },
        )
    }
}
