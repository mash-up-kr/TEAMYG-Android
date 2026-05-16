plugins {
    alias(libs.plugins.teamyg.module.feature.impl)
}

android {
    namespace = "com.teamyg.feature.sample"
}

dependencies {
    implementation(libs.bundles.navigation)
}
