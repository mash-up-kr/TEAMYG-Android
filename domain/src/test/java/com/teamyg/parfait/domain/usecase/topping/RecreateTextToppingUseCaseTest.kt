package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingRecreateResult
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RecreateTextToppingUseCaseTest {
    private val textToppingRepository: TextToppingRepository = mockk()
    private val recreateTextTopping = RecreateTextToppingUseCase(textToppingRepository = textToppingRepository)

    private val transform = ToppingTransform(
        positionX = 0.4,
        positionY = 0.6,
        positionZ = 7,
        scale = 1.2,
        rotation = 30.0,
    )

    private val style = TextToppingStyle(color = "#000000", fontSize = 18.0, isFill = false, isStroke = false)

    private val placed = PlacedTextToppingVO(
        parfaitTextId = NEW_TEXT_ID,
        text = TEXT,
        transform = transform,
        style = style,
        placedBy = ToppingPlacerVO(groupMemberId = GroupMemberId(10L), nickname = GroupNickname("연경이")),
    )

    private fun givenPlaceSucceeds() {
        coEvery {
            textToppingRepository.place(GROUP_ID, PARFAIT_ID, TEXT, transform, style)
        } returns Result.success(placed)
    }

    private fun givenDeleteReturns(result: Result<Unit>) {
        coEvery { textToppingRepository.delete(GROUP_ID, PARFAIT_ID, OLD_TEXT_ID) } returns result
    }

    private suspend fun recreate() = recreateTextTopping(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        oldParfaitTextId = OLD_TEXT_ID,
        text = TEXT,
        transform = transform,
        style = style,
    )

    @Test
    fun invoke_bothStepsSucceed_placesThenDeletesTheOld() = runTest {
        // Given 쌓기와 지우기가 모두 성공한다
        givenPlaceSucceeds()
        givenDeleteReturns(Result.success(Unit))

        // When 다시 만든다
        val result = recreate()

        // Then 새 것을 쌓은 뒤에 옛 것을 지운다. 받은 transform 은 positionZ 까지 그대로 나간다
        assertEquals(TextToppingRecreateResult.Recreated(placed), result)
        coVerifyOrder {
            textToppingRepository.place(GROUP_ID, PARFAIT_ID, TEXT, transform, style)
            textToppingRepository.delete(GROUP_ID, PARFAIT_ID, OLD_TEXT_ID)
        }
    }

    @Test
    fun invoke_placeFails_doesNotDeleteTheOld() = runTest {
        // Given 쌓기가 실패한다
        val error = AppError.Network(cause = null)
        coEvery { textToppingRepository.place(any(), any(), any(), any(), any()) } returns Result.failure(error)

        // When 다시 만든다
        val result = recreate()

        // Then 옛 것을 지우지 않는다 — 지우면 글이 통째로 사라진다
        val notPlaced = assertIs<TextToppingRecreateResult.NotPlaced>(result)
        assertSame(error, notPlaced.cause)
        coVerify(exactly = 0) { textToppingRepository.delete(any(), any(), any()) }
    }

    @Test
    fun invoke_deleteFails_reportsTheNewTextWithTheOldLeft() = runTest {
        // Given 쌓기는 되고 지우기가 실패한다
        givenPlaceSucceeds()
        val error = AppError.Network(cause = null)
        givenDeleteReturns(Result.failure(error))

        // When 다시 만든다
        val result = recreate()

        // Then 새 텍스트 id 와 함께 옛 것만 남았다고 알린다. 쌓기는 다시 부르지 않는다
        assertEquals(TextToppingRecreateResult.OldNotDeleted(placed = placed, cause = error), result)
        coVerify(exactly = 1) { textToppingRepository.place(any(), any(), any(), any(), any()) }
    }

    @Test
    fun invoke_oldAlreadyGone_countsAsRecreated() = runTest {
        // Given 옛 텍스트가 이미 지워져 있다
        givenPlaceSucceeds()
        givenDeleteReturns(
            Result.failure(
                AppError.Server(
                    code = ServerErrorCode.ParfaitText.PARFAIT_TEXT_NOT_FOUND,
                    statusCode = 404,
                    serverMessage = "텍스트를 찾을 수 없습니다",
                ),
            ),
        )

        // When 다시 만든다
        val result = recreate()

        // Then 다시 지울 것이 없으니 성공이다
        assertEquals(TextToppingRecreateResult.Recreated(placed), result)
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val OLD_TEXT_ID = ParfaitTextId(5L)
        val NEW_TEXT_ID = ParfaitTextId(99L)
        const val TEXT = "새 글"
    }
}
