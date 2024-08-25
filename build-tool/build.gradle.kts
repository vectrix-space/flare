plugins {
  id("java")
  id("java-gradle-plugin")
  id("org.cadixdev.licenser") version "0.6.1"
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/groups/public/")
  }
}

dependencies {
  implementation("io.pebbletemplates:pebble:3.1.5")
  implementation("org.snakeyaml:snakeyaml-engine:2.3")
  implementation("net.kyori:mammoth:1.4.0")
}

gradlePlugin {
  plugins {
    create("flare-templates") {
      id = "flare-templates"
      implementationClass = "space.vectrix.flare.templates.TemplateGeneratorPlugin"
    }
  }
}

license {
  header(project.file("license_header.txt"))
  newLine(false)
}
