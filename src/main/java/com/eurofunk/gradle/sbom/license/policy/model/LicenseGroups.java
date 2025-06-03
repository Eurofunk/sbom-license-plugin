package com.eurofunk.gradle.sbom.license.policy.model;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LicenseGroups {

    private final Map<String, LicenseGroup> licenseGroups;

    public LicenseGroups(final Map<String, LicenseGroup> licenseGroups) {
        this.licenseGroups = licenseGroups;
    }

    public static LicenseGroups of(final Collection<LicenseGroup> licenseGroups) {
        final Map<String, LicenseGroup> map = licenseGroups.stream()
                .collect(Collectors.toMap(LicenseGroup::name, lg -> lg));
        return new LicenseGroups(map);
    }

    public boolean isLicenseOfGroup(final String licenseId, final String group) {
        return Optional.ofNullable(licenseGroups.get(group))
                .stream()
                .anyMatch(lg -> lg.licenses().contains(licenseId));
    }

    public Set<String> getOrDefault(final String groupName, final Set<Object> of) {
        return Optional.ofNullable(licenseGroups.get(groupName))
                .map(LicenseGroup::licenses)
                .orElse(Set.of());
    }
}
