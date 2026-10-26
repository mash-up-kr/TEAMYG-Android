package com.teamyg.parfait.data.service.model.response.parfaittext

import com.teamyg.parfait.data.di.JsonModule
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateParfaitTextResponseTest {
    private val json = JsonModule.provideRemoteJson()

    private fun body(textAlign: String) = """
        {
          "parfaitTextId": 301,
          "text": "안녕",
          "positionX": 0.5,
          "positionY": 0.5,
          "positionZ": 1,
          "scale": 1.0,
          "rotation": 0.0,
          "color": "#000000",
          "fontSize": 18.0,
          "isFill": false,
          "isStroke": false$textAlign
        }
    """.trimIndent()

    @Test
    fun decode_withoutTextAlign_readsServerDefault() {
        // Given 정렬 키가 없는 응답
        val response = json.decodeFromString<UpdateParfaitTextResponse>(body(textAlign = ""))

        // Then 서버 기본값으로 읽는다
        assertEquals(DEFAULT_TEXT_ALIGN, response.textAlign)
    }

    @Test
    fun decode_withTextAlign_keepsTheValue() {
        // Given 정렬이 실린 응답
        val response = json.decodeFromString<UpdateParfaitTextResponse>(
            body(textAlign = ",\n  \"textAlign\": \"LEFT\""),
        )

        // Then 받은 값을 그대로 둔다
        assertEquals("LEFT", response.textAlign)
    }
}
