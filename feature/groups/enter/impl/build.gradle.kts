plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groups.enter.impl"
}

dependencies {
    implementation(projects.feature.groups.enter.api)
}
