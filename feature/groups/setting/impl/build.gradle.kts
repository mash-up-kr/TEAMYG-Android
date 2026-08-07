plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.setting.impl"
}

dependencies {
    implementation(projects.feature.groups.setting.api)
}
