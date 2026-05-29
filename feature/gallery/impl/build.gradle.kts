plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.gallery.impl"
}

dependencies {
    implementation(projects.feature.gallery.api)
}
