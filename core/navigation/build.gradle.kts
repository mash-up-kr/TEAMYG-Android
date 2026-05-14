plugins {
    alias(libs.plugins.teamyg.android.library)

}

android {
    namespace = "com.teamyg.navigation"
}

dependencies {
    implementation(libs.bundles.navigation)
}
