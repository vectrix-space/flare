plugins {
  id("net.kyori.indra")
}

indra {
  github("vectrix-space", "flare") {
    ci(true)
  }

  mitLicense()
}
