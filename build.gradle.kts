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

    // The api/ package has no Minecraft imports, so it is testable on the JVM.
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.16")

    // Vulkan bindings are NOT bundled with Minecraft. Uncomment once the backend
    // work starts (M2) and pin the version to the LWJGL version the game ships:
    //
    //   val lwjglVersion = "..." // read from the decompiled game's dependencies
    //   implementation("org.lwjgl:lwjgl-vulkan:$lwjglVersion")
    //   runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion:natives-windows")
    //   runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion:natives-linux")
    //
    // Direct3D 12 and Metal have no LWJGL bindings at all; see native/README.md.
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
