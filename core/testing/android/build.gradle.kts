// `parfait.ui.snapshot` 이 생성하는 프리뷰 스크린샷 테스트(Robolectric 호스트 테스트)의 클래스패스 전용 모듈.
// 프로덕션 코드에서 끌어오면 안 된다.
plugins {
    alias(libs.plugins.parfait.android.library)
}

android {
    namespace = "com.teamyg.parfait.core.testing.android"
}

dependencies {
    implementation(libs.roborazzi.compose.preview.scanner.support)
    implementation(libs.robolectric)
    // compileOnly 가 아니다. 테스트 런타임에 LottieTask.EXECUTOR 를 바꿔 끼운다
    implementation(libs.lottie.compose)
}
