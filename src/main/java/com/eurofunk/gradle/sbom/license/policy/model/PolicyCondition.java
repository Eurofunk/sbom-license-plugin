package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndCondition.class, name = "AND"),
        @JsonSubTypes.Type(value = OrCondition.class, name = "OR"),
        @JsonSubTypes.Type(value = CoordinatesCondition.class, name = "COORDINATES"),
        @JsonSubTypes.Type(value = LicenseGroupCondition.class, name = "LICENSE_GROUP"),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface PolicyCondition {

    Type getType();
}
