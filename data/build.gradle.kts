plugins {
    alias(libs.plugins.teamyg.module.data)
}

android {
    namespace = "com.teamyg.parfait.data"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.util)
}
