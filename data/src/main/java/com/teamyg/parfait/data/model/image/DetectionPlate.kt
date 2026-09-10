package com.teamyg.parfait.data.model.image

import android.graphics.Bitmap

/** [ownedByUs] 가 거짓이면 원본이다. 쓰지도 회수하지도 않는다 */
internal class DetectionPlate(val bitmap: Bitmap, val ownedByUs: Boolean)
