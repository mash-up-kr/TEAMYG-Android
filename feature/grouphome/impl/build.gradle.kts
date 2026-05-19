plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.grouphome.impl"
}

dependencies {
    implementation(projects.feature.grouphome.api)
}
