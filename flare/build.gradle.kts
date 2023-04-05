plugins {
  id("flare.common-conventions")
}

sourceSets.main {
  multirelease {
    alternateVersions(9)
  }
}

dependencies {
  compileOnlyApi(libs.jetbrains.annotations)
}

applyJarMetadata("space.vectrix.flare")
