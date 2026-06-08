plugins {
    alias(libs.plugins.teamyg.module.data)
}

android {
    namespace = "com.teamyg.data"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.util)
}
