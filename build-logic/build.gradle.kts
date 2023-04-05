plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.indra)
  implementation(libs.indra.sonatype)
  implementation(libs.indra.spotless)
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
