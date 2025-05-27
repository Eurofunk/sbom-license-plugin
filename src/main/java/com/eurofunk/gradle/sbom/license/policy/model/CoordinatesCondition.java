package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public record CoordinatesCondition(
        String group,
        String name,
        String version,
        Operator operator
) implements PolicyCondition {
    @JsonCreator
    public CoordinatesCondition(
            @JsonProperty("group") String group,
            @JsonProperty("name") String name,
            @JsonProperty("version") String version,
            @JsonProperty("operator") Operator operator) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.operator = operator;
    }

    @Override
    public Type getType() {
        return ConditionType.COORDINATES;
    }

    @Override
    public @NotNull String toString() {
        return "%s %s [%s:%s:%s]".formatted(getType(), operator, group, name, version);
    }

    public enum Operator {MATCHES, DOES_NOT_MATCH}
}