# SBOM license plugin

[![CI Status](https://github.com/eurofunk/sbom-license-plugin/actions/workflows/gradle.yml/badge.svg)](https://github.com/eurofunk/sbom-license-plugin/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/eurofunk/sbom-license-plugin)](https://github.com/eurofunk/sbom-license-plugin/blob/main/LICENSE)

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
