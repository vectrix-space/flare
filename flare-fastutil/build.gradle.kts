import space.vectrix.flare.templates.GenerateTemplates
import java.util.stream.Collectors

plugins {
  id("flare.common-conventions")
  id("flare-templates")
}

dependencies {
  compileOnlyApi(libs.jetbrains.annotations)
  api(libs.fastutil)
}

val primitiveData = file("src/templateData/primitive.yaml")

sourceSets {
  main {
    multirelease {
      alternateVersions(9)
    }

    templates.templateSets.register("primitive") {
      dataFiles.from(primitiveData)
      variants("double", "float", "int", "long", "short")
    }
  }
  test {
    templates.templateSets.register("primitive") {
      dataFiles.from(primitiveData)
      variants("double", "float", "int", "long", "short")
    }
  }
}

tasks.withType(GenerateTemplates::class) {
  finalizedBy("spotlessJavaApply")
}

tasks.withType(JavaCompile::class) {
  options.compilerArgs.add("-Xlint:-cast") // Skip cast warnings, the generated source is most likely just overly safe.
}

applyJarMetadata("space.vectrix.flare.fastutil")
