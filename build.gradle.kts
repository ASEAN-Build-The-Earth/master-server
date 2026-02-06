import groovy.lang.Closure
import com.palantir.gradle.gitversion.VersionDetails
import org.gradle.kotlin.dsl.invoke
import java.io.FileNotFoundException

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.git.version)
    alias(libs.plugins.run.paper)
}

repositories {
    // mavenLocal() // NEVER use in Production/Commits!
    mavenCentral()

    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
}

dependencies {
    compileOnly(libs.paperapi.latest) {
        // Constraints by DiscordSRV-Ascension sub-build
        exclude("org.slf4j", "slf4j-api")
    }
    compileOnly(libs.jetbrains.annotations)

    // Caffeine
    implementation(libs.caffeine)

    // Fast Async WorldEdit
    implementation(enforcedPlatform(libs.fawe.bom))
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) { isTransitive = false }

    // DiscordSRV
    compileOnly("com.discordsrv:api")
    implementation(libs.discordsrv.bridge)

    // GeoTools Utils
    implementation(libs.geotools.utils)

    testImplementation(enforcedPlatform(libs.junit.platform))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation("com.discordsrv:api")
    testRuntimeOnly(libs.junit.platform.launcher)
    testCompileOnly(libs.jetbrains.annotations)
}

val versionDetails: Closure<VersionDetails> by extra
val details = versionDetails()

group = "asia.buildtheearth.asean"
description = "Server management plugin for ASEAN BTE Master Server"
version = "0.0.0" + "-" + details.commitDistance + "-" + details.gitHash + "-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    maxHeapSize = "1G"

    // Test only - KML class loading required UNNAMED reflections, I think.
    // https://liferay.dev/b/jdk-17-jdk-21-solving-module-does-not-opens-to-unnamed-module
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")

    testLogging {
        events("passed")
    }
}

// region DiscordSRV-Ascension Submodule
val discordsrv: IncludedBuild? = gradle.includedBuild("discordsrv-ascension");

val cleanDiscordSRVJarDir = tasks.register<Delete>("cleanDiscordSRVJarDir") {
    delete(discordsrv?.projectDir?.resolve("jars"))
}

val buildBukkitDiscordSRV = tasks.register("buildBukkitDiscordSRV") {
    val jarDiscordSRV = discordsrv?.task(":bukkit:bukkit-loader:jar")
        ?: throw IllegalStateException("DiscordSRV submodule not found, is it included?")

    dependsOn(jarDiscordSRV)
}!!

/**
 * Get the latest JAR file of DiscordSRV-Ascension submodule
 */
fun getDiscordSRV(): File {
    val dir = discordsrv?.projectDir?.resolve("jars")
    val err = FileNotFoundException(
        "DiscordSRV required to be built first before :runServer, "
        + "do :buildBukkitDiscordSRV"
    )

    val files: Array<File> = dir?.listFiles() ?: throw err
    files.sortByDescending { it.lastModified() }
    val file: File = files.getOrNull(0) ?: throw err

    println("Using " + discordsrv.name + ": " + file.name)

    return file
}
// endregion

tasks.runServer {
    // DiscordSRV Ascension Testing Build
    pluginJars(getDiscordSRV())
    // UNCOMMENT FOR EXTERNAL BUILD
    // downloadPlugins.url("DISCORD_SRV_ASCENSION_PUBLIC_URL")

    // FastAsync WorldEdit integration
    downloadPlugins.url("https://ci.athion.net/job/FastAsyncWorldEdit/1263/artifact/artifacts/FastAsyncWorldEdit-Paper-2.15.0.jar")

    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.21")
}

tasks.shadowJar {
    mergeServiceFiles()

    // Exclude annotation classes (e.g. org.jetbrains.annotations)
    exclude("org/jetbrains/annotations/**")
    // Exclude slf4j classes
    exclude("org/slf4j/**")
    // Exclude signatures, maven/ and proguard/ from META-INF
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/maven/**")
    exclude("META-INF/proguard/**")

    // relocate BACK discordsrv packages we referenced against
    // NOTE: we'll have to use relocated packages if we unlink discordsrv (after it's stable to)
    relocate("net.dv8tion.jda", "com.discordsrv.dependencies.net.dv8tion.jda")

    // archiveClassifier = ""
    // relocationPrefix = "asia.buildtheearth.asean.dependencies"
    // enableAutoRelocation = true
}

tasks.assemble {
    dependsOn(tasks.shadowJar) // Ensure that the shadowJar task runs before the build task
}

tasks.jar {
    archiveClassifier = "UNSHADED"
    enabled = false // Disable the default jar task since we are using shadowJar
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    with(copySpec {
        from("src/main/resources/plugin.yml") {
            expand(
                mapOf(
                    "version" to project.version,
                    "description" to project.description
                )
            )
        }
    })
}
