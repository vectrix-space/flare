plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  implementation("net.kyori", "indra-common", "2.1.1")
  implementation("de.marcphilipp.gradle", "nexus-publish-plugin", "0.4.0")
}
