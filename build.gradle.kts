import net.kyori.indra.IndraLicenseHeaderPlugin
import net.kyori.indra.IndraPlugin
import net.kyori.indra.sonatypeSnapshots

plugins {
  id("net.kyori.indra") version "1.3.1"
  id("net.kyori.indra.license-header") version "1.3.1"
}

subprojects {
  apply<IndraPlugin>()
  apply<IndraLicenseHeaderPlugin>()

  group = "space.vectrix.flare"
  version = "0.1.0-SNAPSHOT"
  description = "Useful thread-safe collections with performance in mind."

  repositories {
    mavenCentral()
    sonatypeSnapshots()
  }

  dependencies {
    testImplementation("com.google.guava:guava-testlib:30.1-jre")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
  }

  indra {
    github("vectrix-space", "flare")

    mitLicense()
  }
}
