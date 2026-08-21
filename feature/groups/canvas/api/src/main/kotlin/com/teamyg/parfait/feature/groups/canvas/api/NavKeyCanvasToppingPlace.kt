package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 배치할 토핑은 초안이 나른다(`adr/0026-topping-draft-datastore-ssot.md`) */
@Serializable
data object NavKeyCanvasToppingPlace : NavKey
