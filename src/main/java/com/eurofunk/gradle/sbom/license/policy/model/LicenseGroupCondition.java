package com.eurofunk.gradle.sbom.license.policy.model;

public record LicenseGroupCondition(
        String groupName,
        LicenseGroupCondition.Operator operator) implements PolicyCondition {
    @Override
    public Type getType() {
        return ConditionType.LICENSE_GROUP;
    }


    public enum Operator {IS, IS_NOT}
}
