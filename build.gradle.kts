plugins {
    id("com.gradle.plugin-publish") version "1.3.1"
    `maven-publish`
    signing
}

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
                val ossrhTokenUsername = (findProperty("ossrhTokenUsername") as String?)
                    ?: System.getenv("OSSRH_TOKEN_USERNAME")
                val ossrhTokenPassword = (findProperty("ossrhTokenPassword") as String?)
                    ?: System.getenv("OSSRH_TOKEN_PASSWORD")

                username = ossrhTokenUsername
                    ?: (findProperty("ossrhUsername") as String?)
                    ?: System.getenv("OSSRH_USERNAME")
                password = ossrhTokenPassword
                    ?: (findProperty("ossrhPassword") as String?)
                    ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKeyId = findProperty("signingKeyId") as String? ?: System.getenv("SIGNING_KEY_ID")
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD")

    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        if (signingKeyId.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKey, signingPassword)
        } else {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        }
    } else if (project.hasProperty("signing.gnupg.keyName") || System.getenv("SIGNING_GNUPG_KEY_NAME") != null) {
        useGpgCmd()
    }

    sign(publishing.publications["mavenJava"])
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
