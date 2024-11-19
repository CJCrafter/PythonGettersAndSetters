plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.20-Beta2"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "me.cjcrafter"
version = "0.5.2"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        pycharmCommunity("2023.2")
        bundledPlugins("PythonCore")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
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
