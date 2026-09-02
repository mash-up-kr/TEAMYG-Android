import com.android.build.api.dsl.ApplicationExtension
import com.teamyg.parfait.buildlogic.setSigningConfig
import org.gradle.kotlin.dsl.findByType

class AndroidApplicationSigningConventionPlugin : BaseConventionPlugin({
    val applicationExtension: ApplicationExtension = extensions.findByType(ApplicationExtension::class)
        ?: error("must be applied com.android.application")

    setSigningConfig(applicationExtension)
})
