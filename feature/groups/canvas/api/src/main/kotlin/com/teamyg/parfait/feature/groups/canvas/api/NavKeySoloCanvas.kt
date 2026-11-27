package com.teamyg.parfait.feature.groups.canvas.api

import kotlinx.serialization.Serializable

/** 혼자 공간 캔버스. 기기 안에만 하나 있어 실을 인자가 없다 */
@Serializable
data object NavKeySoloCanvas : CanvasHomeNavKey
