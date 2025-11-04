plugins {
    id("com.gradle.plugin-publish") version "1.3.1"
    `maven-publish`
    signing
}

import java.util.Base64
import kotlin.text.Charsets
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

group = "io.github.eurofunk"
version = "0.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.cyclonedx.java)
    implementation(libs.spdx.utils)

    testImplementation(libs.classgraph)
    testImplementation(libs.equalsverifier)
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website.set("https://www.eurofunk.com")
    vcsUrl.set("https://github.com/eurofunk/sbom-license-plugin")
    plugins {
        register("sbom-license-plugin") {
            id = "com.eurofunk.gradle.sbom-license-plugin"
            implementationClass = "com.eurofunk.gradle.sbom.license.SbomLicensePlugin"
            displayName = "SBOM License Plugin"
        }
    }
}

val ossrhTokenUsername = (findProperty("ossrhTokenUsername") as String?)
    ?: System.getenv("OSSRH_TOKEN_USERNAME")
val ossrhTokenPassword = (findProperty("ossrhTokenPassword") as String?)
    ?: System.getenv("OSSRH_TOKEN_PASSWORD")
val ossrhUsername = (findProperty("ossrhUsername") as String?)
    ?: System.getenv("OSSRH_USERNAME")
val ossrhPassword = (findProperty("ossrhPassword") as String?)
    ?: System.getenv("OSSRH_PASSWORD")

val resolvedOssrhUsername = ossrhTokenUsername ?: ossrhUsername
val resolvedOssrhPassword = ossrhTokenPassword ?: ossrhPassword
val hasOssrhCredentials =
    !resolvedOssrhUsername.isNullOrBlank() && !resolvedOssrhPassword.isNullOrBlank()

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("SBOM License Plugin")
                description.set("Gradle plugin for validating dependency licenses using SBOM metadata.")
                url.set("https://github.com/eurofunk/sbom-license-plugin")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("eurofunk")
                        name.set("eurofunk Kappacher GmbH")
                        email.set("opensource@eurofunk.com")
                        organization.set("eurofunk Kappacher GmbH")
                        organizationUrl.set("https://www.eurofunk.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/eurofunk/sbom-license-plugin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/eurofunk/sbom-license-plugin.git")
                    url.set("https://github.com/eurofunk/sbom-license-plugin")
                }
            }
        }
    }

    repositories {
        if (hasOssrhCredentials) {
            maven {
                name = "Sonatype"
                val isSnapshot = version.toString().endsWith("SNAPSHOT")
                val releasesRepositoryUrl =
                    (findProperty("ossrhReleasesUrl") as String?)
                        ?: "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                val snapshotsRepositoryUrl =
                    (findProperty("ossrhSnapshotsUrl") as String?)
                        ?: "https://s01.oss.sonatype.org/content/repositories/snapshots/"

                url = uri(if (isSnapshot) snapshotsRepositoryUrl else releasesRepositoryUrl)

                credentials {
                    username = resolvedOssrhUsername
                    password = resolvedOssrhPassword
                }
            }
        } else {
            logger.warn("Skipping Sonatype repository configuration because no OSSRH credentials were found.")
        }
    }
}

