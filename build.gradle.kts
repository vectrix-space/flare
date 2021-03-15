import net.kyori.indra.IndraLicenseHeaderPlugin
import net.kyori.indra.IndraPlugin

plugins {
  id("net.kyori.indra") version "1.3.1"
  id("net.kyori.indra.license-header") version "1.3.1"
}

allprojects {
  apply<IndraPlugin>()
  apply<IndraLicenseHeaderPlugin>()

  group = "space.vectrix.flare"
  version = "0.1.0-SNAPSHOT"
  description = "Useful thread-safe collections with performance in mind."

  repositories {
    mavenCentral()
  }

  dependencies {
    api("org.checkerframework:checker-qual:3.11.0")
  }

  indra {
    github("vectrix-space", "flare")
  }
}
