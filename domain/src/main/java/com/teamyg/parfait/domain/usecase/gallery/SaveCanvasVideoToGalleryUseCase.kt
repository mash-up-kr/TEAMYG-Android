package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

/** 인코딩이 끝난 캔버스 타임랩스 mp4 를 기기 갤러리에 저장한다. 녹화·인코딩은 화면 계층 몫이다 */
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
