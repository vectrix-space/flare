plugins {
  id("java")
  id("java-gradle-plugin")
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
  implementation("net.kyori:mammoth:1.0.0")
}

gradlePlugin {
  plugins {
    creating {
      id = "flare-templates"
      implementationClass = "space.vectrix.flare.templates.TemplateGeneratorPlugin"
    }
  }
}
