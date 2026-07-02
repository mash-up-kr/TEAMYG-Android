plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.home.impl"
}

dependencies {
    implementation(projects.feature.groups.home.api)
}
