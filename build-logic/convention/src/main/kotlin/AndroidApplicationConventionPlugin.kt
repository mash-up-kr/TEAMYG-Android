import com.android.build.api.dsl.ApplicationExtension
import com.teamyg.buildlogic.setConfigAndroidApplication
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.android.application.get().pluginId)
    }

    extensions.configure<ApplicationExtension> {
        setConfigAndroidApplication(this)
    }
})
