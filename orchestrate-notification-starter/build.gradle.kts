import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
    id("io.spring.dependency-management")
}

group = "genius.project"
version = "0.0.1-SNAPSHOT"
description = "Спільний стартер сповіщень команди Orchestrate"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.slf4j:slf4j-api")

    // Webhook-канал вмикається лише тоді, коли в застосунку є spring-web (RestClient).
    compileOnly("org.springframework:spring-web")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson")
    testImplementation("org.springframework:spring-web")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
