plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

group = "com.zai"
version = "1.0.0"
