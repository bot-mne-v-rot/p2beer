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
    val kotlinVersion = "1.4.0"
    val junitVersion = "5.6.2"
    val guavaVersion = "23.0"

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    // Spring Library
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.shell:spring-shell-starter:2.0.0.RELEASE")
    implementation("org.springframework.statemachine:spring-statemachine-starter:2.2.0.RELEASE")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    // JUnit Test implementation
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation(kotlin("test"))
    // Lanterna Library
    implementation("com.googlecode.lanterna:lanterna:3.0.0")
    implementation("com.google.guava:guava:30.0-jre")
    // JSON libs
    implementation("com.google.code.gson:gson:2.8.6")
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
    archiveBaseName.set("p2beer")
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = "ru.emkn.p2beer.app.ui.LoginWindowKt"
    }
}

val runJar by tasks.creating(Exec::class) {
    dependsOn(tasks.shadowJar)
    val argvString = project.findProperty("argv") as String? ?: ""
    val jarFile = tasks.shadowJar.get().outputs.files.singleFile
    val evalArgs = listOf("java", "-jar", jarFile.absolutePath) + argvString.split(" ")
    commandLine(*evalArgs.toTypedArray())
}