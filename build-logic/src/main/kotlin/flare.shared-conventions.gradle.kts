import de.marcphilipp.gradle.nexus.NexusPublishExtension

plugins {
  id("flare.common-conventions")
  id("signing")
  id("net.kyori.indra.publishing")
  id("de.marcphilipp.nexus-publish")
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

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(configurations.archives.get())
}

tasks {
  withType<PublishToMavenRepository>().configureEach {
    onlyIf {
      val version: String = project.version.toString()
      System.getenv("CI") == null || version.endsWith("-SNAPSHOT")
    }
  }

  withType<Sign>().configureEach {
    onlyIf {
      System.getenv("CI") == null
    }
  }
}
