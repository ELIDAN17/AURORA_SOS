pluginManagement {
    repositories {
        google() // <-- Aquí, para los plugins
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // <-- Y aquí, para las librerías (esta es la que da el error)
        mavenCentral()
    }
}
rootProject.name = "AURORA_SOS" // O el nombre que tenga tu proyecto
include(":app")