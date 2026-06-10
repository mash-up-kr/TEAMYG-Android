plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.gallery.impl"
}

dependencies {
    implementation(projects.feature.gallery.api)
}
