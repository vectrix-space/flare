plugins {
  id("java")
  id("me.champeau.jmh") version "0.6.4"
}

group = "space.vectrix.flare"
version = "0.2.0-SNAPSHOT"

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  jmh("org.openjdk.jmh:jmh-core:1.29")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.29")
  implementation(project(":flare"))
}

jmh {
  // The full list of configuration options can be found here:
  // https://github.com/melix/jmh-gradle-plugin

  // Uncomment to run specific benchmarks
  // include = ['SampleBenchmark']

  // Uncomment to collect GC metrics
  // profilers = ['gc']
}
