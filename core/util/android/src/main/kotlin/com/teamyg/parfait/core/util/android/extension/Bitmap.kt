package com.teamyg.parfait.core.util.android.extension

import android.graphics.Bitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap

fun Bitmap.toAndroidBitmap(): AndroidBitmap = AndroidBitmap(delegate = this)
