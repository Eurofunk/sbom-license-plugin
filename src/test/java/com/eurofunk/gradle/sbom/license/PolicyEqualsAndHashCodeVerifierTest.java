package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class PolicyEqualsAndHashCodeVerifierTest {

    @Test
    void policiesOverridesEqualsAndHashCode() {
        try (final ScanResult scanResult = new ClassGraph().whitelistPackages("com.eurofunk")
                .enableClassInfo().scan()) {
            for (final ClassInfo ci : scanResult.getClassesImplementing(PolicyCondition.class)) {
                EqualsVerifier.forClass(ci.loadClass()).verify();
            }
        }
    }
}
