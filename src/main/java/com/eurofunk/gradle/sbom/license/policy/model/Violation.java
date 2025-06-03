package com.eurofunk.gradle.sbom.license.policy.model;

import com.eurofunk.gradle.sbom.license.ComponentUtils;
import org.cyclonedx.model.Component;
import org.jetbrains.annotations.NotNull;

public record Violation(PolicyCondition condition, Component component, String message) {
    @Override
    public @NotNull String toString() {
        return "Violation[condition=%s, component=[%s], message=%s]"
                .formatted(condition, ComponentUtils.componentString(component), message);
    }
}
