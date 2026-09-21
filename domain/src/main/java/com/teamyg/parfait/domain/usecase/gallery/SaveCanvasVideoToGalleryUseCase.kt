package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

/**
 * 인코딩이 끝난 캔버스 타임랩스 mp4 를 기기 갤러리에 저장한다.
 *
 * 녹화와 인코딩은 화면 계층 책임이라(컴포지션 캡처다) 여기서는 완성된 파일만 받는다 —
 * [SaveCanvasToGalleryUseCase] 가 이미 만들어진 비트맵만 받는 것과 같은 경계다.
 */
class SaveCanvasVideoToGalleryUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "SaveCanvasVideoToGalleryUseCase::init" }
    }

    suspend operator fun invoke(
        videoFilePath: String,
        displayName: String,
    ): Result<Unit> = galleryRepository
        .saveVideoToGallery(videoFilePath = videoFilePath, displayName = displayName)
        .onFailure { throwable ->
            useCaseLogger.e(throwable) { "SaveCanvasVideoToGalleryUseCase - 갤러리 저장 실패" }
        }
}
