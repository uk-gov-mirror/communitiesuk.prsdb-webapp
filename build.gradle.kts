import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.noarg") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("io.gatling.gradle") version "3.16.0"
    id("org.flywaydb.flyway") version "12.4.0"
}

group = "uk.gov.communities.prsdb"
version = "latest"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

// Third-party clients still use Jackson 2 alongside Spring Boot's Jackson 3 mapper.
extra["jackson-2-bom.version"] = "2.21.7"

// CVE-2026-19032, -68497, -83557: jackson-databind advisories (fixed in 3.1.6).
// CVE-2026-89407, -89425, -91776, -91777: jackson-core / jackson-databind DoS advisories (fixed in 3.1.7).
extra["jackson-bom.version"] = "3.1.7"

// CVE-2026-107314 and CVE-2026-107315 (fixed in 42.7.14).
extra["postgresql.version"] = "42.7.14"

// CVE-2026-65182 / GHSA-gcx9-497g-6cp6, CVE-2026-65905 / GHSA-9xv2-5v5q-p794,
// CVE-2026-68525 / GHSA-h3x4-894j-xpx5: Tomcat authentication vulnerabilities (fixed in 11.0.25).
extra["tomcat.version"] = "11.0.25"

dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")

    // Migrations
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-session-data-redis")

    // Auth
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")

    // Templating
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.1.1"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-metrics")
    implementation("io.micrometer:micrometer-registry-cloudwatch2")

    // External service clients
    implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("software.amazon.awssdk:s3-transfer-manager")
    implementation("software.amazon.awssdk:aws-crt-client")
    implementation("software.amazon.awssdk:cloudwatch")
    implementation("software.amazon.awssdk:costexplorer")

    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.microsoft.playwright:playwright:1.47.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("com.deque.html.axe-core:playwright:4.4.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    // PhoneNumbers
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.49")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // CSV Data Loading
    implementation("org.apache.commons:commons-csv:1.8")

    // Streaming upload without storing on local system
    implementation("org.apache.commons:commons-fileupload2-jakarta-servlet6:2.0.0-M5")

    // FF4J feature flags
    implementation("org.ff4j:ff4j-spring-boot-starter-webmvc:2.1")

    // Fake data generation
    implementation("net.datafaker:datafaker:2.4.2")

    // Gatling performance tests (src/gatling)
    gatlingImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    gatlingRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Ktlint's parser must use its own compiler rather than the application-managed version.
    "ktlint"("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.10")

    // Force commons-compress (transitive of Testcontainers and axe-core/playwright) to a version without known CVEs.
    constraints {
        testImplementation("org.apache.commons:commons-compress:1.26.0")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

val frontendAssetsSpec: CopySpec =
    copySpec {
        from("dist")
        include("**/*")
    }

// The Gatling performance code lives in src/gatling and drives the running webapp over HTTP, reusing its route
// constants. The One Login simulator fixtures are shared with the simulator integration tests.
sourceSets.named("gatling") {
    resources.srcDir("src/test/resources")
    resources.include("one-login-simulator/*.json", "logback-test.xml", "gatling.conf")
}

gatling {
    includeTestOutput = false
    // Contract simulations use fake servers and must not be offered as real performance runs.
    excludes = listOf("uk.gov.communities.prsdb.webapp.performance.*ContractSimulation")
    systemProperties =
        System
            .getProperties()
            .stringPropertyNames()
            .filter { it.startsWith("gatling.") }
            .associateWith { System.getProperty(it) }
}

tasks.register<Test>("performanceTest") {
    group = "verification"
    description = "Runs the unit and assertion tests for the Gatling performance code in src/gatling (no app, Docker or NFT needed)"
    val gatlingSourceSet = sourceSets["gatling"]
    testClassesDirs = gatlingSourceSet.output.classesDirs
    classpath = gatlingSourceSet.runtimeClasspath
    include("**/*Tests.class")
    // Provided lazily so the classpath is only resolved when the tests actually run.
    val gatlingClasspath = gatlingSourceSet.runtimeClasspath
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-Dperformance.test.classpath=${gatlingClasspath.asPath}") })
}

tasks.register<Exec>("buildFrontendAssets") {
    group = "build"
    description = "Build frontend JavaScript and CSS assets using npm"
    if (OperatingSystem.current().isWindows) {
        commandLine("cmd", "/c", "npm", "run", "build")
    } else {
        commandLine("npm", "run", "build")
    }
}

tasks.register<Copy>("copyBuiltAssets") {
    dependsOn("buildFrontendAssets")
    into(layout.buildDirectory.dir("resources/main/static/assets"))
    with(frontendAssetsSpec)
    outputs.upToDateWhen { false }
}

tasks.withType<KotlinCompile> {
    dependsOn("copyBuiltAssets")
}

// CI runs the test suite as several parallel jobs, each taking a slice of the test classes. Nested
// classes must execute with their enclosing class, so the slice is chosen from the top-level class name
// and every nested class follows it.
val shardIndex = (project.findProperty("shardIndex") as String?)?.toInt()
val shardCount = (project.findProperty("shardCount") as String?)?.toInt()

require(shardCount == null || (shardIndex != null && shardIndex in 1..shardCount)) {
    "shardIndex must be set and within 1..shardCount when shardCount is given"
}

tasks.named<Test>("test") {
    exclude("uk/gov/communities/prsdb/webapp/integration/oneLoginSimulator/**")
}

tasks.register<Test>("testWithOneLoginSimulator") {
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform()
    dependsOn("copyBuiltAssets")
    include("uk/gov/communities/prsdb/webapp/integration/oneLoginSimulator/**")
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn("copyBuiltAssets")
    maxHeapSize = "2g"

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
        events("failed")
    }

    if (shardIndex != null && shardCount != null) {
        // Shards run concurrently when sharding locally, so each needs its own output directories.
        val shardName = "$name-shard$shardIndex"
        binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/$shardName/binary"))
        reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/$shardName"))
        reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/$shardName"))

        exclude { element ->
            if (element.isDirectory) {
                false
            } else {
                val path = element.path
                if (!path.endsWith(".class")) {
                    false
                } else {
                    val topLevelClass = path.removeSuffix(".class").substringBefore('$')
                    Math.floorMod(topLevelClass.hashCode(), shardCount) != shardIndex - 1
                }
            }
        }
    }
}

tasks.register<JavaExec>("playwright") {
    classpath(sourceSets["test"].runtimeClasspath)
    mainClass.set("com.microsoft.playwright.CLI")
}

tasks.register<Test>("testWithoutIntegration") {
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    exclude("uk/gov/communities/prsdb/webapp/integration/**")
    exclude("uk/gov/communities/prsdb/webapp/integration/oneLoginSimulator/**")
}

// Read .env file for local development configuration. The .env file is gitignored and only exists on developer
// machines. This is used solely to configure the Flyway Gradle plugin (flywayClean etc.) with the correct local
// database port — it has no effect on the Spring Boot application, deployed environments, or CI.
val envFile = file(".env")
val envVars = mutableMapOf<String, String>()
if (envFile.exists()) {
    envFile.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotBlank() && !trimmed.startsWith("#") && trimmed.contains("=")) {
            val (key, value) = trimmed.split("=", limit = 2)
            envVars[key.trim()] = value.trim().removeSurrounding("\"")
        }
    }
}

