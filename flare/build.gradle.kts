import net.kyori.indra.sonatype.IndraSonatypePublishingPlugin

plugins {
  id("net.kyori.indra.publishing.sonatype") version "1.3.1"
}

apply<IndraSonatypePublishingPlugin>()

dependencies {
  api("org.checkerframework:checker-qual:3.11.0")
}

tasks.jar {
  manifest.attributes(
    "Automatic-Module-Name" to "space.vectrix.flare"
  )
}

tasks.withType<PublishToMavenRepository>().configureEach {
  onlyIf {
    val version: String = project.version.toString()
    System.getenv("CI") == null || version.endsWith("-SNAPSHOT")
  }
}

indra {
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
