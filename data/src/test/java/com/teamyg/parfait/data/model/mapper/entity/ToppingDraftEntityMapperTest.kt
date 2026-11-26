package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.ToppingDraftEntity
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ToppingDraftEntityMapperTest {
    // JsonModule 의 @LocalJson 과 같은 설정
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private fun roundTrip(draft: ToppingDraft): ToppingDraft? {
        val raw = json.encodeToString(draft.toEntity())
        return json.decodeFromString<ToppingDraftEntity>(raw).toVO()
    }

    @Test
    fun legacyJsonWithoutIsSolo_decodesAsGroup() {
        // Given 혼자 공간 도입 전에 저장된 JSON
        val legacy = """{"groupId":1,"parfaitId":100,"nextPositionZ":3}"""

        // When
        val draft = json.decodeFromString<ToppingDraftEntity>(legacy).toVO()

        // Then
        assertNotNull(draft)
        assertEquals(ToppingDraftTarget.Group(GroupId(1L), ParfaitId(100L)), draft.target)
        assertEquals(3, draft.nextPositionZ)
    }

    @Test
    fun soloDraft_roundTrips() {
        val draft = ToppingDraft(
            target = ToppingDraftTarget.Solo,
            nextPositionZ = 2,
            subjectImagePath = "/cache/subject.png",
            cutoutImagePath = "/cache/cutout.png",
            sourceLongSide = SourceLongSide(1024),
        )

        assertEquals(draft, roundTrip(draft))
    }

    @Test
    fun groupDraft_roundTrips() {
        val draft = ToppingDraft(
            target = ToppingDraftTarget.Group(GroupId(1L), ParfaitId(100L)),
            nextPositionZ = 5,
            subjectImagePath = "/cache/subject.png",
        )

        assertEquals(draft, roundTrip(draft))
    }

    @Test
    fun groupEntityMissingIds_readsAsNoDraft() {
        val entity = ToppingDraftEntity(groupId = 1L, parfaitId = null, nextPositionZ = 0)

        assertNull(entity.toVO())
    }
}
