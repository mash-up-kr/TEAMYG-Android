plugins {
    `kotlin-dsl`
}

group = "com.teamyg.buildlogic"

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
        // TODO register plugins
    }
}

private fun NamedDomainObjectContainer<PluginDeclaration>.pluginRegister(
    pluginName: String,
    className: String,
) {
    register(pluginName) {
        id = "com.teamyg.plugin.$pluginName"
        implementationClass = "${className}ConventionPlugin"
    }
}
