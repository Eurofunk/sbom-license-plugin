package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroup;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroups;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.cyclonedx.model.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class ResourceLoader {

    private static final String DEFAULT_LICENSE_GROUPS =
            "/META-INF/com/eurofunk/gradle/sbom-license-plugin/license-groups.json";

    private final ObjectMapper objectMapper;

    public ResourceLoader() {
        this.objectMapper = new ObjectMapper();
    }

    public ResourceLoader(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LicenseGroups loadLicenseGroups(final File file) {
        try {
            final List<LicenseGroup> licenses = objectMapper.readValue(file,
                    new TypeReference<ArrayList<LicenseGroup>>() {
                    });
            return LicenseGroups.of(licenses);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to read license groups from file %s".formatted(file), e);
        }
    }

    public LicenseGroups loadDefaultLicenseGroups() {
        return loadLicenseGroups(loadDefaultLicenseGroupsFile());
    }

    public File loadDefaultLicenseGroupsFile() {
        try (final InputStream inputStream = getClass().getResourceAsStream(DEFAULT_LICENSE_GROUPS)) {
            if (inputStream == null) {
                throw new IllegalStateException("Failed to read default license groups from classpath");
            }
            final File licenseGroupsFile = File.createTempFile("license-groups", ".json");
            licenseGroupsFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(inputStream, licenseGroupsFile);
            return licenseGroupsFile;
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to load default license groups", e);
        }
    }

    public List<Policy> loadPolicies(final File file) {
        try {
            return objectMapper.readValue(file,
                    new TypeReference<ArrayList<Policy>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read policies from file %s".formatted(file), e);
        }
    }

    public List<Component> loadComponents(final File file) {
        try {
            return objectMapper.readValue(file,
                    new TypeReference<ArrayList<Component>>() {
                    });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read custom licenses from file %s".formatted(file), e);
        }
    }
}
