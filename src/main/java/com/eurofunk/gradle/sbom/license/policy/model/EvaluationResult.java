package com.eurofunk.gradle.sbom.license.policy.model;

import org.cyclonedx.model.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public final class EvaluationResult {
    private final boolean success;
    private final List<Violation> violations;

    public EvaluationResult(boolean success, List<Violation> violations) {
        this.success = success;
        this.violations = violations;
    }

    public static EvaluationResult success() {
        return new EvaluationResult(true, new LinkedList<>());
    }

    public static EvaluationResult failure(final Violation violation) {
        final List<Violation> violations = new LinkedList<>();
        violations.add(violation);
        return new EvaluationResult(false, violations);
    }

    public static EvaluationResult failure(
            final PolicyCondition condition,
            final Component component,
            final String message) {
        return failure(new Violation(condition, component, message));
    }

    public static EvaluationResult failure(final List<Violation> violations) {
        return new EvaluationResult(false, violations);
    }

    public static EvaluationResult combineAnd(final List<EvaluationResult> results) {
        boolean success = results.stream().anyMatch(EvaluationResult::isSuccess);
        if (!success) {
            final List<Violation> violations =
                    results.stream()
                            .filter(EvaluationResult::isFailure)
                            .map(EvaluationResult::getViolations)
                            .flatMap(Collection::stream).toList();
            return EvaluationResult.failure(violations);
        }
        return EvaluationResult.success();
    }

    public static EvaluationResult combineOr(final List<EvaluationResult> results) {
        final List<Violation> combined = new ArrayList<>();
        boolean result = true;
        for (final EvaluationResult er : results) {
            if (er.isFailure()) {
                result = false;
            }
            combined.addAll(er.getViolations());
        }
        return new EvaluationResult(result, combined);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
