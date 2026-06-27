plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.login.impl"
}

dependencies {
    implementation(projects.feature.login.api)
    implementation(projects.feature.grouphome.api)
    implementation(libs.kakao.sdk.user)
}
