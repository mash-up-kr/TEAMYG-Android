import com.teamyg.parfait.buildlogic.setConfigTestAndroid

/**
 * 계측 테스트(`src/androidTest`, 기기/에뮬레이터에서 실행)를 돌리는 모듈에
 * 적용한다. `com.android.application` 또는 `com.android.library`가 먼저
 * 적용돼 있어야 한다.
 *
 * Compose UI를 계측 테스트로 다루려면 [TestComposeConventionPlugin]을 이
 * 플러그인과 함께 적용한다(체인 적용되지 않음, 의도적). 에뮬레이터 없이
 * 순수 JVM 로직만 테스트한다면 [TestUnitConventionPlugin]으로 충분하다.
 */
class TestAndroidConventionPlugin : BaseConventionPlugin({
    setConfigTestAndroid()
})
