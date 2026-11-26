package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.SoloBackgroundEntity
import com.teamyg.parfait.data.model.entity.SoloSpaceRecordEntity
import com.teamyg.parfait.data.model.entity.SoloTextEntity
import com.teamyg.parfait.data.model.entity.SoloToppingEntity
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.TextToppingAlign
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// 혼자 공간에는 멤버가 없다. 화면이 멤버 목록과 맞춰 보는 id 라 실제 멤버 id 와 겹치지 않는 0 을 둔다
private val SOLO_PLACER = ToppingPlacerVO(
    groupMemberId = GroupMemberId(0),
    nickname = GroupNickname("나"),
)

/** [imageUri] 는 `images/` 안의 파일명을 화면이 그릴 수 있는 절대 URI 로 바꾼다 */
internal fun SoloSpaceRecordEntity.toVO(imageUri: (fileName: String) -> String): SoloCanvasVO = SoloCanvasVO(
    background = background?.toVO(imageUri),
    toppings = toppings.map { topping -> topping.toVO(imageUri) },
    texts = texts.map { text -> text.toVO() },
)

internal fun soloToppingEntity(
    id: Long,
    fileName: String,
    transform: ToppingTransform,
    border: ToppingBorder,
    createdAt: Instant,
): SoloToppingEntity = SoloToppingEntity(
    id = id,
    fileName = fileName,
    positionX = transform.positionX,
    positionY = transform.positionY,
    positionZ = transform.positionZ,
    scale = transform.scale,
    rotation = transform.rotation,
    borderColor = (border as? ToppingBorder.Solid)?.color,
    borderWidth = (border as? ToppingBorder.Solid)?.width,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

internal fun soloTextEntity(
    id: Long,
    text: String,
    transform: ToppingTransform,
    style: TextToppingStyle,
    createdAt: Instant,
): SoloTextEntity = SoloTextEntity(
    id = id,
    text = text,
    positionX = transform.positionX,
    positionY = transform.positionY,
    positionZ = transform.positionZ,
    scale = transform.scale,
    rotation = transform.rotation,
    color = style.color,
    fontSize = style.fontSize,
    isFill = style.isFill,
    isStroke = style.isStroke,
    textAlign = style.textAlign.name,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

internal fun SoloToppingEntity.withBorder(border: ToppingBorder): SoloToppingEntity = copy(
    borderColor = (border as? ToppingBorder.Solid)?.color,
    borderWidth = (border as? ToppingBorder.Solid)?.width,
)

private fun SoloBackgroundEntity.toVO(imageUri: (String) -> String): CanvasBackground = when (this) {
    is SoloBackgroundEntity.Color -> CanvasBackground.Color(hex)
    is SoloBackgroundEntity.Image -> CanvasBackground.Image(imageUri(fileName))
}

private fun SoloToppingEntity.toVO(imageUri: (String) -> String): CanvasToppingVO = CanvasToppingVO(
    parfaitImageId = ParfaitImageId(id),
    // 서버 이미지가 없어 따로 발급할 id 가 없다. 배치 id 를 그대로 쓴다
    imageId = ImageId(id),
    imageUrl = imageUri(fileName),
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    border = if (borderColor != null && borderWidth != null) {
        ToppingBorder.Solid(color = borderColor, width = borderWidth)
    } else {
        ToppingBorder.None
    },
    placedBy = SOLO_PLACER,
    isMine = true,
    createdAt = createdAtEpochMillis.toParfaitDateTime(),
)

private fun SoloTextEntity.toVO(): CanvasTextToppingVO = CanvasTextToppingVO(
    parfaitTextId = ParfaitTextId(id),
    text = text,
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    style = TextToppingStyle(
        color = color,
        fontSize = fontSize,
        isFill = isFill,
        isStroke = isStroke,
        textAlign = TextToppingAlign.entries.firstOrNull { align -> align.name == textAlign }
            ?: TextToppingAlign.CENTER,
    ),
    placedBy = SOLO_PLACER,
    isMine = true,
    createdAt = createdAtEpochMillis.toParfaitDateTime(),
)

// 그룹 캔버스의 createdAt 은 KST 벽시계다. 같은 화면이 두 값을 섞어 읽지 않도록 같은 시간대로 맞춘다
private fun Long.toParfaitDateTime(): LocalDateTime = Instant
    .fromEpochMilliseconds(this)
    .toLocalDateTime(PARFAIT_TIME_ZONE)
