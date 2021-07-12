import de.marcphilipp.gradle.nexus.NexusPublishExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
  id("net.kyori.indra.publishing")
  id("de.marcphilipp.nexus-publish")
}

indra {
  javaVersions {
    testWith(8, 11, 16)
  }

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
