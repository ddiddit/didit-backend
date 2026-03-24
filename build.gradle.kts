plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.didit"
version = "0.0.1-SNAPSHOT"
description = "Didit backend service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    create("asciidoctorExt")
}

repositories {
    mavenCentral()
}

val snippetsDir = file("build/generated-snippets")
val appDocsOutDir = layout.buildDirectory.dir("docs/app")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    runtimeOnly("com.mysql:mysql-connector-j")

    testRuntimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "asciidoctorExt"("org.springframework.restdocs:spring-restdocs-asciidoctor")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts", "gradle/**/*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.test {
    useJUnitPlatform()
    outputs.dir(snippetsDir)
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}

tasks.register<org.asciidoctor.gradle.jvm.AsciidoctorTask>("asciidoctorApp") {
    group = "documentation"
    description = "Generate App API docs"

    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
    configurations("asciidoctorExt")
    baseDirFollowsSourceFile()

    setSourceDir(file("src/docs/asciidoc/app"))

    sources {
        include("index.adoc")
    }

    setOutputDir(appDocsOutDir.get().asFile)
}

tasks.bootJar {
    dependsOn(tasks.named("asciidoctorApp"))

    from(appDocsOutDir.get().asFile) {
        into("static/docs/app")
    }
}
