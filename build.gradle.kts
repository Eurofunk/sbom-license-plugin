plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.publish.plugin)
}

group = "io.github.eurofunk"

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
                        email.set("art-workflow-manager@eurofunk.com")
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
        maven {
            name = "Staging"
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

jreleaser {
    project {
        description.set("A Gradle plugin to check SBOM licenses against a policy.")
        copyright.set("Eurofunk Kappacher GmbH")
        links {
            homepage.set("https://github.com/eurofunk/sbom-license-plugin")
        }
    }
    release {
        github {
            overwrite.set(true)
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().asFile.absolutePath)
                }
            }
        }
    }
}


val prepareJReleaser by tasks.registering {
    doLast {
        layout.buildDirectory.dir("jreleaser").get().asFile.mkdirs()
    }
}

tasks.withType<org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask> {
    dependsOn(prepareJReleaser)
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
