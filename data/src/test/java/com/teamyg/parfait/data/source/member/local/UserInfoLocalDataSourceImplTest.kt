package com.teamyg.parfait.data.source.member.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.security.CryptoManager
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class UserInfoLocalDataSourceImplTest {
    /** Keystore 를 요구하지 않는 통과 암호화 — 저장 왕복만 검증하면 되므로 변환하지 않는다 */
    private val passthroughCrypto: CryptoManager = mockk {
        every { encrypt(any()) } answers { firstArg() }
        every { decrypt(any()) } answers { firstArg() }
    }

    /**
     * 실제로 값을 바꾸는(암호화하는 척하는) 페이크. `save()` 가 `cryptoManager.encrypt(...)`
     * 를 건너뛰고 평문을 그대로 저장해도 [passthroughCrypto] 로는 잡히지 않는다 —
     * encrypt/decrypt 가 항등 함수라 평문 저장과 구분이 안 된다. 이 페이크로 DataStore
     * 원문이 평문 JSON 과 **다르다**는 것 자체를 그물로 건다.
     */
    private val prefixingCrypto: CryptoManager = mockk {
        every { encrypt(any()) } answers { ENCRYPTED_PREFIX + firstArg<String>() }
        every { decrypt(any()) } answers { firstArg<String>().removePrefix(ENCRYPTED_PREFIX) }
    }

    private fun dataSource(
        dataStore: DataStore<Preferences>,
        crypto: CryptoManager = passthroughCrypto,
    ) = UserInfoLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json { ignoreUnknownKeys = true },
        cryptoManager = crypto,
    )

    @Test
    fun save_thenRead_roundTripsEveryField() = runTest {
        // Given 값 클래스와 enum 을 품은 계정 정보
        val dataStore = FakePreferencesDataStore()
        val source = dataSource(dataStore)
        val account = MyAccountVO(
            memberId = MemberId(7L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )

        // When 저장하고 다시 읽는다
        source.save(account)

        // Then 필드가 하나도 뒤바뀌지 않는다 — memberId 와 nickname 은 타입이 달라도
        // 매퍼가 뒤집히면 컴파일러가 막지 못한다
        assertEquals(account, source.myAccount.first())
    }

    @Test
    fun save_storesEncryptedValue_notPlainJson() = runTest {
        // Given 실제로 값을 바꾸는 암호화 페이크와, 평문으로 새면 안 되는 계정 정보
        val dataStore = FakePreferencesDataStore()
        val source = dataSource(dataStore, prefixingCrypto)
        val account = MyAccountVO(
            memberId = MemberId(7L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )
        val plainJson = """{"memberId":7,"provider":"KAKAO","nickname":"모카"}"""

        // When 저장한다
        source.save(account)

        // Then DataStore 에 쓰인 원문은 평문 JSON 이 아니다 — save() 가 encrypt() 호출을
        // 건너뛰고 평문을 그대로 심어도 통과하던 그물 구멍을 막는다
        val stored = dataStore.data.first()[UserInfoLocalDataSourceImpl.USER_INFO_KEY]
        assertNotEquals(plainJson, stored)
        assertEquals(ENCRYPTED_PREFIX + plainJson, stored)
    }

    @Test
    fun myAccount_nothingSaved_isNull() = runTest {
        // Given 저장분이 없는 상태
        val source = dataSource(FakePreferencesDataStore())

        // When 읽는다
        // Then 빈 값이 아니라 null 이다 — 화면이 "아직 없음"을 로딩으로 구분해야 한다
        assertNull(source.myAccount.first())
    }

    @Test
    fun myAccount_decryptFails_isNullAndDiscardsStoredValue() = runTest {
        // Given 저장 후 키가 바뀌어 복호화가 실패하는 상태
        val dataStore = FakePreferencesDataStore()
        val failingCrypto: CryptoManager = mockk {
            every { encrypt(any()) } answers { firstArg() }
            every { decrypt(any()) } throws IllegalStateException("키 유실")
        }
        dataSource(dataStore).save(
            MyAccountVO(MemberId(7L), LoginProvider.KAKAO, GlobalNickname("모카")),
        )

        // When 복호화가 실패하는 저장소로 읽는다
        val read = dataSource(dataStore, failingCrypto).myAccount.first()

        // Then null 이고 저장분은 버려진다 — 영구히 못 읽는 값을 들고 있지 않는다
        assertNull(read)
        assertNull(dataSource(dataStore).myAccount.first())
    }

    @Test
    fun myAccount_storedProviderUnknownToApp_fallsBackToUnknown() = runTest {
        // Given 앱이 모르는 provider 문자열이 저장돼 있다(서버가 provider 를 늘린 뒤)
        val dataStore = FakePreferencesDataStore()
        dataStore.putRaw(
            key = UserInfoLocalDataSourceImpl.USER_INFO_KEY_NAME,
            value = """{"memberId":7,"provider":"GOOGLE","nickname":"모카"}""",
        )

        // When 읽는다
        val read = dataSource(dataStore).myAccount.first()

        // Then 크래시하지 않고 UNKNOWN 으로 떨어진다
        assertEquals(LoginProvider.UNKNOWN, read?.provider)
    }

    private companion object {
        const val ENCRYPTED_PREFIX = "enc:"
    }
}

/**
 * 메모리 [MutableStateFlow] 로 구현한 [DataStore]. Keystore·디스크 IO 없이 `edit`/`data`
 * 왕복만 검증하면 되는 테스트 전용 대역이다. `putRaw` 는 테스트가 저장 형태를 직접
 * 심어(암호화되지 않은 원문 그대로) provider 알 수 없음 같은 케이스를 세팅하는 헬퍼다.
 */
private class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }

    fun putRaw(
        key: String,
        value: String,
    ) {
        val mutable: MutablePreferences = state.value.toMutablePreferences()
        mutable[stringPreferencesKey(key)] = value
        state.value = mutable
    }
}
