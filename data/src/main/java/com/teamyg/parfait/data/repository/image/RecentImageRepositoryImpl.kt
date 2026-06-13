package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.core.util.model.DayWindow
import com.teamyg.parfait.data.source.image.local.RecentImageFileLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentImageRepositoryImpl
@Inject
constructor(
    private val recentImageLocalDataSource: RecentImageLocalDataSource,
    private val recentImageFileLocalDataSource: RecentImageFileLocalDataSource,
) : RecentImageRepository {
    override val recentCacheImages: Flow<List<String>>
        get() = flow {
            clearOutsideDayWindow()

            emitAll(
                recentImageLocalDataSource.values
                    .distinctUntilChanged(),
            )
        }

    /**
     * 데이 윈도우(당일 03:00 ~ 익일 02:59)를 벗어난 캐시 이미지를 메타데이터와 파일에서 모두 제거
     * 갤러리 조회와 동일한 윈도우 기준을 사용
     */
    private suspend fun clearOutsideDayWindow() {
        val window: DayWindow = DayWindow.current()
        val current: List<String> = recentImageLocalDataSource.values.first()

        val outdated: List<String> = current.filterNot { uri ->
            recentImageFileLocalDataSource.getLastModified(uri) in window
        }

        if (outdated.isEmpty()) {
            return
        }

        recentImageLocalDataSource.remove(outdated)
        outdated.forEach {
            recentImageFileLocalDataSource.delete(it)
        }
    }

    override suspend fun addRecentImage(uri: String) {
        val stableUri: String = runCatching { recentImageFileLocalDataSource.store(uri) }
            .getOrNull()
            ?: return

        val evicted: List<String> = recentImageLocalDataSource.addAndGetEvicted(stableUri)

        evicted.forEach {
            recentImageFileLocalDataSource.delete(it)
        }
    }
}
