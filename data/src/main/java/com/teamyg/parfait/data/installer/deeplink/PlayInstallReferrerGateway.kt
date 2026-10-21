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
    override suspend fun getReferrer(): ReferrerLookup = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()

        // endConnection 을 두 경로(정상 응답, 코루틴 취소) 모두에서 호출해야 서비스 바인딩이 샌다.
        continuation.invokeOnCancellation { client.endConnection() }

        fun finish(lookup: ReferrerLookup) {
            client.endConnection()
            if (continuation.isActive) continuation.resume(lookup)
        }

        // 일부 기기는 startConnection 자체가 SecurityException 등을 던진다.
        runCatching {
            client.startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        finish(lookupFor(client, responseCode))
                    }

                    // 이 콜백으로 끝나면 setup 콜백은 오지 않아 resume 하지 않으면 영구 대기한다.
                    override fun onInstallReferrerServiceDisconnected() {
                        repositoryLogger.i { "[APP-LINK] Install Referrer 서비스 연결이 끊겼다" }
                        finish(ReferrerLookup.TransientFailure)
                    }
                },
            )
        }.onFailure {
            repositoryLogger.w(it) { "[APP-LINK] Install Referrer 연결을 시작하지 못했다" }
            finish(ReferrerLookup.TransientFailure)
        }
    }

    private fun lookupFor(
        client: InstallReferrerClient,
        responseCode: Int,
    ): ReferrerLookup = when (responseCode) {
        InstallReferrerResponse.OK -> runCatching { client.installReferrer.installReferrer }
            .onFailure { repositoryLogger.w(it) { "[APP-LINK] Install Referrer 조회가 실패했다" } }
            .getOrNull()
            ?.let(ReferrerLookup::Found)
            ?: ReferrerLookup.TransientFailure

        InstallReferrerResponse.SERVICE_UNAVAILABLE,
        InstallReferrerResponse.SERVICE_DISCONNECTED,
        -> ReferrerLookup.TransientFailure

        else -> {
            repositoryLogger.i { "[APP-LINK] Install Referrer 응답 코드 $responseCode" }
            ReferrerLookup.NotAvailable
        }
    }
}
