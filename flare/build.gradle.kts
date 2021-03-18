import de.marcphilipp.gradle.nexus.NexusPublishExtension
import de.marcphilipp.gradle.nexus.NexusPublishPlugin
import net.kyori.indra.IndraPublishingPlugin

plugins {
  id("net.kyori.indra.publishing")
  id("de.marcphilipp.nexus-publish") version "0.4.0"
}

apply<IndraPublishingPlugin>()
apply<NexusPublishPlugin>()

dependencies {
  api("org.checkerframework:checker-qual:3.11.0")
}

tasks.jar {
  manifest.attributes(
    "Automatic-Module-Name" to "space.vectrix.flare"
  )
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

indra {
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
