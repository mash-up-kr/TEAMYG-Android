import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.buildlogic.setConfigCompose
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.findByType

class JetpackComposeConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.kotlin.compose.get().pluginId)
    }

    val applicationExtension: ApplicationExtension? = extensions.findByType(ApplicationExtension::class)
    val libraryExtension: LibraryExtension? = extensions.findByType(LibraryExtension::class)

    when {
        applicationExtension != null -> {
            setConfigCompose(applicationExtension)
        }

        libraryExtension != null -> {
            setConfigCompose(libraryExtension)
        }

        else -> error("must be applied com.android.application or com.android.library")
    }
})