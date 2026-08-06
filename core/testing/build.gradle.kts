// 테스트 클래스패스 전용 모듈. JUnit/coroutines-test 를 재노출(api)할 뿐이므로
// 프로덕션 코드에서 implementation 으로 끌어와서는 안 된다.
plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
}

dependencies {
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
