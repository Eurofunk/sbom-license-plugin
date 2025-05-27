package com.eurofunk.gradle.sbom.license;

import com.eurofunk.gradle.sbom.license.policy.engine.PolicyEvaluator;
import com.eurofunk.gradle.sbom.license.policy.model.DependenciesCheck;
import com.eurofunk.gradle.sbom.license.policy.model.EvaluationResult;
import com.eurofunk.gradle.sbom.license.policy.model.LicenseGroups;
import com.eurofunk.gradle.sbom.license.policy.model.Policy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.license.Expression;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CheckLicensesTask extends DefaultTask {

    /**
     * At which level are dependencies checked.
     */
    private Property<DependenciesCheck> dependenciesCheck;
    /**
     * License groups config. If this property is configured it has precedence before {@link #licenseGroupsFile}.
     */
    private Property<LicenseGroups> licenseGroups;
    /**
     * License groups file. If not configured and also {@link #licenseGroups} is not configured then default from
     * classpath is used.
     */
    private Property<File> licenseGroupsFile;
    /**
     * Policies config. If this property is configured it has precedence before {@link #policiesFile}.
     */
    private ListProperty<Policy> policies;
    /**
     * Policies file.
     */
    private Property<File> policiesFile;
    /**
     * Path to the bom file.
     */
    private Property<File> sbomFile;

    /**
     * Customization of licenses. If this property is configured it has precedence before {@link #customLicensesFile}.
     */
    private ListProperty<Component> customLicenses;
    /**
     * Custom licenses file.
     */
    private Property<File> customLicensesFile;

    private Config config;
    private ResourceLoader resourceLoader;

    public CheckLicensesTask() throws IOException {
        resourceLoader = new ResourceLoader();
        dependenciesCheck = getProject().getObjects().property(DependenciesCheck.class);
        dependenciesCheck.convention(DependenciesCheck.TRANSITIVE);

        licenseGroups = getProject().getObjects().property(LicenseGroups.class);

        licenseGroupsFile = getProject().getObjects().property(File.class);

        final File licenseGroupsFileValue = resourceLoader.loadDefaultLicenseGroupsFile();
        licenseGroupsFile.convention(licenseGroupsFileValue);

        policies = getProject().getObjects().listProperty(Policy.class);
        policiesFile = getProject().getObjects().property(File.class);

        sbomFile = getProject().getObjects().property(File.class);
        customLicenses = getProject().getObjects().listProperty(Component.class);
        customLicensesFile = getProject().getObjects().property(File.class);
    }

    private static Bom getBom(final Config config) {
        final Bom bom = BomUtils.getBom(config.sbomFile());
        if (CollectionUtils.isNotEmpty(config.customLicenses())) {
            for (final Component custLic : config.customLicenses()) {
                bom.getComponents()
                        .stream()
                        .filter(c -> matches(c.getGroup(), custLic.getGroup()) &&
                                matches(c.getName(), custLic.getName()) &&
                                matches(c.getVersion(), custLic.getVersion()) &&
                                matches(c.getBomRef(), custLic.getBomRef()))
                        .forEach(c -> mapLicense(c, custLic));
                        // TODO throw an exception if the custom license component is not found?
            }
        }
        return bom;
    }

    private static void mapLicense(final Component component, final Component customLicense) {
        if (component == null || customLicense == null) {
            throw new IllegalArgumentException("Component and customLicense must not be null");
        }
        final LicenseChoice customLicenseChoice = customLicense.getLicenses();
        final LicenseChoice componentLicenseChoice = component.getLicenses();

        if (customLicenseChoice != null) {
            final Expression expression = customLicenseChoice.getExpression();
            if (expression != null && StringUtils.isNotEmpty(expression.getValue())) {
                if (componentLicenseChoice == null) {
                    component.setLicenses(customLicenseChoice);
                }
                java.util.Optional.ofNullable(component.getLicenses())
                        .map(LicenseChoice::getExpression)
                        .ifPresentOrElse(e -> e.setValue(expression.getValue()),
                                () -> componentLicenseChoice.setExpression(
                                        new Expression(expression.getValue())));
            }
            if (CollectionUtils.isNotEmpty(customLicenseChoice.getLicenses())) {

                if (componentLicenseChoice == null) {
                    component.setLicenses(customLicenseChoice);
                    return;
                }
                final List<License> newLicenses = new LinkedList<>();
                for (final License license : CollectionUtils.emptyIfNull(customLicenseChoice.getLicenses())) {
                    CollectionUtils.emptyIfNull(componentLicenseChoice.getLicenses())
                            .stream()
                            .filter(l -> matches(l.getId(), license.getId()))
                            .findFirst()
                            .ifPresentOrElse(l -> {
                                if (StringUtils.isNotEmpty(license.getName())) {
                                    l.setName(license.getName());
                                }
                                if (StringUtils.isNotEmpty(license.getUrl())) {
                                    l.setUrl(license.getUrl());
                                }
                                if (StringUtils.isNotEmpty(license.getBomRef())) {
                                    l.setBomRef(license.getBomRef());
                                }
                                l.setLicenseText(mapAttachmentText(l.getAttachmentText(), license.getAttachmentText()));
                            }, () -> newLicenses.add(license));
                }
                if (!newLicenses.isEmpty()) {
                    if (componentLicenseChoice.getLicenses() == null) {
                        componentLicenseChoice.setLicenses(newLicenses);
                    } else {
                        componentLicenseChoice.getLicenses().addAll(newLicenses);
                    }
                }
            }
        }
    }

    private static AttachmentText mapAttachmentText(
            final AttachmentText componentAttachmentText,
            final AttachmentText customLicenseAttachmentText) {
        if (customLicenseAttachmentText == null) {
            return componentAttachmentText;
        }
        final AttachmentText modifiedAttachmentText = componentAttachmentText == null ?
                new AttachmentText() : componentAttachmentText;

        if (StringUtils.isNotEmpty(customLicenseAttachmentText.getText())) {
            modifiedAttachmentText.setText(customLicenseAttachmentText.getText());
        }
        if (StringUtils.isNotEmpty(customLicenseAttachmentText.getContentType())) {
            modifiedAttachmentText.setContentType(customLicenseAttachmentText.getContentType());
        }
        if (StringUtils.isNotEmpty(customLicenseAttachmentText.getEncoding())) {
            modifiedAttachmentText.setEncoding(customLicenseAttachmentText.getEncoding());
        }
        return modifiedAttachmentText;
    }

    private static boolean matches(final String componentPart, final String conditionPart) {
        return conditionPart == null || componentPart.matches(conditionPart);
    }

    @Input
    @Optional
    public Property<DependenciesCheck> getDependenciesCheck() {
        return dependenciesCheck;
    }

    public void setDependenciesCheck(final DependenciesCheck dependenciesCheck) {
        this.dependenciesCheck.set(dependenciesCheck);
    }

    @Input
    @Optional
    public Property<LicenseGroups> getLicenseGroups() {
        return licenseGroups;
    }

    public void setLicenseGroups(final LicenseGroups licenseGroups) {
        this.licenseGroups.set(licenseGroups);
    }

    @Input
    @Optional
    public Property<File> getLicenseGroupsFile() {
        return licenseGroupsFile;
    }

    public void setLicenseGroupsFile(final File licenseGroupsFile) {
        this.licenseGroupsFile.set(licenseGroupsFile);
    }

    @Input
    @Optional
    public ListProperty<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(final List<Policy> policies) {
        this.policies.set(policies);
    }

    @Input
    @Optional
    public Property<File> getPoliciesFile() {
        return policiesFile;
    }

    public void setPoliciesFile(final File policiesFile) {
        this.policiesFile.set(policiesFile);
    }

    @Input
    public Property<File> getSbomFile() {
        return sbomFile;
    }

    public void setSbomFile(final File sbomFile) {
        this.sbomFile.set(sbomFile);
    }

    @Input
    @Optional
    public ListProperty<Component> getCustomLicenses() {
        return customLicenses;
    }

    public void setCustomLicenses(final List<Component> customLicenses) {
        this.customLicenses.set(customLicenses);
    }

    @Input
    @Optional
    public Property<File> getCustomLicensesFile() {
        return customLicensesFile;
    }

    public void setCustomLicensesFile(final File customLicensesFile) {
        this.customLicensesFile.set(customLicensesFile);
    }

    @Internal
    public Config getConfig() {
        return config;
    }

    @Internal
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    private void init() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ResourceLoader resourceLoader = new ResourceLoader(objectMapper);

        final LicenseGroups licenseGroups = getLicenseGroups()
                .getOrElse(resourceLoader.loadLicenseGroups(licenseGroupsFile.get()));

        final List<Policy> policies = this.policies.filter(CollectionUtils::isNotEmpty)
                .orElse(policiesFile.map(resourceLoader::loadPolicies))
                .get();

        final List<Component> customLicenses = this.customLicenses.filter(CollectionUtils::isNotEmpty)
                .orElse(customLicensesFile.map(resourceLoader::loadComponents))
                .getOrElse(Collections.emptyList());

        this.config = new Config(licenseGroups, policies, sbomFile.get(), dependenciesCheck.get(), customLicenses);
    }

    @TaskAction
    public void checkLicenses() {
        init();
        final Bom bom = getBom(config);
        final PolicyEvaluator policyEvaluator = new PolicyEvaluator(config.licenseGroups());
        final List<Policy> policies = this.config.policies();
        final Map<Policy, EvaluationResult> executionResult = new LinkedHashMap<>(policies.size());
        boolean hasError = false;
        for (final Policy policy : policies) {
            final EvaluationResult result = policyEvaluator.evaluate(policy, bom, config.dependenciesCheck());
            executionResult.put(policy, result);
            if (!result.isSuccess()) {
                hasError = true;
            }
        }

        executionResult.forEach((policy, result) -> {
            if (result.isSuccess()) {
                getLogger().debug("Policy [{}] evaluated without errors", policy.name());
            } else {
                getLogger().error("Policy [{}] has violations: [{}]", policy.name(), result.getViolations());
            }
        });

        if (hasError) {
            throw new GradleException("Some policy violations were detected. See log for details.");
        } else {
            getProject().getLogger().lifecycle("No policy violations detected. All dependencies are compliant " +
                    "with the configured policies.");
        }
    }

    public record Config(LicenseGroups licenseGroups, List<Policy> policies, File sbomFile,
                         DependenciesCheck dependenciesCheck, List<Component> customLicenses) {
    }
}
