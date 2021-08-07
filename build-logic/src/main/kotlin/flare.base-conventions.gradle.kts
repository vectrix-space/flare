plugins {
  id("net.kyori.indra")
  id("net.kyori.indra.license-header")
}

indra {
  github("vectrix-space", "flare") {
    ci(true)
  }

  mitLicense()
}
