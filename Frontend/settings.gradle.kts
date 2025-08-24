pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // ← compose plugin을 여기서 가져옵니다
    }
    plugins {
        id("com.google.devtools.ksp") version "1.9.20-1.0.15"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dive"
include(":app")
//include(":divewearos")
include(":wearos")
