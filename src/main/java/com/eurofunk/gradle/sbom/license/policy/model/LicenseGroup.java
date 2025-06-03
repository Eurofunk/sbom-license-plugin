package com.eurofunk.gradle.sbom.license.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record LicenseGroup(String name,
                           int riskWeight,
                           Set<String> licenses) {

    @JsonCreator
    public LicenseGroup(
            @JsonProperty("name") final String name,
            @JsonProperty("riskWeight") final int riskWeight,
            @JsonProperty("licenses") final Set<String> licenses) {
        this.name = name;
        this.riskWeight = riskWeight;
        this.licenses = licenses;
    }
}