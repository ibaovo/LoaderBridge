package cn.ibax.loaderbridge;

import cn.ibax.loaderbridge.bridge.BootstrapReport;
import cn.ibax.loaderbridge.bridge.BridgeBootstrap;
import cn.ibax.loaderbridge.placeholder.PlaceholderBootstrapReport;
import cn.ibax.loaderbridge.placeholder.PlaceholderBridge;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(Loaderbridge.MODID)
public final class Loaderbridge {
    public static final String MODID = "loaderbridge";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile BootstrapReport BOOTSTRAP_REPORT = BootstrapReport.notBootstrapped(MODID);
    private static volatile PlaceholderBootstrapReport PLACEHOLDER_REPORT = PlaceholderBootstrapReport.disabled("尚未启动");
    private static volatile boolean PLACEHOLDER_BOOTSTRAP_PENDING = true;
    private static volatile boolean PLACEHOLDER_BOOTSTRAP_COMPLETED;

    public Loaderbridge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        BootstrapReport report = BridgeBootstrap.bootstrap(this, MODID);
        BOOTSTRAP_REPORT = report;

        if (report.available()) {
            LOGGER.info("LoaderBridge 已连接到 {}，已启用能力：{}", report.runtimeName(), report.capabilities());
            if (Config.logDiscoveredPlugins && !report.pluginNames().isEmpty()) {
                LOGGER.info("已发现插件：{}", report.pluginNames());
            }
        } else {
            LOGGER.warn("LoaderBridge 桥接未启用：{}", report.message());
            if (Config.debugLogging && report.error() != null) {
                LOGGER.error("桥接探测过程中出现异常", report.error());
            }
        }

        initializePlaceholderBridge("模组初始化");
    }

    private void serverStarted(final ServerStartedEvent event) {
        if (PLACEHOLDER_BOOTSTRAP_COMPLETED || !PLACEHOLDER_BOOTSTRAP_PENDING) {
            return;
        }
        initializePlaceholderBridge("服务器启动后");
    }

    private void initializePlaceholderBridge(final String phase) {
        if (PLACEHOLDER_BOOTSTRAP_COMPLETED) {
            return;
        }

        PlaceholderBootstrapReport placeholderReport = PlaceholderBridge.bootstrap();
        PLACEHOLDER_REPORT = placeholderReport;

        if (placeholderReport.enabled()) {
            PLACEHOLDER_BOOTSTRAP_COMPLETED = true;
            PLACEHOLDER_BOOTSTRAP_PENDING = false;
            registerBuiltInPlaceholders();
            LOGGER.info("占位符桥已启用（{}），已镜像命名空间：{}", phase, placeholderReport.mirroredNamespaces());
            return;
        }

        PLACEHOLDER_BOOTSTRAP_PENDING = true;
        LOGGER.info("占位符桥暂未启用（{}）：{}，将在服务器插件加载完成后重试", phase, placeholderReport.message());
        if (Config.debugLogging && placeholderReport.error() != null) {
            LOGGER.error("占位符桥启动失败", placeholderReport.error());
        }
    }

    private void registerBuiltInPlaceholders() {
        PlaceholderBridge.register(MODID, "version", context -> currentVersion());
        PlaceholderBridge.register(MODID, "runtime", context -> {
            PlaceholderBootstrapReport report = PLACEHOLDER_REPORT;
            return report.runtimeName();
        });
        PlaceholderBridge.register(MODID, "modid", context -> MODID);
    }

    private static String currentVersion() {
        Package currentPackage = Loaderbridge.class.getPackage();
        if (currentPackage != null && currentPackage.getImplementationVersion() != null) {
            return currentPackage.getImplementationVersion();
        }
        return "dev";
    }

    public static BootstrapReport bootstrapReport() {
        return BOOTSTRAP_REPORT;
    }

    public static PlaceholderBootstrapReport placeholderReport() {
        return PLACEHOLDER_REPORT;
    }
}
