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
// 계측 테스트를 가진 모듈을 손으로 나열하면 새 모듈이 빠진다. `parfait.test.android` 를 적용한
// 모듈이 스스로 여기에 걸리게 해서, CI 가 이 태스크 하나만 부르면 되게 한다.
// 에뮬레이터가 필요 없도록 테스트 APK 조립까지만 한다 — 실행은 하지 않는다.
val assembleAllDebugAndroidTest by tasks.registering {
    group = "verification"
    description = "Assembles debug instrumented test APKs of every module applying parfait.test.android."
}
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    pluginManager.withPlugin(rootProject.libs.plugins.parfait.test.android.get().pluginId) {
        val modulePath = path
        assembleAllDebugAndroidTest.configure { dependsOn("$modulePath:assembleDebugAndroidTest") }
    }
}
apply(from = "gradle/projectDependencyGraph.gradle")
