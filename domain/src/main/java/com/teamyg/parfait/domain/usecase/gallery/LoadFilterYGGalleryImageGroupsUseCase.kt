package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 03시 하루 창의 사진만 불러온다. 지금 화면은 [LoadGalleryImageGroupsUseCase] 를 쓰고 이것은 쓰지 않는다 —
 * 내부 배포 반응을 본 뒤 제한 해제가 확정되면 지운다. 되돌릴 때는 ViewModel 이 부르는 UseCase 만 바꾼다.
 */
class LoadFilterYGGalleryImageGroupsUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "LoadFilterYGGalleryImageGroupsUseCase::init" }
    }

    suspend operator fun invoke(): List<GalleryImageGroup> {
        val hashMap: LinkedHashMap<LocalDate, MutableList<String>> = galleryRepository.loadFilterYGGalleryImages()

        useCaseLogger.d { "LoadFilterYGGalleryImageGroupsUseCase - hashMap.size: ${hashMap.size}" }

        return hashMap.map { (date, uris) ->
            GalleryImageGroup(
                date = date,
                images = uris.toList(),
            )
        }
    }
}
