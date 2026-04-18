package cn.ibax.loaderbridge.bridge;

import cn.ibax.loaderbridge.testharness.FakeBridgeEnvironment;
import cn.ibax.loaderbridge.testharness.ModSideCallPoint;
import cn.ibax.loaderbridge.testharness.TestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeManagerTest {
    private BridgeEnvironment previousEnvironment;

    @BeforeEach
    void installFakeEnvironment() {
        previousEnvironment = BridgeManager.environment();
        BridgeManager.install(FakeBridgeEnvironment.standard());
    }

    @AfterEach
    void restoreEnvironment() {
        BridgeManager.install(previousEnvironment);
    }

    @Test
    void shouldExposePluginLookupAndInvocation() {
        assertTrue(BridgeManager.available(), "伪环境应该被视为已启用");
        assertEquals("测试运行时", BridgeManager.runtimeName());
        assertTrue(BridgeManager.capabilities().contains(BridgeCapability.PLUGIN_LOOKUP));
        assertFalse(BridgeManager.capabilities().contains(BridgeCapability.MIXIN_EXTENSION_SLOT), "第一版不应启用 Mixin 扩展槽位");

        Optional<Class<?>> pluginClass = BridgeManager.findPluginClass("示例插件", "cn.ibax.loaderbridge.testharness.TestPlugin");
        assertTrue(pluginClass.isPresent(), "应该能定位到测试插件类");

        BridgeResult<Object> staticResult = BridgeManager.invokePluginStatic(
                "示例插件",
                "cn.ibax.loaderbridge.testharness.TestPlugin",
                "staticGreeting",
                new Class<?>[0]
        );
        assertTrue(staticResult.success(), "静态调用应该成功");
        assertEquals("静态问候", staticResult.value());

        BridgeResult<Object> instanceResult = BridgeManager.invokePluginInstance(
                "示例插件",
                "greet",
                new Class<?>[]{String.class},
                "世界"
        );
        assertTrue(instanceResult.success(), "实例调用应该成功");
        assertEquals("你好，世界", instanceResult.value());

        Optional<Object> service = BridgeManager.findService(TestService.class);
        assertTrue(service.isPresent(), "应该能定位到测试服务");
        assertNotNull(service.get());
    }

    @Test
    void modSideCallPointShouldDelegateToBridgeManager() {
        ModSideCallPoint callPoint = new ModSideCallPoint();

        assertEquals(Optional.of("你好，开发者"), callPoint.greetPlugin("示例插件", "开发者"));
    }
}
