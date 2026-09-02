import com.teamyg.parfait.buildlogic.setConfigKotlinJvm
import com.teamyg.parfait.buildlogic.utils.extensions.libs

class KotlinJvmConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply("java-library")
        apply(libs.plugins.kotlin.jvm.get().pluginId)
    }

    setConfigKotlinJvm()
})
