import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.setConfigAndroidLibrary
import com.teamyg.parfait.buildlogic.setConfigKotlinAndroid
import com.teamyg.parfait.buildlogic.utils.extensions.libs
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
