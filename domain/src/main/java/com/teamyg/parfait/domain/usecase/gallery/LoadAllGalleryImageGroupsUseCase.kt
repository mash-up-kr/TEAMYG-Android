package com.teamyg.parfait.domain.usecase.gallery

import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import javax.inject.Inject

class LoadAllGalleryImageGroupsUseCase
@Inject
constructor(
    private val galleryRepository: GalleryRepository,
) {
    init {
        useCaseLogger.i { "LoadAllGalleryImageGroupsUseCase::init" }
    }

    suspend operator fun invoke(): List<GalleryImageGroup> {
        val hashMap: LinkedHashMap<String, MutableList<String>> = galleryRepository.loadAllGalleryImages()

        useCaseLogger.d { "LoadAllGalleryImageGroupsUseCase - hashMap.size: ${hashMap.size}" }

        return hashMap.map { (date, uris) ->
            GalleryImageGroup(
                date = date,
                images = uris.toList(),
            )
        }
    }
}
