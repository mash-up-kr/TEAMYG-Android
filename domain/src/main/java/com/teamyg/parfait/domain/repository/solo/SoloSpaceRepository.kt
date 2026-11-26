package com.teamyg.parfait.domain.repository.solo

import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.solo.SoloBackgroundEdit
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import kotlinx.coroutines.flow.Flow

/**
 * 기기 안에만 저장하는 혼자 공간. 서버를 부르지 않는다.
 *
 * 쓰기 결과는 `Unit`이다. 화면은 [canvas] 하나만 보고 그린다.
 */
interface SoloSpaceRepository {
    /** 처음 구독하면 파일을 읽는다. 파일이 없으면 빈 캔버스다. 읽기·파싱 실패는 실패로 흘린다(덮어쓰지 않는다) */
    val canvas: Flow<Result<SoloCanvasVO>>

    suspend fun changeBackground(edit: SoloBackgroundEdit): Result<Unit>

    /** [filePath]의 파일을 혼자 공간으로 복사해 배치한다. 올리기와 배치가 한 번에 끝난다 */
    suspend fun placeTopping(filePath: String, transform: ToppingTransform, border: ToppingBorder): Result<Unit>

    suspend fun updateTransforms(
        updates: List<ToppingTransformUpdate>,
        textUpdates: List<TextToppingTransformUpdate>,
    ): Result<Unit>

    suspend fun updateBorder(id: ParfaitImageId, border: ToppingBorder): Result<Unit>

    suspend fun deleteTopping(id: ParfaitImageId): Result<Unit>

    suspend fun placeText(text: String, transform: ToppingTransform, style: TextToppingStyle): Result<Unit>

    suspend fun deleteText(id: ParfaitTextId): Result<Unit>

    /** 배경·토핑·텍스트를 전부 비운다. 손상된 파일도 읽지 않고 빈 기록으로 덮는다 */
    suspend fun reset(): Result<Unit>

    /** 통째로 지운다. 다음에 열면 빈 캔버스다 */
    suspend fun delete(): Result<Unit>
}
