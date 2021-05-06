dependencies {
  api("org.checkerframework:checker-qual:3.13.0")
}

tasks.jar {
  manifest.attributes(
    "Automatic-Module-Name" to "space.vectrix.flare"
  )
}
