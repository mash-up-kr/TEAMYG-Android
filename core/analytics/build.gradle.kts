plugins {
    alias(libs.plugins.teamyg.android.library)
}

android {
    namespace = "com.teamyg.analytics"
}

dependencies {
    implementation(projects.core.util)
}
