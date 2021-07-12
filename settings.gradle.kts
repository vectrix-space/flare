pluginManagement {
  includeBuild("build-logic")
  includeBuild("build-tool")
}

rootProject.name = "flare-parent"

include(":flare")
include(":flare-fastutil")
include(":benchmark")
