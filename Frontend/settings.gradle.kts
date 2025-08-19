pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // ← compose plugin을 여기서 가져옵니다
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
