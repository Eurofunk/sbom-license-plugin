# SBOM license plugin

[![CI Status](https://github.com/eurofunk/sbom-license-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/eurofunk/sbom-license-plugin/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/eurofunk/sbom-license-plugin)](https://github.com/eurofunk/sbom-license-plugin/blob/main/LICENSE)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?color=blue&label=Gradle%20Plugin%20Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fapi%2Fgradle%2Fcom.eurofunk.gradle.sbom-license-plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.eurofunk.gradle.sbom-license-plugin)

The main purpose of this plugin is to provide a way how to check licenses of dependencies in the Gradle project based on
the SBOM (software bill of material) file. To use this plugin, you need to have a valid SBOM file in your project. For
generation of SBOM file you can use the [cyclonedx gradle plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin).

## Usage

Apply the plugin to your project:

```kotlin
plugins {
    id("com.eurofunk.gradle.sbom-license-plugin") version "0.0.1"
}
```

### Task checkLicenses

This task checks the licenses of dependencies in the project based on the SBOM file.

```kotlin
tasks.checkLicenses {
    sbomFile = file("path/to/your/sbom.json")
}
```

Parameters:

| Name               | Type | Optional | Default Value                                                                                                                                                                                                            | Description                                                                       |
|--------------------| ---- |----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| sbomFile           | File | No       |                                                                                                                                                                                                                          | Path to the SBOM file.                                                            |
| dependenciesCheck  | EnumSet<DependencyCheck> | Yes      | DependencyCheck.TRANSITIVE                                                                                                                                                                                               | Level at which dependencies are checked. Possible values: `DIRECT`, `TRANSITIVE`. |
| licenseGroups      | Collection<LicenseGroups> | Yes      | If not set then content of file [license-groups.json](src/main/resources/META-INF/com/eurofunk/gradle/sbom-license-plugin/license-groups.json) is used                                                                   | Collection of license groups.                                                     |
| licenseGroupsFile  | File | Yes      | If not set then file [license-groups.json](src/main/resources/META-INF/com/eurofunk/gradle/sbom-license-plugin/license-groups.json) is used. If `licenseGroups` is also set then it has precedense before this property. | Path to the license groups file.                                                  |
| policies           | Collection<LicensePolicy> | Yes      | Either this property or `policiesFile` must be set                                                                                                                                                                       | Collection of license policies.                                                   |
| policiesFile       | File | Yes      | Either this property or `policies` must be set. If `policies` is also set, it has precedense before this property.                                                                                                       | Path to the license policies file.                                                |
| customLicenses     | Collection<CustomLicense> | Yes      | Either this property or `customLicensesFile` must be set                                                                                                                                                                 | Collection of custom licenses.                                                    |
| customLicensesFile | File | Yes      | Either this property or `customLicenses` must be set. If `customLicenses` is also set, it has precedense before this property.                                                                                             | Path to the custom licenses file.                                                 |

#### Policies

With policies, it's possible to define custom conditions for license validation. The plugin provides several built-in
conditions:

- `AndCondition`: Combines multiple conditions with a logical AND.
- `OrCondition`: Combines multiple conditions with a logical OR.
- `LicenseGroupCondition`: Validates if the license is part of a specific license group.
- `CoordinatesCondition`: Validates if the dependency coordinates match a specific pattern.

There is always one root condition inside the policy, which could contain multiple nested conditions (in case AND or OR
conditions). Policies can be defined inline or loaded from a file.

```kotlin
tasks.checkLicenses {
    policies = listOf(
        Policy (
            name = "Is not permissive besides com.example:example.*",
            rootCondition = AndCondition(
                conditions = listOf(
                    LicenseGroupCondition(
                        groupName = "Permissive",
                        operator = LicenseGroupCondition.Operator.IS_NOT
                    ),
                    CoordinatesCondition(
                        group = "com.example",
                        name = "example.*",
                        operator = CoordinatesCondition.Operator.DOES_NOT_MATCH
                    )
                )
            )
        )
    )
}
```
```kotlin
tasks.checkLicenses {
    policiesFile = file("path/to/your/policies.json")
}
```


##### LicenseGroupCondition

With policy `LicenseGroupCondition` it's possible to validate if license is/is not part of the license group. The
operator can be set to either `IS` or `IS_NOT`. It's possible to use either inline definition of license groups or use
the file with license groups. There is already a default configuration of license groups in the
file [license-groups.json](./src/main/resources/META-INF/com/eurofunk/gradle/sbom-license-plugin/license-groups.json).

```kotlin
tasks.checkLicenses {
    licenseGroups = listOf(
        LicenseGroup(
            name = "Permissive",
            riskWeight = 0,
            licenses = listOf("Apache-2.0", "Apache-1.1")
        ),
        LicenseGroup(
            name = "Weak Copyleft",
            riskWeight = 1,
            licenses = listOf("AFL-1.1", "LGPL-2.1", "LGPL-3.0", "MPL-2.0")
        )
    )
    policies = listOf(
        Policy(
            name = "Prohibit weak copyleft licenses",
            rootCondition = LicenseGroupCondition(
                groupName = "Weak Copyleft",
                operator = LicenseGroupCondition.Operator.IS
            )
        )
    )
}
```
```kotlin
tasks.checkLicenses {
    licenseGroupsFile = file("path/to/your/license-groups.json")
}
```
There is also a possibility to define a custom licenses in case the license is not party of any license or there's no license at all. This can be done using the `customLicenses` or `customLicensesFile` properties.

```kotlin
tasks.checkLicenses {
    customLicenses = listOf(
        ComponentBuilder().withGroup("com.eurofunk.*")
            .withExpression(ExpressionBuilder().withValue("Apache-2.0").build()).build()
    )
}
```
```kotlin
tasks.checkLicenses {
    customLicensesFile = file("path/to/your/custom-licenses.json")
}
```

##### CoordinatesCondition
With policy `CoordinatesCondition` it's possible to validate if dependency coordinates match a specific pattern. The pattern can be a simple string or a regex pattern. The operator can be set to either `MATCHES` or `DOES_NOT_MATCH`.

```kotlin
tasks.checkLicenses {
    policies = listOf(
        Policy(
            name = "Prohibit com.example:example.*",
            rootCondition = CoordinatesCondition(
                group = "com.example",
                name = "example.*",
                operator = CoordinatesCondition.Operator.MATCHES
            )
        )
    )
}
```
```kotlin
tasks.checkLicenses {
    policiesFile = file("path/to/your/policies.json")
}
```

## Publishing setup TODO (Sonatype OSSRH)

Store secrets in `~/.gradle/gradle.properties`, your CI secret store, or environment
variables as noted below. The checklist assumes that the project is published through
the Sonatype OSSRH infrastructure with token-based authentication (preferred) and
falls back to legacy username/password credentials if necessary. When these
credentials (and the signing keys described below) are absent, running
`./gradlew publish` will skip the remote Sonatype repository and signing tasks so
that local verification builds continue to succeed.

- [ ] Confirm OSSRH project access
  - Sign in at <https://s01.oss.sonatype.org/> with the Sonatype account that owns the
    `io.github.eurofunk` groupId. If the namespace has not yet been approved, follow
    the steps in the [OSSRH guide](https://central.sonatype.org/publish/publish-guide/)
    to request access.
  - Ensure you can see the `Staging Repositories` menu entry before attempting to publish.

- [ ] `ossrhTokenUsername` / `OSSRH_TOKEN_USERNAME`
  - From the OSSRH web UI, open **Profile → User Token** and click **Access User Token**.
    Copy the generated **Token Username** and store it as the Gradle property
    `ossrhTokenUsername` or environment variable `OSSRH_TOKEN_USERNAME`.
  - Legacy fallback: the build still honors `ossrhUsername` / `OSSRH_USERNAME` if tokens are
    not available.

- [ ] `ossrhTokenPassword` / `OSSRH_TOKEN_PASSWORD`
  - In the same dialog, copy the **Token Password** and store it as the Gradle property
    `ossrhTokenPassword` or environment variable `OSSRH_TOKEN_PASSWORD`.
  - Legacy fallback: provide `ossrhPassword` / `OSSRH_PASSWORD` if you must use the classic
    credentials.

- [ ] (optional) Override publishing endpoints
  - Releases deploy to `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/` by default.
    Override with the Gradle property `ossrhReleasesUrl` only if Sonatype assigns a different
    host for your project.
  - Snapshot artifacts deploy to `https://s01.oss.sonatype.org/content/repositories/snapshots/`.
    Override with `ossrhSnapshotsUrl` if required.

- [ ] `signingKeyId` / `SIGNING_KEY_ID` *(optional when using in-memory keys)*
  - Run `gpg --list-secret-keys --keyid-format=long` and copy the key ID for your publishing key (for example `ABCDEF1234567890`).
  - Provide the key ID via the Gradle property `signingKeyId` or environment variable `SIGNING_KEY_ID`. When omitted—or when the supplied value does not look like a hexadecimal key ID such as `0xABCDEF1234567890`—the build signs with the default key material embedded in the private key.

- [ ] `signingKey` / `SIGNING_KEY`
  - Export the ASCII-armored private key with `gpg --armor --export-secret-keys <KEY_ID>` (replace `<KEY_ID>` with the value above).
  - Paste the full output—including the `BEGIN/END PGP PRIVATE KEY BLOCK` markers—into the Gradle property `signingKey` or environment variable `SIGNING_KEY`.

- [ ] `signingPassword` / `SIGNING_PASSWORD`
  - Use the passphrase chosen when creating the GPG key (from `gpg --full-generate-key`).
  - Store it as the Gradle property `signingPassword` or environment variable `SIGNING_PASSWORD`.

- [ ] `signing.gnupg.keyName` / `SIGNING_GNUPG_KEY_NAME` *(only if using the local GPG executable)*
  - If you prefer Gradle to call the local `gpg` binary, set this to the key name returned by `gpg --list-secret-keys` (for example `User Name <user@example.com>`).
  - Ensure `signing.gnupg.executable` points to the desired GPG binary and that the key is available in the local keyring or CI agent.

### Using GitHub Actions secrets

When the project builds in GitHub Actions, reference the organization or repository
secrets as environment variables so Gradle can pick them up automatically. Secrets
exposed via the workflow `env` section are available to every step; you can also
scope them to the publish job only. Because the signing key is multi-line, the
workflow writes the values to `~/.gradle/gradle.properties` before invoking Gradle,
which ensures the full key material (including embedded newlines or `\n` escape
sequences) is preserved. The build script also accepts base64-encoded keys via the
same variables.

```yaml
jobs:
  publish:
    runs-on: ubuntu-latest
    env:
      OSSRH_TOKEN_USERNAME: ${{ secrets.OSSRH_TOKEN_USERNAME }}
      OSSRH_TOKEN_PASSWORD: ${{ secrets.OSSRH_TOKEN_PASSWORD }}
      SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
      SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
      SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }} # optional when the key contains its own ID
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Configure Gradle publishing credentials
        run: |
          mkdir -p "$HOME/.gradle"
          cat <<EOF > "$HOME/.gradle/gradle.properties"
          ossrhTokenUsername=${OSSRH_TOKEN_USERNAME}
          ossrhTokenPassword=${OSSRH_TOKEN_PASSWORD}
          signingKeyId=${SIGNING_KEY_ID}
          signingPassword=${SIGNING_PASSWORD}
          signingKey=${SIGNING_KEY}
          EOF
          chmod 600 "$HOME/.gradle/gradle.properties"
      - name: Publish artifacts
        run: ./gradlew publish
      - name: Clean up Gradle credentials
        if: always()
        run: rm -f "$HOME/.gradle/gradle.properties"
```

Gradle reads the variables listed above (and their legacy fallbacks) directly from the
environment, so no extra configuration is required. If you prefer Gradle properties
instead, write the secrets to `~/.gradle/gradle.properties` in a preceding workflow
step and delete the file afterwards to avoid leaking credentials in later jobs. The
example above performs these steps automatically.
