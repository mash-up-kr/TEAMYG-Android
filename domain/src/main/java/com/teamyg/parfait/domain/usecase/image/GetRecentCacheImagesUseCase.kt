package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.domain.model.DayWindow
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetRecentCacheImagesUseCase
@Inject
constructor(
    private val recentImageRepository: RecentImageRepository,
) {
    operator fun invoke(): Flow<List<String>> = recentImageRepository.recentCacheImages
        .onStart { clearOutsideDayWindow() }
        .distinctUntilChanged()

    /**
     * 데이 윈도우(당일 03:00 ~ 익일 02:59)를 벗어난 캐시 이미지를 메타데이터와 파일에서 모두 제거
     * 갤러리 조회와 동일한 윈도우 기준을 사용
     */
    private suspend fun clearOutsideDayWindow() {
        val window: DayWindow = DayWindow.current()
        val current: List<String> = recentImageRepository.recentCacheImages.first()

        val outdated: List<String> = current.filterNot { uri ->
            val lastModified = recentImageRepository.getLastModifiedCacheFile(uri) ?: 0L

            lastModified in window
        }

        if (outdated.isEmpty()) {
            return
        }

        recentImageRepository.removeCacheFileName(outdated)
        outdated.forEach {
            recentImageRepository.deleteRecentImageInInternalStorage(it)
        }
    }
}
