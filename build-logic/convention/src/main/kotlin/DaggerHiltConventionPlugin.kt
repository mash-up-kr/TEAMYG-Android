import com.teamyg.buildlogic.setConfigDaggerHilt
import com.teamyg.buildlogic.utils.extensions.libs

class DaggerHiltConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.dagger.hilt.get().pluginId)
        apply(libs.plugins.kotlin.ksp.get().pluginId)
    }

    setConfigDaggerHilt(useCompose = false)
})
