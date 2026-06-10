import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleFeatureImplConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.teamyg.android.library.get().pluginId)
        apply(libs.plugins.teamyg.jetpack.compose.get().pluginId)
        apply(libs.plugins.teamyg.dagger.hilt.compose.get().pluginId)
    }

    dependencies {
        implementation(project(":core:designsystem"))
        implementation(project(":core:ui"))
        implementation(project(":core:util"))
        implementation(project(":core:navigation"))

        implementation(project(":domain"))

        implementation(libs.bundles.navigation)
    }
})
