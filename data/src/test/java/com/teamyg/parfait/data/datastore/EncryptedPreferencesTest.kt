package com.teamyg.parfait.data.datastore

import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.security.CryptoManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptedPreferencesTest {
    /** 실제로 값을 바꾸는 암호화 페이크 — 항등 함수면 평문 저장과 구분이 안 된다 */
    private val prefixingCrypto: CryptoManager = mockk {
        every { encrypt(any()) } answers { ENCRYPTED_PREFIX + firstArg<String>() }
        every { decrypt(any()) } answers { firstArg<String>().removePrefix(ENCRYPTED_PREFIX) }
    }

    @Test
    fun write_thenRead_roundTripsThroughCipherText() = runTest {
        // Given 빈 저장소
        val dataStore = FakePreferencesDataStore()
        val preferences = EncryptedPreferences(dataStore, prefixingCrypto)

        // When 쓰고 읽는다
        preferences.write(KEY, "모카")

        // Then 값은 그대로 돌아오고, 저장된 원문은 평문이 아니다
        assertEquals("모카", preferences.read(KEY) { it })
        assertNotEquals("모카", dataStore.data.first()[KEY])
    }

    @Test
    fun write_multipleValues_goesOutAsSingleEdit() = runTest {
        // Given 한 짝으로 저장돼야 하는 값 둘(예: access·refresh 토큰)
        val dataStore = FakePreferencesDataStore()
        val preferences = EncryptedPreferences(dataStore, prefixingCrypto)

        // When 한 번에 쓴다
        preferences.write(mapOf(Pair(KEY, "값1"), Pair(OTHER_KEY, "값2")))

        // Then edit 은 한 번만 나간다 — 나눠 쓰면 구독자가 반쪽만 저장된 상태를 본다
        assertEquals(1, dataStore.updateCount)
        assertEquals("값1", preferences.read(KEY) { it })
        assertEquals("값2", preferences.read(OTHER_KEY) { it })
    }

    @Test
    fun observe_unrelatedKeyChanges_doesNotDecryptAgain() = runTest {
        // Given 이 저장소를 여러 소비자가 공유한다 — 관심 없는 키가 따로 바뀐다
        val dataStore = FakePreferencesDataStore()
        val preferences = EncryptedPreferences(dataStore, prefixingCrypto)
        preferences.write(KEY, "모카")

        // When 구독하는 도중 무관한 키가 바뀐다
        val seen = mutableListOf<String?>()
        val values = preferences.observe(KEY) { it }
        seen += values.first()
        preferences.write(OTHER_KEY, "무관한 값")
        seen += values.first()

        // Then 값은 그대로이고 복호화가 재실행되지 않는다 — Keystore 를 다시 두드리지 않고,
        // 편집 중인 화면이 무관한 쓰기로 값을 되돌려 받지도 않는다
        assertTrue(seen.all { it == "모카" })
        verify(exactly = 2) { prefixingCrypto.decrypt(any()) } // first() 구독 2회분뿐
    }

    @Test
    fun read_decodeFails_discardsStoredValueAndReturnsNull() = runTest {
        // Given 저장분은 있으나 해석에 실패한다(키 회전·저장 형태 손상)
        val dataStore = FakePreferencesDataStore()
        val preferences = EncryptedPreferences(dataStore, prefixingCrypto)
        preferences.write(KEY, "모카")

        // When 해석이 던지는 decode 로 읽는다
        val read = preferences.read(KEY) { error("해석 불가") }

        // Then null 이고 저장분은 버려진다 — 영구히 못 읽는 값을 남기지 않는다
        assertNull(read)
        assertNull(dataStore.data.first()[KEY])
    }

    @Test
    fun read_decodeFails_discardScopeIsCallerDecision() = runTest {
        // Given 두 값이 한 짝으로 저장돼 있다(하나가 깨지면 다른 하나도 못 읽는 관계)
        val dataStore = FakePreferencesDataStore()
        val preferences = EncryptedPreferences(dataStore, prefixingCrypto)
        preferences.write(mapOf(Pair(KEY, "값1"), Pair(OTHER_KEY, "값2")))

        // When 호출부가 "짝 전체를 버린다"를 넘긴다
        val read = preferences.read(
            key = KEY,
            onDecodeFailure = { preferences.remove(KEY, OTHER_KEY) },
        ) { error("해석 불가") }

        // Then 읽던 키만이 아니라 짝이 함께 버려진다 — 범위를 프록시가 정하지 않는다
        assertNull(read)
        assertNull(dataStore.data.first()[KEY])
        assertNull(dataStore.data.first()[OTHER_KEY])
    }

    @Test
    fun read_nothingStored_isNullWithoutTouchingCrypto() = runTest {
        // Given 저장분이 없다
        val preferences = EncryptedPreferences(FakePreferencesDataStore(), prefixingCrypto)

        // When 읽는다
        // Then null 이고 복호화를 시도하지 않는다
        assertNull(preferences.read(KEY) { it })
        verify(exactly = 0) { prefixingCrypto.decrypt(any()) }
    }

    private companion object {
        const val ENCRYPTED_PREFIX = "enc:"
        val KEY = stringPreferencesKey("test_key")
        val OTHER_KEY = stringPreferencesKey("other_key")
    }
}
