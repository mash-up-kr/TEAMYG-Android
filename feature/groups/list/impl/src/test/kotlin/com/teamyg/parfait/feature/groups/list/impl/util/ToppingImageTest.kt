package com.teamyg.parfait.feature.groups.list.impl.util

import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ToppingImageTest {
    private fun group(
        groupId: Long,
        recentImageUrl: String?,
    ) = MyParfaitGroupVO(
        groupId = GroupId(groupId),
        groupName = GroupName("모카의 파르페"),
        recentImageUrl = recentImageUrl,
        recentImageUploadedAt = null,
        lastPlacedByNametagChip = NametagChipType.DEFAULT,
    )

    @Test
    fun withRecentImage_isRemote() {
        // Given 최근 토핑 이미지가 있는 그룹
        val group = group(groupId = 1L, recentImageUrl = "https://cdn.example.com/a.png")

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage()

        // Then 서버가 준 URL 을 그대로 쓴다
        assertEquals(YGToppingImage.Remote("https://cdn.example.com/a.png"), image)
    }

    @Test
    fun withoutRecentImage_isTemplateNotError() {
        // Given 아직 토핑이 올라오지 않은 그룹
        val group = group(groupId = 1L, recentImageUrl = null)

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage()

        // Then 조회 실패 그래픽이 아니라 템플릿이 걸린다
        assertIs<YGToppingImage.Template>(image)
    }

    @Test
    fun withoutRecentImage_sameGroupKeepsSameTemplate() {
        // Given 같은 그룹을 두 번 그린다
        val group = group(groupId = 7L, recentImageUrl = null)

        // When 각각 이미지를 고른다
        // Then 목록 위치와 무관하게 같은 템플릿이 나온다
        assertEquals(group.toToppingImage(), group.toToppingImage())
    }

    @Test
    fun withoutRecentImage_differentGroupsSpreadOverTemplates() {
        // Given 그룹 id 가 연달아 있는 그룹들
        val images = (1L..6L).map { id -> group(groupId = id, recentImageUrl = null).toToppingImage() }

        // Then 6종이 겹치지 않게 배분된다
        assertEquals(6, images.toSet().size)
    }
}
