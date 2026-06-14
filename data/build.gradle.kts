plugins {
    alias(libs.plugins.parfait.module.data)
}

android {
    namespace = "com.teamyg.parfait.data"
}

dependencies {
    implementation(projects.domain)
    implementation(projects.core.util)
}
