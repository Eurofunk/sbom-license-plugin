package com.eurofunk.gradle.sbom.license.policy.model;

/**
 * Enum to choose at which level dependencies are checked.
 */
public enum DependenciesCheck {
    /**
     * Only direct dependencies are checked.
     */
    DIRECT,
    /**
     * Transitive dependencies are checked.
     */
    TRANSITIVE;
}