package com.teamyg.parfait.data.source.temp.mapper

import com.teamyg.parfait.data.model.dto.TempDto
import com.teamyg.parfait.data.service.model.response.TempResponse

internal fun TempResponse.toTempDto(): TempDto = TempDto(
    id = this.id,
    name = this.name,
)
