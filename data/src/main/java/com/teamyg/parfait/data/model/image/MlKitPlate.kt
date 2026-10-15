package com.teamyg.parfait.data.model.image

import android.graphics.Bitmap
import com.teamyg.parfait.domain.model.SegmentationBounds

/** ML Kit 이 내준 판 한 장. 검출 공간이 곧 원본 공간이다 */
internal class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds)
