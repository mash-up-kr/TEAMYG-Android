package com.teamyg.parfait.data.source.parfait.local

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanvasLocalDataSourceImpl @Inject constructor() : CanvasLocalDataSource {
    private val canvases = MutableStateFlow<Map<GroupId, CanvasVO>>(emptyMap())

    /**
     * 맵 전체가 아니라 한 그룹으로 좁혀서 낸다 — [distinctUntilChanged] 가 없으면 남의 그룹
     * 캔버스가 저장될 때마다 이 구독자까지 재방출된다.
     */
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvases
        .map { it[groupId] }
        .distinctUntilChanged()

    override fun cachedTodayCanvas(groupId: GroupId): CanvasVO? = canvases.value[groupId]

    override fun saveTodayCanvas(
        groupId: GroupId,
        canvas: CanvasVO,
    ) {
        canvases.update { it + (groupId to canvas) }
    }

    override fun clear() {
        canvases.value = emptyMap()
    }
}
