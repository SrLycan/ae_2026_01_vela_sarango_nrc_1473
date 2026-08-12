plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	kotlin("plugin.jpa") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("jacoco")
}

group = "com.spotsapp"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Web (en Spring Boot 4.1 el starter de MVC se separó de "web" genérico)
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// Persistencia
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")

	// Seguridad — Cognito / JWT como Resource Server OAuth2
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")

	// Validación
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Kotlin
	// En Spring Boot 4.1 el módulo Kotlin de Jackson se movió de com.fasterxml a tools.jackson
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// AWS SDK (S3 — usado a partir de la Fase 5, MediaService)
	implementation(platform("software.amazon.awssdk:bom:2.28.11"))
	implementation("software.amazon.awssdk:s3")

	// Dev
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Test — Spring Boot 4.1 expone un starter de test por módulo en vez de uno agregado
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.12")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
	testImplementation("com.h2database:h2") // BD en memoria para tests de repositorio
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

// Las entidades JPA deben quedar "open" para que Hibernate pueda generar proxies;
// kotlin("plugin.jpa") ya cubre esto para @Entity, pero se deja explícito para
// @MappedSuperclass y @Embeddable por si se agregan en fases futuras.
allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}
