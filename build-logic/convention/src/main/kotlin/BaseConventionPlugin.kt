import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class BaseConventionPlugin(
    private val block: Project.() -> Unit,
) : Plugin<Project> {

    final override fun apply(target: Project) {
        with(
            receiver = target,
            block = block,
        )
    }
}
