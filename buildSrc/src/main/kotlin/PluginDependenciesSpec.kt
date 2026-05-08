import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.aliasYG(pluginName: String): PluginDependencySpec =
    id("com.teamyg.plugin.$pluginName")
