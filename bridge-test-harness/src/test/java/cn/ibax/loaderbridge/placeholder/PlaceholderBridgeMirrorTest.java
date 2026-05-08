package cn.ibax.loaderbridge.placeholder;

import cn.ibax.loaderbridge.bridge.BridgeEnvironment;
import cn.ibax.loaderbridge.bridge.BridgeManager;
import cn.ibax.loaderbridge.testharness.FakeBridgeEnvironment;
import cn.ibax.loaderbridge.testharness.FakePlaceholderApiPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderBridgeMirrorTest {
    private BridgeEnvironment previousEnvironment;

    @BeforeEach
    void installEnvironment() {
        previousEnvironment = BridgeManager.environment();
        BridgeManager.install(FakeBridgeEnvironment.withPlaceholderApi());
        PlaceholderBridge.resetForTesting();
        PlaceholderBridge.installNamespaceResolverForTesting(() -> Optional.of("testmod"));
    }

    @AfterEach
    void restoreEnvironment() {
        PlaceholderBridge.resetForTesting();
        BridgeManager.install(previousEnvironment);
    }

    @Test
    void shouldMirrorLocalPlaceholdersIntoPlaceholderApi() {
        PlaceholderBootstrapReport report = PlaceholderBridge.bootstrap();

        assertTrue(report.enabled(), report.toString());
        assertTrue(report.placeholderApiAvailable(), report.toString());
        assertTrue(PlaceholderBridge.available(), "占位符桥启用后应可用");

        PlaceholderRegistrationResult status = PlaceholderBridge.register("status", context -> "在线");
        assertTrue(status.registered(), "本地占位符应注册成功");
        assertEquals("testmod", status.key().namespace(), "默认 namespace 应来自调用方 modid");
        assertEquals("status", status.key().path(), "路径应保持原样");

        PlaceholderRegistrationResult runtime = PlaceholderBridge.register("runtime", context -> "Forge");
        assertTrue(runtime.registered(), "同一 namespace 下的其他 exact 占位符也应注册成功");

        PlaceholderRegistrationResult modid = PlaceholderBridge.register("modid", context -> "loaderbridge");
        assertTrue(modid.registered(), "同一 namespace 下的第三个 exact 占位符也应注册成功");

        PlaceholderRegistrationResult duplicate = PlaceholderBridge.register("status", context -> "重复");
        assertFalse(duplicate.registered(), "同一个 key 的重复注册应被拒绝");
        assertEquals(PlaceholderRegistrationStatus.DUPLICATE, duplicate.status(), "重复注册应返回明确状态");

        assertEquals("状态=在线", PlaceholderBridge.render("状态=%testmod_status%", PlaceholderContext.empty()));

        FakePlaceholderApiPlugin plugin = FakePlaceholderApiPlugin.instance();
        assertTrue(plugin.getLocalExpansionManager().getIdentifiers().contains("testmod"), "PAPI 应看到回挂的命名空间");

        PlaceholderExpansion expansion = plugin.getLocalExpansionManager()
                .findExpansionByIdentifier("testmod")
                .orElseThrow();
        assertEquals("在线", expansion.onRequest(null, "status"), "插件侧应能解析 status 占位符");
        assertEquals("Forge", expansion.onRequest(null, "runtime"), "插件侧应按参数分发到 runtime 占位符");
        assertEquals("loaderbridge", expansion.onRequest(null, "modid"), "插件侧应按参数分发到 modid 占位符");
    }

    @Test
    void shouldExposePlaceholderApiNamespacesToModLayer() {
        PlaceholderBootstrapReport report = PlaceholderBridge.bootstrap();

        assertTrue(report.enabled(), report.toString());
        assertTrue(report.placeholderApiAvailable(), report.toString());
        assertFalse(PlaceholderBridge.definitions().isEmpty(), PlaceholderBridge.definitions().toString());
        assertTrue(PlaceholderBridge.namespaces().contains("server"), "应能枚举到 PAPI 的命名空间");
        assertTrue(PlaceholderBridge.definitions().stream().anyMatch(definition ->
                definition.source() == PlaceholderSource.MIRRORED_PAPI
                        && definition.key().namespace().equals("server")
                        && definition.placeholders().contains("tps")
        ), "应能读取到 PAPI 占位符目录");
        assertEquals("TPS=20.0", PlaceholderBridge.render("TPS=%server_tps%", PlaceholderContext.empty()));
    }
}
