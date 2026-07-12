package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationResult

interface ImageSegmentationRepository {
    suspend fun decodeImage(uri: String): BitmapWrapper

    suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<SegmentationResult>
}
