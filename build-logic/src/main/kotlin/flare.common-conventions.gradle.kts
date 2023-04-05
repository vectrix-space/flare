import com.diffplug.gradle.spotless.FormatExtension

plugins {
  id("flare.base-conventions")
  id("net.kyori.indra")
  id("net.kyori.indra.checkstyle")
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
  checkstyle(libs.stylecheck)
  testImplementation(libs.jodah.concurrentunit)
  testImplementation(libs.guava.testlib)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.engine)
}

spotless {
  fun FormatExtension.applyCommon() {
    trimTrailingWhitespace()
    endWithNewline()
    indentWithSpaces(2)
  }

  java {
    importOrderFile(rootProject.file(".spotless/project.importorder"))
    applyCommon()
  }
}
