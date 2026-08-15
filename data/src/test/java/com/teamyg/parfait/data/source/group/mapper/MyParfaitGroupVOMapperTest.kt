package com.teamyg.parfait.data.source.group.mapper

import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class MyParfaitGroupVOMapperTest {
    private fun response(recentImageUploadedAt: String?) = MyParfaitGroupResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        recentImageUrl = "https://cdn.example.com/a.png",
        recentImageUploadedAt = recentImageUploadedAt,
    )

    @Test
    fun uploadedAtWithZuluOffset_isParsedAsThatInstant() {
        // Given 서버가 UTC 오프셋(`Z`)을 붙여 보낸다
        val response = response("2026-08-15T05:17:10.240Z")

        // When VO 로 바꾼다
        val vo = response.toMyParfaitGroupVO()

        // Then 오프셋째로 읽어 그 시점이 그대로 남는다
        assertEquals(Instant.parse("2026-08-15T05:17:10.240Z"), vo.recentImageUploadedAt)
    }

    @Test
    fun uploadedAtWithNumericOffset_isTheSameInstantAsItsUtcForm() {
        // Given 같은 순간을 KST 오프셋 표기로 보낸다
        val vo = response("2026-08-15T14:17:10.240+09:00").toMyParfaitGroupVO()

        // Then 표기가 달라도 UTC 표기와 같은 시점이다
        assertEquals(Instant.parse("2026-08-15T05:17:10.240Z"), vo.recentImageUploadedAt)
    }

    @Test
    fun uploadedAtMissing_isNull() {
        // Given 아직 아무도 토핑을 올리지 않아 시각이 없다
        val vo = response(null).toMyParfaitGroupVO()

        // Then 그대로 비워 둔다
        assertNull(vo.recentImageUploadedAt)
    }
}
