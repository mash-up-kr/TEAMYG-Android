plugins {
    alias(libs.plugins.parfait.android.library)
}

android {
    namespace = "com.teamyg.parfait.core.util"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
