plugins {
  id("flare.base-conventions")
  id("net.kyori.indra.license-header")
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/groups/public/")
  }
}

dependencies {
  compileOnlyApi("org.checkerframework:checker-qual:3.48.1")
  testImplementation("net.jodah:concurrentunit:0.4.6")
  testImplementation("com.google.guava:guava-testlib:31.0.1-jre")
  testImplementation(platform("org.junit:junit-bom:5.8.2"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-engine")
}