flyway {
    val postgresPort = envVars["POSTGRES_PORT"] ?: "5433"
    url = "jdbc:postgresql://localhost:$postgresPort/prsdblocal"
    user = "postgres"
    password = "notarealpassword"
}

tasks.register("messageFileCheck") {
    group = "verification"
    description = "Check message YAML files for escaped unicode apostrophes (\\u2019) that should be literal curly apostrophes"
    doLast {
        val messagesDir = file("src/main/resources/messages")
        val escapedApostrophe = "\\u2019"
        val violations = mutableListOf<String>()

        messagesDir.listFiles()?.filter { it.extension == "yml" }?.sorted()?.forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                if (line.contains(escapedApostrophe)) {
                    violations.add("${file.name}:${index + 1}: $line")
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Found escaped unicode apostrophes (\\u2019) in message files. " +
                    "Use the literal curly apostrophe character (\u2019) instead.\n" +
                    "Run './gradlew messageFileFormat' to auto-fix.\n\n" +
                    violations.joinToString("\n"),
            )
        }
    }
}

tasks.register("messageFileFormat") {
    group = "verification"
    description = "Replace escaped unicode apostrophes (\\u2019) with literal curly apostrophes in message YAML files"
    doLast {
        val messagesDir = file("src/main/resources/messages")
        val escapedApostrophe = "\\u2019"
        val curlyApostrophe = "\u2019"
        var totalReplacements = 0

        messagesDir.listFiles()?.filter { it.extension == "yml" }?.sorted()?.forEach { file ->
            val content = file.readText()
            if (content.contains(escapedApostrophe)) {
                val count = content.windowed(escapedApostrophe.length).count { it == escapedApostrophe }
                file.writeText(content.replace(escapedApostrophe, curlyApostrophe))
                totalReplacements += count
                println("Fixed $count occurrence(s) in ${file.name}")
            }
        }

        if (totalReplacements > 0) {
            println("\nReplaced $totalReplacements escaped apostrophe(s) with literal curly apostrophes.")
        } else {
            println("No escaped apostrophes found. All message files are clean.")
        }
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.postgresql:postgresql:42.7.14")
        classpath("org.flywaydb:flyway-database-postgresql:12.4.0")
    }
    configurations.classpath {
        resolutionStrategy {
            // Application dependency management does not apply to the build classpath.
            force("org.apache.commons:commons-lang3:3.18.0")
            force("com.fasterxml.jackson:jackson-bom:2.22.3")
            force("tools.jackson:jackson-bom:3.1.7")
            force("org.apache.httpcomponents.client5:httpclient5:5.6.4")
            force("org.apache.httpcomponents.core5:httpcore5:5.4.3")
            force("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
        }
    }
}
