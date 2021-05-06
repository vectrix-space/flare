plugins {
//  id("flare-templates")
}

dependencies {
  api("org.checkerframework:checker-qual:3.13.0")
  api("it.unimi.dsi:fastutil:8.5.4")
}

tasks.jar {
  manifest.attributes(
    "Automatic-Module-Name" to "space.vectrix.flare.fastutil"
  )
}
