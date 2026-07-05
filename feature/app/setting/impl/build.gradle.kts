plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.app.setting.impl"
}

dependencies {
    implementation(projects.feature.app.setting.api)
}
