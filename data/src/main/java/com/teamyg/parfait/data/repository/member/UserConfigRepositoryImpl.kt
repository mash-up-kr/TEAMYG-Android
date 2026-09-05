package com.teamyg.parfait.data.repository.member

import com.teamyg.parfait.data.source.member.local.UserConfigLocalDataSource
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO
import com.teamyg.parfait.domain.repository.member.UserConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserConfigRepositoryImpl @Inject constructor(
    private val localDataSource: UserConfigLocalDataSource,
) : UserConfigRepository {
    override val userConfig: Flow<UserConfigVO?> = localDataSource.userConfig

    /**
     * 저장분이 없는 최초 실행에서도 반드시 쓴다 — 설정이 이미 있을 때만 갱신하면 튜토리얼을
     * 끝냈다는 사실이 어디에도 남지 않아, 앱을 처음 켠 사람에게 매번 다시 뜬다.
     *
     * 목록에 더하는 형태라 먼저 끝낸 다른 튜토리얼의 기록을 덮지 않는다.
     */
    override suspend fun markTutorialSeen(tutorial: TutorialKind) {
        val current = userConfig.first() ?: UserConfigVO()
        localDataSource.save(current.copy(seenTutorials = current.seenTutorials + tutorial))
    }

    override suspend fun clearConfig() = localDataSource.clear()
}
