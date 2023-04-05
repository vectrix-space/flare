plugins {
  id("net.kyori.indra.publishing")
}

// Expose version catalog
val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

indra {
  javaVersions {
    val testVersions = (project.property("testJdks") as String)
      .split(",")
      .map { it.trim().toInt() }

    testWith().addAll(testVersions)
  }

  checkstyle(libs.versions.checkstyle.get())

  github("vectrix-space", "flare") {
    ci(true)
  }

  mitLicense()

  configurePublications {
    pom {
      developers {
        developer {
          id.set("VectrixDevelops")
          name.set("Vectrix")
        }
      }
    }
  }
}
