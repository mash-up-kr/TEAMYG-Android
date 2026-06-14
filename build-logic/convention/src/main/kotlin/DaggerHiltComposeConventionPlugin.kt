import com.teamyg.parfait.buildlogic.setConfigDaggerHilt
import com.teamyg.parfait.buildlogic.utils.extensions.libs

class DaggerHiltComposeConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.google.dagger.hilt.get().pluginId)
        apply(libs.plugins.google.ksp.get().pluginId)
    }

    setConfigDaggerHilt(useCompose = true)
})
