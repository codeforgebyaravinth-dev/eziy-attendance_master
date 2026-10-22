pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = java.net.URI("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
        maven { url = java.net.URI("https://download.linphone.org/maven_repository/") }
        maven { url = java.net.URI("https://linphone.org/snapshots/maven_repository/") }
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
rootProject.name = "AttendanceMaster"
include(":app")
include(":shared")
include(":desktop")
