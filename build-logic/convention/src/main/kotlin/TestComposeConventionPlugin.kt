import com.teamyg.parfait.buildlogic.setConfigTestCompose

/**
 * Compose UI 계측 테스트에 필요한 의존성(`ui-test-junit4`, `ui-test-manifest`)만
 * 추가한다. [TestAndroidConventionPlugin]을 대체하지 않으므로 **항상 그 플러그인과
 * 함께** 적용해야 한다 — 계측 러너·매니페스트 설정은 여전히 그쪽 책임이다.
 *
 * 두 플러그인은 서로 체인 적용하지 않는다(의도적). JVM 유닛 테스트만 필요하면
 * [TestUnitConventionPlugin]을 대신 쓴다.
 */
class TestComposeConventionPlugin : BaseConventionPlugin({
    setConfigTestCompose()
})
