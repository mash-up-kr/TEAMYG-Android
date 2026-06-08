import com.teamyg.buildlogic.setConfigDaggerHilt
import com.teamyg.buildlogic.utils.extensions.libs

class DaggerHiltCoreConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.google.dagger.hilt.get().pluginId)
        apply(libs.plugins.google.ksp.get().pluginId)
    }

    setConfigDaggerHilt(useCompose = false)
})
