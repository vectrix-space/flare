plugins {
  id("java")
  id("java-gradle-plugin")
  alias(libs.plugins.spotless)
}

dependencies {
  implementation(libs.mammoth)
  implementation(libs.pebble)
  implementation(libs.snakeyamlEngine)
}

dependencies {
  compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
  plugins {
    create("flare-templates") {
      id = "flare-templates"
      implementationClass = "space.vectrix.flare.templates.TemplateGeneratorPlugin"
    }
  }
}

spotless {
  java {
    licenseHeaderFile(project.file("license_header.txt"))
  }
}
