import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleDomainConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.parfait.kotlin.jvm.get().pluginId)
    }

    dependencies {
        implementation(project(":core:util:jvm"))

        implementation(libs.javax.inject)
    }
})
