import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories

plugins {
  id("flare.base-conventions")
  id("signing")
  id("net.kyori.indra")
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
  compileOnlyApi("org.checkerframework:checker-qual:3.15.0")
  testImplementation("net.jodah:concurrentunit:0.4.6")
  testImplementation("com.google.guava:guava-testlib:30.1.1-jre")
  testImplementation(platform("org.junit:junit-bom:5.7.2"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-engine")
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(configurations.archives.get())
}

tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf {
    val version: String = project.version.toString()
    System.getenv("CI") == null || version.endsWith("-SNAPSHOT")
  }
}
