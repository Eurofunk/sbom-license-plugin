package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.engine.PolicyEvaluator;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseCountCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.license.Expression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseCountConditionEvaluationTest {

    private PolicyEvaluator policyEvaluator;

    @BeforeEach
    void setUp() {
        policyEvaluator = new PolicyEvaluator(null); // LicenseGroups not needed for this test
    }

    private Component createComponentWithExpression(String expression) {
        Component component = new Component();
        component.setGroup("com.example");
        component.setName("test-component");
        component.setVersion("1.0.0");

        LicenseChoice licenseChoice = new LicenseChoice();
        Expression licExpression = new Expression();
        licExpression.setValue(expression);
        licenseChoice.setExpression(licExpression);
        component.setLicenses(licenseChoice);

        return component;
    }

    @Test
    void whenComponentHasMoreLicensesThanAllowed_evaluationFails() {
        // given
        Component component = createComponentWithExpression("MIT OR Apache-2.0");
        Policy policy = new Policy("Fail on more than one license",
                new LicenseCountCondition(1, LicenseCountCondition.Operator.GREATER_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with 2 licenses when only 1 is allowed");
    }

    @Test
    void whenComponentHasAllowedNumberOfLicenses_evaluationSucceeds() {
        // given
        Component component = createComponentWithExpression("MIT");
        Policy policy = new Policy("Fail on more than one license",
                new LicenseCountCondition(1, LicenseCountCondition.Operator.GREATER_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with 1 license");
    }
    
    @Test
    void whenComponentHasConjunctiveLicenses_evaluationFailsForGreaterThanOne() {
        // given
        Component component = createComponentWithExpression("MIT AND Apache-2.0");
        Policy policy = new Policy("Fail on more than one license",
                new LicenseCountCondition(1, LicenseCountCondition.Operator.GREATER_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with 2 conjunctive licenses");
    }

    @Test
    void whenComponentHasNoLicenseAndPolicyAllowsZero_evaluationSucceeds() {
        // given
        Component component = new Component();
        component.setName("no-license-component");
        Policy policy = new Policy("Fail on more than zero licenses",
                new LicenseCountCondition(0, LicenseCountCondition.Operator.GREATER_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with 0 licenses");
    }

    @Test
    void whenComponentHasExactNumberOfLicenses_equalsOperatorFails() {
        // given
        Component component = createComponentWithExpression("MIT OR Apache-2.0");
        Policy policy = new Policy("Fail on exactly 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.EQUALS));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with exactly 2 licenses");
    }

    @Test
    void whenComponentHasDifferentNumberOfLicenses_equalsOperatorSucceeds() {
        // given
        Component component = createComponentWithExpression("MIT");
        Policy policy = new Policy("Fail on exactly 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.EQUALS));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with 1 license when 2 are prohibited");
    }

    @Test
    void whenComponentHasFewerLicensesThanProhibited_lessThanOperatorFails() {
        // given
        Component component = createComponentWithExpression("MIT");
        Policy policy = new Policy("Fail on less than 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.LESS_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with 1 license when less than 2 are prohibited");
    }

    @Test
    void whenComponentHasMoreLicensesThanProhibited_lessThanOperatorSucceeds() {
        // given
        Component component = createComponentWithExpression("MIT OR Apache-2.0 OR GPL-3.0");
        Policy policy = new Policy("Fail on less than 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.LESS_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with 3 licenses when less than 2 are prohibited");
    }

    @Test
    void whenComponentHasExactlyThresholdLicenses_lessThanOperatorSucceeds() {
        // given
        Component component = createComponentWithExpression("MIT OR Apache-2.0");
        Policy policy = new Policy("Fail on less than 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.LESS_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with exactly 2 licenses when less than 2 are prohibited");
    }

    @Test
    void whenComponentHasExactlyThresholdLicenses_greaterThanOperatorSucceeds() {
        // given
        Component component = createComponentWithExpression("MIT OR Apache-2.0");
        Policy policy = new Policy("Fail on more than 2 licenses",
                new LicenseCountCondition(2, LicenseCountCondition.Operator.GREATER_THAN));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertTrue(result.isSuccess(), "Evaluation should succeed for component with exactly 2 licenses when more than 2 are prohibited");
    }

    @Test
    void whenComponentHasEmptyExpression_evaluationHandlesAsZero() {
        // given
        Component component = createComponentWithExpression("");
        Policy policy = new Policy("Fail on exactly 0 licenses",
                new LicenseCountCondition(0, LicenseCountCondition.Operator.EQUALS));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with empty expression (0 licenses)");
    }

    @Test
    void whenComponentHasBlankExpression_evaluationHandlesAsZero() {
        // given
        Component component = createComponentWithExpression("   ");
        Policy policy = new Policy("Fail on exactly 0 licenses",
                new LicenseCountCondition(0, LicenseCountCondition.Operator.EQUALS));

        // when
        EvaluationResult result = policyEvaluator.evaluate(policy, component);

        // then
        assertFalse(result.isSuccess(), "Evaluation should fail for component with blank expression (0 licenses)");
    }
}
