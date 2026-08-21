package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.error.AppError
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingPlaceFailureTest {
    private fun server(
        code: String,
        statusCode: Int?,
    ) = AppError.Server(
        code = code,
        statusCode = statusCode,
        serverMessage = "서버 메시지",
    )

    @Test
    fun isPermanentPlaceFailure_closedParfait_isPermanent() {
        // Given 03시 회전이 캔버스를 닫은 사이 화면이 열려 있었다
        val error = server(code = "PARFAIT_ALREADY_CLOSED", statusCode = 409)

        // Then 재시도해도 같은 결과라 되감아야 한다
        assertTrue(error.isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_notJoinedOrMissingParfait_isPermanent() {
        // 배치 POST 는 그룹 참여 → 파르페 존재 → 파르페 상태 순으로 검사한다.
        // 마감만 되감으면 앞의 둘에서 사용자가 실패만 반복한다
        assertTrue(server(code = "GROUP_NOT_JOINED", statusCode = 403).isPermanentPlaceFailure())
        assertTrue(server(code = "PARFAIT_NOT_FOUND", statusCode = 404).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_nullStatusCode_stillPermanent() {
        // 서버가 200 에 실패 봉투를 실으면 ApiCaller 가 statusCode 를 null 로 채운다.
        // 그 갈래에서 판정 불가로 두면 사용자가 영원히 실패하는 재시도만 반복한다
        assertTrue(server(code = "PARFAIT_ALREADY_CLOSED", statusCode = null).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_badRequestCodes_arePermanent() {
        // 재시도가 발급부터 4단계를 다시 태워도 같은 400이라, 되감지 않으면 참조되지 않는
        // 이미지가 확인을 누를 때마다 서버에 쌓인다
        assertTrue(server(code = "INVALID_REQUEST", statusCode = 400).isPermanentPlaceFailure())
        assertTrue(server(code = "INVALID_BORDER", statusCode = 400).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_otherServerCode_isNotPermanent() {
        assertFalse(server(code = "IMAGE_NOT_FOUND", statusCode = 404).isPermanentPlaceFailure())
        assertFalse(server(code = "IMAGE_NOT_CONFIRMED", statusCode = 409).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_networkAndUnexpected_areNotPermanent() {
        // 연결 실패는 재시도가 의미 있는 유일한 갈래다
        assertFalse(AppError.Network(IOException("connection reset")).isPermanentPlaceFailure())
        assertFalse(AppError.Unexpected(IllegalStateException("매핑 실패")).isPermanentPlaceFailure())
    }
}
