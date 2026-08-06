package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.extensions.androidTestImplementation
import com.teamyg.parfait.buildlogic.utils.extensions.debugImplementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import com.teamyg.parfait.buildlogic.utils.extensions.project
import com.teamyg.parfait.buildlogic.utils.extensions.testImplementation
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

internal fun Project.setConfigTestUnit() {
    dependencies {
        testImplementation(libs.bundles.test.unit)
        testImplementation(project(":core:testing"))
    }
}

internal fun Project.setConfigTestAndroid() {
    dependencies {
        androidTestImplementation(libs.bundles.test.android)
        androidTestImplementation(project(":core:testing"))
    }

    val applicationExtension: ApplicationExtension? =
        extensions.findByType(ApplicationExtension::class)
    val libraryExtension: LibraryExtension? =
        extensions.findByType(LibraryExtension::class)

    when {
        applicationExtension != null -> applicationExtension.configureInstrumentationTest()
        libraryExtension != null -> libraryExtension.configureInstrumentationTest()
        else -> error("must be applied com.android.application or com.android.library")
    }
}

private fun ApplicationExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private fun LibraryExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private const val ANDROID_JUNIT_RUNNER = "androidx.test.runner.AndroidJUnitRunner"

internal fun Project.setConfigTestCompose() {
    dependencies {
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)

        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
}
