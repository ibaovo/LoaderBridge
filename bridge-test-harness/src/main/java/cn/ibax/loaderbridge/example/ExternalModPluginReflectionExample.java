package cn.ibax.loaderbridge.example;

import cn.ibax.loaderbridge.bridge.BridgeManager;
import cn.ibax.loaderbridge.bridge.BridgeResult;
import cn.ibax.loaderbridge.bridge.PluginHandle;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * 第三方 mod 通过 LoaderBridge 反射 Bukkit 插件的示例。
 *
 * 这个类可以直接复制到其他 mod 里，然后把里面的插件名、类名和方法名改成你自己的。
 *
 * 使用原则：
 * 1. 只在服务端生命周期里调用。
 * 2. 目标方法最好是目标类自己声明的。
 * 3. 找不到插件时直接跳过，不要影响客户端或纯 Forge 环境。
 */
public final class ExternalModPluginReflectionExample {
    private static final Logger LOGGER = Logger.getLogger(ExternalModPluginReflectionExample.class.getName());

    /**
     * 这里先用 PlaceholderAPI 做演示。
     * 你可以换成自己的插件名，例如 LuckPerms、Vault 相关插件，或者你自己服里的任意插件。
     */
    private static final String TARGET_PLUGIN = "PlaceholderAPI";

    /**
     * 这里只是演示用的主类名。
     * 如果你要调用的插件不是 PlaceholderAPI，把它改成目标插件的主类全限定名。
     */
    private static final String TARGET_MAIN_CLASS = "me.clip.placeholderapi.PlaceholderAPIPlugin";

    private static volatile boolean bootstrapped;

    private ExternalModPluginReflectionExample() {
    }

    /**
     * 服务端初始化入口。
     *
     * 建议放在服务器已启动或插件已加载完成之后调用，这样更稳。
     */
    public static void bootstrapServerSide() {
        if (bootstrapped) {
            return;
        }

        if (!BridgeManager.available()) {
            LOGGER.info("LoaderBridge 未启用，跳过插件反射示例。");
            return;
        }

        bootstrapped = true;

        Optional<PluginHandle> pluginHandle = BridgeManager.findPlugin(TARGET_PLUGIN);
        if (pluginHandle.isEmpty()) {
            LOGGER.info("未找到插件：" + TARGET_PLUGIN + "，跳过插件反射示例。");
            return;
        }

        PluginHandle handle = pluginHandle.get();
        LOGGER.info(() -> "已找到插件：" + handle.name() + "，主类：" + handle.mainClassName());
        LOGGER.info(() -> "插件类加载器：" + handle.classLoader());

        handle.findClass(TARGET_MAIN_CLASS).ifPresentOrElse(
                clazz -> LOGGER.info(() -> "插件主类已加载：" + clazz.getName()),
                () -> LOGGER.info("插件主类未加载：" + TARGET_MAIN_CLASS)
        );
    }

    /**
     * 按类名检查目标插件的某个类是否存在。
     *
     * @param pluginName 插件名
     * @param className  插件内的类全限定名
     * @return 找到时返回类对象，否则返回空
     */
    public static Optional<Class<?>> findPluginClass(final String pluginName, final String className) {
        if (!BridgeManager.available()) {
            return Optional.empty();
        }
        return BridgeManager.findPluginClass(pluginName, className);
    }

    /**
     * 调用目标插件主类上“自己声明的方法”。
     *
     * 注意：
     * LoaderBridge 当前是按 declared method 查找，所以这里的方法名必须是目标类自己声明的，
     * 不是父类或者接口继承来的公共方法。
     *
     * @param pluginName      插件名
     * @param methodName      方法名
     * @param parameterTypes  方法参数类型
     * @param args            方法参数值
     * @return 调用结果
     */
    public static BridgeResult<Object> invokePluginMainDeclaredMethod(
            final String pluginName,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        if (!BridgeManager.available()) {
            return BridgeResult.failure("LoaderBridge 未启用");
        }
        return BridgeManager.invokePluginInstance(pluginName, methodName, parameterTypes, args);
    }

    /**
     * 调用目标插件某个工具类上的静态方法。
     *
     * @param pluginName      插件名
     * @param className       插件内类的全限定名
     * @param methodName      静态方法名
     * @param parameterTypes  方法参数类型
     * @param args            方法参数值
     * @return 调用结果
     */
    public static BridgeResult<Object> invokePluginStaticDeclaredMethod(
            final String pluginName,
            final String className,
            final String methodName,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        if (!BridgeManager.available()) {
            return BridgeResult.failure("LoaderBridge 未启用");
        }
        return BridgeManager.invokePluginStatic(pluginName, className, methodName, parameterTypes, args);
    }

    /**
     * 在目标插件的类加载器里创建一个对象。
     *
     * @param pluginName      插件名
     * @param className       类全限定名
     * @param parameterTypes  构造器参数类型
     * @param args            构造器参数值
     * @return 创建结果
     */
    public static BridgeResult<Object> createPluginObject(
            final String pluginName,
            final String className,
            final Class<?>[] parameterTypes,
            final Object... args
    ) {
        if (!BridgeManager.available()) {
            return BridgeResult.failure("LoaderBridge 未启用");
        }
        return BridgeManager.createPluginInstance(pluginName, className, parameterTypes, args);
    }

    /**
     * 打印一个插件的基础信息。
     *
     * @param pluginName 插件名
     */
    public static void logPluginMetadata(final String pluginName) {
        if (!BridgeManager.available()) {
            LOGGER.info("LoaderBridge 未启用，跳过插件信息输出。");
            return;
        }

        BridgeManager.findPlugin(pluginName).ifPresentOrElse(handle -> {
            LOGGER.info(() -> "插件：" + handle.name());
            LOGGER.info(() -> "主类：" + handle.mainClassName());
            LOGGER.info(() -> "类加载器：" + handle.classLoader());
        }, () -> LOGGER.info("未找到插件：" + pluginName));
    }

    /**
     * 下面是一个最小化的调用示意。
     *
     * 你在真实项目里可以把它改成自己的方法名和参数。
     */
    public static void exampleCall() {
        BridgeResult<Object> result = invokePluginMainDeclaredMethod(
                TARGET_PLUGIN,
                "yourDeclaredMethod",
                new Class<?>[0]
        );

        if (result.success()) {
            LOGGER.info(() -> "调用成功，返回值：" + result.value());
            return;
        }

        LOGGER.info(() -> "调用失败：" + result.message());
    }
}
