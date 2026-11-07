plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.app.notice.impl"
}

dependencies {
    implementation(projects.feature.app.notice.api)
}
