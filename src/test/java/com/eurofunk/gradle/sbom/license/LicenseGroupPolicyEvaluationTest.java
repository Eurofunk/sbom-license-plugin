package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.model.AndCondition;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroupCondition;
import com.eurofunk.gradle.sbom.license.policy.model.OrCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.license.Expression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LicenseGroupPolicyEvaluationTest extends PolicyEvaluatorTest {

    private static Component createComponentWithLicenses(final String... licenseIds) {
        final Component component = new Component();
        final LicenseChoice choice = new LicenseChoice();
        choice.setLicenses(
                java.util.Arrays.stream(licenseIds)
                        .map(id -> {
                            License l = new License();
                            l.setId(id);
                            return l;
                        })
                        .toList()
        );
        component.setLicenses(choice);
        component.setName("TestComponent");
        return component;
    }

    private static Component componentWithLicenseExpression(final String expression) {
        final Component component = new Component();
        final LicenseChoice choice = new LicenseChoice();
        choice.setExpression(new Expression(expression));
        component.setLicenses(choice);
        component.setName("TestComponent");
        return component;
    }

    private EvaluationResult evaluate(
            final String group,
            final LicenseGroupCondition.Operator op,
            final Component component) {
        final PolicyCondition cond = new LicenseGroupCondition(group, op);
        final Policy policy = new Policy("Policy", cond);
        return evaluator.evaluate(policy, component);
    }

    @ParameterizedTest
    @CsvSource({
            // licenseId, group, operator, expectedSuccess, expectedViolations
            "Apache-2.0, Copyleft, IS, true, 0",
            "Apache-2.0, Copyleft, IS_NOT, false, 1",
            "Apache-2.0, Permissive, IS, false, 1",
            "Apache-2.0, Permissive, IS_NOT, true, 0",
    })
    void testSingleLicensePolicies(final String licenseId, final String group, final String op,
                                   final boolean expectedSuccess, final int expectedViolations) {
        final Component component = createComponentWithLicenses(licenseId);
        final EvaluationResult result = evaluate(group, LicenseGroupCondition.Operator.valueOf(op), component);
        assertEquals(expectedSuccess, result.isSuccess());
        assertEquals(expectedViolations, result.getViolations().size());
    }

    @Test
    void testOrAndNestedConditions() {
         final Component gpl = createComponentWithLicenses("GPL-3.0");
         final Component mit = createComponentWithLicenses("MIT");
         final Component mitApache = createComponentWithLicenses("MIT", "Apache-2.0");
         final Component mitGpl = createComponentWithLicenses("MIT", "GPL-3.0");
         final Component bsd = createComponentWithLicenses("BSD-3-Clause");
         final Component eplGpl = createComponentWithLicenses("EPL-2.0", "GPL-3.0");

         final PolicyCondition copyleft = new LicenseGroupCondition("Copyleft", LicenseGroupCondition.Operator.IS);
         final PolicyCondition weakCopyleft = new LicenseGroupCondition("Weak Copyleft", LicenseGroupCondition.Operator.IS);
         final PolicyCondition permissive = new LicenseGroupCondition("Permissive", LicenseGroupCondition.Operator.IS);
         final PolicyCondition mitCond = new LicenseGroupCondition("MIT", LicenseGroupCondition.Operator.IS);
         final PolicyCondition gplCond = new LicenseGroupCondition("GPL", LicenseGroupCondition.Operator.IS);

        // OrCondition
        assertFalse(evaluator.evaluate(new Policy("Or", new OrCondition(List.of(copyleft, weakCopyleft))), gpl).isSuccess());

        // AndCondition
        assertTrue(evaluator.evaluate(new Policy("And", new AndCondition(List.of(permissive, copyleft))), mit).isSuccess());
        assertTrue(evaluator.evaluate(new Policy("And", new AndCondition(List.of(mitCond, gplCond))), bsd).isSuccess());

        // Nested Or with multiple licenses
        assertFalse(evaluator.evaluate(new Policy("Or", new OrCondition(List.of(permissive, copyleft))), mitGpl).isSuccess());

        // Nested And with multiple licenses
        assertFalse(evaluator.evaluate(new Policy("And", new AndCondition(List.of(permissive, permissive))), mitApache).isSuccess());

        // Deeply nested
        PolicyCondition orNested = new OrCondition(List.of(copyleft, weakCopyleft));
        PolicyCondition andNested = new AndCondition(List.of(orNested, permissive));
        assertFalse(evaluator.evaluate(new Policy("Deep", andNested), mitGpl).isSuccess());

        PolicyCondition and2 = new AndCondition(List.of(permissive, mitCond));
        PolicyCondition or2 = new OrCondition(List.of(and2, permissive));
        assertTrue(evaluator.evaluate(new Policy("DeepNone", or2), eplGpl).isSuccess());
    }

    @ParameterizedTest
    @CsvSource({
            "Apache-2.0 AND MIT, Permissive, IS, false, 1",
            "Apache-2.0 AND MIT, Permissive, IS_NOT, true, 0",
            "Apache-2.0 AND MIT, Copyleft, IS_NOT, false, 1",
            "Apache-2.0 AND MIT, Copyleft, IS, true, 0",
            "Apache-2.0, Permissive, IS, false, 1",
            "Apache-2.0, Permissive, IS_NOT, true, 0",
            "Apache-2.0, Copyleft, IS, true, 0",
            "Apache-2.0, Copyleft, IS_NOT, false, 1",
            "Apache-2.0 OR MIT, Permissive, IS, false, 2",
            "Apache-2.0 OR MIT, Permissive, IS_NOT, true, 0",
            "Apache-2.0 OR MIT, Copyleft, IS, true, 0",
            "Apache-2.0 OR MIT, Copyleft, IS_NOT, false, 2",
    })
    void expressionEvaluation(final String expression,
                              final String licenseGroup,
                              final LicenseGroupCondition.Operator operator,
                              final boolean expectedSuccess,
                              final int expectedViolations) {
        final Component component = componentWithLicenseExpression(expression);
        final PolicyCondition condition = new LicenseGroupCondition(licenseGroup, operator);
        final Policy policy = new Policy("Policy", condition);
        final EvaluationResult result = evaluator.evaluate(policy, component);
        assertEquals(expectedSuccess, result.isSuccess());
        assertEquals(expectedViolations, result.getViolations().size());
    }
}
