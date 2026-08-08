plugins {
    alias(libs.plugins.parfait.module.feature.impl)
}

android {
    namespace = "com.teamyg.parfait.feature.intro.impl"
}

dependencies {
    implementation(projects.feature.intro.api)
    implementation(projects.feature.login.api)
    implementation(projects.feature.groups.list.api)
}
