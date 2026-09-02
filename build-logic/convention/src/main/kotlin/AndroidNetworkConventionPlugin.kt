import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.setConfigNetwork
import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class AndroidNetworkConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.kotlin.serialization.get().pluginId)
    }

    val libraryExtension: LibraryExtension = extensions.findByType(LibraryExtension::class)
        ?: error("must be applied com.android.library")

    setConfigNetwork(libraryExtension)

    dependencies {
        implementation(libs.bundles.network)
        implementation(libs.kotlinx.serialization)
    }
})
