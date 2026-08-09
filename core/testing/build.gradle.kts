// 테스트 클래스패스 전용 모듈. 프로덕션 코드에서 끌어오면 안 된다.
//
// MainDispatcherRule 은 TestWatcher 를 상속하고 TestDispatcher 를 노출한다. 그래서 이 룰을
// 쓰는 쪽도 junit4 와 coroutines-test 가 있어야 컴파일된다. 여기서 api 로 넘겨주지 않으니
// 소비자가 직접 갖춰야 하고, `parfait.test.unit` 의 bundles.test-unit 이 그걸 넣어준다.
// 번들에서 둘 중 하나를 빼면 이 모듈을 쓰는 테스트가 깨진다.
plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
}

dependencies {
    implementation(libs.junit4)
    implementation(libs.kotlinx.coroutines.test)
}
