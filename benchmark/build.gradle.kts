plugins {
  id("me.champeau.jmh") version "0.6.5"
}

dependencies {
  jmh("org.openjdk.jmh:jmh-core:1.32")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.32")
  implementation(project(":flare"))
  implementation(project(":flare-fastutil"))
}

jmh {
  // The full list of configuration options can be found here:
  // https://github.com/melix/jmh-gradle-plugin

  includes.add("Low*")

  // Uncomment to collect GC metrics
  // profilers.add("gc")
}

// Don't publish benchmark
tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf { false }
}
