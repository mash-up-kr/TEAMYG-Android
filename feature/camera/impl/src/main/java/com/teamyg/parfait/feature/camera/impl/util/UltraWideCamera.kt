package com.teamyg.parfait.feature.camera.impl.util

import com.teamyg.parfait.feature.camera.impl.model.LensOptics

// 초점거리만 보면 매크로·심도 센서도 초광각처럼 보인다. 화각이 메인의 이 비율 이하로 좁아야 초광각으로 친다
private const val ULTRA_WIDE_MAX_FOV_RATIO = 0.7f

/**
 * 메인 후면 카메라보다 화각이 확실히 넓은 후면 카메라 중 가장 넓은 것. 없으면 null.
 */
internal fun findUltraWideCamera(
    main: LensOptics,
    backCameras: List<LensOptics>,
): LensOptics? {
    val threshold = main.fieldOfViewIndex / ULTRA_WIDE_MAX_FOV_RATIO

    return backCameras
        .filter { it.cameraId != main.cameraId && it.fieldOfViewIndex >= threshold }
        .maxByOrNull { it.fieldOfViewIndex }
}
