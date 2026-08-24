package com.teamyg.parfait.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.model.Key
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

            consumerProguardFiles("consumer-rules.pro")
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
        }
    }
}

internal fun Project.setSigningConfig(extension: ApplicationExtension) {
    val releaseName = "release"
    val debugName = "debug"

    val releaseKey = PropertySettingManager.loadReleaseKey(project = project, rootProject = rootProject)
    val debugKey = PropertySettingManager.loadDebugKey(project = project, rootProject = rootProject)

    extension.signingConfigs {
        create(releaseName) {
            releaseKey?.let { key ->
                storeFile = file(key.storeFile)
                storePassword = key.storePassword
                keyAlias = key.keyAlias
                keyPassword = key.keyPassword
            }
        }

        getByName(debugName) {
            debugKey?.let { key ->
                storeFile = file(key.storeFile)
                storePassword = key.storePassword
                keyAlias = key.keyAlias
                keyPassword = key.keyPassword
            }
        }
    }

    extension.buildTypes {
        getByName(releaseName) {
            signingConfig = extension.signingConfigs.getByName(releaseName)
        }
    }

    failWhenStoreFileMissing(
        configName = releaseName,
        key = releaseKey,
        storeFilePropertyKey = PropertySettingManager.RELEASE_STORE_FILE_KEY,
    )
    failWhenStoreFileMissing(
        configName = debugName,
        key = debugKey,
        storeFilePropertyKey = PropertySettingManager.DEBUG_STORE_FILE_KEY,
    )
}

// 서명이 실제로 필요한 순간(validateSigning*)에만 막는다. 설정 단계에서 터뜨리면 키를 받지
// 못한 사람과 CI 가 ktlint·테스트조차 못 돌린다 — CI 는 키를 주입하지 않는다
private fun Project.failWhenStoreFileMissing(
    configName: String,
    key: Key?,
    storeFilePropertyKey: String,
) {
    val problem = when {
        key == null -> "$storeFilePropertyKey is not set"
        !file(key.storeFile).exists() -> "$storeFilePropertyKey points at ${file(key.storeFile)}, which does not exist"
        else -> return
    }

    val validateTaskName = "validateSigning" + configName.replaceFirstChar(Char::uppercaseChar)

    tasks.matching { task -> task.name == validateTaskName }.configureEach {
        doFirst {
            error("Signing config '$configName' has no keystore: $problem. Set it in local.properties or pass -P$storeFilePropertyKey=...")
        }
    }
}
