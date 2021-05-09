import space.vectrix.flare.templates.GenerateTemplates

plugins {
  id("flare-templates")
}

dependencies {
  api("org.checkerframework:checker-qual:3.13.0")
  api("it.unimi.dsi:fastutil:8.5.4")
}

sourceSets {
  main {
    templates.templateSets.register("primitive") {
      dataFiles.from(files("src/templateData/primitive.yaml"))
      variants("int", "long", "float", "double")
    }
  }
}

tasks {
  jar {
    manifest.attributes(
      "Automatic-Module-Name" to "space.vectrix.flare.fastutil"
    )
  }

  withType(GenerateTemplates::class) {
    finalizedBy(licenseFormat)
  }
}
