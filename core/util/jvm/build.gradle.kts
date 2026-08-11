import com.teamyg.parfait.buildlogic.utils.extensions.implementation

plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
    alias(libs.plugins.parfait.test.unit)
}

dependencies {
    implementation(libs.kermit)
}
