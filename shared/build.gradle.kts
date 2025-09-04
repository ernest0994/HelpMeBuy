import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Ktor Gradle plugin is intended for server apps and is not compatible with KMP library modules.
    // Removed to avoid the warning: "The Ktor Gradle plugin is not fully compatible with the Kotlin Multiplatform plugin." 
    alias(libs.plugins.ksp)
}

// Work around KLIB resolver duplicate unique_name warnings on iOS metadata
// Exclude Compose internal commonMain klibs that duplicate AndroidX commonMain modules
configurations.all {
    exclude(group = "org.jetbrains.compose", module = "annotation-internal-annotation")
    exclude(group = "org.jetbrains.compose", module = "collection-internal-collection")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Set bundle ID to avoid warnings for iOS frameworks
    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java).configureEach {
        binaries.withType(org.jetbrains.kotlin.gradle.plugin.mpp.Framework::class.java).configureEach {
            baseName = "Shared"
            binaryOptions["bundleId"] = "com.helpmebuyapp.helpmebuy.shared"
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientAuth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.clientCio)
            implementation(libs.kotlin.logging)
            // Compose Multiplatform UI
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
        androidMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.test.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlin.test)
            implementation(libs.junit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.clientCio)
            implementation(libs.kotlin.logging)
        }
        jvmMain.dependencies {
            implementation(libs.logback) // Backend for JVM logging
            implementation(libs.aws.kotlin.dynamodb)
        }
        jvmTest.dependencies {
            implementation(libs.logback) // Backend for JVM tests
        }
    }
}

android {
    namespace = "com.helpmebuyapp.helpmebuy.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

ksp {
    arg("room.schemaLocation", file("schemas").path)
}
