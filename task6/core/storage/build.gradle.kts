plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.gitlab.arturbosch.detekt")
}

group = "com.example"
version = "1.0.0"

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":core:model"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
            }
        }
    }
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}
