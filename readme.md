Flare
=====

Useful thread-safe collections with performance in mind.

## Building
__Note:__ If you do not have [Gradle] installed then use `./gradlew` for Unix systems or Git Bash and gradlew.bat for Windows systems in place of any 'gradle' command.

In order to build Flare you simply need to run the `gradle` command. You can find the compiled JAR file in `./flare/build/libs` labeled 'flare-0.1.0.jar'.

## Dependency

* Maven
```xml
<dependency>
  <groupId>space.vectrix.flare</groupId>
  <artifactId>flare</artifactId>
  <version>0.1.1</version>
</dependency>
```

* Gradle
```groovy
repositories {
  mavenCentral()
}

dependencies {
  compile "space.vectrix.flare:flare:0.1.1"
}
```

## Credits

Various concepts inspired by [Go](https://golang.org/).

- [connorhartley](https://github.com/connorhartley)
- [astei](https://github.com/astei)

