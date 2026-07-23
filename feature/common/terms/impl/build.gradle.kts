plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.common.terms.impl"
}

dependencies {
    implementation(projects.feature.common.terms.api)
}
