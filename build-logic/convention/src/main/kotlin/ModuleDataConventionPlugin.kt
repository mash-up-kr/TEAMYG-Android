import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleDataConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.parfait.android.library.get().pluginId)
        apply(libs.plugins.parfait.dagger.hilt.core.get().pluginId)
        apply(libs.plugins.kotlin.serialization.get().pluginId)
    }

    dependencies {
        implementation(libs.kotlinx.serialization)

        implementation(libs.androidx.datastore.preferences)

        implementation(libs.bundles.network)
        implementation(libs.kakao.sdk.user)
    }
})
