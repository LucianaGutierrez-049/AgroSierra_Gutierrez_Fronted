import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

sqldelight { databases { create("AgroDb") { packageName.set("pe.edu.upeu.agrosierra_gutierrez.db") } } }

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    android {
       namespace = "pe.edu.upeu.agrosierra_gutierrez.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    sourceSets {
        iosMain.dependencies { implementation("app.cash.sqldelight:native-driver:2.1.0") }
        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.1.0")
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("app.cash.sqldelight:runtime:2.1.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
            implementation("io.insert-koin:koin-core:4.2.2")
            implementation("io.insert-koin:koin-compose:4.2.2")
            implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")
            implementation("io.ktor:ktor-client-core:3.6.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
            implementation("io.ktor:ktor-client-logging:3.6.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            implementation("io.ktor:ktor-client-mock:3.6.0")
            implementation(libs.kotlin.test)
        }
        getByName("androidHostTest").dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:2.1.0")
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}