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
        browser()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":core:model"))
                implementation(project(":core:network"))
                implementation(project(":feature:settings"))

                // Decompose
                implementation("com.arkivanov.decompose:decompose:3.2.2")
                implementation("com.arkivanov.decompose:extensions-compose:3.2.2")

                // MVIKotlin
                implementation("com.arkivanov.mvikotlin:mvikotlin:4.2.0")
                implementation("com.arkivanov.mvikotlin:mvikotlin-extensions-coroutines:4.2.0")

                // Essenty
                implementation("com.arkivanov.essenty:lifecycle:2.1.0")
                implementation("com.arkivanov.essenty:instance-keeper:2.1.0")
                implementation("com.arkivanov.essenty:state-keeper:2.1.0")

                // Compose
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation("org.jetbrains.compose.ui:ui:1.10.2")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    source.setFrom(
        files("src/wasmJsMain/kotlin"),
        files("src/wasmJsTest/kotlin"),
    )
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
