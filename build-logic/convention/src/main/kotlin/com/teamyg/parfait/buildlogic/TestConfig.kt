package com.teamyg.parfait.buildlogic

import com.teamyg.parfait.buildlogic.utils.extensions.libs
import com.teamyg.parfait.buildlogic.utils.extensions.project
import com.teamyg.parfait.buildlogic.utils.extensions.testImplementation
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.setConfigTestUnit() {
    dependencies {
        testImplementation(libs.bundles.test.unit)
        testImplementation(project(":core:testing"))
    }
}
