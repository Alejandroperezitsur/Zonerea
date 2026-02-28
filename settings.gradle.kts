// Archivo: C:/Users/Alejandro/AndroidStudioProjects/Zonerea/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // <-- Asegúrate de que esta línea también esté aquí
    }
}

rootProject.name = "Zonerea"
include(":app")
