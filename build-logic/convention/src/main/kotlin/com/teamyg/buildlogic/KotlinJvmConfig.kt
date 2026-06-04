package com.teamyg.buildlogic

import com.teamyg.buildlogic.utils.extensions.implementation
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.setConfigKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    dependencies {
        implementation(libs.javax.inject)
        implementation(libs.kotlinx.datetime)
    }
}
