import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/////////////////
// VARIABLES TO CHANGE
object Variables {
    val starsectorDirectory = "C:/Program Files (x86)/Fractal Softworks/Starsector"
    val modVersion = "2.0.3"
    val questgiverVersion = "3.0.0"
    val jarFileName = "PerseanChronicles.jar"

    val modId = "wisp_perseanchronicles"
    val modName = "Persean Chronicles"
    val author = "Wisp"
    val description = "Adds a small collection of quests to bars around the Persean Sector."
    val gameVersion = "0.95a-RC15"
    val jars = arrayOf("jars/PerseanChronicles.jar", "libs/Questgiver-$questgiverVersion.jar")
    val modPlugin = "org.wisp.stories.LifecyclePlugin"
    val isUtilityMod = false
    val masterVersionFile = "https://raw.githubusercontent.com/davidwhitman/stories/master/$modId.version"
    val modThreadId = "19830"
}
/////////////////

val starsectorCoreDirectory = "${Variables.starsectorDirectory}/starsector-core"
val starsectorModDirectory = "${Variables.starsectorDirectory}/mods"

plugins {
    kotlin("jvm") version "1.3.60"
    java
}

version = Variables.modVersion

repositories {
    maven(url = uri("$projectDir/libs"))
    jcenter()
}

dependencies {
    val kotlinVersionInLazyLib = "1.4.21"

    // Questgiver lib
    implementation(fileTree("libs")
    {
        include("Questgiver-${Variables.questgiverVersion}.jar")
    })

    // Get kotlin sdk from LazyLib during runtime, only use it here during compile time
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersionInLazyLib")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersionInLazyLib")

    compileOnly(fileTree("$starsectorModDirectory/LazyLib/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/Console Commands/jars") { include("*.jar") })

    // Starsector jars and dependencies
    implementation(fileTree(starsectorCoreDirectory) {
        include(
            "starfarer.api.jar",
            "starfarer.api-sources.jar",
            "starfarer_obf.jar",
            "fs.common_obf.jar",
            "json.jar",
            "xstream-1.4.10.jar",
            "log4j-1.2.9.jar",
            "lwjgl.jar",
            "lwjgl_util.jar"
        )
//        exclude("*_obf.jar")
    })
}

tasks {
    named<Jar>("jar")
    {
        destinationDirectory.set(file("$rootDir/jars"))
        archiveFileName.set(Variables.jarFileName)
    }

    register("debug-starsector", Exec::class) {
        println("Starting debugger for Starsector...")
        workingDir = file(starsectorCoreDirectory)


        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "debug-starsector.bat")
        } else {
            listOf("./starsectorDebug.bat")
        }
    }

    register("run-starsector", Exec::class) {
        println("Starting Starsector...")
        workingDir = file(starsectorCoreDirectory)

        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "starsector.bat")
        } else {
            listOf("./starsector.bat")
        }
    }

    register("create-metadata-files") {
        val versionObject = Variables.modVersion.split(".").let { javaslang.Tuple3(it[0], it[1], it[2]) }

        File(projectDir, "mod_info.json")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts.
                    {
                        "id": "${Variables.modId}",
                        "name": "${Variables.modName}",
                        "author": "${Variables.author}",
                        "utility": "${Variables.isUtilityMod}",
                        "version": "${listOf(versionObject._1, versionObject._2, versionObject._3).joinToString(separator = ".")}",
                        "description": "${Variables.description}",
                        "gameVersion": "${Variables.gameVersion}",
                        "jars":[${Variables.jars.joinToString() { "\"$it\"" }}],
                        "modPlugin":"${Variables.modPlugin}",
                        "dependencies": [
                            {
                                "id": "lw_lazylib",
                                "name": "LazyLib",
                                # "version": "2.6" # If a specific version or higher is required, include this line
                            }
                        ]
                    }
                """.trimIndent()
            )

        File(projectDir, "${Variables.modId}.version")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts.
                    {
                        "masterVersionFile":"${Variables.masterVersionFile}",
                        "modName":"${Variables.modName}",
                        "modThreadId":${Variables.modThreadId},
                        "modVersion":
                        {
                            "major":${versionObject._1},
                            "minor":${versionObject._2},
                            "patch":${versionObject._3}
                        }
                    }
                """.trimIndent()
            )
    }
}

// Compile to Java 6 bytecode so that Starsector can use it
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}