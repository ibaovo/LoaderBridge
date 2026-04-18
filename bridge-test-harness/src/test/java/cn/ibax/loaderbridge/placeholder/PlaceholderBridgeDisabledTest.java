package cn.ibax.loaderbridge.placeholder;

import cn.ibax.loaderbridge.bridge.BridgeEnvironment;
import cn.ibax.loaderbridge.bridge.BridgeManager;
import cn.ibax.loaderbridge.testharness.FakeBridgeEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderBridgeDisabledTest {
    private BridgeEnvironment previousEnvironment;

    @BeforeEach
    void installEnvironment() {
        previousEnvironment = BridgeManager.environment();
        BridgeManager.install(FakeBridgeEnvironment.standard());
        PlaceholderBridge.resetForTesting();
    }

    @AfterEach
    void restoreEnvironment() {
        PlaceholderBridge.resetForTesting();
        BridgeManager.install(previousEnvironment);
    }

    @Test
    void shouldRemainDisabledWhenPlaceholderApiIsMissing() {
        PlaceholderBootstrapReport report = PlaceholderBridge.bootstrap();

        assertNotNull(report, "启动报告不能为空");
        assertFalse(report.enabled(), "当没有 PlaceholderAPI 时，占位符桥应保持禁用");
        assertFalse(PlaceholderBridge.available(), "禁用状态下不应对外宣称可用");

        PlaceholderRegistrationResult result = PlaceholderBridge.register("version", context -> "1.0.0");
        assertFalse(result.registered(), "禁用状态下注册应被拒绝");
        assertEquals(PlaceholderRegistrationStatus.DISABLED, result.status(), "状态应明确标记为禁用");

        assertEquals("原始文本", PlaceholderBridge.render("原始文本", PlaceholderContext.empty()));
        assertEquals(Optional.empty(), PlaceholderBridge.namespaces().stream().findFirst());
        assertTrue(PlaceholderBridge.definitions().isEmpty(), "禁用状态下不应暴露任何定义");
    }
}
