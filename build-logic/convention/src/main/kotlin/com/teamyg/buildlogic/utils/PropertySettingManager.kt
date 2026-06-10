package com.teamyg.buildlogic.utils

import com.teamyg.buildlogic.model.Key
import org.gradle.api.Project
import java.util.Properties

internal object PropertySettingManager {
    private const val RELEASE_STORE_FILE_KEY = "YG_RELEASE_STORE_FILE"
    private const val RELEASE_STORE_PASSWORD_KEY = "YG_RELEASE_STORE_PASSWORD"
    private const val RELEASE_KEY_ALIAS_KEY = "YG_RELEASE_KEY_ALIAS"
    private const val RELEASE_KEY_PASSWORD = "YG_RELEASE_KEY_PASSWORD"
    private const val DEBUG_STORE_FILE_KEY = "YG_DEBUG_STORE_FILE"
    private const val DEBUG_STORE_PASSWORD_KEY = "YG_DEBUG_STORE_PASSWORD"
    private const val DEBUG_KEY_ALIAS_KEY = "YG_DEBUG_KEY_ALIAS"
    private const val DEBUG_KEY_PASSWORD = "YG_DEBUG_KEY_PASSWORD"

    fun loadReleaseKey(
        project: Project,
        rootProject: Project,
    ): Key {
        if (project.findProperty(RELEASE_STORE_FILE_KEY) != null) {
            val storeFile = project.findProperty(RELEASE_STORE_FILE_KEY)?.toString() ?: "./error.jks"
            val storePassword = project.findProperty(RELEASE_STORE_PASSWORD_KEY)?.toString().orEmpty()
            val keyAlias = project.findProperty(RELEASE_KEY_ALIAS_KEY)?.toString().orEmpty()
            val keyPassword = project.findProperty(RELEASE_KEY_PASSWORD)?.toString().orEmpty()

            return Key(
                storeFile = storeFile,
                storePassword = storePassword,
                keyAlias = keyAlias,
                keyPassword = keyPassword,
            )
        }

        val file = rootProject.file("local.properties")

        if (file.exists()) {
            with(
                Properties().apply {
                    load(file.inputStream())
                },
            ) {
                if (this.getProperty(RELEASE_STORE_FILE_KEY) != null) {
                    val storeFile = this.getProperty(RELEASE_STORE_FILE_KEY) ?: "./error.jks"
                    val storePassword = this.getProperty(RELEASE_STORE_PASSWORD_KEY).orEmpty()
                    val keyAlias = this.getProperty(RELEASE_KEY_ALIAS_KEY).orEmpty()
                    val keyPassword = this.getProperty(RELEASE_KEY_PASSWORD).orEmpty()

                    return Key(
                        storeFile = storeFile,
                        storePassword = storePassword,
                        keyAlias = keyAlias,
                        keyPassword = keyPassword,
                    )
                }
            }
        }

        return Key(
            storeFile = "./error.jks",
            storePassword = "",
            keyAlias = "",
            keyPassword = "",
        )
    }

    fun loadDebugKey(
        project: Project,
        rootProject: Project,
    ): Key {
        if (project.findProperty(DEBUG_STORE_FILE_KEY) != null) {
            val storeFile = project.findProperty(DEBUG_STORE_FILE_KEY)?.toString() ?: "./error.jks"
            val storePassword = project.findProperty(DEBUG_STORE_PASSWORD_KEY)?.toString().orEmpty()
            val keyAlias = project.findProperty(DEBUG_KEY_ALIAS_KEY)?.toString().orEmpty()
            val keyPassword = project.findProperty(DEBUG_KEY_PASSWORD)?.toString().orEmpty()

            return Key(
                storeFile = storeFile,
                storePassword = storePassword,
                keyAlias = keyAlias,
                keyPassword = keyPassword,
            )
        }

        val file = rootProject.file("local.properties")

        if (file.exists()) {
            with(
                Properties().apply {
                    load(file.inputStream())
                },
            ) {
                if (this.getProperty(DEBUG_STORE_FILE_KEY) != null) {
                    val storeFile = this.getProperty(DEBUG_STORE_FILE_KEY) ?: "./error.jks"
                    val storePassword = this.getProperty(DEBUG_STORE_PASSWORD_KEY).orEmpty()
                    val keyAlias = this.getProperty(DEBUG_KEY_ALIAS_KEY).orEmpty()
                    val keyPassword = this.getProperty(DEBUG_KEY_PASSWORD).orEmpty()

                    return Key(
                        storeFile = storeFile,
                        storePassword = storePassword,
                        keyAlias = keyAlias,
                        keyPassword = keyPassword,
                    )
                }
            }
        }

        return Key(
            storeFile = "./error.jks",
            storePassword = "",
            keyAlias = "",
            keyPassword = "",
        )
    }
}
