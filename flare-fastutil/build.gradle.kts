import space.vectrix.flare.templates.GenerateTemplates

plugins {
  id("flare.common-conventions")
  id("flare-templates")
}

dependencies {
  api("it.unimi.dsi:fastutil:8.5.4")
}

sourceSets {
  main {
    templates.templateSets.register("primitive") {
      dataFiles.from(files("src/templateData/primitive.yaml"))
      variants("double", "float", "int", "long", "short")
    }
  }
  test {
    templates.templateSets.register("primitive") {
      dataFiles.from(files("src/templateData/primitive.yaml"))
      variants("double", "float", "int", "long", "short")
    }
  }
}

tasks {
  withType(JavaCompile::class) {
    options.compilerArgs.add("-Xlint:-cast") // Skip cast warnings, the generated source is most likely just overly safe.
  }

  withType(GenerateTemplates::class) {
    finalizedBy(licenseFormat)
  }
}

applyJarMetadata("space.vectrix.flare.fastutil")
