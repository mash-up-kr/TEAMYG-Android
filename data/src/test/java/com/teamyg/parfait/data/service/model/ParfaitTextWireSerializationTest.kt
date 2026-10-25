package com.teamyg.parfait.data.service.model

import com.teamyg.parfait.data.di.JsonModule
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageItemRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImagesRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.PlaceParfaitTextRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.UpdateParfaitTextRequest
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitTextResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImagesResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 서비스 테스트는 DTO 를 mock 으로 주고받아 JSON 키와 기본값 직렬화를 거치지 않는다. 여기서 잠근다. */
class ParfaitTextWireSerializationTest {
    private val json = JsonModule.provideRemoteJson()

    private fun encode(value: PlaceParfaitTextRequest): JsonObject =
        json.parseToJsonElement(json.encodeToString(PlaceParfaitTextRequest.serializer(), value)).jsonObject

    @Test
    fun placeRequest_keepsIsPrefixedStyleKeys() {
        // Given 채움만 켠 배치 요청
        val request = PlaceParfaitTextRequest(
            text = "안녕",
            positionX = 1.0,
            positionY = 2.0,
            positionZ = 3,
            scale = 1.5,
            rotation = 0.0,
            color = "#FFFFFF",
            fontSize = 16.0,
            isFill = true,
            isStroke = false,
        )

        // When 인코딩
        val body = encode(request)

        // Then 서버(Jackson @JsonProperty)가 받는 is 접두사 키 그대로 나간다
        assertTrue(body.getValue("isFill").jsonPrimitive.boolean)
        assertEquals(false, body.getValue("isStroke").jsonPrimitive.boolean)
    }

    @Test
    fun updateTextRequest_untouchedFieldsAreSentAsExplicitNull() {
        // Given 아무것도 바꾸지 않는 수정 요청
        val body = json
            .parseToJsonElement(
                json.encodeToString(UpdateParfaitTextRequest.serializer(), UpdateParfaitTextRequest()),
            ).jsonObject

        // Then 키가 빠지지 않고 null 로 실린다 — 서버에게 null 은 기존 값 유지라 키 부재와 같은 뜻이다
        assertEquals(JsonNull, body.getValue("color"))
        assertEquals(JsonNull, body.getValue("isFill"))
    }

    @Test
    fun updateImagesRequest_imageOnly_sendsEmptyTextItems() {
        // Given 이미지 항목만 있는 일괄 수정
        val request = UpdateParfaitImagesRequest(items = listOf(UpdateParfaitImageItemRequest(parfaitImageId = 1L)))

        // When 인코딩
        val body = json
            .parseToJsonElement(
                json.encodeToString(UpdateParfaitImagesRequest.serializer(), request),
            ).jsonObject

        // Then textItems 가 빈 배열로 실린다(서버 기본값과 같다)
        assertEquals(JsonArray(emptyList()), body.getValue("textItems"))
    }

    @Test
    fun todayTextResponse_decodesServerSample() {
        // Given 서버 배치 응답 본문(소수 초가 붙은 createdAt, ownerType 포함)
        val body = """
            {
              "parfaitTextId": 7,
              "text": "첫 줄\n둘째 줄",
              "positionX": 10.5,
              "positionY": 20.0,
              "positionZ": 4,
              "scale": 1.2,
              "rotation": 90.0,
              "color": "#000000",
              "fontSize": 18.0,
              "isFill": false,
              "isStroke": true,
              "placedBy": { "groupMemberId": 3, "nickname": "연경이", "nameTagChip": "PINK", "ownerType": "ME" },
              "createdAt": "2026-10-25T10:00:00.123456"
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString(TodayParfaitTextResponse.serializer(), body)

        // Then is 접두사 키와 중첩 배치자가 읽힌다
        assertEquals(7L, response.parfaitTextId)
        assertEquals("첫 줄\n둘째 줄", response.text)
        assertEquals(false, response.isFill)
        assertEquals(true, response.isStroke)
        assertEquals("ME", response.placedBy.ownerType)
        assertEquals("2026-10-25T10:00:00.123456", response.createdAt)
    }

    @Test
    fun updateImagesResponse_withoutTexts_decodesToEmptyList() {
        // Given texts 키가 없거나 null 인 응답
        val missing = """{ "images": [] }"""
        val explicitNull = """{ "images": [], "texts": null }"""

        // Then 둘 다 빈 목록이다
        assertEquals(emptyList(), json.decodeFromString(UpdateParfaitImagesResponse.serializer(), missing).texts)
        assertEquals(emptyList(), json.decodeFromString(UpdateParfaitImagesResponse.serializer(), explicitNull).texts)
    }

    @Test
    fun todayResponse_nullTexts_decodesToNull() {
        // Given 텍스트가 0건인 캔버스 — 서버는 빈 배열이 아니라 null 을 준다
        val body = """
            {
              "parfaitId": 1,
              "groupName": "그룹",
              "date": "2026-10-25",
              "status": "ACTIVE",
              "lastClosedDate": null,
              "groupMembers": [],
              "background": null,
              "images": null,
              "texts": null
            }
        """.trimIndent()

        // When 디코딩
        val response = json.decodeFromString(GetTodayParfaitResponse.serializer(), body)

        // Then texts 는 null 이다
        assertNull(response.texts)
    }
}
