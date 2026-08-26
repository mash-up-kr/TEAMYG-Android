package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.service.model.request.parfait.ChangeParfaitBackgroundRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.BackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.ChangeParfaitBackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.GroupMemberResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitImageResponse
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitRemoteDataSourceImplTest {
    private val parfaitService: ParfaitService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitRemoteDataSourceImpl(
        parfaitService = parfaitService,
        apiCaller = apiCaller,
    )

    private fun toppingResponse(
        borderType: String = "SOLID",
        borderColor: String? = "#FF0000",
        borderWidth: Double? = 4.0,
    ) = TodayParfaitImageResponse(
        parfaitImageId = 7L,
        imageId = 11L,
        imageUrl = "https://example.com/topping.png",
        positionX = 10.5,
        positionY = 20.5,
        positionZ = 3,
        scale = 1.5,
        rotation = 30.0,
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
        placedBy = PlacedByResponse(groupMemberId = 5L, nickname = "행복한 판다", ownerType = "ME"),
        createdAt = "2026-08-15T09:30:00",
    )

    private fun todaySuccess(
        status: String = "ACTIVE",
        lastClosedDate: String? = "2026-08-14",
        background: BackgroundResponse? = BackgroundResponse(type = "COLOR", value = "#FFEEDD"),
        images: List<TodayParfaitImageResponse>? = listOf(toppingResponse()),
        memberChip: String? = "TYPE6",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = GetTodayParfaitResponse(
            parfaitId = 100L,
            date = "2026-08-15",
            status = status,
            lastClosedDate = lastClosedDate,
            groupMembers = listOf(
                GroupMemberResponse(id = 5L, nickname = "행복한 판다", nameTagChip = memberChip),
            ),
            background = background,
            images = images,
        ),
    )

    private fun pastSuccess(parfaits: List<PastParfaitResponse>) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PastParfaitsResponse(parfaits = parfaits),
    )

    private fun backgroundSuccess(
        type: String,
        value: String,
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ChangeParfaitBackgroundResponse(
            background = BackgroundResponse(type = type, value = value),
        ),
    )

    private fun <T : Any> businessFailure(code: String) = ApiResponse<T>(
        success = false,
        code = code,
        message = "실패",
        data = null,
    )

    @Test
    fun getTodayCanvas_serviceReturnsFullCanvas_mapsEveryLayer() = runTest {
        // Given 서버가 멤버·배경·토핑이 모두 있는 캔버스를 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess()

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 3층 중첩이 전부 제자리에 들어간다
        assertEquals(ParfaitId(100L), canvas.parfaitId)
        assertEquals(LocalDate.parse("2026-08-15"), canvas.date)
        assertEquals(CanvasStatus.ACTIVE, canvas.status)
        assertEquals(LocalDate.parse("2026-08-14"), canvas.lastClosedDate)
        assertEquals(GroupMemberId(5L), canvas.members.single().groupMemberId)
        assertEquals(GroupNickname("행복한 판다"), canvas.members.single().nickname)
        assertEquals(CanvasBackground.Color("#FFEEDD"), canvas.background)

        val topping = canvas.toppings.single()
        assertEquals(ParfaitImageId(7L), topping.parfaitImageId)
        assertEquals(ImageId(11L), topping.imageId)
        assertEquals(3, topping.transform.positionZ)
        assertEquals(ToppingBorder.Solid(color = "#FF0000", width = 4.0), topping.border)
        assertEquals(GroupMemberId(5L), topping.placedBy.groupMemberId)
        assertTrue(topping.placedBy.isMine)
        assertEquals(LocalDateTime.parse("2026-08-15T09:30:00"), topping.createdAt)
    }

    @Test
    fun getTodayCanvas_imagesNull_foldsToEmptyList() = runTest {
        // Given 배치가 0건이라 서버가 images 를 null 로 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(images = null)

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 널이 아니라 빈 목록으로 접힌다
        assertEquals(emptyList(), canvas.toppings)
    }

    @Test
    fun getTodayCanvas_backgroundNull_staysNull() = runTest {
        // Given 배경이 설정되지 않았다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(background = null)

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 미설정은 의미 있는 상태라 널로 남는다
        assertNull(canvas.background)
    }

    @Test
    fun getTodayCanvas_lastClosedDateNull_staysNull() = runTest {
        // Given 마지막으로 닫힌 날짜가 없다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(lastClosedDate = null)

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 미설정은 그대로 널로 남는다
        assertNull(canvas.lastClosedDate)
    }

    @Test
    fun getTodayCanvas_unknownBackgroundType_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 배경 타입을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(background = BackgroundResponse(type = "GRADIENT", value = "x"))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 미설정과 같은 값으로 접힌다
        assertNull(canvas.background)
    }

    @Test
    fun getTodayCanvas_imageBackground_mapsToImageCase() = runTest {
        // Given 배경이 이미지다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(background = BackgroundResponse(type = "IMAGE", value = "https://example.com/bg.png"))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then value 가 url 자리로 간다
        assertEquals(CanvasBackground.Image("https://example.com/bg.png"), canvas.background)
    }

    @Test
    fun getTodayCanvas_unknownStatus_fallsBackToUnknown() = runTest {
        // Given 서버가 상태를 하나 늘렸다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(status = "ARCHIVED")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 크래시하지 않고 UNKNOWN 으로 떨어진다
        assertEquals(CanvasStatus.UNKNOWN, canvas.status)
    }

    @Test
    fun getTodayCanvas_lowercaseStatus_fallsBackToUnknown() = runTest {
        // Given 서버가 소문자로 준다(대소문자 민감성 확인)
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(status = "active")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 매핑은 정확히 일치할 때만 성립한다
        assertEquals(CanvasStatus.UNKNOWN, canvas.status)
    }

    @Test
    fun getTodayCanvas_borderTypeNone_mapsToNoneIgnoringColor() = runTest {
        // Given 테두리가 NONE 인데 색·두께가 함께 실려 왔다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(images = listOf(toppingResponse(borderType = "NONE")))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 값은 무시되고 None 이 된다
        assertEquals(ToppingBorder.None, canvas.toppings.single().border)
    }

    @Test
    fun getTodayCanvas_solidBorderMissingWidth_fallsBackToNone() = runTest {
        // Given SOLID 인데 두께가 없다(서버가 막는 조합이지만 이미 저장된 행일 수 있다)
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            todaySuccess(images = listOf(toppingResponse(borderWidth = null)))

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 크래시 대신 테두리를 그리지 않는다
        assertEquals(ToppingBorder.None, canvas.toppings.single().border)
    }

    @Test
    fun getTodayCanvas_groupNotJoined_returnsBusinessFailure() = runTest {
        // Given 참여하지 않은 그룹이다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns
            businessFailure<GetTodayParfaitResponse>("GROUP_NOT_JOINED")

        // When 오늘의 캔버스 조회
        val result = dataSource.getTodayCanvas(GroupId(1L))

        // Then 번역하지 않고 Business 로 흐른다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
    }

    @Test
    fun getTodayCanvas_memberChip_becomesThatType() = runTest {
        // Given 서버가 그룹 멤버마다 배정된 칩을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess()

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 도메인 enum 으로 넘어온다 — 상단 멤버 칩을 계약으로 그릴 수 있다
        assertEquals(NametagChipType.TYPE6, canvas.members.single().nametagChip)
    }

    @Test
    fun getTodayCanvas_unknownMemberChip_foldsToDefault() = runTest {
        // Given 서버가 앱이 모르는 값을 준다 — 열린 입력이다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsToday(1L) } returns todaySuccess(memberChip = "TYPE99")

        // When 오늘의 캔버스 조회
        val canvas = dataSource.getTodayCanvas(GroupId(1L)).getOrThrow()

        // Then 던지지 않고 DEFAULT 로 접는다 — 모르는 색은 중립으로 그린다
        assertEquals(NametagChipType.DEFAULT, canvas.members.single().nametagChip)
    }

    @Test
    fun getPastCanvases_serviceReturnsList_mapsCountAndThumbnail() = runTest {
        // Given 서버가 과거 캔버스 둘을 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(
            listOf(
                PastParfaitResponse(
                    parfaitId = 3L,
                    date = "2026-08-14",
                    thumbnailUrl = "https://example.com/thumb.png",
                    imageCount = 2,
                ),
                PastParfaitResponse(parfaitId = 2L, date = "2026-08-13", thumbnailUrl = null, imageCount = 0),
            ),
        )

        // When 과거 목록 조회
        val canvases = dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 서버 순서를 유지하고 imageCount 가 toppingCount 로 간다
        assertEquals(listOf(ParfaitId(3L), ParfaitId(2L)), canvases.map { it.parfaitId })
        assertEquals(listOf(2, 0), canvases.map { it.toppingCount })
        assertEquals(LocalDate.parse("2026-08-14"), canvases.first().date)
        assertEquals("https://example.com/thumb.png", canvases.first().thumbnailUrl)
    }

    @Test
    fun getPastCanvases_rangeOmitted_passesNullQueries() = runTest {
        // Given 범위를 넘기지 않는다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, null, null) } returns pastSuccess(emptyList())

        // When 과거 목록 조회
        dataSource.getPastCanvases(GroupId(1L)).getOrThrow()

        // Then 서버 기본값(오늘 - 30일)이 살도록 쿼리를 비워 보낸다
        coVerify { parfaitService.getGroupsByGroupIdParfaits(groupId = 1L, from = null, to = null) }
    }

    @Test
    fun getPastCanvases_rangeGiven_sendsIsoStrings() = runTest {
        // Given 범위를 지정한다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, "2026-08-01", "2026-08-15") } returns
            pastSuccess(emptyList())

        // When 과거 목록 조회
        dataSource
            .getPastCanvases(
                groupId = GroupId(1L),
                from = LocalDate.parse("2026-08-01"),
                to = LocalDate.parse("2026-08-15"),
            ).getOrThrow()

        // Then ISO-8601 문자열로 실린다
        coVerify {
            parfaitService.getGroupsByGroupIdParfaits(groupId = 1L, from = "2026-08-01", to = "2026-08-15")
        }
    }

    @Test
    fun getPastCanvases_invalidDateRange_returnsBusinessFailure() = runTest {
        // Given from 이 to 보다 늦다
        coEvery { parfaitService.getGroupsByGroupIdParfaits(1L, "2026-08-20", "2026-08-15") } returns
            businessFailure<PastParfaitsResponse>("INVALID_DATE_RANGE")

        // When 과거 목록 조회
        val result = dataSource.getPastCanvases(
            groupId = GroupId(1L),
            from = LocalDate.parse("2026-08-20"),
            to = LocalDate.parse("2026-08-15"),
        )

        // Then 서버 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_DATE_RANGE", error.code)
    }

    @Test
    fun getCanvasDetail_serviceReturnsCanvas_mapsSameShapeAsToday() = runTest {
        // Given 지난 캔버스 상세를 서버가 오늘 조회와 같은 형태로 준다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsByParfaitId(1L, 100L) } returns
            todaySuccess(status = "CLOSED")

        // When 캔버스 상세 조회
        val canvas = dataSource.getCanvasDetail(GroupId(1L), ParfaitId(100L)).getOrThrow()

        // Then 오늘 조회와 같은 매퍼를 타 전 계층이 채워진다
        assertEquals(ParfaitId(100L), canvas.parfaitId)
        assertEquals(CanvasStatus.CLOSED, canvas.status)
        assertEquals(CanvasBackground.Color("#FFEEDD"), canvas.background)
        assertEquals(GroupMemberId(5L), canvas.members.single().groupMemberId)
        assertEquals(ParfaitImageId(7L), canvas.toppings.single().parfaitImageId)
    }

    @Test
    fun getCanvasDetail_parfaitNotFound_returnsBusinessFailure() = runTest {
        // Given 없는 파르페이거나 다른 그룹 소속이다 — 서버는 둘을 구분하지 않는다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsByParfaitId(1L, 999L) } returns
            businessFailure<GetTodayParfaitResponse>("PARFAIT_NOT_FOUND")

        // When 캔버스 상세 조회
        val result = dataSource.getCanvasDetail(GroupId(1L), ParfaitId(999L))

        // Then 404 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_NOT_FOUND", error.code)
    }

    @Test
    fun changeCanvasBackground_colorEdit_sendsHexInValueAndLeavesImageIdNull() = runTest {
        // Given 단색 배경으로 바꾼다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns backgroundSuccess(type = "COLOR", value = "#FF5733")

        // When 색 배경 변경
        dataSource
            .changeCanvasBackground(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(100L),
                background = CanvasBackgroundEdit.Color("#FF5733"),
            ).getOrThrow()

        // Then 서버가 요구하는 조건부 필수대로 value 만 채우고 imageId 는 비운다
        coVerify {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(
                groupId = 1L,
                parfaitId = 100L,
                request = ChangeParfaitBackgroundRequest(type = "COLOR", value = "#FF5733", imageId = null),
            )
        }
    }

    @Test
    fun changeCanvasBackground_imageEdit_sendsImageIdAndLeavesValueNull() = runTest {
        // Given 업로드 확인을 마친 이미지로 바꾼다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns backgroundSuccess(type = "IMAGE", value = "https://example.com/bg.png")

        // When 이미지 배경 변경
        dataSource
            .changeCanvasBackground(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(100L),
                background = CanvasBackgroundEdit.Image(ImageId(11L)),
            ).getOrThrow()

        // Then value 가 아니라 imageId 로 실린다 — 서버는 id 로 받고 URL 로 돌려준다
        coVerify {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(
                groupId = 1L,
                parfaitId = 100L,
                request = ChangeParfaitBackgroundRequest(type = "IMAGE", value = null, imageId = 11L),
            )
        }
    }

    @Test
    fun changeCanvasBackground_imageEdit_returnsUrlEchoedByServer() = runTest {
        // Given 서버가 저장한 이미지 URL 을 돌려준다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns backgroundSuccess(type = "IMAGE", value = "https://example.com/bg.png")

        // When 이미지 배경 변경
        val background = dataSource
            .changeCanvasBackground(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(100L),
                background = CanvasBackgroundEdit.Image(ImageId(11L)),
            ).getOrThrow()

        // Then 앱은 imageId 만 알았으므로 echo 된 URL 을 그대로 받아야 그릴 수 있다
        assertEquals(CanvasBackground.Image("https://example.com/bg.png"), background)
    }

    @Test
    fun changeCanvasBackground_unknownTypeEchoed_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 type 을 돌려준다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns backgroundSuccess(type = "GRADIENT", value = "#FF5733")

        // When 배경 변경
        val background = dataSource
            .changeCanvasBackground(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(100L),
                background = CanvasBackgroundEdit.Color("#FF5733"),
            ).getOrThrow()

        // Then 조회와 같은 규칙으로 접힌다 — 저장은 됐지만 그릴 수 없다
        assertNull(background)
    }

    @Test
    fun changeCanvasBackground_invalidBackground_returnsBusinessFailure() = runTest {
        // Given HEX 형식이 서버 검증을 통과하지 못한다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns businessFailure<ChangeParfaitBackgroundResponse>("INVALID_BACKGROUND")

        // When 배경 변경
        val result = dataSource.changeCanvasBackground(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(100L),
            background = CanvasBackgroundEdit.Color("#FFF"),
        )

        // Then 서버 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_BACKGROUND", error.code)
    }

    @Test
    fun changeCanvasBackground_imageNotFound_returnsImageDomainCode() = runTest {
        // Given imageId 에 해당하는 이미지 메타가 없다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns businessFailure<ChangeParfaitBackgroundResponse>("IMAGE_NOT_FOUND")

        // When 이미지 배경 변경
        val result = dataSource.changeCanvasBackground(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(100L),
            background = CanvasBackgroundEdit.Image(ImageId(404L)),
        )

        // Then parfait 도메인 밖(ImageErrorCode) 코드도 번역 없이 그대로 흐른다 —
        // 이 엔드포인트는 세 enum 의 코드를 섞어 내므로 도메인으로 분기하면 놓친다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("IMAGE_NOT_FOUND", error.code)
    }

    @Test
    fun changeCanvasBackground_imageNotConfirmed_returnsBusinessFailure() = runTest {
        // Given 업로드 확인을 마치지 않은 이미지다
        coEvery {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(1L, 100L, any())
        } returns businessFailure<ChangeParfaitBackgroundResponse>("BACKGROUND_IMAGE_NOT_CONFIRMED")

        // When 이미지 배경 변경
        val result = dataSource.changeCanvasBackground(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(100L),
            background = CanvasBackgroundEdit.Image(ImageId(11L)),
        )

        // Then image 도메인이 아니라 parfait 도메인 코드로 온다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("BACKGROUND_IMAGE_NOT_CONFIRMED", error.code)
    }

    @Test
    fun getCanvasDetail_groupNotJoined_returnsBusinessFailure() = runTest {
        // Given 참여하지 않은 그룹이다 — 멤버십 검사가 파르페 조회보다 먼저다
        coEvery { parfaitService.getGroupsByGroupIdParfaitsByParfaitId(1L, 100L) } returns
            businessFailure<GetTodayParfaitResponse>("GROUP_NOT_JOINED")

        // When 캔버스 상세 조회
        val result = dataSource.getCanvasDetail(GroupId(1L), ParfaitId(100L))

        // Then 403 코드가 그대로 흐른다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
    }
}
