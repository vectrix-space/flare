Flare [![Discord](https://img.shields.io/discord/819522977586348052?style=for-the-badge)](https://discord.gg/rYpaxPFQrj)
=====
![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/vectrix-space/flare/build/main)
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](license.txt)
[![Maven Central](https://img.shields.io/maven-central/v/space.vectrix.flare/flare?label=stable)](https://search.maven.org/search?q=g:space.vectrix.flare%20AND%20a:flare*)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/space.vectrix.flare/flare?label=dev&server=https%3A%2F%2Fs01.oss.sonatype.org)

Useful thread-safe collections with performance in mind.

## Building
__Note:__ If you do not have [Gradle] installed then use `./gradlew` for Unix systems or Git Bash and gradlew.bat for Windows systems in place of any 'gradle' command.

In order to build Flare you simply need to run the `gradle` command. You can find the compiled JAR file in `./flare/build/libs` labeled 'flare-0.3.0.jar'.

## Dependency

Gradle:
```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation "space.vectrix.flare:flare:0.3.0"
  implementation "space.vectrix.flare-fastutil:0.3.0"
}
```

Maven:
```xml
<dependencies>
  <dependency>
    <groupId>space.vectrix.flare</groupId>
    <artifactId>flare</artifactId>
    <version>0.3.0</version>
  </dependency>
  <dependency>
    <groupId>space.vectrix.flare</groupId>
    <artifactId>flare-fastutil</artifactId>
    <version>0.3.0</version>
  </dependency>
</dependencies>
```

## Credits

Various concepts inspired by [Go].

- [connorhartley]
- [astei]

Initially designed for [Mineteria](https://mineteria.com/).

[Go]: https://golang.org/

[connorhartley]: https://github.com/connorhartley
[astei]: https://github.com/astei

[Gradle]: https://www.gradle.org/
