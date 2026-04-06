rootProject.name = "zai-chat"

// Core modules
include(":core")
include(":core:model")
include(":core:network")

// Chat module (Decompose + MVIKotlin)
include(":chat")

// Feature modules
include(":feature:chat")
include(":feature:settings")

// App entry point
include(":composeApp")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
