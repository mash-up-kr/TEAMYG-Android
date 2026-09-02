package com.teamyg.parfait.data.source.token.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.EncryptedPreferences
import com.teamyg.parfait.data.security.CryptoManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class EncryptedTokenStoreTest {
    private val dataStore: DataStore<Preferences> = mockk()
    private val cryptoManager: CryptoManager = mockk()

    // 프록시는 실물을 통과시킨다 — 암호화·복호화·손상분 폐기가 이 저장소의 계약이라
    // mock 으로 바꾸면 그 계약이 테스트에서 사라진다.
    private val tokenStore = EncryptedTokenStore(
        preferences = EncryptedPreferences(dataStore = dataStore, cryptoManager = cryptoManager),
    )

    private val accessTokenKey = stringPreferencesKey("access_token")

    @Test
    fun getAccessToken_stored_returnsDecryptedValue() = runTest {
        // Given 암호화된 access token 이 저장돼 있다
        every { dataStore.data } returns flowOf(preferencesOf(accessTokenKey to "cipher"))
        every { cryptoManager.decrypt("cipher") } returns "access-1"

        // When 읽는다
        val token = tokenStore.getAccessToken()

        // Then 복호화된 값이 나온다
        assertEquals("access-1", token)
    }

    @Test
    fun getAccessToken_absent_returnsNull() = runTest {
        // Given 저장된 것이 없다
        every { dataStore.data } returns flowOf(preferencesOf())

        // When 읽는다
        val token = tokenStore.getAccessToken()

        // Then null 이다
        assertNull(token)
    }

    @Test
    fun getAccessToken_decryptionFails_returnsNullToForceReLogin() = runTest {
        // Given 저장분은 있으나 복호화가 깨진다(키 회전·백업 복원 등)
        every { dataStore.data } returns flowOf(preferencesOf(accessTokenKey to "cipher"))
        every { cryptoManager.decrypt("cipher") } throws IllegalStateException("bad key")

        // When 읽는다
        val token = tokenStore.getAccessToken()

        // Then 재로그인을 유도하려고 null 을 돌려준다
        assertNull(token)
    }

    @Test
    fun getAccessToken_readFails_returnsNull() = runTest {
        // Given 저장소 읽기 자체가 실패한다
        every { dataStore.data } returns flow { throw IOException("disk") }

        // When 읽는다
        val token = tokenStore.getAccessToken()

        // Then null 이다
        assertNull(token)
    }

    @Test
    fun getAccessToken_cancelledWhileReading_rethrowsInsteadOfReportingNoToken() = runTest {
        // Given 저장소를 기다리는 동안 코루틴이 취소된다
        every { dataStore.data } returns flow { throw CancellationException("cancelled") }

        // When·Then 취소를 "토큰 없음"으로 바꾸지 않고 그대로 재던진다.
        // stdlib `runCatching` 으로 감싸면 여기서 null 이 나오고, 호출부는 그것을
        // 로그아웃 상태로 읽는다 — `runSuspendCatching` 이 막는 회귀다.
        assertFailsWith<CancellationException> { tokenStore.getAccessToken() }
    }
}
