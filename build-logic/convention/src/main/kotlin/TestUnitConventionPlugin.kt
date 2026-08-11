import com.teamyg.parfait.buildlogic.setConfigTestUnit

/**
 * JVM 유닛 테스트(`src/test`)를 돌리는 모듈에 적용한다 — 에뮬레이터/기기가 필요 없는
 * 로직 테스트가 대상이다.
 *
 * 계측 테스트가 필요하면 [TestAndroidConventionPlugin]을, 그 위에 Compose UI
 * 계측 테스트까지 필요하면 [TestComposeConventionPlugin]도 함께 적용한다. 세
 * 플러그인은 서로 체인 적용하지 않으므로(의도적) 필요한 조합을 모듈의
 * `build.gradle.kts`에서 직접 나열해야 한다.
 */
class TestUnitConventionPlugin : BaseConventionPlugin({
    setConfigTestUnit()
})
