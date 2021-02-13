import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("com.dorongold.task-tree") version "1.5"
    id("com.google.protobuf") version "0.8.14"
}

group = "ru.emkn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
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
    implementation("com.googlecode.lanterna:lanterna:3.1.0")
    implementation("com.google.guava:guava:30.0-jre")
    // JSON libs
    implementation("com.google.code.gson:gson:2.8.6")
    // Google protobuf
    implementation("com.google.protobuf:protobuf-java:3.13.0")
    implementation("io.grpc:grpc-stub:1.15.1")
    implementation("io.grpc:grpc-protobuf:1.15.1")
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        implementation("javax.annotation:javax.annotation-api:1.3.1")
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.15.1"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", "build/generated/source/proto/main/java")
        }
    }
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