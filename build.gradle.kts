// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.google.dagger.hilt) apply false

    alias(libs.plugins.ktlint) apply false

    alias(libs.plugins.google.firebase) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}
// CI 가 계측 테스트 모듈을 손으로 나열하지 않게, `parfait.test.android` 적용 모듈을 여기로 모은다
val assembleAllDebugAndroidTest by tasks.registering {
    group = "verification"
}
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    pluginManager.withPlugin(rootProject.libs.plugins.parfait.test.android.get().pluginId) {
        val modulePath = path
        assembleAllDebugAndroidTest.configure { dependsOn("$modulePath:assembleDebugAndroidTest") }
    }
}
apply(from = "gradle/projectDependencyGraph.gradle")
