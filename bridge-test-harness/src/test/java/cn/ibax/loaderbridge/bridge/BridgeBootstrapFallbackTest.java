package cn.ibax.loaderbridge.bridge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeBootstrapFallbackTest {
    private final BridgeEnvironment previousEnvironment = BridgeManager.environment();

    @AfterEach
    void restoreEnvironment() {
        BridgeManager.install(previousEnvironment);
    }

    @Test
    void bootstrapShouldDisableBridgeWhenBukkitIsMissing() {
        BootstrapReport report = BridgeBootstrap.bootstrap(new Object(), "loaderbridge");

        assertFalse(report.available(), "在没有 Bukkit 运行时的环境里，桥接应当自动降级");
        assertTrue(BridgeManager.currentMod().isPresent(), "即使桥接降级，也应保留当前模组句柄");
        assertTrue(BridgeManager.currentMod().get().modId().equals("loaderbridge"));
    }
}
