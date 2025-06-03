package com.eurofunk.gradle.sbom.license;

import org.cyclonedx.model.license.Expression;

/**
 * Builder for building a {@link org.cyclonedx.model.license.Expression}.
 */
public class ExpressionBuilder {

    private final Expression expression;

    public ExpressionBuilder() {
        this.expression = new Expression();
    }

    public ExpressionBuilder withBomRef(final String bomRef) {
        expression.setBomRef(bomRef);
        return this;
    }

    public ExpressionBuilder withValue(final String value) {
        expression.setValue(value);
        return this;
    }

    public Expression build() {
        return expression;
    }
}
