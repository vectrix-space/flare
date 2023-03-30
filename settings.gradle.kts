pluginManagement {
  includeBuild("build-logic")
  includeBuild("build-tool")
  repositories {
    gradlePluginPortal()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
  }
}

rootProject.name = "flare-parent"

include(":flare")
include(":flare-fastutil")
include(":benchmark")
