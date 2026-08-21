package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

/**
 * 누끼 이미지를 올리고 그 결과를 캔버스에 배치한다.
 *
 * 두 단계의 순서는 서버 계약이 정한 도메인 규칙이지 화면 관심사가 아니라 여기서 조율한다
 * (`specs/2026-08-20-c106-topping-place-api.md`). 배치까지 마치지 못해도 앞 단계를 되돌리지
 * 않는 것이 같은 스펙의 결정이다.
 */
class AddToppingUseCase @Inject constructor(
    private val imageUploadRepository: ImageUploadRepository,
    private val toppingRepository: ToppingRepository,
) {
    /**
     * @param filePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
     * @param transform 화면 좌표가 아니라 정규화된 서버 좌표다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        filePath: String,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> {
        // 용도를 파라미터로 열지 않는다. 잘못 고르면 객체가 엉뚱한 S3 접두사에 앉는데
        // 배치는 그것을 검사하지 않아 아무 실패도 드러나지 않는다(api/image.md 키 규칙)
        val imageId = imageUploadRepository
            .upload(filePath = filePath, imageType = ImageType.NUKKI)
            .getOrElse { throwable -> return Result.failure(throwable) }

        return toppingRepository.place(
            groupId = groupId,
            parfaitId = parfaitId,
            imageId = imageId,
            transform = transform,
            border = border,
        )
    }
}
