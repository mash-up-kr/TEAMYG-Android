plugins {
    alias(libs.plugins.teamyg.android.library)
}

android {
    namespace = "com.teamyg.core.datastore"
}

dependencies {
    implementation(projects.core.util)
}
