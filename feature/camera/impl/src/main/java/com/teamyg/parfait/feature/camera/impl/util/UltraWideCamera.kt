package com.teamyg.parfait.feature.camera.impl.util

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
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

/**
 * 기본 후면 카메라 기준으로 [findUltraWideCamera]를 돌린다. 광학값을 못 읽는 카메라는 후보에서 뺀다.
 */
internal fun ProcessCameraProvider.findBackUltraWideCamera(): LensOptics? = runCatching {
    // filter는 기기가 필요한 정보를 못 주면 IllegalArgumentException을 던진다. 그때는 초광각이 없다고 본다
    val backInfos = availableCameraInfos.filter { it.lensFacing == CameraSelector.LENS_FACING_BACK }
    val main = CameraSelector.DEFAULT_BACK_CAMERA
        .filter(backInfos)
        .firstOrNull()
        ?.toLensOptics() ?: return@runCatching null

    findUltraWideCamera(
        main = main,
        backCameras = backInfos.mapNotNull { it.toLensOptics() },
    )
}.getOrNull()

/** [cameraId] 하나만 고르는 셀렉터 */
@OptIn(ExperimentalCamera2Interop::class)
internal fun cameraSelectorOf(cameraId: String): CameraSelector = CameraSelector
    .Builder()
    .addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == cameraId } }
    .build()

@OptIn(ExperimentalCamera2Interop::class)
private fun CameraInfo.toLensOptics(): LensOptics? {
    val camera2Info = Camera2CameraInfo.from(this)
    val sensorWidth = camera2Info
        .getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        ?.width ?: return null
    val minFocalLength = camera2Info
        .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.minOrNull() ?: return null

    return LensOptics(
        cameraId = camera2Info.cameraId,
        sensorWidthMm = sensorWidth,
        minFocalLengthMm = minFocalLength,
    )
}
