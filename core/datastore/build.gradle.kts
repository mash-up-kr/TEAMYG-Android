plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.dagger.hilt.core)
}

android {
    namespace = "com.teamyg.core.datastore"
}

dependencies {
    implementation(projects.core.util)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
