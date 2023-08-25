import java.util.Properties

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

buildscript {
    repositories { gradlePluginPortal() }
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman", "shadow", "7.1.2")
    }
}

group = "com.iridium"
version = "4.0.5"
description = "IridiumSkyblock"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://nexus.iridiumdevelopment.net/repository/maven-releases/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    mavenCentral()
}

dependencies {
    // Dependencies that we want to shade in
    implementation("org.jetbrains", "annotations", "24.0.1")
    implementation("com.j256.ormlite", "ormlite-core", "6.1")
    implementation("com.j256.ormlite", "ormlite-jdbc", "6.1")
    implementation("com.iridium", "IridiumTeams", "2.1.4")

    // Other dependencies that are not required or already available at runtime
    compileOnly("org.projectlombok", "lombok", "1.18.26")
    compileOnly("org.spigotmc", "spigot-api", "1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl", "VaultAPI", "1.7")
    compileOnly("net.ess3", "EssentialsXSpawn", "2.16.1")
    compileOnly("com.sk89q.worldedit", "worldedit-bukkit", "7.2.13-SNAPSHOT")
}

// Set the Java version and vendor
val targetJavaVersion = 17
java {
    val javaVersion: JavaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks {
    // "Replace" the build task with the shadowJar task (probably bad but who cares)
    build { dependsOn(shadowJar) }

    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
        fun relocate(origin: String) =
                relocate(origin, "com.iridium.iridiumskyblock.dependencies${origin.substring(origin.lastIndexOf('.'))}")
        // Remove the archive classifier suffix
        archiveClassifier.set("")

        // Relocate dependencies
        relocate("com.j256.ormlite")
        relocate("org.bstats")
        relocate("de.jeff_media.updatechecker")

        // Remove unnecessary files from the jar
        minimize()
    }

    // Set UTF-8 as the encoding
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(targetJavaVersion)
    }

    withType<Jar> {
        val names = project.gradle.startParameter.taskNames
        val name = if (names.size == 0) "build" else names[0]

        val exc = "The build path doesn't exist. Build it on the default path."
        val localProperties = project.rootProject.file("local.properties")
        try {
            val properties = Properties()
            val stream = localProperties.inputStream()
            properties.load(stream)

            val buildDir = properties.getProperty("${name}Dir")
            if (buildDir != null && buildDir.isNotBlank()) {
                this.destinationDirectory.set(file(buildDir))
            } else println(exc)
            stream.close()
        } catch (ignored: Exception) {
            localProperties.writeText("buildDir=\ndeployDir=\n")
            println(exc)
        }
    }

    // Process Placeholders for the plugin.yml
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version, "description" to project.description)
        }
    }
}
