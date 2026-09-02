package com.teamyg.parfait.domain.usecase.image

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import javax.inject.Inject

class DecodeImageUseCase
@Inject
constructor(
    private val repository: ImageSegmentationRepository,
) {
    init {
        useCaseLogger.i { "DecodeImageUseCase::init" }
    }

    /**
     * 만료된 URI·깨진 파일은 드문 사고가 아니라 정상 경로다. 호출부마다 감싸면 취소를 실패로
     * 접는 실수가 호출부 수만큼 반복되므로 여기서 한 번만 감싼다.
     */
    suspend operator fun invoke(uri: String): Result<BitmapWrapper> = runSuspendCatching {
        repository.decodeImage(uri)
    }
}
