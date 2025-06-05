# How to Create a Release in GitHub

## Steps

1. ### Open the [GitHub Releases Page](https://github.com/Eurofunk/sbom-license-plugin/releases).

2. ### Click “Draft a New Release”
   This opens the release creation form.

3. ### Fill in the Release Details

    - **Tag version:**
      Use an existing tag or create a new one (e.g., `v1.0.0`).
      If creating a new tag, choose the target branch (usually `main` or `release`).

    - **Release title:**
      A short, descriptive title (e.g., _"Initial Release"_).

    - **Description:**
      Write release notes. Include:
        - New features
        - Bug fixes
        - Breaking changes
        - Upgrade instructions
        - Attach binaries or assets (optional):

   Pre-release checkbox:
    - Check this if the release is not stable (e.g., alpha, beta).

4. ### Publish the Release
   Click **"Publish release"** to make it live.

Read
more: [Managing releases in a repository](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository#about-release-management).

---
Once a release is published, the [gradle-publish.yml](../.github/workflows/gradle-publish.yml) workflow is executed
automatically.
It builds and publishes the artifacts to the repositories defined in [build.gradle.kts](../build.gradle.kts):

```kotlin
publishing {
    repositories {
        mavenLocal()
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/eurofunk/sbom-license-plugin")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
                }
            }
        }
    }
}
```

By default, the published packages can be found
on the [GitHub Packages](https://github.com/orgs/Eurofunk/packages?repo_name=sbom-license-plugin) page.