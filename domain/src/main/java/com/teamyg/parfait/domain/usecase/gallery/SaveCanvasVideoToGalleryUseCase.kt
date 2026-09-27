package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import java.io.File
import javax.inject.Inject

/**
 * 이미 인코딩된 캔버스 동영상 파일을 기기 갤러리에 저장한다.
 *
 * 인코딩 자체(media3 Transformer)는 이 유스케이스의 책임이 아니다 — 호출부가 이미 완성된
 * mp4 파일을 들고 있어야 한다. [SaveCanvasToGalleryUseCase]가 이미 만들어진 비트맵만
 * 받는 것과 같은 경계다.
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
        videoFile: File,
        displayName: String,
    ): Result<Unit> = galleryRepository
        .saveVideoToGallery(videoFile = videoFile, displayName = displayName)
        .onFailure { throwable ->
            useCaseLogger.e(throwable) { "SaveCanvasVideoToGalleryUseCase - 갤러리 동영상 저장 실패" }
        }
}
