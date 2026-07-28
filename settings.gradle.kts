/**
 * Gradle Plugin Management (Settings), add/change/include dependencies here.
 */
rootProject.name = "Master-Server"

pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven { url = uri("https://maven.fabricmc.net/") }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Buildscript
            plugin("shadow", "com.gradleup.shadow").version("8.3.9")
            plugin("git-version", "com.palantir.git-version").version("4.2.0") // https://github.com/palantir/gradle-git-version/releases

            // Run platforms
            version("runtask", "3.0.2")
            plugin("run-paper", "xyz.jpenilla.run-paper").versionRef("runtask")
            plugin("run-velocity", "xyz.jpenilla.run-velocity").versionRef("runtask") // unused
            plugin("run-waterfall", "xyz.jpenilla.run-waterfall").versionRef("runtask") // unused

            // Bukkit (UNUSED)
            library("bukkit-minimum", "org.bukkit", "bukkit").version("1.8.8-R0.1-SNAPSHOT")
            library("spigotapi-onetwelve", "org.spigotmc", "spigot-api").version("1.12.2-R0.1-SNAPSHOT")
            library("spigotapi-latest", "org.spigotmc", "spigot-api").version("1.21.11-R0.1-SNAPSHOT")

            // Paper
            library("paperapi-latest", "io.papermc.paper", "paper-api").version("1.21.11-R0.1-SNAPSHOT")
            library("paperapi-minimum", "com.destroystokyo.paper", "paper-api").version("1.16.5-R0.1-SNAPSHOT") // unused
            library("folia", "dev.folia", "folia-api").version("1.21.11-R0.1-SNAPSHOT") // unused

            // Bungee
            library("bungee", "net.md-5", "bungeecord-api").version("1.21-R0.4-SNAPSHOT") // unused

            // Velocity
            library("velocity", "com.velocitypowered", "velocity-api").version("3.4.0-SNAPSHOT") // unused

            // Fabric
            version("fabric-loom", "1.16-SNAPSHOT") // unused
            plugin("fabric-loom", "fabric-loom").versionRef("fabric-loom") // unused

            // Annotations
            library("jetbrains-annotations", "org.jetbrains", "annotations").version("24.1.0")

            // Caffeine
            library("caffeine", "com.github.ben-manes.caffeine", "caffeine").version {
                prefer("2.9.3")
                reject("[3,)") // Java 11
            }

            // FAWE
            version("fawe-bom", "1.55") // Ref: https://github.com/IntellectualSites/bom
            library("fawe-bom", "com.intellectualsites.bom", "bom-newest").versionRef("fawe-bom")
            library("fawe-core", "com.fastasyncworldedit", "FastAsyncWorldEdit-Core").versionRef("fawe-bom")
            library("fawe-bukkit", "com.fastasyncworldedit", "FastAsyncWorldEdit-Bukkit").versionRef("fawe-bom")

            // GeoTools Utils
            library("geotools-utils", "asia.buildtheearth.asean.geotools", "geotools-utils").version("1.0.0")

            // DiscordSRV
            includeBuild("discordsrv-ascension") // library("discordsrv-ascension", "com.discordsrv", "api").version("3.0.0-SNAPSHOT")
            library("discordsrv-bridge", "asia.buildtheearth.asean.discord", "discordsrv-bridge").version("2.0.0")

            // DB
            library("hikaricp", "com.zaxxer", "HikariCP").version {
                prefer("4.0.3")
                reject("[5,)") // Java 11
            }
            library("h2", "com.h2database", "h2").version("2.1.210")
            library("mysql", "mysql", "mysql-connector-java").version("8.0.28")
            library("mariadb", "org.mariadb.jdbc", "mariadb-java-client").version("3.1.4")

            // Logging (slf4j has constraints by DiscordSRV sub-build)
            // library("slf4j-api", "org.slf4j", "slf4j-api").version {
            //     prefer("1.7.36")
            //     reject("[2,)") // Uses ServiceLoader
            // }
            library("log4j-core", "org.apache.logging.log4j", "log4j-core").version("2.0-beta9")

            // Adventure
            version("adventure", "4.26.1") // Ref: https://github.com/IntellectualSites/bo
            library("adventure-api", "net.kyori", "adventure-api").versionRef("adventure")
//            library('adventure-serializer-plain', 'net.kyori', 'adventure-text-serializer-plain').versionRef('adventure')
//            library('adventure-serializer-legacy', 'net.kyori', 'adventure-text-serializer-legacy').versionRef('adventure')
//            library('adventure-serializer-gson', 'net.kyori', 'adventure-text-serializer-gson').versionRef('adventure')
//            library('adventure-serializer-ansi', 'net.kyori', 'adventure-text-serializer-ansi').versionRef('adventure')
//
//            // Adventure Platform
//            version('adventure-platform', '4.4.1')
//            library('adventure-platform-bukkit', 'net.kyori', 'adventure-platform-bukkit').versionRef('adventure-platform')
//            library('adventure-platform-bungee', 'net.kyori', 'adventure-platform-bungeecord').versionRef('adventure-platform')
//            library('adventure-serializer-bungee', 'net.kyori', 'adventure-text-serializer-bungeecord').versionRef('adventure-platform')


            // JUnit
            version("junit", "6.0.2")
            library("junit-platform", "org.junit", "junit-bom").versionRef("junit")
            library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").versionRef("junit")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.platform", "junit-platform-engine").versionRef("junit")
        }
    }
}
