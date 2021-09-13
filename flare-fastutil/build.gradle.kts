import space.vectrix.flare.templates.GenerateTemplates
import java.util.stream.Collectors

plugins {
  id("flare.shared-conventions")
  id("flare-templates")
}

dependencies {
  api("it.unimi.dsi:fastutil:8.5.6")
}

val licenseText : Property<String> = objects.property(String::class.java)
licenseText.set(providers.provider {
  val text : String = File("license_header.txt").readText(Charsets.UTF_8)
  val splitText : List<String> = text.lines()
  val lineEnding : String = license.lineEnding.get()
  splitText.subList(0, splitText.size - 1).stream()
    .map { if(it.isEmpty()) { " *" } else { " * $it" } }
    .collect(Collectors.joining(lineEnding, "/*$lineEnding", "$lineEnding */"))
})
licenseText.finalizeValueOnRead()

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
  configureEach {
    templates.templateSets.configureEach {
      header.set(licenseText)
    }
  }
}

tasks.withType(JavaCompile::class) {
  options.compilerArgs.add("-Xlint:-cast") // Skip cast warnings, the generated source is most likely just overly safe.
}

applyJarMetadata("space.vectrix.flare.fastutil")
