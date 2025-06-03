package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Policy(String name, PolicyCondition rootCondition) {
    @JsonCreator
    public Policy(
            @JsonProperty("name") String name,
            @JsonProperty("rootCondition") PolicyCondition rootCondition) {
        this.name = name;
        this.rootCondition = rootCondition;
    }
}
