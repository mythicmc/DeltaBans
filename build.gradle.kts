import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    id("net.kyori.blossom") version "2.2.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3" // IntelliJ + Blossom integration
}

group = "com.gmail.tracebachi"
version = "2.2.0"

description = "Banning plugin for BungeeCord and Spigot servers that relies on SQL storage, DbShare for connection pooling, and SockExchange"

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven(url = "https://nexus.velocitypowered.com/repository/maven-public/")
    maven {
        name = "mythicmcReleases"
        url = uri("https://maven.mythicmc.org/releases")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.3")
    compileOnly("com.velocitypowered:velocity-api:3.0.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1")

    compileOnly("com.gmail.tracebachi:dbshare:2.1.2")
    compileOnly("com.gmail.tracebachi:sockexchange:1.1.2")
    compileOnly("io.github.kyzderp:bungeelogger:1.2.0-all")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

sourceSets {
    main {
        blossom {
            resources {
                property("version", project.version.toString())
                property("description", project.description ?: "")
            }
            javaSources {
                property("version", project.version.toString())
                property("description", project.description ?: "")
            }
        }
    }
}
