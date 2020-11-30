import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.dorongold.task-tree") version "1.5"
}

group = "ru.emkn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val junitVersion = "5.6.2"

    implementation(kotlin("stdlib"))
    implementation("org.bouncycastle:bcprov-jdk15to18:1.67")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation(kotlin("test"))
}

tasks.withType(KotlinCompile::class.java) {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.shadowJar {
    archiveBaseName.set("runnable")
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = "ru.emkn.p2beer.MainKt"
    }
}

val runJar by tasks.creating(Exec::class) {
    dependsOn(tasks.shadowJar)
    val argvString = project.findProperty("argv") as String? ?: ""
    val jarFile = tasks.shadowJar.get().outputs.files.singleFile
    val evalArgs = listOf("java", "-jar", jarFile.absolutePath) + argvString.split(" ")
    commandLine(*evalArgs.toTypedArray())
}