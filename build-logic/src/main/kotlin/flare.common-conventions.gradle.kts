plugins {
  id("flare.base-conventions")
  id("net.kyori.indra")
  id("net.kyori.indra.licenser.spotless")
}

// Expose version catalog
val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

configurations {
  testCompileClasspath {
    exclude(group = "junit") // brought in by google's libs
  }
}

dependencies {
  testImplementation(libs.jodah.concurrentunit)
  testImplementation(libs.guava.testlib)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.engine)
}
