plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
}

dependencies {
  val indraVersion = "2.0.5"
  implementation("net.kyori", "indra-common", indraVersion)
  implementation("de.marcphilipp.gradle", "nexus-publish-plugin", "0.4.0")
}
