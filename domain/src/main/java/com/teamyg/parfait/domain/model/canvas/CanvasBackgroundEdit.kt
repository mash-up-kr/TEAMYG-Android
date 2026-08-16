package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.id.ImageId

/**
 * 캔버스 배경을 바꿀 때 보내는 값. 읽기 모델 [CanvasBackground] 와 짝이지만 같은 타입이 아니다 —
 * 이미지 배경을 **쓸 때는 imageId**, **읽을 때는 URL** 이라 서버 계약이 비대칭이기 때문이다
 * (`api/parfait.md`).
 *
 * 서버 요청은 { type, value, imageId } 평면이고 type 에 따라 value 또는 imageId 가 필수인데
 * 둘 다 널 허용이다. 그 조건부 필수를 sealed 로 세워 **잘못된 조합이 컴파일에서 막히게** 한다 —
 * 평면으로 두면 INVALID_BACKGROUND 를 런타임에야 알게 된다.
 */
sealed interface CanvasBackgroundEdit {
    /**
     * @param hex 서버가 `#` + 6자리만 받는다(3자리 축약·알파 8자리는 거부).
     */
    @JvmInline
    value class Color(val hex: String) : CanvasBackgroundEdit

    /**
     * @param imageId 업로드 확인(`POST /api/v1/images/{imageId}/confirm`)을 마친 이미지여야 한다.
     *   확인 전이면 BACKGROUND_IMAGE_NOT_CONFIRMED 다.
     */
    @JvmInline
    value class Image(val imageId: ImageId) : CanvasBackgroundEdit
}
