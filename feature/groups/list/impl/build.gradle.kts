plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.list.impl"
}

dependencies {
    implementation(projects.feature.groups.list.api)
    implementation(projects.feature.groups.enter.api)
    implementation(projects.feature.app.setting.api)
}
