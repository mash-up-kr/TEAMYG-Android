plugins {
    `kotlin-dsl`
}

group = "com.teamyg.parfait.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)

    compileOnly(files((libs as Any).javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        pluginRegister(
            pluginName = "android.application",
            className = "AndroidApplication",
        )
        pluginRegister(
            pluginName = "android.application.signing",
            className = "AndroidApplicationSigning",
        )
        pluginRegister(
            pluginName = "android.library",
            className = "AndroidLibrary",
        )
        pluginRegister(
            pluginName = "dagger.hilt.core",
            className = "DaggerHiltCore",
        )
        pluginRegister(
            pluginName = "dagger.hilt.compose",
            className = "DaggerHiltCompose",
        )
        pluginRegister(
            pluginName = "jetpack.compose",
            className = "JetpackCompose",
        )
        pluginRegister(
            pluginName = "kotlin.jvm",
            className = "KotlinJvm",
        )
        pluginRegister(
            pluginName = "module.data",
            className = "ModuleData",
        )
        pluginRegister(
            pluginName = "module.domain",
            className = "ModuleDomain",
        )
        pluginRegister(
            pluginName = "module.feature.impl",
            className = "ModuleFeatureImpl",
        )
        pluginRegister(
            pluginName = "module.feature.api",
            className = "ModuleFeatureApi",
        )
    }
}

private fun NamedDomainObjectContainer<PluginDeclaration>.pluginRegister(
    pluginName: String,
    className: String,
) {
    register(pluginName) {
        id = "com.teamyg.parfait.plugin.$pluginName"
        implementationClass = "${className}ConventionPlugin"
    }
}
