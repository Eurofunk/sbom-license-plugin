package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OrCondition(List<PolicyCondition> conditions) implements PolicyCondition {
    @JsonCreator
    public OrCondition(@JsonProperty("conditions") final List<PolicyCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public Type getType() {
        return LogicalType.OR;
    }
}
