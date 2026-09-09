package com.teamyg.parfait.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceInfoTest {
    @Test
    fun buildDeviceInfo_givenBuildValues_assemblesEachProperty() {
        // Given, When
        val info = buildDeviceInfo(
            osVer = "14",
            appVer = "1.1.1",
            appVerCode = 8,
            manufacturer = "samsung",
            model = "SM-S928N",
            appId = "com.teamyg.parfait",
        )

        // Then 코드는 문자열로 싣는다 — 사용자 속성 값은 String 만 받는다
        assertEquals("Android", info.osType)
        assertEquals("14", info.osVer)
        assertEquals("1.1.1", info.appVer)
        assertEquals("8", info.appVerCode)
        assertEquals("samsung SM-S928N", info.device)
        assertEquals("com.teamyg.parfait", info.appId)
    }

    @Test
    fun buildDeviceInfo_longManufacturerAndModel_truncatesDeviceToLimit() {
        // Given 제조사와 모델을 이으면 사용자 속성 값 상한을 넘는 기기
        val info = buildDeviceInfo(
            osVer = "14",
            appVer = "1.1.1",
            appVerCode = 8,
            manufacturer = "VeryLongManufacturerName",
            model = "VeryLongModelIdentifier-2026",
            appId = "com.teamyg.parfait",
        )

        // Then GA4 가 조용히 자르기 전에 우리가 자른다
        assertEquals(USER_PROPERTY_VALUE_MAX_LENGTH, info.device.length)
        assertEquals("VeryLongManufacturerName VeryLongMod", info.device)
    }
}
