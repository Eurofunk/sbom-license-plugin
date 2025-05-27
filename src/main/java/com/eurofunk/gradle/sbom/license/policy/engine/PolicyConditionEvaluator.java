package com.eurofunk.gradle.sbom.license.policy.engine;

import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.PolicyCondition;
import org.cyclonedx.model.Component;

public interface PolicyConditionEvaluator<T extends PolicyCondition> {
    EvaluationResult evaluate(T condition, Component component);
}