package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingRecreateResult
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import javax.inject.Inject

/**
 * 텍스트 내용을 고친다. 서버 PATCH 가 text 를 받지 않아 새로 쌓고 옛 것을 지운다
 * (`specs/2026-10-26-text-topping-api.md`).
 */
class RecreateTextToppingUseCase @Inject constructor(
    private val textToppingRepository: TextToppingRepository,
) {
    /**
     * 옛 것은 새 것이 쌓인 **뒤에만** 지운다 — 거꾸로 하면 쌓기가 실패했을 때 글이 통째로 사라진다.
     * 쌓기는 다시 시도하지 않는다.
     *
     * @param transform positionZ 까지 그대로 보낸다. 겹침 순서를 이어받으려면 부르는 쪽이 옛
     *   텍스트의 값을 넣는다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        oldParfaitTextId: ParfaitTextId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): TextToppingRecreateResult {
        val placed = textToppingRepository
            .place(groupId = groupId, parfaitId = parfaitId, text = text, transform = transform, style = style)
            .getOrElse { throwable -> return TextToppingRecreateResult.NotPlaced(throwable) }

        return textToppingRepository
            .delete(groupId = groupId, parfaitId = parfaitId, parfaitTextId = oldParfaitTextId)
            .fold(
                onSuccess = { TextToppingRecreateResult.Recreated(placed) },
                onFailure = { throwable ->
                    // 응답만 잃은 재시도나 다른 기기가 먼저 지운 경우다 — 옛 것은 이미 없다
                    val isAlreadyGone = throwable is AppError.Server &&
                        throwable.code == ServerErrorCode.ParfaitText.PARFAIT_TEXT_NOT_FOUND

                    if (isAlreadyGone) {
                        TextToppingRecreateResult.Recreated(placed)
                    } else {
                        TextToppingRecreateResult.OldNotDeleted(placed = placed, cause = throwable)
                    }
                },
            )
    }
}
