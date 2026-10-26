package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingAlign
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasLayerOrderTest {
    @Test
    fun order_byZ_thenImageBelowText_thenId() {
        // Given 이미지 z2(id5), 텍스트 z1(id9), 텍스트 z2(id5)
        val order = canvasLayerOrder(
            images = listOf(image(id = 5, z = 2)),
            texts = listOf(text(id = 5, z = 2), text(id = 9, z = 1)),
        )

        // Then z 가 낮은 쪽이 아래, 같은 z 에서는 이미지가 아래다
        assertEquals(
            listOf(CanvasToppingKey.Text(9), CanvasToppingKey.Image(5), CanvasToppingKey.Text(5)),
            order.map { it.key },
        )
    }

    @Test
    fun order_sameZSameKind_byIdAscending() {
        val order = canvasLayerOrder(
            images = listOf(image(id = 8, z = 1), image(id = 3, z = 1)),
            texts = emptyList(),
        )

        assertEquals(listOf(CanvasToppingKey.Image(3), CanvasToppingKey.Image(8)), order.map { it.key })
    }

    private fun transform(z: Int) = ToppingTransform(0.5, 0.5, z, 1.0, 0.0)

    private val placer = ToppingPlacerVO(GroupMemberId(1L), GroupNickname("연경이"))

    private fun image(
        id: Long,
        z: Int,
    ) = CanvasToppingVO(
        parfaitImageId = ParfaitImageId(id),
        imageId = ImageId(id),
        imageUrl = "https://cdn.example.com/.png",
        transform = transform(z),
        border = ToppingBorder.None,
        placedBy = placer,
        isMine = false,
        createdAt = LocalDateTime(2026, 8, 20, 12, 0),
    )

    private fun text(
        id: Long,
        z: Int,
    ) = CanvasTextToppingVO(
        parfaitTextId = ParfaitTextId(id),
        text = "안녕",
        transform = transform(z),
        style = TextToppingStyle(
            "#000000",
            24.0,
            isFill = false,
            isStroke = false,
            textAlign = TextToppingAlign.CENTER,
        ),
        placedBy = placer,
        isMine = false,
        createdAt = LocalDateTime(2026, 8, 20, 12, 0),
    )
}
