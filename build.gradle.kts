plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "genius.project"
version = "0.0.1-SNAPSHOT"
description = "orchestrate"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.1.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-events-api")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    implementation(project(":orchestrate-notification-starter"))

    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.flywaydb:flyway-database-postgresql")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testImplementation("com.microsoft.playwright:playwright:1.64.0")
}

val e2eTest = tasks.register<Test>("e2eTest") {
    description = "Наскрізні браузерні тести (*E2EIT) з Testcontainers і Playwright."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*E2EIT*")
    systemProperty("e2e.artifacts.dir", layout.buildDirectory.dir("e2e-artifacts").get().asFile.path)
    shouldRunAfter(tasks.named("test"))
}

tasks.named<Test>("test") {
    exclude("**/*E2EIT*")
}

tasks.named("check") {
    dependsOn(e2eTest)
}

tasks.register<JavaExec>("installPlaywright") {
    description = "Завантажує Chromium для Playwright разом із системними залежностями."
    group = "verification"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args("install", "--with-deps", "chromium")
}
