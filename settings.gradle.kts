pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Mheenotes"

include(":app")
include(":core:model")
include(":core:commands")
include(":core:storage")
include(":ink:engine")
include(":gesture:engine")
