package com.eurofunk.gradle.sbom.license.policy.engine;

import com.eurofunk.gradle.sbom.license.ComponentUtils;
import com.eurofunk.gradle.sbom.license.policy.model.CoordinatesCondition;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.Violation;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Component;

public class CoordinatesConditionEvaluator implements PolicyConditionEvaluator<CoordinatesCondition> {

    private static boolean matches(
            final CoordinatesCondition.Operator operator,
            final String componentPart,
            final String conditionPart) {
        if (conditionPart == null) {
            return true;
        }

        if (StringUtils.isNotEmpty(componentPart)) {
            if (CoordinatesCondition.Operator.MATCHES == operator) {
                return componentPart.matches(conditionPart);
            } else if (CoordinatesCondition.Operator.DOES_NOT_MATCH == operator) {
                return !componentPart.matches(conditionPart);
            }
        }

        return false;
    }

    private static Violation coordinatesViolation(
            final CoordinatesCondition condition,
            final Component component,
            final CoordinatesCondition.Operator operator) {
        return new Violation(condition, component,
                "Component [%s] coordinates %s [%s:%s:%s]".formatted(ComponentUtils.componentString(component),
                        operator.name(), condition.group(), condition.name(), condition.version()));
    }

    @Override
    public EvaluationResult evaluate(CoordinatesCondition condition, Component component) {
        final CoordinatesCondition.Operator operator = condition.operator();

        final boolean groupMatch = matches(operator, component.getGroup(), condition.group());
        final boolean nameMatch = matches(operator, component.getName(), condition.name());
        final boolean versionMatch = matches(operator, component.getVersion(), condition.version());

        if (CoordinatesCondition.Operator.MATCHES == condition.operator() && groupMatch && nameMatch && versionMatch) {
            return EvaluationResult.failure(coordinatesViolation(condition, component, operator));
        } else if (CoordinatesCondition.Operator.DOES_NOT_MATCH == condition.operator() &&
                ((condition.group() != null && groupMatch)
                        || (condition.name() != null && nameMatch)
                        || (condition.version() != null && versionMatch))) {
            return EvaluationResult.failure(coordinatesViolation(condition, component, operator));
        }

        return EvaluationResult.success();
    }
}