plugins {
    alias(libs.plugins.parfait.module.data)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
}

android {
    namespace = "com.teamyg.parfait.data"
}
