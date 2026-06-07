import com.teamyg.buildlogic.utils.extensions.libs

class ModuleDomainConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.teamyg.kotlin.jvm.get().pluginId)
    }
})
