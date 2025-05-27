package com.eurofunk.gradle.sbom.license.policy.engine;

import com.eurofunk.gradle.sbom.license.ComponentUtils;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroupCondition;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroups;
import com.eurofunk.gradle.sbom.license.policy.model.Violation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.ossreviewtoolkit.utils.spdx.SpdxCompoundExpression;
import org.ossreviewtoolkit.utils.spdx.SpdxExpression;
import org.ossreviewtoolkit.utils.spdx.SpdxSingleLicenseExpression;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class LicenseGroupConditionEvaluator implements PolicyConditionEvaluator<LicenseGroupCondition> {

    private final LicenseGroups licenseGroups;

    public LicenseGroupConditionEvaluator(final LicenseGroups licenseGroups) {
        this.licenseGroups = licenseGroups;
    }

    private static boolean conditionEvaluationFails(final LicenseGroupCondition condition, final boolean matches) {
        return (condition.operator() == LicenseGroupCondition.Operator.IS && matches)
                || (condition.operator() == LicenseGroupCondition.Operator.IS_NOT && !matches);
    }

    private static String operatorToString(final LicenseGroupCondition.Operator operator) {
        return switch (operator) {
            case IS -> "is";
            case IS_NOT -> "is not";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    @Override
    public EvaluationResult evaluate(final LicenseGroupCondition condition, final Component component) {
        final Set<String> licensesOfGroup = licenseGroups.getOrDefault(condition.groupName(), Set.of());
        final LicenseChoice licenseChoice = component.getLicenses();
        if (licenseChoice == null || CollectionUtils.isEmpty(licenseChoice.getLicenses()) && (
                licenseChoice.getExpression() == null || StringUtils.isBlank(licenseChoice.getExpression().getValue()))) {
            return EvaluationResult.failure(condition, component, "No license found");
        }

        if (licenseChoice.getExpression() != null &&
                StringUtils.isNotBlank(licenseChoice.getExpression().getValue())) {
            return evaluateExpression(condition, component, licenseChoice, List.copyOf(licensesOfGroup));
        }

        if (CollectionUtils.isEmpty(licenseChoice.getLicenses())) {
            // TODO should we considered a failure?
            return EvaluationResult.success();
        }

        final boolean anyMatch = CollectionUtils.emptyIfNull(component.getLicenses().getLicenses())
                .stream()
                .map(License::getId)
                .filter(Objects::nonNull)
                .anyMatch(licensesOfGroup::contains);

        final boolean fails = conditionEvaluationFails(condition, anyMatch);

        if (fails) {
            final String message = String.format(
                    "Component [%s] license group %s the group '%s'",
                    ComponentUtils.componentString(component),
                    operatorToString(condition.operator()),
                    condition.groupName()
            );
            return EvaluationResult.failure(condition, component, message);
        }

        return EvaluationResult.success();
    }

    private EvaluationResult evaluateExpression(
            final LicenseGroupCondition condition,
            final Component component,
            final LicenseChoice licenseChoice,
            final List<String> licensesOfGroup) {

            final SpdxExpression expression = SpdxExpression.parse(licenseChoice.getExpression().getValue());
            if (!isValidLicenseExpression(expression)) {
                return EvaluationResult.failure(condition, component,
                        "License expression [%s] is not valid".formatted(expression.toString()));
            }
            switch (expression) {
                case SpdxCompoundExpression compoundExpression -> {
                    final EvaluationResult result = validateCompoundExpression(condition, component,
                            compoundExpression, licensesOfGroup);
                    if (!result.isSuccess()) {
                        return result;
                    }
                }
                case SpdxSingleLicenseExpression singleLicenseExpression -> {
                    final List<String> componentLicenses = currentLicensesIds(component.getLicenses());
                    final EvaluationResult result = validateSingleLicenseExpression(
                            condition,
                            component,
                            singleLicenseExpression,
                            componentLicenses,
                            licensesOfGroup
                    );
                    if (!result.isSuccess()) {
                        return result;
                    }
                }
            }

        return EvaluationResult.success();
    }

    private boolean isValidLicenseExpression(final SpdxExpression expression) {
        return expression.isValid(SpdxExpression.Strictness.ALLOW_LICENSEREF_EXCEPTIONS);
    }

    private EvaluationResult validateCompoundExpression(
            final LicenseGroupCondition condition,
            final Component component,
            final SpdxCompoundExpression expression,
            final List<String> licensesOfGroup) {
        final List<String> componentLicenses = currentLicensesIds(component.getLicenses());
        final List<Violation> violations = expression.validChoices()
                .stream()
                .map(spdxExpression -> {
                    if (spdxExpression instanceof final SpdxCompoundExpression compoundExpression) {
                        final List<String> expressionLicenses = compoundExpression.licenses();
                        switch (compoundExpression.getOperator()) {
                            case AND -> {
                                final boolean matchesLicenseOfGroup =
                                        expressionLicenses.stream().allMatch(licensesOfGroup::contains);
                                final boolean matches = CollectionUtils.isEmpty(componentLicenses) ?
                                        matchesLicenseOfGroup :
                                        expressionLicenses.stream().allMatch(componentLicenses::contains) &&
                                                matchesLicenseOfGroup;
                                if (conditionEvaluationFails(condition, matches)) {
                                    return expressionInGroupFailure(condition, component, spdxExpression);
                                }
                            }
                            case OR -> {
                                final boolean matchesLicenseOfGroup =
                                        expressionLicenses.stream().anyMatch(licensesOfGroup::contains);
                                final boolean matches = CollectionUtils.isEmpty(componentLicenses) ?
                                        matchesLicenseOfGroup :
                                        expressionLicenses.stream().anyMatch(componentLicenses::contains) &&
                                                matchesLicenseOfGroup;
                                if (conditionEvaluationFails(condition, matches)) {
                                    return expressionInGroupFailure(condition, component, spdxExpression);
                                }
                            }
                        }

                    } else if (spdxExpression instanceof final SpdxSingleLicenseExpression singleLicenseExpression) {
                        final EvaluationResult result = validateSingleLicenseExpression(
                                condition,
                                component,
                                singleLicenseExpression,
                                componentLicenses,
                                licensesOfGroup
                        );
                        if (!result.isSuccess()) {
                            return result;
                        }
                    } else {
                        return EvaluationResult.failure(condition, component,
                                "Unsupported license expression type: %s".formatted(spdxExpression));
                    }
                    return EvaluationResult.success();
                })
                .filter(r -> !r.isSuccess())
                .map(EvaluationResult::getViolations)
                .flatMap(Collection::stream)
                .toList();

        return CollectionUtils.isNotEmpty(violations) ?
                EvaluationResult.failure(violations) :
                EvaluationResult.success();
    }

    private EvaluationResult validateSingleLicenseExpression(
            final LicenseGroupCondition condition,
            final Component component,
            final SpdxSingleLicenseExpression expression,
            final List<String> componentLicenses,
            final List<String> licensesOfGroup
    ) {
        final String licenseId = expression.licenses().get(0);
        final boolean isLicenseOfGroup = licensesOfGroup.contains(licenseId);
        final boolean matches = CollectionUtils.isEmpty(componentLicenses) ? isLicenseOfGroup :
                componentLicenses.contains(licenseId) && isLicenseOfGroup;
        if (conditionEvaluationFails(condition, matches)) {
            return EvaluationResult.failure(condition, component,
                    "Component's [%s] license [%s] of expression [%s] %s in license group [%s]"
                            .formatted(
                                    ComponentUtils.componentString(component),
                                    licenseId,
                                    expression,
                                    operatorToString(condition.operator()),
                                    condition.groupName()
                            ));
        }
        return EvaluationResult.success();
    }

    private EvaluationResult expressionInGroupFailure(final LicenseGroupCondition condition,
                                                      final Component component,
                                                      final SpdxExpression expression) {
        return EvaluationResult.failure(condition, component,
                "Component's [%s] license expression [%s] %s in license group [%s]"
                        .formatted(
                                ComponentUtils.componentString(component),
                                expression,
                                operatorToString(condition.operator()),
                                condition.groupName()
                        ));
    }

    private List<String> currentLicensesIds(final LicenseChoice licenseChoice) {
        return Optional.ofNullable(licenseChoice)
                .map(LicenseChoice::getLicenses)
                .filter(CollectionUtils::isNotEmpty)
                .stream()
                .flatMap(Collection::stream)
                .map(License::getId)
                .toList();
    }


}