signing {
    fun decodeSigningKey(rawKey: String?): String? {
        if (rawKey.isNullOrBlank()) {
            return null
        }

        fun containsArmor(value: String): Boolean {
            return value.contains("BEGIN PGP") && value.contains("PRIVATE KEY")
        }

        val normalized = rawKey.replace("\\n", "\n").trim()
        if (containsArmor(normalized)) {
            return normalized
        }

        val decodedArmor = runCatching {
            val decoded = Base64.getDecoder().decode(normalized)
            String(decoded, Charsets.UTF_8).replace("\\n", "\n").trim()
        }.getOrNull()

        if (decodedArmor != null && containsArmor(decodedArmor)) {
            return decodedArmor
        }

        logger.warn(
            "Ignoring signing key because it does not contain an ASCII-armored PGP PRIVATE KEY " +
                "block. Provide the direct output of 'gpg --armor --export-secret-keys' or a " +
                "base64-encoded version of that export."
        )

        return null
    }

    fun normalizeSigningKeyId(candidate: String?): String? {
        val trimmed = candidate?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }

        val hexPattern = Regex("^(0[xX])?[0-9A-Fa-f]+$")
        if (!hexPattern.matches(trimmed)) {
            logger.warn(
                "Ignoring signing key ID because it is not a valid hexadecimal key identifier. " +
                    "Use values such as 'ABCDEF1234567890' or '0xABCDEF1234567890'."
            )
            return null
        }

        val normalized = trimmed.removePrefix("0x").removePrefix("0X")
        return when (normalized.length) {
            8, 16 -> normalized.uppercase()
            40 -> {
                logger.warn(
                    "Received a 40-character GPG fingerprint; using the lower 16 hexadecimal " +
                        "characters as the signing key ID."
                )
                normalized.takeLast(16).uppercase()
            }
            else -> {
                logger.warn(
                    "Ignoring signing key ID because it must be 8 or 16 hexadecimal characters (" +
                        "optionally prefixed with 0x)."
                )
                null
            }
        }
    }

    val signingKeyId = normalizeSigningKeyId(
        (findProperty("signingKeyId") as String?)
            ?: (findProperty("signing.keyId") as String?)
            ?: System.getenv("SIGNING_KEY_ID")
            ?: System.getenv("SIGNING_KEYID")
    )
    val rawSigningKey = (findProperty("signingKey") as String?)
        ?: (findProperty("signing.key") as String?)
        ?: (findProperty("signingKeyBase64") as String?)
        ?: (findProperty("signing.keyBase64") as String?)
        ?: System.getenv("SIGNING_KEY")
        ?: System.getenv("SIGNING_KEY_BASE64")
    val signingKey = decodeSigningKey(rawSigningKey)
    val signingPassword = (findProperty("signingPassword") as String?)
        ?: (findProperty("signing.password") as String?)
        ?: System.getenv("SIGNING_PASSWORD")
        ?: System.getenv("SIGNING_PASSPHRASE")
    val gpgKeyName =
        (findProperty("signing.gnupg.keyName") as String?) ?: System.getenv("SIGNING_GNUPG_KEY_NAME")
    val hasInMemoryKeys = !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()
    val hasGpgConfiguration =
        !gpgKeyName.isNullOrBlank() || project.hasProperty("signing.gnupg.keyName") ||
            project.hasProperty("signing.gnupg.executable") ||
            project.hasProperty("signing.gnupg.homeDir") ||
            System.getenv("SIGNING_GNUPG_EXECUTABLE") != null ||
            System.getenv("SIGNING_GNUPG_HOME_DIR") != null

    fun configureInMemoryKeys(signingKeyId: String?, keyMaterial: String, password: String): Boolean {
        return runCatching {
            if (!signingKeyId.isNullOrBlank()) {
                logger.info(
                    "Configuring in-memory signing keys using the provided key material; the explicit " +
                        "signing key ID '$signingKeyId' will be ignored and Gradle will infer the ID from " +
                        "the key itself."
                )
            }

            useInMemoryPgpKeys(keyMaterial, password)
        }.onFailure {
            logger.warn(
                "Failed to configure in-memory signing keys. Artifact signing will be skipped.",
                it
            )
        }.isSuccess
    }

    val signatoryConfigured = when {
        hasInMemoryKeys -> configureInMemoryKeys(signingKeyId, signingKey!!, signingPassword!!)
        hasGpgConfiguration -> {
            useGpgCmd()
            true
        }
        else -> false
    }

    isRequired = signatoryConfigured

    if (signatoryConfigured) {
        sign(publishing.publications["mavenJava"])
    } else {
        val reason = if (hasInMemoryKeys || hasGpgConfiguration) {
            "the provided signing configuration could not be applied"
        } else {
            "no signing credentials were provided"
        }
        logger.warn("Skipping artifact signing because $reason.")
    }
}

val signingExtension = extensions.getByType<SigningExtension>()

tasks.withType<Sign>().configureEach {
    onlyIf { signingExtension.isRequired }
}


// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}
