package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.CoordinatesCondition;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import org.cyclonedx.model.Component;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CoordinatesConditionEvaluationTest extends PolicyEvaluatorTest {

    @ParameterizedTest
    @CsvSource({
            "com\\.example, test-artifact, 1\\.2\\.3, MATCHES, com.example, test-artifact, 1.2.3, false, 1",
            "com\\.example, other-artifact, 1\\.2\\.3, MATCHES, com.example, test-artifact, 1.2.3, true, 0",
            "com\\.example, test-artifact, 1\\.2\\.3, DOES_NOT_MATCH, com.example, test-artifact, 1.2.3, true, 0",
            "com\\.example, other-artifact, 1\\.2\\.3, DOES_NOT_MATCH, com.example, test-artifact, 2.0.0, false, 1",
            "com\\.example, test-artifact, 1\\.2\\.3, DOES_NOT_MATCH, com.example, test-artifact, 2.0.0, false, 1",
            "null, test-artifact, null, MATCHES, com.example, test-artifact, 1.2.3, false, 1",
            ".*, null, null, MATCHES, com.example, test-artifact, 1.2.3, false, 1",
            "null, .*, null, MATCHES, com.example, test-artifact, 1.2.3, false, 1",
            "null, null, .*, MATCHES, com.example, test-artifact, 1.2.3, false, 1",
    })
    void testCoordinatesCondition(
            final String groupPattern, final String namePattern, final String versionPattern, final String operator,
            final String componentGroup, final String componentName, final String componentVersion,
            final boolean expectedSuccess, final int expectedViolations) {

        final Component component = new Component();
        component.setGroup(componentGroup);
        component.setName(componentName);
        component.setVersion(componentVersion);

        final CoordinatesCondition.Operator conditionOperator = CoordinatesCondition.Operator.valueOf(operator);
        final PolicyCondition cond = new CoordinatesCondition(
                "null".equals(groupPattern) ? null : groupPattern,
                "null".equals(namePattern) ? null : namePattern,
                "null".equals(versionPattern) ? null : versionPattern,
                conditionOperator
        );

        final Policy policy = new Policy("Test CoordinatesCondition", cond);
        final EvaluationResult result = evaluator.evaluate(policy, component);

        assertEquals(expectedSuccess, result.isSuccess(), "Unexpected evaluation result");
        assertEquals(expectedViolations, result.getViolations().size(), "Unexpected number of violations");
    }

    @ParameterizedTest
    @CsvSource({
            // Empty strings as patterns doesn't match
            "'', '', '', MATCHES, com.example, test-artifact, 1.2.3, true, 0",
            "'', '', '', DOES_NOT_MATCH, com.example, test-artifact, 1.2.3, false, 1",
            // Special characters in patterns
            "com\\.example, test-artifact, 1\\.2\\.3\\+, MATCHES, com.example, test-artifact, 1.2.3+, false, 1",
            "com\\.example, test-artifact, 1\\.2\\.3\\+, DOES_NOT_MATCH, com.example, test-artifact, 1.2.3+, true, 0",
            // Null component fields
            "com\\.example, test-artifact, 1\\.2\\.3, MATCHES, null, null, null, true, 0",
            "com\\.example, test-artifact, 1\\.2\\.3, DOES_NOT_MATCH, null, null, null, true, 0",
            // Mixed null and non-null component fields
            "com\\.example, null, 1\\.2\\.3, MATCHES, com.example, null, 1.2.3, false, 1",
            "com\\.example, null, 1\\.2\\.3, DOES_NOT_MATCH, com.example, null, 1.2.3, true, 0",
            // Patterns with regex metacharacters
            "com\\.example\\..*, test-.*, 1\\.2\\..*, MATCHES, com.example.sub, test-artifact, 1.2.3, false, 1",
            "com\\.example\\..*, test-.*, 1\\.2\\..*, DOES_NOT_MATCH, com.example.sub, test-artifact, 1.2.3, true, 0"
    })
    void testCoordinatesCondition_EdgeCases(
            final String groupPattern, final String namePattern, final String versionPattern, final String operator,
            final String componentGroup, final String componentName, final String componentVersion,
            final boolean expectedSuccess, final int expectedViolations) {

        final Component component = new Component();
        component.setGroup("null".equals(componentGroup) ? null : componentGroup);
        component.setName("null".equals(componentName) ? null : componentName);
        component.setVersion("null".equals(componentVersion) ? null : componentVersion);

        final CoordinatesCondition.Operator conditionOperator = CoordinatesCondition.Operator.valueOf(operator);
        final PolicyCondition cond = new CoordinatesCondition(
                "null".equals(groupPattern) ? null : groupPattern,
                "null".equals(namePattern) ? null : namePattern,
                "null".equals(versionPattern) ? null : versionPattern,
                conditionOperator
        );

        final Policy policy = new Policy("Test CoordinatesCondition Edge Cases", cond);
        final EvaluationResult result = evaluator.evaluate(policy, component);

        assertEquals(expectedSuccess, result.isSuccess(), "Unexpected evaluation result");
        assertEquals(expectedViolations, result.getViolations().size(), "Unexpected number of violations");
    }
}
