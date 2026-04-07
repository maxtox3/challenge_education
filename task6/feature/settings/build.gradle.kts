@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
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
                implementation(project(":core:storage"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.decompose)
                implementation(libs.decompose.compose)
                implementation(libs.mvikotlin)
                implementation(libs.mvikotlin.extensions.coroutines)
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
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
