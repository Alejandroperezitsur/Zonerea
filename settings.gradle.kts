// Archivo: C:/Users/Alejandro/AndroidStudioProjects/Zonerea/settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // <-- Asegúrate de que esta línea también esté aquí
    }
}

rootProject.name = "Zonerea"
include(":app")
