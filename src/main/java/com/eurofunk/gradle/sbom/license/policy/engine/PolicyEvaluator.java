package com.eurofunk.gradle.sbom.license.policy.engine;

import com.eurofunk.gradle.sbom.license.BomUtils;
import com.eurofunk.gradle.sbom.license.policy.model.AndCondition;
import com.eurofunk.gradle.sbom.license.policy.model.CoordinatesCondition;
import com.eurofunk.gradle.sbom.license.policy.model.DependenciesCheck;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroupCondition;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroups;
import com.eurofunk.gradle.sbom.license.policy.model.OrCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import com.eurofunk.gradle.sbom.license.policy.model.Violation;
import org.apache.commons.collections4.CollectionUtils;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;

import java.util.List;

public final class PolicyEvaluator {

    private final LicenseGroups licenseGroups;

    public PolicyEvaluator(final LicenseGroups licenseGroups) {
        this.licenseGroups = licenseGroups;
    }

    public EvaluationResult evaluate(final Policy policy, final Bom bom, final DependenciesCheck dependenciesCheck) {
        if (CollectionUtils.isEmpty(bom.getComponents())) {
            return EvaluationResult.success();
        }

        final List<EvaluationResult> results = BomUtils.getComponents(bom, dependenciesCheck)
                .stream()
                .map(component -> evaluateCondition(policy.rootCondition(), component))
                .toList();

        return EvaluationResult.combineOr(results);
    }

    public EvaluationResult evaluate(final Policy policy, final Component component) {
        return evaluateCondition(policy.rootCondition(), component);
    }

    private EvaluationResult evaluateCondition(final PolicyCondition condition, final Component component) {
        return switch (condition) {
            case final AndCondition andCondition -> evaluateAnd(andCondition, component);
            case final OrCondition orCondition -> evaluateOr(orCondition, component);
            case final CoordinatesCondition coordinatesCond ->
                    new CoordinatesConditionEvaluator().evaluate(coordinatesCond, component);
            case final LicenseGroupCondition licenseCond ->
                    new LicenseGroupConditionEvaluator(licenseGroups).evaluate(licenseCond, component);
            case null, default ->
                    EvaluationResult.failure(new Violation(condition, component, "Unknown condition type"));
        };
    }

    private EvaluationResult evaluateAnd(final AndCondition andCondition, final Component component) {
        final List<EvaluationResult> results = andCondition.conditions()
                .stream()
                .map(c -> evaluateCondition(c, component))
                .toList();
        return EvaluationResult.combineAnd(results);
    }

    private EvaluationResult evaluateOr(final OrCondition orCondition, final Component component) {
        final List<EvaluationResult> results = orCondition.conditions().stream()
                .map(c -> evaluateCondition(c, component))
                .toList();
        return EvaluationResult.combineOr(results);
    }


}
