package com.teamyg.parfait.analytics

import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.analytics.AnalyticsScreen
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class UserIdRecordingLogger : AnalyticsLogger {
    val userIds = mutableListOf<String?>()

    override fun setCollectionEnabled(enabled: Boolean) = Unit

    override fun setUserProperty(
        name: String,
        value: String,
    ) = Unit

    override fun setUserId(id: String?) {
        userIds += id
    }

    override fun logScreenView(screen: AnalyticsScreen) = Unit

    override fun logEvent(event: AnalyticsEvent) = Unit
}

private class FakeMemberRepository(
    override val myAccount: MutableStateFlow<MyAccountVO?>,
) : MemberRepository {
    override suspend fun refreshMyAccount() = error("not used")

    override suspend fun changeGlobalNickname(nickname: GlobalNickname) = error("not used")

    override suspend fun clearMyAccount() = error("not used")

    override suspend fun withdraw() = error("not used")
}

private fun account(
    memberId: Long,
    nickname: String = "parfait",
) = MyAccountVO(
    memberId = MemberId(memberId),
    provider = LoginProvider.KAKAO,
    nickname = GlobalNickname(nickname),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsUserIdSyncTest {
    private val myAccount = MutableStateFlow<MyAccountVO?>(null)
    private val logger = UserIdRecordingLogger()

    private fun TestScope.startSync() {
        AnalyticsUserIdSync(
            getMyAccountFlow = GetMyAccountFlowUseCase(FakeMemberRepository(myAccount)),
            analyticsLogger = logger,
            scope = backgroundScope,
        ).start()
        runCurrent()
    }

    @Test
    fun start_loginThenLogout_setsMemberIdThenClears() = runTest {
        // Given 로그인 전
        startSync()

        // When 로그인으로 계정이 채워졌다가 로그아웃으로 지워진다
        myAccount.value = account(memberId = 42)
        runCurrent()
        myAccount.value = null
        runCurrent()

        // Then memberId 를 넣었다가 지운다
        assertEquals(listOf(null, "42", null), logger.userIds)
    }

    @Test
    fun start_nicknameChangedOnly_doesNotSetAgain() = runTest {
        // Given 로그인한 상태
        myAccount.value = account(memberId = 42)
        startSync()

        // When 닉네임만 바뀐다
        myAccount.value = account(memberId = 42, nickname = "changed")
        runCurrent()

        // Then 같은 회원이라 다시 넣지 않는다
        assertEquals(listOf<String?>("42"), logger.userIds)
    }

    @Test
    fun start_accountSwitched_setsNewMemberId() = runTest {
        // Given 회원 42 로 로그인한 상태
        myAccount.value = account(memberId = 42)
        startSync()

        // When 로그아웃 없이 다른 회원 계정으로 덮인다
        myAccount.value = account(memberId = 7)
        runCurrent()

        // Then 새 회원으로 바꾼다
        assertEquals(listOf<String?>("42", "7"), logger.userIds)
    }
}
