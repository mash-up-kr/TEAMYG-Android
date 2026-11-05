import com.teamyg.parfait.buildlogic.setConfigUiSnapshot

/**
 * 모듈의 `@Preview`를 Robolectric으로 렌더해 Roborazzi 스크린샷으로 남긴다.
 * `com.android.library` 모듈에만 적용한다.
 */
class UiSnapshotConventionPlugin : BaseConventionPlugin({
    setConfigUiSnapshot()
})
