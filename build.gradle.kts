plugins {
    id("net.fabricmc.fabric-loom") version "1.18.2"
    `java-library`
}

val minecraftVersion = property("minecraft_version") as String
val loaderVersion = property("loader_version") as String
val fabricApiVersion = property("fabric_api_version") as String
val modVersion = property("mod_version") as String
val mavenGroup = property("maven_group") as String
val archivesBaseName = property("archives_base_name") as String

version = modVersion
group = mavenGroup

base {
    archivesName = archivesBaseName
}

// Client-only mod: no split source sets, everything lives in src/main.
// See https://docs.fabricmc.net/develop/loom for the 26.x workflow.
// (26.1+ is unobfuscated, so there are no `*Remap`/`mod*` configurations and
// dependencies are declared with plain `implementation`.)

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    mavenCentral()
}

dependencies {
    // No `mappings` line: 26.1+ is distributed unobfuscated with Mojang names.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // BackendPlan and SoftRaster have no Minecraft imports, so they are
    // testable on a bare JVM.
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

java {
    // No toolchain block on purpose: any JDK >= 25 works, and `options.release`
    // pins the bytecode level. Add a toolchain if you want Gradle to enforce it.
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to version))
    }
}
