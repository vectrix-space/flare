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
  compileOnlyApi("org.checkerframework:checker-qual:3.18.0")
  testImplementation("net.jodah:concurrentunit:0.4.6")
  testImplementation("com.google.guava:guava-testlib:30.1.1-jre")
  testImplementation(platform("org.junit:junit-bom:5.7.2"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-engine")
}
