package com.eurofunk.gradle.sbom.license;

import org.cyclonedx.model.Component;

public final class ComponentUtils {

    private ComponentUtils() {
    }

    public static String componentString(final Component component) {
        return String.format("%s:%s:%s", component.getGroup(), component.getName(), component.getVersion());
    }
}
