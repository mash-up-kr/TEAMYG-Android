package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

class LoadFilterYGGalleryImageGroupsUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "LoadFilterYGGalleryImageGroupsUseCase::init" }
    }

    suspend operator fun invoke(): List<GalleryImageGroup> {
        val hashMap: LinkedHashMap<String, MutableList<String>> = galleryRepository.loadFilterYGGalleryImages()

        useCaseLogger.d { "LoadFilterYGGalleryImageGroupsUseCase - hashMap.size: ${hashMap.size}" }

        return hashMap.map { (date, uris) ->
            GalleryImageGroup(
                date = date,
                images = uris.toList(),
            )
        }
    }
}
