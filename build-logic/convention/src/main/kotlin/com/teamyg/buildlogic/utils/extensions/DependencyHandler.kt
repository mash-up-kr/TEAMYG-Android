package com.teamyg.buildlogic.utils.extensions

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.implementation(dependencyNotation: Any) =
    add("implementation", dependencyNotation)

fun DependencyHandler.testImplementation(dependencyNotation: Any) =
    add("testImplementation", dependencyNotation)

fun DependencyHandler.debugImplementation(dependencyNotation: Any) =
    add("debugImplementation", dependencyNotation)

fun DependencyHandler.androidTestImplementation(dependencyNotation: Any) =
    add("androidTestImplementation", dependencyNotation)

fun DependencyHandler.ksp(dependencyNotation: Any) =
    add("ksp", dependencyNotation)

fun DependencyHandler.kspTest(dependencyNotation: Any) =
    add("kspTest", dependencyNotation)

fun DependencyHandler.coreLibraryDesugaring(dependencyNotation: Any) =
    add("coreLibraryDesugaring", dependencyNotation)

fun DependencyHandler.project(
    path: String,
    configuration: String? = null,
): Dependency = project(
    mapOf(
        "path" to path,
        "configuration" to configuration,
    ).filterValues { it != null }
)
