import com.teamyg.buildlogic.utils.extensions.implementation
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleFeatureApiConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.teamyg.android.library.get().pluginId)
        apply(libs.plugins.kotlin.serialization.get().pluginId)
    }

    dependencies {
        implementation(libs.kotlinx.serialization)
        implementation(libs.bundles.navigation)
    }
})
