plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.dagger.hilt.core)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.core.analytics"
}

dependencies {
    implementation(projects.core.util.jvm)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
