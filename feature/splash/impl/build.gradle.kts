plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.splash.impl"
}

dependencies {
    implementation(projects.feature.splash.api)
    implementation(projects.feature.login.api)
}
