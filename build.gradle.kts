import groovy.lang.Closure
import com.palantir.gradle.gitversion.VersionDetails
import org.gradle.kotlin.dsl.invoke

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
    compileOnly(libs.paperapi.latest)
    compileOnly(libs.jetbrains.annotations)

    // Caffeine
    implementation(libs.caffeine)

    // Fast Async WorldEdit
    implementation(enforcedPlatform(libs.fawe.bom))
    compileOnly(libs.fawe.core)
    compileOnly(libs.fawe.bukkit) { isTransitive = false }

    // DiscordSRV
    compileOnly(libs.discordsrv.ascension)
    implementation(libs.discordsrv.bridge)

    // GeoTools Utils
    implementation(libs.geotools.utils)

    testImplementation(enforcedPlatform(libs.junit.platform))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.engine)
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

tasks.runServer {
    // DiscordSRV Ascension Testing Build
    downloadPlugins.url("LOCAL_BUILD")

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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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
