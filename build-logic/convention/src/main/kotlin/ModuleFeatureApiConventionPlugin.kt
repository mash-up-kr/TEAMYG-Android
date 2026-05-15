import com.teamyg.buildlogic.utils.extensions.libs

class ModuleFeatureApiConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.teamyg.android.library.get().pluginId)
    }
})
