@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

group = "com.zai"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(17)

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.10.2")
                implementation("org.jetbrains.compose.foundation:foundation:1.10.2")
                implementation("org.jetbrains.compose.material:material:1.10.2")
                implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
                implementation("org.jetbrains.compose.ui:ui:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("io.ktor:ktor-client-core:3.4.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.4.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.1")
            }
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

ktlint {
    android.set(false)
    outputColorName.set("RED")
    filter {
        exclude("**/generated/**")
    }
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ktlint_experimental" to "enabled",
            "ktlint_standard_filename" to "disabled",
            "ktlint_standard_function-naming" to "disabled",
            "ktlint_standard_no-wildcard-imports" to "disabled",
            "ktlint_standard_trailing-comma-on-call-site" to "disabled",
            "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        ),
    )
}

tasks.named("check") {
    dependsOn("ktlintCheck")
    dependsOn("detekt")
}
