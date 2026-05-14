import com.android.build.api.dsl.LibraryExtension
import com.teamyg.buildlogic.setConfigAndroidLibrary
import com.teamyg.buildlogic.setConfigKotlinAndroid
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.android.library.get().pluginId)
    }

    extensions.configure<LibraryExtension> {
        setConfigAndroidLibrary(this)
        setConfigKotlinAndroid()
    }
})
