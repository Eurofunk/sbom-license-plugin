package com.eurofunk.gradle.sbom.license;

import org.junit.jupiter.api.Test;
import org.ossreviewtoolkit.utils.spdx.SpdxExpression;

public class ExpressionTest {

    @Test
    void testit() {
        final SpdxExpression expression = SpdxExpression.parse("GPL-3.0 AND (MIT OR Apache-2.0)");
        final SpdxExpression expressionException = SpdxExpression.parse("GPL-3.0 WITH Classpath-exception-2.0");
        System.out.println(expression);
    }
}
