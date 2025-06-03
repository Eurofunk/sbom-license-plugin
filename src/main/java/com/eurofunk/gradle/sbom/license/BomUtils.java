package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.DependenciesCheck;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.BomReference;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.parsers.BomParserFactory;
import org.cyclonedx.parsers.Parser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Utilities for {@link Bom}.
 */
public final class BomUtils {

    private BomUtils() {
    }

    /**
     * @param file with bom content.
     * @return {@link Bom} parsed from {@code} file
     */
    public static Bom getBom(final File file) {
        try {
            final Parser bomParser = BomParserFactory.createParser(file);
            return bomParser.parse(file);
        } catch (final ParseException e) {
            throw new IllegalStateException("Error while getting bom from file %s".formatted(file), e);
        }
    }

    /**
     * Retrieves direct components (direct dependencies) of this bom by {@code bom-ref}.
     *
     * @param bom bom
     * @return {@link Component}s which are direct dependencies of this bom.
     * @throws IllegalStateException when {@code bom-ref} is not present or there are no direct components.
     */
    public static List<Component> getDirectComponents(final Bom bom) {
        final String bomRef = Optional.ofNullable(bom.getMetadata())
                .map(Metadata::getComponent)
                .map(Component::getBomRef)
                .orElseThrow(() -> new IllegalStateException("Unable to get direct dependencies, bom-ref is missing"));
        final Dependency directDependencies = bom.getDependencies()
                .stream()
                .filter(d -> d.getRef().equals(bomRef))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Unable to get direct dependencies by bom-ref %s".formatted(bomRef)));

        final List<String> directDependenciesRefs = directDependencies.getDependencies()
                .stream()
                .map(BomReference::getRef)
                .toList();

        return bom.getComponents()
                .stream()
                .filter(d -> directDependenciesRefs.contains(directDependencies.getRef()))
                .toList();
    }

    public static List<Component> getComponents(final Bom bom, final DependenciesCheck dependenciesCheck) {
        if (dependenciesCheck == DependenciesCheck.TRANSITIVE) {
            return bom.getComponents();
        } else if (dependenciesCheck == DependenciesCheck.DIRECT) {
            return getDirectComponents(bom);
        } else {
            throw new IllegalArgumentException("Unknown dependencies check: " + dependenciesCheck);
        }
    }
}
