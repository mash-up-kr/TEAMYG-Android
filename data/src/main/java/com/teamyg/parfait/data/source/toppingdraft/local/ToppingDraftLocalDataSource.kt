package com.teamyg.parfait.data.source.toppingdraft.local

import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow

interface ToppingDraftLocalDataSource {
    /** 흐름 밖이면 null 이다 */
    val draft: Flow<ToppingDraft?>

    /** 병합하지 않고 통째로 덮어쓴다 */
    suspend fun save(draft: ToppingDraft)

    suspend fun clear()
}
