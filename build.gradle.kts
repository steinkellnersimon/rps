plugins {
    java
    kotlin("jvm") version "1.4.10"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "at.stnwtr"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Javalin webserver
    implementation("io.javalin:javalin:3.10.1")

    // SLF4J logger
    implementation("org.slf4j:slf4j-simple:1.7.30")

    // Jackson json object mapper
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.2")

    // Gson json parser
    implementation("com.google.code.gson:gson:2.8.6")

    // SQLite persistent database
    implementation("org.xerial:sqlite-jdbc:3.32.3.2")

    // Kotlin recursive dependencies
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

application {
    mainClassName = "at.stnwtr.rps.LauncherKt"
}
