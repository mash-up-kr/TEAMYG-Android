package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.ksp
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

internal fun Project.setConfigDaggerHilt(useCompose: Boolean) {
    val isAndroidProject: Boolean = extensions.findByType(ApplicationExtension::class) != null ||
        extensions.findByType(LibraryExtension::class) != null

    dependencies {
        if (isAndroidProject) {
            implementation(libs.hilt.android)
        }

        ksp(libs.hilt.compiler)

        if (useCompose) {
            implementation(libs.hilt.navigation)
            implementation(libs.hilt.viewmodel)
        }
    }
}
