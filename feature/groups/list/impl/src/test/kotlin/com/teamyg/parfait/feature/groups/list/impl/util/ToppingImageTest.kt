package com.teamyg.parfait.feature.groups.list.impl.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingBorder
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToppingImageTest {
    private fun group(
        groupId: Long,
        recentImageUrl: String?,
        recentImageBorder: ToppingBorder = ToppingBorder.None,
        recentToppingType: ToppingContentType? = recentImageUrl?.let { ToppingContentType.IMAGE },
    ) = MyParfaitGroupVO(
        groupId = GroupId(groupId),
        groupName = GroupName("모카의 파르페"),
        recentToppingType = recentToppingType,
        recentImageUrl = recentImageUrl,
        recentImageBorder = recentImageBorder,
        recentImageUploadedAt = null,
        lastPlacedByNametagChip = NametagChipType.DEFAULT,
    )

    @Test
    fun withRecentImage_isRemote() {
        // Given 오늘 캔버스에 토핑이 있는 그룹
        val group = group(groupId = 1L, recentImageUrl = "https://cdn.example.com/a.png")

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 서버가 준 URL 을 그대로 쓴다
        assertEquals(YGToppingImage.Remote("https://cdn.example.com/a.png"), image)
    }

    @Test
    fun withoutRecentImage_isTemplateNotError() {
        // Given 오늘 캔버스에 토핑이 없는 그룹
        val group = group(groupId = 1L, recentImageUrl = null)

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 조회 실패 그래픽이 아니라 템플릿이 걸린다
        assertIs<YGToppingImage.Template>(image)
    }

    @Test
    fun withoutRecentImage_sameGroupKeepsSameTemplate() {
        // Given 같은 그룹을 두 번 그린다
        val group = group(groupId = 7L, recentImageUrl = null)

        // When 각각 이미지를 고른다
        // Then 목록 위치와 무관하게 같은 템플릿이 나온다
        assertEquals(group.toToppingImage(emptyMap()), group.toToppingImage(emptyMap()))
    }

    @Test
    fun withoutRecentImage_differentGroupsSpreadOverTemplates() {
        // Given 그룹 id 가 연달아 있는 그룹들
        val images = (1L..6L).map { id -> group(groupId = id, recentImageUrl = null).toToppingImage(emptyMap()) }

        // Then 6종이 겹치지 않게 배분된다
        assertEquals(6, images.toSet().size)
    }

    @Test
    fun solidBorderWithOutline_carriesColorWidthAndOutline() {
        // Given 테두리가 있는 토핑과 이미 떠 둔 거리판
        val url = "https://cdn.example.com/a.png"
        val outline = ToppingOutline.of(4, 4) { _, _ -> 255 }
        val group = group(
            groupId = 1L,
            recentImageUrl = url,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(mapOf(url to outline))

        // Then 색·굵기(dp)·거리판이 함께 실린다
        assertEquals(
            YGToppingImage.Remote(
                url = url,
                border = YGToppingBorder(color = Color.Red, width = 6.dp, outline = outline),
            ),
            image,
        )
    }

    @Test
    fun solidBorderBeforeOutlineLoads_keepsBorderWithoutOutline() {
        // Given 거리판을 아직 받지 못한 테두리 토핑
        val url = "https://cdn.example.com/a.png"
        val group = group(
            groupId = 1L,
            recentImageUrl = url,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 거리판 없이 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 테두리 값은 남아 크기가 먼저 줄고, 거리판만 비어 있다
        assertEquals(YGToppingBorder(color = Color.Red, width = 6.dp, outline = null), image.border)
    }

    @Test
    fun unreadableColor_dropsBorder() {
        // Given 색 문자열을 읽을 수 없는 테두리
        val group = group(
            groupId = 1L,
            recentImageUrl = "https://cdn.example.com/a.png",
            recentImageBorder = ToppingBorder.Solid(color = "빨강", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 임의의 색으로 칠하지 않고 테두리를 버린다
        assertNull(image.border)
    }

    @Test
    fun noneBorder_hasNoBorder() {
        // Given 테두리 없는 토핑
        val group = group(groupId = 1L, recentImageUrl = "https://cdn.example.com/a.png")

        // When 띄울 이미지를 고른다
        val image = assertIs<YGToppingImage.Remote>(group.toToppingImage(emptyMap()))

        // Then 테두리가 없다
        assertNull(image.border)
    }

    @Test
    fun withoutRecentImage_ignoresBorder() {
        // Given 토핑 URL 은 없는데 테두리 값만 있는 그룹
        val group = group(
            groupId = 1L,
            recentImageUrl = null,
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
        )

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 템플릿이 걸리고 테두리는 보지 않는다
        assertIs<YGToppingImage.Template>(image)
    }

    @Test
    fun textRecentTopping_isTextTemplate() {
        // Given 오늘 마지막 토핑이 텍스트라 이미지가 비어 온 그룹
        val group = group(groupId = 1L, recentImageUrl = null, recentToppingType = ToppingContentType.TEXT)

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 일반 템플릿이 아니라 텍스트 템플릿이 걸린다
        assertIs<YGToppingImage.TextTemplate>(image)
    }

    @Test
    fun textRecentTopping_winsOverImageUrl() {
        // Given 종류는 텍스트인데 이미지 URL 도 실려 온 그룹
        val group = group(
            groupId = 1L,
            recentImageUrl = "https://cdn.example.com/a.png",
            recentToppingType = ToppingContentType.TEXT,
        )

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 종류를 따라 텍스트 템플릿이 걸린다
        assertIs<YGToppingImage.TextTemplate>(image)
    }

    @Test
    fun textRecentTopping_sameGroupKeepsSameTemplateAndGroupsSplitAcrossBoth() {
        // Given 텍스트가 마지막인 그룹들
        val templates = (1L..4L).map { id ->
            group(
                groupId = id,
                recentImageUrl = null,
                recentToppingType = ToppingContentType.TEXT,
            ).toToppingImage(emptyMap())
        }

        // When 같은 그룹을 다시 그린다
        val again = group(groupId = 1L, recentImageUrl = null, recentToppingType = ToppingContentType.TEXT)
            .toToppingImage(emptyMap())

        // Then 같은 그룹은 같은 템플릿이고, 두 템플릿이 모두 쓰인다
        assertEquals(templates.first(), again)
        assertEquals(2, templates.toSet().size)
    }

    @Test
    fun textTemplate_doesNotFollowCardRotation() {
        // Given 텍스트가 마지막인 그룹과 오늘 토핑이 없는 그룹
        val text = group(groupId = 1L, recentImageUrl = null, recentToppingType = ToppingContentType.TEXT)
        val empty = group(groupId = 1L, recentImageUrl = null)

        // When 띄울 이미지를 고른다
        val textImage = text.toToppingImage(emptyMap())
        val emptyImage = empty.toToppingImage(emptyMap())

        // Then 텍스트 템플릿만 카드 기울기를 따르지 않는다
        assertFalse(textImage.followsCardRotation)
        assertTrue(emptyImage.followsCardRotation)
    }

    @Test
    fun borderedImageUrls_skipsTextRecentTopping() {
        // Given 종류는 텍스트인데 테두리 있는 이미지 URL 도 실려 온 그룹
        val group = group(
            groupId = 1L,
            recentImageUrl = "https://cdn.example.com/a.png",
            recentImageBorder = ToppingBorder.Solid(color = "#FF0000", width = 6.0),
            recentToppingType = ToppingContentType.TEXT,
        )

        // When 테두리를 그릴 URL 을 모은다
        val urls = listOf(group).borderedImageUrls()

        // Then 텍스트 템플릿이 걸리므로 거리판을 뜨지 않는다
        assertTrue(urls.isEmpty())
    }

    @Test
    fun unknownRecentTopping_withoutImage_isTemplate() {
        // Given 앱이 모르는 종류가 마지막이라 이미지가 없는 그룹
        val group = group(groupId = 1L, recentImageUrl = null, recentToppingType = ToppingContentType.UNKNOWN)

        // When 띄울 이미지를 고른다
        val image = group.toToppingImage(emptyMap())

        // Then 일반 템플릿으로 접힌다
        assertIs<YGToppingImage.Template>(image)
    }

    @Test
    fun borderedImageUrls_onlyUrlsWithDrawableBorder() {
        // Given 테두리 상태가 제각각인 그룹들
        val solid = ToppingBorder.Solid(color = "#FF0000", width = 6.0)
        val groups = listOf(
            group(groupId = 1L, recentImageUrl = "https://cdn.example.com/1.png", recentImageBorder = solid),
            group(groupId = 2L, recentImageUrl = "https://cdn.example.com/2.png"),
            group(groupId = 3L, recentImageUrl = null, recentImageBorder = solid),
            group(
                groupId = 4L,
                recentImageUrl = "https://cdn.example.com/4.png",
                recentImageBorder = ToppingBorder.Solid(color = "빨강", width = 6.0),
            ),
            group(groupId = 5L, recentImageUrl = "https://cdn.example.com/5.png", recentImageBorder = solid),
        )

        // When 거리판을 뜰 URL 을 고른다
        val urls = groups.borderedImageUrls()

        // Then 테두리를 실제로 그릴 토핑만 목록 순서대로 남는다
        assertEquals(listOf("https://cdn.example.com/1.png", "https://cdn.example.com/5.png"), urls)
    }
}
