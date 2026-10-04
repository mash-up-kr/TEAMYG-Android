package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 캔버스에 놓인 토핑 하나.
 *
 * 위치·크기가 Dp 가 아니라 Canvas-Area 대비 0~1 비율인 이유: 저장된 배치가 그 단위이고
 * (`CanvasToppingVO.transform`), ViewModel 은 화면 크기를 모른다. Dp 로 들고 있으면 기기마다
 * 다른 자리에 놓이고 캔버스 메인(`CanvasToppingLayer`)과도 어긋난다 — 같은 캔버스가 두
 * 화면에서 다르게 보이면 안 된다.
 *
 * @param imageUrl 서버에 저장된 토핑 이미지 주소. 테두리는 픽셀에 굽지 않고 [border] 로 따로
 *   나른다(`adr/0025-topping-border-as-server-field.md`).
 */
data class EditableTopping(
    val parfaitImageId: Long,
    val isMine: Boolean,
    val imageUrl: String,
    val positionX: Float,
    val positionY: Float,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val border: ToppingBorderStyle? = null,
)
