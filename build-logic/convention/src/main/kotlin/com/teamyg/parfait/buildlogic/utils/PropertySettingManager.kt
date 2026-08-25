package com.teamyg.parfait.buildlogic.utils

import com.teamyg.parfait.buildlogic.model.Key
import org.gradle.api.Project
import java.util.Properties

internal object PropertySettingManager {
    const val RELEASE_STORE_FILE_KEY = "YG_RELEASE_STORE_FILE"
    const val DEBUG_STORE_FILE_KEY = "YG_DEBUG_STORE_FILE"

    private const val RELEASE_STORE_PASSWORD_KEY = "YG_RELEASE_STORE_PASSWORD"
    private const val RELEASE_KEY_ALIAS_KEY = "YG_RELEASE_KEY_ALIAS"
    private const val RELEASE_KEY_PASSWORD = "YG_RELEASE_KEY_PASSWORD"
    private const val DEBUG_STORE_PASSWORD_KEY = "YG_DEBUG_STORE_PASSWORD"
    private const val DEBUG_KEY_ALIAS_KEY = "YG_DEBUG_KEY_ALIAS"
    private const val DEBUG_KEY_PASSWORD = "YG_DEBUG_KEY_PASSWORD"
    private const val BASE_URL_KEY = "YG_BASE_URL"
    private const val BASE_URL_FALLBACK = "https://TODO.example.com/"

    fun loadReleaseKey(
        project: Project,
        rootProject: Project,
    ): Key? = loadKey(
        project = project,
        rootProject = rootProject,
        storeFileKey = RELEASE_STORE_FILE_KEY,
        storePasswordKey = RELEASE_STORE_PASSWORD_KEY,
        keyAliasKey = RELEASE_KEY_ALIAS_KEY,
        keyPasswordKey = RELEASE_KEY_PASSWORD,
    )

    fun loadDebugKey(
        project: Project,
        rootProject: Project,
    ): Key? = loadKey(
        project = project,
        rootProject = rootProject,
        storeFileKey = DEBUG_STORE_FILE_KEY,
        storePasswordKey = DEBUG_STORE_PASSWORD_KEY,
        keyAliasKey = DEBUG_KEY_ALIAS_KEY,
        keyPasswordKey = DEBUG_KEY_PASSWORD,
    )

    // 키가 없으면 null 을 돌려준다 — 예전처럼 ./error.jks 로 채우면 AGP 가 그 가짜 경로를
    // 그대로 믿어서, 정작 필요한 "어떤 프로퍼티가 비었는지" 를 아무도 말해 주지 않는다
    private fun loadKey(
        project: Project,
        rootProject: Project,
        storeFileKey: String,
        storePasswordKey: String,
        keyAliasKey: String,
        keyPasswordKey: String,
    ): Key? {
        project.findProperty(storeFileKey)?.toString()?.let { storeFile ->
            return Key(
                storeFile = storeFile,
                storePassword = project.findProperty(storePasswordKey)?.toString().orEmpty(),
                keyAlias = project.findProperty(keyAliasKey)?.toString().orEmpty(),
                keyPassword = project.findProperty(keyPasswordKey)?.toString().orEmpty(),
            )
        }

        val properties = rootProject.localProperties() ?: return null
        val storeFile = properties.getProperty(storeFileKey) ?: return null

        return Key(
            storeFile = storeFile,
            storePassword = properties.getProperty(storePasswordKey).orEmpty(),
            keyAlias = properties.getProperty(keyAliasKey).orEmpty(),
            keyPassword = properties.getProperty(keyPasswordKey).orEmpty(),
        )
    }

    fun loadBaseUrl(
        project: Project,
        rootProject: Project,
    ): String {
        project.findProperty(BASE_URL_KEY)?.toString()?.let { return it }
        rootProject.localProperties()?.getProperty(BASE_URL_KEY)?.let { return it }
        return BASE_URL_FALLBACK
    }

    private fun Project.localProperties(): Properties? {
        val file = file("local.properties")
        if (!file.exists()) return null
        return Properties().apply { file.inputStream().use { load(it) } }
    }
}
