plugins {
  `java-library`
  id("me.champeau.jmh") version "0.6.6"
}

repositories {
  mavenCentral()
}

dependencies {
  jmh("org.openjdk.jmh:jmh-core:1.34")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.34")
  implementation(project(":flare"))
  implementation(project(":flare-fastutil"))
}

jmh {
  // The full list of configuration options can be found here:
  // https://github.com/melix/jmh-gradle-plugin

  // Uncomment to collect GC metrics
  // profilers.add("gc")
}

// Don't publish benchmark
tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf { false }
}
