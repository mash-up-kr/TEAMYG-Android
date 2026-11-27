package com.teamyg.parfait.feature.groups.canvas.api

import kotlinx.serialization.Serializable

/** 혼자 공간 배경 편집. 기기 안에 캔버스가 하나뿐이라 실을 인자가 없다 */
@Serializable
data object NavKeySoloBGEdit : CanvasBGEditNavKey
