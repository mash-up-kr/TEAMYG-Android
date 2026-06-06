import com.teamyg.buildlogic.setConfigKotlinJvm
import com.teamyg.buildlogic.utils.extensions.libs

class KotlinJvmConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply("java-library")
        apply(libs.plugins.jetbrains.kotlin.jvm.get().pluginId)
    }

    setConfigKotlinJvm()
})
