package com.teamyg.parfait.analytics

import android.os.Build
import com.teamyg.parfait.BuildConfig

/** GA4 사용자 속성 값의 상한 */
const val USER_PROPERTY_VALUE_MAX_LENGTH = 36

private const val OS_TYPE_ANDROID = "Android"

data class DeviceInfo(
    val osType: String,
    val osVer: String,
    val appVer: String,
    val appVerCode: String,
    val device: String,
    val appId: String,
)

/** `Build`·`BuildConfig` 를 읽지 않아 JVM 유닛으로 덮인다 */
fun buildDeviceInfo(
    osVer: String,
    appVer: String,
    appVerCode: Int,
    manufacturer: String,
    model: String,
    appId: String,
): DeviceInfo = DeviceInfo(
    osType = OS_TYPE_ANDROID,
    osVer = osVer,
    appVer = appVer,
    appVerCode = appVerCode.toString(),
    device = "$manufacturer $model".take(USER_PROPERTY_VALUE_MAX_LENGTH),
    appId = appId,
)

fun currentDeviceInfo(): DeviceInfo = buildDeviceInfo(
    osVer = Build.VERSION.RELEASE,
    appVer = BuildConfig.VERSION_NAME,
    appVerCode = BuildConfig.VERSION_CODE,
    manufacturer = Build.MANUFACTURER,
    model = Build.MODEL,
    appId = BuildConfig.APPLICATION_ID,
)
