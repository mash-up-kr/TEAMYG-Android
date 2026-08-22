package com.teamyg.parfait.core.util.android.permission

import android.Manifest
import android.content.Context
import android.os.Build
import com.teamyg.parfait.core.util.android.extension.isGrantedPermission

/** API 29+는 자기 앱이 만든 MediaStore 항목을 쓰는 데 권한이 필요 없다 — 그 아래에서만 확인한다. */
object GalleryWritePermissionManager {
    const val PERMISSION: String = Manifest.permission.WRITE_EXTERNAL_STORAGE

    private val isRequired: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun hasPermission(context: Context): Boolean = !isRequired || context.isGrantedPermission(PERMISSION)
}
