plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy {
            // Force a single Compose Multiplatform version to avoid mixed 1.8.2/1.9.0-beta KLIBs in commonMain
            force("org.jetbrains.compose.runtime:runtime:${libs.versions.composeMultiplatform.get()}")
            force("org.jetbrains.compose.foundation:foundation:${libs.versions.composeMultiplatform.get()}")
            force("org.jetbrains.compose.ui:ui:${libs.versions.composeMultiplatform.get()}")
            force("org.jetbrains.compose.material3:material3:${libs.versions.composeMultiplatform.get()}")
        }
    }
}