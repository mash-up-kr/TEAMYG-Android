package com.teamyg.parfait.feature.groups.canvas.impl.model

/**
 * 편집 화면이 다루는 토핑 하나.
 *
 * 위치·크기가 Dp 가 아니라 Canvas-Area 대비 0~1 비율인 이유: 저장된 배치가 그 단위이고
 * (`CanvasToppingVO.transform`), ViewModel 은 화면 크기를 모른다. Dp 로 들고 있으면 기기마다
 * 다른 자리에 놓이고 캔버스 메인(`CanvasToppingLayer`)과도 어긋난다 — 같은 캔버스가 두
 * 화면에서 다르게 보이면 안 된다.
 *
 * @param imageUrl 서버에 저장된 토핑 이미지 주소.
 * @param editedImagePath 편집을 마치고 나온 알맹이의 로컬 경로. 투명 여백이 걷혀 있고,
 *   테두리는 픽셀에 굽지 않고 [border] 로 따로 나른다(`adr/0025-topping-border-as-server-field.md`).
 *   있으면 [imageUrl] 대신 이걸 그린다 — 아직 서버에 올리기 전이라 이쪽이 최신이다.
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 투명 여백을
 *   걷지 않는다 — 여백이 걷힌 [editedImagePath] 로는 대신할 수 없다.
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
    val editedImagePath: String? = null,
    val cutoutImagePath: String? = null,
)
