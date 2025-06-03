package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.engine.PolicyEvaluator;

public abstract class PolicyEvaluatorTest {

    protected final ResourceLoader RESOURCE_LOADER = new ResourceLoader();

    protected final PolicyEvaluator evaluator = new PolicyEvaluator(RESOURCE_LOADER.loadDefaultLicenseGroups());


}
