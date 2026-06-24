package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.PropertySettingManager
import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import kotlin.text.toInt

internal fun Project.setConfigAndroidApplication(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig {
            minSdk = libs.versions.minSdk.get().toInt()
            targetSdk = libs.versions.targetSdk.get().toInt()

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isMinifyEnabled = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

internal fun Project.setConfigAndroidLibrary(extension: LibraryExtension) {
    extension.apply {
        compileSdk = libs.versions.compileSdk.get().toInt()

        defaultConfig {
            minSdk = libs.versions.minSdk.get().toInt()

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

internal fun Project.setConfigKotlinAndroid() {
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.datetime)

            implementation(libs.kermit)
        }
    }
}

internal fun Project.setSigningConfig(extension: ApplicationExtension) {
    extension.signingConfigs {
        val releaseName = "release"
        val debugName = "debug"

        val releaseKey = PropertySettingManager.loadReleaseKey(
            project = project,
            rootProject = rootProject,
        )

        val debugKey = PropertySettingManager.loadDebugKey(
            project = project,
            rootProject = rootProject,
        )

        create(releaseName) {
            storeFile = file(releaseKey.storeFile)
            storePassword = releaseKey.storePassword
            keyAlias = releaseKey.keyAlias
            keyPassword = releaseKey.keyPassword
        }

        getByName(debugName) {
            storeFile = file(debugKey.storeFile)
            storePassword = debugKey.storePassword
            keyAlias = debugKey.keyAlias
            keyPassword = debugKey.keyPassword
        }
    }
}
