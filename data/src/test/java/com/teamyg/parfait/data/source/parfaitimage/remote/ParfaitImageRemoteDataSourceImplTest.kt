package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageBorderRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImagesRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImagePlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageBorderResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImagesResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitImageRemoteDataSourceImplTest {
    private val parfaitImageService: ParfaitImageService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitImageRemoteDataSourceImpl(
        parfaitImageService = parfaitImageService,
        apiCaller = apiCaller,
    )

    private val transform = ToppingTransform(
        positionX = 120.5,
        positionY = 340.2,
        positionZ = 1,
        scale = 1.0,
        rotation = 0.0,
    )

    private fun placeSuccess() = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PlaceParfaitImageResponse(
            parfaitImageId = 201L,
            imageId = 77L,
            imageUrl = "https://example.com/image",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 1,
            scale = 1.0,
            rotation = 0.0,
            placedBy = PlaceParfaitImagePlacedByResponse(groupMemberId = 10L, nickname = "연경이"),
        ),
    )

    private suspend fun place(border: ToppingBorder = ToppingBorder.None) = dataSource.placeTopping(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(5L),
        imageId = ImageId(77L),
        transform = transform,
        border = border,
    )

    @Test
    fun placeTopping_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 배치 성공 응답을 준다
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When 토핑 배치
        val vo = place().getOrThrow()

        // Then 식별자·URL·transform 이 제자리에 들어간다
        assertEquals(ParfaitImageId(201L), vo.parfaitImageId)
        assertEquals(ImageId(77L), vo.imageId)
        assertEquals("https://example.com/image", vo.imageUrl)
        assertEquals(transform, vo.transform)
    }

    @Test
    fun placeTopping_mapsNestedPlacedBy() = runTest {
        // Given 배치자 정보가 담긴 성공 응답
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When 토핑 배치
        val vo = place().getOrThrow()

        // Then 중첩 객체가 VO 로 풀리고 닉네임은 그룹 닉네임 타입이다
        assertEquals(GroupMemberId(10L), vo.placedBy.groupMemberId)
        assertEquals(GroupNickname("연경이"), vo.placedBy.nickname)
    }

    @Test
    fun placeTopping_noneBorder_sendsNoColorAndWidth() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When 테두리 없이 배치
        place(border = ToppingBorder.None)

        // Then borderType 만 NONE 이고 색·두께는 보내지 않는다
        assertEquals("NONE", request.captured.borderType)
        assertNull(request.captured.borderColor)
        assertNull(request.captured.borderWidth)
    }

    @Test
    fun placeTopping_solidBorder_sendsColorAndWidth() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When SOLID 테두리로 배치
        place(border = ToppingBorder.Solid(color = "#FFFFFF", width = 4.0))

        // Then 색·두께가 함께 실린다 (서버가 SOLID 인데 둘 중 하나가 없으면 400 INVALID_BORDER)
        assertEquals("SOLID", request.captured.borderType)
        assertEquals("#FFFFFF", request.captured.borderColor)
        assertEquals(4.0, request.captured.borderWidth)
    }

    @Test
    fun placeTopping_buildsRequestBodyFromTransform() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When 배치
        place()

        // Then transform 5필드와 imageId 가 그대로 실린다 (Double 이 넷이라 뒤바뀌어도 컴파일된다)
        assertEquals(77L, request.captured.imageId)
        assertEquals(120.5, request.captured.positionX)
        assertEquals(340.2, request.captured.positionY)
        assertEquals(1, request.captured.positionZ)
        assertEquals(1.0, request.captured.scale)
        assertEquals(0.0, request.captured.rotation)
    }

    @Test
    fun placeTopping_unwrapsIdsForPathVariables() = runTest {
        // Given 성공 응답
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When value class 로 감싼 id 로 배치
        place()

        // Then 경로 변수에는 raw Long 이 들어간다 (Retrofit 경계에서 벗긴다)
        coVerify(exactly = 1) {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(1L, 5L, any())
        }
    }

    @Test
    fun placeTopping_groupNotJoined_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답 (HTTP status 축은 여기서 잡지 않는다.
        // 실제 서버의 403 은 Retrofit 이 HttpException 을 던지는 별도 경로를 탄다)
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "GROUP_NOT_JOINED",
            message = "참여하지 않은 그룹입니다",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
    }

    @Test
    fun placeTopping_imageNotConfirmed_returnsBusinessException() = runTest {
        // Given 업로드가 확인되지 않은 이미지 (HTTP status 축은 여기서 잡지 않는다.
        // 실제 서버의 409 는 Retrofit 이 HttpException 을 던지는 별도 경로를 탄다)
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "IMAGE_NOT_CONFIRMED",
            message = "업로드가 확인되지 않은 이미지입니다",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        assertEquals("IMAGE_NOT_CONFIRMED", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }

    @Test
    fun placeTopping_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } throws IOException("connection reset")

        // When 토핑 배치
        val result = place()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun placeTopping_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertEquals("SUCCESS", assertIs<ApiException.EmptyBody>(result.exceptionOrNull()).code)
    }

    private fun updateResponse(
        parfaitImageId: Long,
        positionX: Double = 200.0,
        positionY: Double = 400.0,
        positionZ: Int = 1,
        scale: Double = 1.5,
        rotation: Double = 45.0,
    ) = UpdateParfaitImageResponse(
        parfaitImageId = parfaitImageId,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    )

    private fun updateSuccess(vararg images: UpdateParfaitImageResponse) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = UpdateParfaitImagesResponse(images = images.toList()),
    )

    @Test
    fun updateToppings_serviceReturnsSuccess_returnsMergedTransforms() = runTest {
        // Given 서버가 병합된 값 둘을 준다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns updateSuccess(
            updateResponse(parfaitImageId = 201L),
            updateResponse(
                parfaitImageId = 202L,
                positionX = 10.0,
                positionY = 20.0,
                positionZ = 2,
                scale = 1.0,
                rotation = 0.0,
            ),
        )

        // When 토핑 둘을 한 번에 수정
        val vos = dataSource
            .updateToppings(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(5L),
                updates = listOf(
                    ToppingTransformUpdate(
                        parfaitImageId = ParfaitImageId(201L),
                        positionX = 200.0,
                        positionY = 400.0,
                        scale = 1.5,
                        rotation = 45.0,
                    ),
                    ToppingTransformUpdate(parfaitImageId = ParfaitImageId(202L), positionX = 10.0, positionY = 20.0),
                ),
            ).getOrThrow()

        // Then 응답은 부분이 아니라 전체 transform 이고 원소 순서를 유지한다
        assertEquals(listOf(ParfaitImageId(201L), ParfaitImageId(202L)), vos.map { it.parfaitImageId })
        assertEquals(
            ToppingTransform(positionX = 200.0, positionY = 400.0, positionZ = 1, scale = 1.5, rotation = 45.0),
            vos.first().transform,
        )
    }

    @Test
    fun updateToppings_buildsOneItemPerUpdate() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitImagesRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns updateSuccess(updateResponse(parfaitImageId = 201L), updateResponse(parfaitImageId = 202L))

        // When 토핑 둘을 수정
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(
                ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0),
                ToppingTransformUpdate(parfaitImageId = ParfaitImageId(202L), scale = 2.0),
            ),
        )

        // Then 요청 한 번에 항목 둘이 실린다 — 호출도 한 번뿐이다
        assertEquals(listOf(201L, 202L), request.captured.items.map { it.parfaitImageId })
        coVerify(exactly = 1) {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        }
    }

    @Test
    fun updateToppings_omittedFieldsAreSentAsNull() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitImagesRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns updateSuccess(updateResponse(parfaitImageId = 201L))

        // When z-order 만 바꾼다
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionZ = 3)),
        )

        // Then 지정한 필드만 값이 있고 나머지는 null 이다 (서버가 null 을 미변경으로 읽는다)
        val item = request.captured.items.single()
        assertEquals(3, item.positionZ)
        assertNull(item.positionX)
        assertNull(item.positionY)
        assertNull(item.scale)
        assertNull(item.rotation)
    }

    @Test
    fun updateToppings_unwrapsIdsForPathVariablesAndItems() = runTest {
        // Given 성공 응답
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns updateSuccess(updateResponse(parfaitImageId = 201L))

        // When value class 로 감싼 id 로 수정
        dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then 경로 변수 둘에 raw Long 이 들어간다 — parfaitImageId 는 경로가 아니라 바디로 간다
        coVerify(exactly = 1) {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(1L, 5L, any())
        }
    }

    @Test
    fun updateToppings_notOwned_returnsBusinessException() = runTest {
        // Given 항목 중 하나가 본인 배치가 아니다 (그룹 미참여도 같은 코드로 온다.
        // HTTP status 축은 여기서 잡지 않는다 - 실제 서버의 403 은 Retrofit 이 HttpException 을
        // 던지는 별도 경로를 탄다)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_OWNED",
            message = "본인이 배치한 토핑이 아닙니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Business 예외로 실패한다 — 서버가 전부 롤백했으므로 부분 성공이 없다
        assertTrue(result.isFailure)
        assertEquals(
            "PARFAIT_IMAGE_NOT_OWNED",
            assertIs<ApiException.Business>(result.exceptionOrNull()).code,
        )
    }

    @Test
    fun updateToppings_alreadyClosed_returnsBusinessException() = runTest {
        // Given 마감된 캔버스다 — 일괄은 마감 검사가 항목별 소유권보다 앞이라 단건과 다른 코드가 온다
        // (`api/parfait-image.md` 검사 순서)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_ALREADY_CLOSED",
            message = "이미 마감된 파르페입니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        assertEquals(
            "PARFAIT_ALREADY_CLOSED",
            assertIs<ApiException.Business>(result.exceptionOrNull()).code,
        )
    }

    @Test
    fun updateToppings_successButNullData_returnsEmptyBodyException() = runTest {
        // Given 성공인데 본문이 비었다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(success = true, code = "SUCCESS", message = "성공", data = null)

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertEquals("SUCCESS", assertIs<ApiException.EmptyBody>(result.exceptionOrNull()).code)
    }

    @Test
    fun updateToppings_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } throws IOException("connection reset")

        // When 수정
        val result = dataSource.updateToppings(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            updates = listOf(ToppingTransformUpdate(parfaitImageId = ParfaitImageId(201L), positionX = 200.0)),
        )

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun updateToppingBorder_solid_sendsColorAndWidth() = runTest {
        // Given 서버가 저장된 테두리를 돌려준다
        val request = slot<UpdateParfaitImageBorderRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = capture(request),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "SOLID",
                borderColor = "#FF0000",
                borderWidth = 4.0,
            ),
        )

        // When SOLID 테두리로 바꾼다
        val vo = dataSource
            .updateToppingBorder(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                parfaitImageId = ParfaitImageId(3L),
                border = ToppingBorder.Solid(color = "#FF0000", width = 4.0),
            ).getOrThrow()

        // Then sealed 가 평면 3필드로 펴져 나가고 응답이 sealed 로 복원된다
        assertEquals("SOLID", request.captured.borderType)
        assertEquals("#FF0000", request.captured.borderColor)
        assertEquals(4.0, request.captured.borderWidth)
        assertEquals(ParfaitImageId(3L), vo.parfaitImageId)
        assertEquals(ToppingBorder.Solid(color = "#FF0000", width = 4.0), vo.border)
    }

    @Test
    fun updateToppingBorder_none_sendsNullColorAndWidth() = runTest {
        // Given 테두리를 없앤다
        val request = slot<UpdateParfaitImageBorderRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = capture(request),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "NONE",
                borderColor = null,
                borderWidth = null,
            ),
        )

        // When NONE 으로 바꾼다
        val vo = dataSource
            .updateToppingBorder(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                parfaitImageId = ParfaitImageId(3L),
                border = ToppingBorder.None,
            ).getOrThrow()

        // Then 색·두께를 보내지 않는다
        assertEquals("NONE", request.captured.borderType)
        assertNull(request.captured.borderColor)
        assertNull(request.captured.borderWidth)
        assertEquals(ToppingBorder.None, vo.border)
    }

    @Test
    fun updateToppingBorder_solidResponseMissingWidth_fallsBackToNone() = runTest {
        // Given 서버가 SOLID 라면서 두께를 빠뜨렸다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = any(),
            )
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = UpdateParfaitImageBorderResponse(
                parfaitImageId = 3L,
                borderType = "SOLID",
                borderColor = "#FF0000",
                borderWidth = null,
            ),
        )

        // When 테두리를 바꾼다
        val vo = dataSource
            .updateToppingBorder(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                parfaitImageId = ParfaitImageId(3L),
                border = ToppingBorder.Solid(color = "#FF0000", width = 4.0),
            ).getOrThrow()

        // Then Solid 를 만들 수 없으므로 None 으로 떨어진다
        assertEquals(ToppingBorder.None, vo.border)
    }

    @Test
    fun updateToppingBorder_notOwned_returnsBusinessFailure() = runTest {
        // Given 본인이 배치한 토핑이 아니다(그룹 미참여도 같은 코드다)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
                request = any(),
            )
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_OWNED",
            message = "본인이 배치한 토핑이 아닙니다",
            data = null,
        )

        // When 테두리를 바꾼다
        val result = dataSource.updateToppingBorder(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
            border = ToppingBorder.None,
        )

        // Then 서버 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_IMAGE_NOT_OWNED", error.code)
    }

    @Test
    fun deleteTopping_serviceReturnsSuccess_returnsUnit() = runTest {
        // Given 서버가 200 과 빈 data 를 준다
        coEvery {
            parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
            )
        } returns ApiResponse(success = true, code = "SUCCESS", message = "성공", data = null)

        // When 토핑을 지운다
        val result = dataSource.deleteTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
        )

        // Then data 가 없어도 성공이다 — envelope 는 오지만 payload 가 없는 경로다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun deleteTopping_alreadyDeleted_returnsBusinessFailure() = runTest {
        // Given 이미 지운 배치를 다시 지운다(삭제는 멱등이 아니다)
        coEvery {
            parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = 1L,
                parfaitId = 2L,
                parfaitImageId = 3L,
            )
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_FOUND",
            message = "존재하지 않는 배치입니다",
            data = null,
        )

        // When 토핑을 지운다
        val result = dataSource.deleteTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitImageId = ParfaitImageId(3L),
        )

        // Then 두 번째 호출은 404 다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_IMAGE_NOT_FOUND", error.code)
    }
}
