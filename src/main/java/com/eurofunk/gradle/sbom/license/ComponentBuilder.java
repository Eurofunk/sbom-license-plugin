package com.eurofunk.gradle.sbom.license;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.license.Expression;


/**
 * Helper class for building a {@link org.cyclonedx.model.Component}.
 */
public class ComponentBuilder {

    private final Component component;

    public ComponentBuilder() {
        this.component = new Component();
    }

    public ComponentBuilder withType(final Component.Type type) {
        component.setType(type);
        return this;
    }

    public ComponentBuilder withGroup(final String group) {
        if (group != null && !group.isEmpty()) {
            component.setGroup(group);
        }
        return this;
    }

    public ComponentBuilder withName(final String name) {
        component.setName(name);
        return this;
    }

    public ComponentBuilder withVersion(final String version) {
        component.setVersion(version);
        return this;
    }

    public ComponentBuilder withDescription(final String description) {
        component.setDescription(description);
        return this;
    }

    public ComponentBuilder withExpression(final Expression expression) {
        if (component.getLicenses() == null) {
            component.setLicenses(new LicenseChoice());
        }
        component.getLicenses().setExpression(expression);
        return this;
    }

    public org.cyclonedx.model.Component build() {
        return component;
    }
}
