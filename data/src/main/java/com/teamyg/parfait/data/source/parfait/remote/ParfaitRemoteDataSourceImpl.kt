package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.source.parfait.mapper.toPastCanvasVOList
import com.teamyg.parfait.data.source.parfait.mapper.toTodayCanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.canvas.TodayCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class ParfaitRemoteDataSourceImpl @Inject constructor(
    private val parfaitService: ParfaitService,
    private val apiCaller: ApiCaller,
) : ParfaitRemoteDataSource {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = apiCaller
        .safeApiCall(
            block = { parfaitService.getGroupsByGroupIdParfaitsYear(groupId.value) },
            transform = { it.years },
        )

    override suspend fun getTodayCanvas(groupId: GroupId): Result<TodayCanvasVO> = apiCaller.safeApiCall(
        block = { parfaitService.getGroupsByGroupIdParfaitsToday(groupId.value) },
        transform = { it.toTodayCanvasVO() },
    )

    override suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate?,
        to: LocalDate?,
    ): Result<List<PastCanvasVO>> = apiCaller.safeApiCall(
        block = {
            parfaitService.getGroupsByGroupIdParfaits(
                groupId = groupId.value,
                from = from?.toString(),
                to = to?.toString(),
            )
        },
        transform = { it.toPastCanvasVOList() },
    )
}
