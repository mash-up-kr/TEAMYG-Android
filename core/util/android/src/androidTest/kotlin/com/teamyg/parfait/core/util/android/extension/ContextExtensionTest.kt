package com.teamyg.parfait.core.util.android.extension

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ContextExtensionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun buildAppSettingsIntent_anyContext_targetsApplicationDetailsSettings() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then 앱 상세 설정 화면을 가리킨다
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    }

    @Test
    fun buildAppSettingsIntent_anyContext_encodesOwnPackageName() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then data 에 자기 패키지명이 담긴다
        assertEquals("package", intent.data?.scheme)
        assertEquals(context.packageName, intent.data?.schemeSpecificPart)
    }

    @Test
    fun buildAppSettingsIntent_anyContext_setsNewTaskFlag() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then NEW_TASK 플래그가 설정된다
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
