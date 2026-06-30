plugins {
    alias(libs.plugins.parfait.android.library)
}

android {
    namespace = "com.teamyg.parfait.core.util.android"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
