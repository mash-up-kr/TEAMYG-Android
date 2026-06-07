import com.teamyg.buildlogic.utils.extensions.implementation
import com.teamyg.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleDomainConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.teamyg.kotlin.jvm.get().pluginId)
    }

    dependencies {
        implementation(libs.javax.inject)
    }
})
