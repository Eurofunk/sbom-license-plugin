plugins {
    id("com.gradle.plugin-publish") version "1.3.1"
    id("signing")
    id("pl.allegro.tech.build.axion-release") version "1.21.1"
}

group = "com.eurofunk.gradle"

scmVersion {
    tag {
        prefix.set("v")
    }
}
version = scmVersion.version

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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
            description = "A Gradle plugin to check SBOM licenses against a policy."
            tags.set(listOf("sbom", "license", "compliance", "cyclonedx"))
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("SBOM License Plugin")
                description.set("A Gradle plugin to check SBOM licenses against a policy.")
                url.set("https://github.com/eurofunk/sbom-license-plugin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("eurofunk")
                        name.set("Eurofunk Kappacher GmbH")
                        email.set("opensource@eurofunk.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/eurofunk/sbom-license-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com:eurofunk/sbom-license-plugin.git")
                    url.set("https://github.com/eurofunk/sbom-license-plugin")
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/eurofunk/sbom-license-plugin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
        maven {
            name = "OSSRH"
            val isSnapshot = version.toString().endsWith("-SNAPSHOT")
            url = if (isSnapshot) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = (project.findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY"))?.takeIf { it.isNotBlank() }
    val signingPassword = (project.findProperty("signingPassword") as String? ?: System.getenv("SIGNING_PASSWORD"))?.takeIf { it.isNotBlank() }

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        isRequired = false
    }
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
