package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.AndCondition;
import com.eurofunk.gradle.sbom.license.policy.model.CoordinatesCondition;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroupCondition;
import com.eurofunk.gradle.sbom.license.policy.model.OrCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PolicySerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializeAndDeserializeTest() throws JsonProcessingException {
        final PolicyCondition coordinates = new CoordinatesCondition("group", "name", "version", CoordinatesCondition.Operator.MATCHES);
        final PolicyCondition licenseGroup = new LicenseGroupCondition("another-test-troup",
                LicenseGroupCondition.Operator.IS_NOT);
        final PolicyCondition orCondition = new OrCondition(List.of(coordinates, licenseGroup));
        final PolicyCondition andCondition = new AndCondition(List.of(orCondition,
                new LicenseGroupCondition("test-group", LicenseGroupCondition.Operator.IS)));
        final Policy policy = new Policy("Test Policy", andCondition);

        final String policyString = objectMapper.writeValueAsString(policy);

        final Policy deserializedPolicy = objectMapper.readValue(policyString, Policy.class);
        assertEquals(deserializedPolicy, policy);
    }
}
