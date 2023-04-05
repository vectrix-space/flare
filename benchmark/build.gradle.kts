plugins {
  `java-library`
  alias(libs.plugins.jmh)
}

dependencies {
  implementation(project(":flare"))
  implementation(project(":flare-fastutil"))
}

dependencies {
  compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

jmh {
  // The full list of configuration options can be found here:
  // https://github.com/melix/jmh-gradle-plugin

  jmhVersion.set(libs.versions.jmh)

  // Uncomment to collect GC metrics
  // profilers.add("gc")
}
