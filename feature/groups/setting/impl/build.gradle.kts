plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.setting.impl"
}

dependencies {
    implementation(projects.feature.groups.setting.api)
}
