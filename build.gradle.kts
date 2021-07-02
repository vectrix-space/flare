import de.marcphilipp.gradle.nexus.NexusPublishExtension

plugins {
  id("signing")
  id("net.kyori.indra") version "2.0.5"
  id("net.kyori.indra.publishing") version "2.0.5" apply false
  id("net.kyori.indra.license-header") version "2.0.5" apply false
  id("de.marcphilipp.nexus-publish") version "0.4.0" apply false
}

group = "space.vectrix.flare"
version = "0.3.0"
description = "Useful thread-safe collections with performance in mind."

subprojects {
  apply(plugin = "net.kyori.indra")
  apply(plugin = "net.kyori.indra.publishing")
  apply(plugin = "net.kyori.indra.license-header")
  apply(plugin = "de.marcphilipp.nexus-publish")

  group = rootProject.group
  version = rootProject.version
  description = rootProject.description

  repositories {
    mavenLocal()
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/groups/public/")
    }
  }

  dependencies {
    testImplementation("net.jodah:concurrentunit:0.4.6")
    testImplementation("com.google.guava:guava-testlib:30.1.1-jre")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
  }

  signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(configurations.archives.get())
  }

  indra {
    github("vectrix-space", "flare") {
      ci(true)
    }

    mitLicense()

    extensions.configure<NexusPublishExtension> {
      repositories.sonatype {
        nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
        snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      }
    }

    configurePublications {
      pom {
        developers {
          developer {
            id.set("VectrixDevelops")
            name.set("Vectrix")
          }
        }
      }
    }
  }

  tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
      val version: String = project.version.toString()
      System.getenv("CI") == null || version.endsWith("-SNAPSHOT")
    }
  }
}
