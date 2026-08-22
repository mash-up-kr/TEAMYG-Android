package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasBGEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getParfaitDetail: GetParfaitDetailUseCase = mockk()
    private val getGroupDetail: GetGroupDetailUseCase = mockk()
    private val refreshGroupDetail: RefreshGroupDetailUseCase = mockk()
    private val deleteTopping: DeleteToppingUseCase = mockk()

    @Before
    fun stubTheHappyPath() {
        coEvery { getParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID)) } returns Result.success(canvas())
        coEvery { refreshGroupDetail(GroupId(GROUP_ID)) } returns Result.success(Unit)
        every { getGroupDetail(GroupId(GROUP_ID)) } returns flowOf(groupDetail())
    }

    private fun viewModel() = CanvasBGEditViewModel(
        groupIdValue = GROUP_ID,
        parfaitIdValue = PARFAIT_ID,
        getParfaitDetailUseCase = getParfaitDetail,
        getGroupDetailUseCase = getGroupDetail,
        refreshGroupDetailUseCase = refreshGroupDetail,
        deleteToppingUseCase = deleteTopping,
    )

    /** 토핑 로딩(그룹 상세 + 캔버스 상세)이 끝날 때까지 기다린 상태로 시작한다 */
    private fun TestScope.loadedViewModel() = viewModel().also { advanceUntilIdle() }

    /** 실제 스텁 중 내 것 하나를 선택해 둔다 — 삭제는 내 토핑에서만 열린다 */
    private fun CanvasBGEditViewModel.selectMyTopping(): CanvasToppingItem {
        val topping = state.value.toppings.first { it.isMine }
        processIntent(CanvasBGEditIntent.OnClickTopping(topping))
        return topping
    }

    @Test
    fun init_loadsToppings_marksMineByNicknameMatch() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 화면이 열려 토핑 목록을 받는다
        val viewModel = loadedViewModel()

        // Then 그룹 상세의 내 닉네임과 같은 배치자만 isMine 이다
        val toppings = viewModel.state.value.toppings
        assertEquals(true, toppings.first { it.parfaitImageId == MY_PARFAIT_IMAGE_ID }.isMine)
        assertEquals(false, toppings.first { it.parfaitImageId == OTHER_PARFAIT_IMAGE_ID }.isMine)
    }

    @Test
    fun deleteToppingDialogConfirm_useCaseSucceeds_removesTopping() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 선택했고, 삭제 API 는 성공한다
        val viewModel = loadedViewModel()
        val topping = viewModel.selectMyTopping()
        coEvery {
            deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(topping.parfaitImageId))
        } returns Result.success(Unit)

        // When 삭제 모달의 "삭제하기" 를 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then 목록에서 사라지고 선택이 풀린다
        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == topping.parfaitImageId },
        )
        assertNull(viewModel.state.value.selectedToppingId)
    }

    @Test
    fun deleteToppingDialogConfirm_useCaseFails_keepsTopping() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 선택했고, 삭제 API 는 실패한다
        val viewModel = loadedViewModel()
        val topping = viewModel.selectMyTopping()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        // When 삭제 모달의 "삭제하기" 를 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then 목록은 그대로다 — 서버에 반영되지 않았으므로 화면에서도 지우지 않는다. 크래시도 안 난다
        assertTrue(
            viewModel.state.value.toppings
                .any { it.parfaitImageId == topping.parfaitImageId },
        )
    }

    @Test
    fun deleteToppingDialogConfirm_noSelection_doesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 선택된 토핑이 없다
        val viewModel = loadedViewModel()

        // When 그래도 삭제 확인 인텐트가 온다(방어적 상황)
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then API 를 부르지 않는다
        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
    }

    private companion object {
        const val GROUP_ID = 7L
        const val PARFAIT_ID = 42L
        const val MY_GROUP_MEMBER_ID = 10L
        const val OTHER_GROUP_MEMBER_ID = 11L
        const val MY_PARFAIT_IMAGE_ID = 100L
        const val OTHER_PARFAIT_IMAGE_ID = 101L
        const val MY_NICKNAME = "나"
        const val OTHER_NICKNAME = "남"

        fun topping(
            parfaitImageId: Long,
            groupMemberId: Long,
            nickname: String,
        ) = CanvasToppingVO(
            parfaitImageId = ParfaitImageId(parfaitImageId),
            imageId = ImageId(1L),
            imageUrl = "https://example.com/topping.png",
            transform = ToppingTransform(
                positionX = 0.5,
                positionY = 0.5,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
            ),
            border = ToppingBorder.None,
            placedBy = ToppingPlacerVO(
                groupMemberId = GroupMemberId(groupMemberId),
                nickname = GroupNickname(nickname),
            ),
            createdAt = LocalDateTime.parse("2026-08-15T09:30:00"),
        )

        fun canvas() = CanvasVO(
            parfaitId = ParfaitId(PARFAIT_ID),
            date = LocalDate.parse("2026-08-15"),
            status = CanvasStatus.ACTIVE,
            lastClosedDate = null,
            members = listOf(
                CanvasMemberVO(GroupMemberId(MY_GROUP_MEMBER_ID), GroupNickname(MY_NICKNAME), NametagChipType.DEFAULT),
                CanvasMemberVO(
                    GroupMemberId(OTHER_GROUP_MEMBER_ID),
                    GroupNickname(OTHER_NICKNAME),
                    NametagChipType.DEFAULT,
                ),
            ),
            background = null,
            toppings = listOf(
                topping(MY_PARFAIT_IMAGE_ID, MY_GROUP_MEMBER_ID, MY_NICKNAME),
                topping(OTHER_PARFAIT_IMAGE_ID, OTHER_GROUP_MEMBER_ID, OTHER_NICKNAME),
            ),
        )

        fun groupDetail() = ParfaitGroupDetailVO(
            groupId = GroupId(GROUP_ID),
            groupName = GroupName("그룹"),
            groupNickname = GroupNickname(MY_NICKNAME),
            inviteCode = InviteCode("ABC123"),
            memberLimit = 8,
            members = emptyList(),
        )
    }
}
