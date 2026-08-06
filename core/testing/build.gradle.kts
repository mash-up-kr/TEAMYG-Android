plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
}

dependencies {
    api(projects.domain)

    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
