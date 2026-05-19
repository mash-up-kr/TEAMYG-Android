plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.login.impl"
}

dependencies {
    implementation(projects.feature.login.api)
}
