plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.enter.impl"
}

dependencies {
    implementation(projects.feature.groups.enter.api)
    implementation(projects.feature.groups.canvas.api)
}
