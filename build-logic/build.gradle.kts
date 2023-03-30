plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.indra)
  implementation(libs.indra.crossdoc)
  implementation(libs.indra.license)
  implementation(libs.indra.sonatype)
}

dependencies {
  compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
  target {
    compilations.configureEach {
      kotlinOptions {
        jvmTarget = "1.8"
      }
    }
  }
}
