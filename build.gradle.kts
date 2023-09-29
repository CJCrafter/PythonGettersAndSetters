plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20-Beta2"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "me.cjcrafter"
version = "0.5.1"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    plugins.set(project.findProperty("platformPlugins").toString().split(',').map(String::trim).filter(String::isNotEmpty))

    version.set("2023.2")
    type.set("PY") // PY = PyCharm Professional, PC = PyCharm Community

    plugins.set(listOf("Pythonid")) // Pythonid = PyCharm Professional, PythonCore = PyCharm Community
    downloadSources.set(false)
}

java.sourceSets["main"].java {
    srcDir("src/main/kotlin")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
