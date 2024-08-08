import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*


/////////////////
// VARIABLES TO CHANGE
val props = Properties().apply {
    load(project.rootProject.file("local.properties").reader())
}

val modVersion = "3.1.0"
val starsectorDirectory = props.getProperty("gamePath") //"C:/Program Files (x86)/Fractal Softworks/Starsector"
val jarFileName = "PerseanChronicles.jar"
//val questgiverVersion = "4.1.0"

val modId = "wisp_perseanchronicles"
val modName = "Persean Chronicles"
val author = "Wisp"
val modDescription = "Adds a collection of quests to bars around the Persean Sector."
val gameVersion = "0.97a-RC11"
//val jars = arrayOf("jars/PerseanChronicles.jar", "libs/wisp/questgiver/$questgiverVersion/Questgiver-$questgiverVersion.jar")
val jars = arrayOf("jars/PerseanChronicles.jar")
val modPlugin = "wisp.perseanchronicles.PerseanChroniclesModPlugin"
val isUtilityMod = false
val masterVersionFile = "https://raw.githubusercontent.com/wispborne/stories/master/$modId.version"
val modThreadId = "19830"
/////////////////

val starsectorCoreDirectory = props["gameCorePath"] ?: "${starsectorDirectory}/starsector-core"
val starsectorModDirectory = props["modsPath"] ?: "${starsectorDirectory}/mods"

plugins {
    kotlin("jvm") version "1.9.23"
    java
}

version = modVersion

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.1")
    }
}

repositories {
    maven(url = uri("$projectDir/libs"))
    mavenCentral()
}

dependencies {
    println("Mod folder: $starsectorModDirectory")
    val kotlinVersionInLazyLib = "1.6.21"

//    // Questgiver lib
//    implementation(fileTree("libs")
//    {
//        include("Questgiver-4.0.0.jar")
//    })

    // Get kotlin sdk from LazyLib during runtime, only use it here during compile time
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersionInLazyLib")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersionInLazyLib")

    compileOnly(fileTree("$starsectorModDirectory/LazyLib/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/MagicLib/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/Console Commands/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/GraphicsLib/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/Secrets of the Frontier/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/Nexerelin-0.11.0b/jars") { include("*.jar") })

    // This grabs local files from the /libs folder, see `repositories` block.
    compileOnly("starfarer:starfarer-api:1.0.0")
//    compileOnly("wisp:questgiver:$questgiverVersion")

    // Starsector jars and dependencies
    compileOnly(fileTree(starsectorCoreDirectory) {
        include(
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
        // Build fat jar with all dependencies bundled.
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)

        destinationDirectory.set(file("$rootDir/jars"))
        archiveFileName.set(jarFileName)
    }

//    register("fatJar", Jar::class.java) {
//        archiveClassifier.set("all")
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
////        manifest {
////            attributes("Main-Class" to mainClass)
////        }
//        from(configurations.runtimeClasspath.get()
//            .onEach { println("add from dependencies: ${it.name}") }
//            .map { if (it.isDirectory) it else zipTree(it) })
//        val sourcesMain = sourceSets.main.get()
//        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
//        from(sourcesMain.output)
//    }

    register<Exec>("debug-starsector") {
        println("Starting debugger for Starsector...")
        workingDir = file(starsectorCoreDirectory)


        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "debug-starsector.bat")
        } else {
            listOf("./starsectorDebug.bat")
        }
    }

    register<Exec>("run-starsector") {
        println("Starting Starsector...")
        workingDir = file(starsectorCoreDirectory)

        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "starsector.bat")
        } else {
            listOf("./starsector.bat")
        }
    }

    register("create-metadata-files") {
        val versionObject = modVersion.split(".").let { javaslang.Tuple3(it[0], it[1], it[2]) }

        val modVersionString = listOf(versionObject._1, versionObject._2, versionObject._3)
            .joinToString(separator = ".")
        File(projectDir, "mod_info.json")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts.
                    {
                        "id": "${modId}",
                        "name": "${modName}",
                        "author": "${author}",
                        "utility": "${isUtilityMod}",
                        "version": "$modVersionString",
                        "description": "${modDescription}",
                        "gameVersion": "${gameVersion}",
                        "jars":[${jars.joinToString() { "\"$it\"" }}],
                        "modPlugin":"${modPlugin}",
                        "dependencies": [
                            {
                                "id": "lw_lazylib",
                                "name": "LazyLib",
                                # "version": "2.6" # If a specific version or higher is required, include this line
                            },
                            {
                                "id": "MagicLib",
                                "name": "MagicLib",
                                # "version": "2.6" # If a specific version or higher is required, include this line
                            }
                        ]
                    }
                """.trimIndent()
            )

        File(projectDir, "${modId}.version")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts.
                    {
                        "masterVersionFile":"${masterVersionFile}",
                        "modName":"${modName}",
                        "modThreadId":${modThreadId},
                        "directDownloadURL":"https://github.com/wispborne/stories/releases/download/$modVersionString/Persean-Chronicles-$modVersionString.zip",
                        "changelogURL":"https://raw.githubusercontent.com/wispborne/stories/$modVersionString/changelog.md",
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

    register("transpileYamlFiles") {
        val outputDirName = "compiled"

        val yamlFiles = projectDir.resolve("data/strings")
            .listFiles { file -> file.name.endsWith(".yaml") }
            .toList()
        println("Transpiling yaml files:\n${yamlFiles.joinToString(separator = "\n")}")

        val yamlMapper = YAMLMapper()
        val jsonMapper = JsonMapper().writerWithDefaultPrettyPrinter()
        yamlFiles.map { file ->
            yamlMapper.readTree(file)
                .let { node ->
                    val jsonFileOutput = file.parentFile.resolve("$outputDirName/${file.nameWithoutExtension}.hjson")
                    jsonFileOutput.parentFile.mkdirs()
                    jsonMapper.writeValue(jsonFileOutput, node)
                }
        }
    }
}

sourceSets {
    // Note that just declaring this sourceset creates two configurations.
    main {
        java {
            srcDir("src/main/kotlin")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}