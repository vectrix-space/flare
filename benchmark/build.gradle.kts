plugins {
  id("me.champeau.jmh") version "0.6.4"
}

dependencies {
  jmh("org.openjdk.jmh:jmh-core:1.29")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.29")
  implementation(project(":flare"))
  implementation(project(":flare-fastutil"))
}

jmh {
  // The full list of configuration options can be found here:
  // https://github.com/melix/jmh-gradle-plugin

  // Uncomment to run specific benchmarks
  // include = ['SampleBenchmark']

  // Uncomment to collect GC metrics
  // profilers = ['gc']
}

// Don't publish benchmark
tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf { false }
}
