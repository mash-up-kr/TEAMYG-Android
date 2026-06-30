plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.groupenter.impl"
}

dependencies {
    implementation(projects.feature.groupenter.api)
}
