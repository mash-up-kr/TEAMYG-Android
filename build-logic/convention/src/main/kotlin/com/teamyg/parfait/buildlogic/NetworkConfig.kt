package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.PropertySettingManager
import org.gradle.api.Project

internal fun Project.setConfigNetwork(extension: LibraryExtension) {
    extension.apply {
        buildFeatures {
            buildConfig = true
        }
        val baseUrl = PropertySettingManager.loadBaseUrl(
            project = project,
            rootProject = rootProject,
        )
        defaultConfig {
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        }
    }
}
