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
    private val etags = MutableStateFlow<Map<GroupId, String>>(emptyMap())

    /**
     * 맵 전체가 아니라 한 그룹으로 좁혀서 낸다 — [distinctUntilChanged] 가 없으면 남의 그룹
     * 캔버스가 저장될 때마다 이 구독자까지 재방출된다.
     */
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvases
        .map { it[groupId] }
        .distinctUntilChanged()

    override fun cachedTodayCanvas(groupId: GroupId): CanvasVO? = canvases.value[groupId]

    override fun cachedTodayEtag(groupId: GroupId): String? = etags.value[groupId]

    override fun saveTodayCanvas(
        groupId: GroupId,
        canvas: CanvasVO,
        etag: String?,
    ) {
        // 구독자가 새 캔버스를 받는 시점에 짝이 되는 ETag 가 이미 있어야 해서 캔버스보다 먼저 쓴다
        etags.update { if (etag == null) it - groupId else it + (groupId to etag) }
        canvases.update { it + (groupId to canvas) }
    }

    override fun clear() {
        etags.value = emptyMap()
        canvases.value = emptyMap()
    }
}
