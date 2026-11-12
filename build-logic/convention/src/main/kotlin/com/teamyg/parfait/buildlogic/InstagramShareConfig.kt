package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.PropertySettingManager
import org.gradle.api.Project

internal fun Project.setConfigInstagramShare(extension: LibraryExtension) {
    extension.apply {
        buildFeatures {
            buildConfig = true
        }
        val facebookAppId = PropertySettingManager.loadFacebookAppId(
            project = project,
            rootProject = rootProject,
        )
        defaultConfig {
            buildConfigField("String", "FACEBOOK_APP_ID", "\"$facebookAppId\"")
        }
    }
}
