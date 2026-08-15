plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.app.setting.impl"
}

dependencies {
    implementation(projects.feature.app.setting.api)
    implementation(projects.feature.common.terms.api)
    implementation(projects.feature.login.api)
}
