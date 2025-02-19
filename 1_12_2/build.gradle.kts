plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pizzaaxx.bteconosur"
version = "2.0"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
    maven("https://maven.daporkchop.net/")
    maven("https://repo.opencollab.dev/snapshot/")
    maven("https://maven.elmakers.com/repository/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-core:6.1.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:6.1.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-legacy:6.2")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("net.dv8tion:JDA:5.0.0-beta.2")
    implementation("com.github.BuildTheEarth:terraplusplus:v1.1.0")
    implementation("xyz.upperlevel.spigot.book:spigot-book-api:1.6")
    implementation("fr.minuskube:netherboard-bukkit:1.2.2")
    implementation("com.mysql:mysql-connector-j:8.0.32")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")
    implementation("com.github.micycle1:Clipper2-java:1.0.6")
    implementation("com.github.FreshLlamanade:polylabel-java:7639b75b53")
    compileOnly("me.clip:placeholderapi:2.11.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")
    implementation("org.jogamp.jogl:jogl-all-main:2.3.2")
    implementation("com.networknt:json-schema-validator:1.0.72")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}