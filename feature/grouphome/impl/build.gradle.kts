plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.grouphome.impl"
}

dependencies {
    implementation(projects.feature.grouphome.api)
}
