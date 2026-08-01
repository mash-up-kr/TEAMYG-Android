import com.teamyg.parfait.buildlogic.utils.extensions.implementation
import com.teamyg.parfait.buildlogic.utils.extensions.libs
import org.gradle.kotlin.dsl.dependencies

class ModuleDataConventionPlugin : BaseConventionPlugin({
    with(plugins) {
        apply(libs.plugins.parfait.android.library.get().pluginId)
        apply(libs.plugins.parfait.dagger.hilt.core.get().pluginId)
        apply(libs.plugins.parfait.android.network.get().pluginId)
    }

    dependencies {
        implementation(project(":core:util:android"))
        implementation(project(":core:util:jvm"))

        implementation(project(":domain"))

        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.datastore.preferences)

        implementation(libs.kakao.sdk.user)

        implementation(libs.google.mlkit.subject.segmentation)
    }
})
