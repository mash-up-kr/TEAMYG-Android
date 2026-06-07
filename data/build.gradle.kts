plugins {
    alias(libs.plugins.teamyg.android.library)
    alias(libs.plugins.teamyg.dagger.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.teamyg.data"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.util)

    implementation(libs.bundles.network)
    implementation(libs.kotlinx.serialization)

    implementation(libs.kakao.sdk.user)
}
