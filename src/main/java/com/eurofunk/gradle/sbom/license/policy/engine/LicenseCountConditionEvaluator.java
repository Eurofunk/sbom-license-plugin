package com.eurofunk.gradle.sbom.license.policy.engine;

import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseCountCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Violation;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.LicenseChoice;
import org.ossreviewtoolkit.utils.spdx.SpdxExpression;


public class LicenseCountConditionEvaluator implements PolicyConditionEvaluator<LicenseCountCondition> {

    @Override
    public EvaluationResult evaluate(final LicenseCountCondition condition, final Component component) {
        final LicenseChoice licenseChoice = component.getLicenses();
        if (licenseChoice == null) {
            return check(0, condition, component);
        }

        int licenseCount = 0;
        if (licenseChoice.getExpression() != null && licenseChoice.getExpression().getValue() != null) {
            licenseCount = countLicenses(licenseChoice.getExpression().getValue());
        } else if (licenseChoice.getLicenses() != null) {
            licenseCount = licenseChoice.getLicenses().size();
        }

        return check(licenseCount, condition, component);
    }

    private EvaluationResult check(
            final int licenseCount,
            final LicenseCountCondition condition,
            final Component component) {
        boolean violated = switch (condition.operator()) {
            case GREATER_THAN -> licenseCount > condition.count();
            case LESS_THAN -> licenseCount < condition.count();
            case EQUALS -> licenseCount == condition.count();
        };

        if (violated) {
            return EvaluationResult.failure(new Violation(condition, component,
                    String.format("Component has %d licenses, which violates the condition %s %d",
                            licenseCount, condition.operator(), condition.count())));
        }

        return EvaluationResult.success();
    }

    private int countLicenses(final String expressionValue) {
        if (expressionValue == null || expressionValue.trim().isEmpty()) {
            return 0;
        }

        final SpdxExpression expression = SpdxExpression.parse(expressionValue);
        return expression.licenses().size();
    }
}
