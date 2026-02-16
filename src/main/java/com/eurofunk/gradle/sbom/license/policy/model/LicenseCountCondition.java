package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class LicenseCountCondition implements PolicyCondition {
    private final int count;
    private final Operator operator;

    @JsonCreator
    public LicenseCountCondition(
            @JsonProperty("count") final int count,
            @JsonProperty("operator") final Operator operator) {
        this.count = count;
        this.operator = operator;
    }

    public int count() {
        return count;
    }

    public Operator operator() {
        return operator;
    }

    @Override
    public Type getType() {
        return ConditionType.LICENSE_COUNT;
    }

    public enum Operator {
        GREATER_THAN,
        LESS_THAN,
        EQUALS
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LicenseCountCondition) obj;
        return this.count == that.count &&
                Objects.equals(this.operator, that.operator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, operator);
    }

    @Override
    public String toString() {
        return "LicenseCountCondition[" +
                "count=" + count + ", " +
                "operator=" + operator + ']';
    }
}
