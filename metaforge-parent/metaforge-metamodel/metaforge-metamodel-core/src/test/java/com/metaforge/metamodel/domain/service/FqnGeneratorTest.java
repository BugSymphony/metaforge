package com.metaforge.metamodel.domain.service;

import com.metaforge.metamodel.domain.model.valueobject.FqnParts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FQN 统一生成器单元测试（场景 3）。
 */
class FqnGeneratorTest {

    private final FqnGenerator fqnGenerator = new FqnGeneratorImpl();

    @Test
    void testGenerateBundleFqn() {
        assertEquals("order", fqnGenerator.bundle("order"));
    }

    @Test
    void testGenerateBundleVersionFqn() {
        assertEquals("order:1.0.0", fqnGenerator.bundleVersion("order", "1.0.0"));
    }

    @Test
    void testGenerateEntitySchemaFqn() {
        assertEquals("order:1.0.0.pkg_order.Order",
                fqnGenerator.entitySchema("order:1.0.0.pkg_order", "Order"));
    }

    @Test
    void testParseFqn() {
        FqnParts parts = fqnGenerator.parse("order:1.0.0.pkg_order.Order");
        assertEquals("order", parts.bundleCode());
        assertEquals("1.0.0", parts.version());
        assertEquals(List.of("pkg_order"), parts.segments());
        assertEquals("Order", parts.shortName());
        assertEquals("order:1.0.0.pkg_order", parts.parentFqn());
    }

    @Test
    void testStripTypePrefix() {
        String stripped = fqnGenerator.stripTypePrefix("entity:order:1.0.0.pkg_order.Order");
        assertEquals("order:1.0.0.pkg_order.Order", stripped);
    }

    @Test
    void testToFilePath() {
        assertEquals("order/1.0.0/pkg_order/Order.json",
                fqnGenerator.toFilePath("order:1.0.0.pkg_order.Order"));
    }
}
