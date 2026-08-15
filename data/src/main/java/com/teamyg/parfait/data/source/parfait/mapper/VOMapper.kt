package com.teamyg.parfait.data.source.parfait.mapper

import com.teamyg.parfait.data.service.model.response.parfait.BackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.GroupMemberResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitImageResponse
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

private const val BACKGROUND_TYPE_COLOR = "COLOR"
private const val BACKGROUND_TYPE_IMAGE = "IMAGE"
private const val BORDER_TYPE_SOLID = "SOLID"

internal fun GetTodayParfaitResponse.toTodayCanvasVO(): TodayCanvasVO = TodayCanvasVO(
    parfaitId = ParfaitId(parfaitId),
    date = LocalDate.parse(date),
    status = status.toCanvasStatus(),
    lastClosedDate = lastClosedDate?.let(LocalDate::parse),
    members = groupMembers.map { it.toCanvasMemberVO() },
    background = background?.toCanvasBackground(),
    toppings = images.orEmpty().map { it.toCanvasToppingVO() },
)

internal fun PastParfaitsResponse.toPastCanvasVOList(): List<PastCanvasVO> = parfaits.map {
    PastCanvasVO(
        parfaitId = ParfaitId(it.parfaitId),
        date = LocalDate.parse(it.date),
        thumbnailUrl = it.thumbnailUrl,
        toppingCount = it.imageCount,
    )
}

private fun String.toCanvasStatus(): CanvasStatus = when (this) {
    CanvasStatus.ACTIVE.name -> CanvasStatus.ACTIVE
    CanvasStatus.CLOSED.name -> CanvasStatus.CLOSED
    CanvasStatus.EMPTY.name -> CanvasStatus.EMPTY
    else -> CanvasStatus.UNKNOWN
}

/**
 * 미지 type 은 null 로 접는다 — 그리라는 뜻을 모르는 것과 배경 미설정은 화면에서 같다.
 */
private fun BackgroundResponse.toCanvasBackground(): CanvasBackground? = when (type) {
    BACKGROUND_TYPE_COLOR -> CanvasBackground.Color(value)
    BACKGROUND_TYPE_IMAGE -> CanvasBackground.Image(value)
    else -> null
}

private fun GroupMemberResponse.toCanvasMemberVO(): CanvasMemberVO = CanvasMemberVO(
    groupMemberId = GroupMemberId(id),
    nickname = GroupNickname(nickname),
)

private fun TodayParfaitImageResponse.toCanvasToppingVO(): CanvasToppingVO = CanvasToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    border = toToppingBorder(),
    placedBy = placedBy.toToppingPlacerVO(),
    createdAt = LocalDateTime.parse(createdAt),
)

/**
 * SOLID 인데 색이나 두께가 없으면 Solid 를 만들 수 없으므로 None 으로 떨어뜨린다.
 * 서버는 그 조합을 저장 시점에 막지만(INVALID_BORDER) 이미 저장된 행이 있을 수 있고,
 * 앱이 크래시하는 것보다 테두리를 안 그리는 편이 낫다.
 */
private fun TodayParfaitImageResponse.toToppingBorder(): ToppingBorder {
    if (borderType != BORDER_TYPE_SOLID) return ToppingBorder.None
    val color = borderColor ?: return ToppingBorder.None
    val width = borderWidth ?: return ToppingBorder.None
    return ToppingBorder.Solid(color = color, width = width)
}

private fun PlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
)
