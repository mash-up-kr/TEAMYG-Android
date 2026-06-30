package com.teamyg.parfait.core.util.android.model

import android.graphics.Bitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper

@JvmInline
value class AndroidBitmap
internal constructor(
    private val delegate: Bitmap,
) : BitmapWrapper {
    // TODO delegate 사용하도록 수정

    fun getRawData(): Bitmap = this.delegate
}
