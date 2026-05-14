package com.teamyg.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.buildlogic.utils.extensions.debugImplementation
import com.teamyg.buildlogic.utils.extensions.implementation
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.setConfigCompose(extension: ApplicationExtension) {
    extension.apply {
        buildFeatures {
            compose = true
        }
    }

    setComposeDependencies()
}

internal fun Project.setConfigCompose(extension: LibraryExtension) {
    extension.apply {
        buildFeatures {
            compose = true
        }
    }

    setComposeDependencies()
}

private fun Project.setComposeDependencies() {
    dependencies {
        implementation(libs.androidx.activity.compose)

        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)

        debugImplementation(libs.androidx.compose.ui.tooling)

        implementation(libs.androidx.lifecycle.viewmodel.compose)

        implementation(libs.coil.compose)
    }
}
