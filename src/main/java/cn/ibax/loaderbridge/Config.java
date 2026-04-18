package cn.ibax.loaderbridge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 桥接模组的运行配置。
 */
@Mod.EventBusSubscriber(modid = Loaderbridge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("是否输出详细的桥接探测日志")
            .define("debugLogging", false);

    private static final ForgeConfigSpec.BooleanValue LOG_DISCOVERED_PLUGINS = BUILDER
            .comment("是否在启动时记录已发现的插件名称")
            .define("logDiscoveredPlugins", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean debugLogging;
    public static boolean logDiscoveredPlugins;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        syncValues();
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        syncValues();
    }

    private static void syncValues() {
        debugLogging = DEBUG_LOGGING.get();
        logDiscoveredPlugins = LOG_DISCOVERED_PLUGINS.get();
    }
}
