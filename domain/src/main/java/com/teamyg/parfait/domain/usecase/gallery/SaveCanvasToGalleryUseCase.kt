package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

/**
 * 화면에서 캡처해 둔 캔버스 비트맵을 기기 갤러리에 저장한다.
 *
 * 캡처 자체(Compose GraphicsLayer)는 화면 계층 책임이라, 여기서는 이미 만들어진 비트맵만
 * 받아 갤러리 저장만 담당한다.
 */
class SaveCanvasToGalleryUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "SaveCanvasToGalleryUseCase::init" }
    }

    suspend operator fun invoke(
        capturedBitmap: BitmapWrapper,
        displayName: String,
    ): Result<Unit> = galleryRepository
        .saveImageToGallery(bitmap = capturedBitmap, displayName = displayName)
        .onFailure { throwable ->
            useCaseLogger.e(throwable) { "SaveCanvasToGalleryUseCase - 갤러리 저장 실패" }
        }
}
