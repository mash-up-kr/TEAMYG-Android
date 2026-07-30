package com.teamyg.parfait.data.source.temp.mapper

import com.teamyg.parfait.data.service.model.response.TempResponse
import com.teamyg.parfait.domain.model.TempVO

internal fun TempResponse.toTempVO(): TempVO = TempVO(
    id = this.id,
    name = this.name,
)
