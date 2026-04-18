package cn.ibax.loaderbridge.example;

import cn.ibax.loaderbridge.placeholder.PlaceholderBridge;
import cn.ibax.loaderbridge.placeholder.PlaceholderContext;
import cn.ibax.loaderbridge.placeholder.PlaceholderDefinition;
import cn.ibax.loaderbridge.placeholder.PlaceholderRegistrationResult;
import cn.ibax.loaderbridge.placeholder.PlaceholderSource;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 第三方 mod 的服务端占位符接入示例。
 *
 * 这个类可以直接复制到你的其他 mod 里，然后把里面的 `MODID` 改成你自己的 modid。
 * 重点有三件事：
 * 1. 只在服务端生命周期里调用注册方法。
 * 2. 注册自己的 PAPI 占位符时，优先使用显式 namespace，最稳妥。
 * 3. 读取 PlaceholderAPI 的数据时，直接用 `PlaceholderBridge.render(...)` 解析字符串。
 */
public final class ExternalModPlaceholderExample {
    /**
     * 把这里改成你自己的 modid。
     * 这个值只用于示例代码里的显式 namespace。
     */
    private static final String MODID = "examplemod";

    private static final Logger LOGGER = Logger.getLogger(ExternalModPlaceholderExample.class.getName());

    /**
     * 避免同一个启动流程里重复注册。
     * 如果你的 mod 里有更明确的初始化状态管理，也可以替换掉这个标记。
     */
    private static volatile boolean registered;

    private ExternalModPlaceholderExample() {
    }

    /**
     * 服务端初始化入口。
     *
     * 建议在你的 dedicated server 启动事件、common setup 末尾或者服务器开始启动后调用。
     * 不要放在静态初始化块里，也不要放在客户端初始化逻辑里。
     */
    public static void bootstrapServerSide() {
        if (registered) {
            return;
        }

        if (!PlaceholderBridge.available()) {
            LOGGER.info("占位符桥未启用，跳过注册。");
            return;
        }

        registered = true;

        registerExplicitNamespacePlaceholders();
        registerAutoNamespacePlaceholder();

        LOGGER.info(() -> "当前可见的占位符命名空间：" + PlaceholderBridge.namespaces());
        LOGGER.info(() -> "当前镜像到 mod 层的 PAPI 占位符：" + listMirroredPapiNames());
    }

    /**
     * 显式 namespace 写法。
     *
     * 这是一种最稳的写法，适合你明确知道自己的 namespace 就是当前 modid 的场景。
     * 对外最终会表现为：
     * - `%examplemod_balance%`
     * - `%examplemod_server_name%`
     */
    public static void registerExplicitNamespacePlaceholders() {
        PlaceholderRegistrationResult balance = PlaceholderBridge.register(MODID, "balance", context -> {
            // 这里可以接你的经济系统、数据库或者缓存。
            // 示例里直接返回一个固定值，方便你看懂调用链。
            return "100";
        });
        logRegistration("explicit/balance", balance);

        PlaceholderRegistrationResult serverName = PlaceholderBridge.register(MODID, "server_name", context -> {
            // 这个示例说明：resolver 可以完全不依赖玩家上下文。
            return "演示服务器";
        });
        logRegistration("explicit/server_name", serverName);
    }

    /**
     * 自动 namespace 写法。
     *
     * 如果你是在自己的 mod 生命周期里调用这个方法，LoaderBridge 会尝试自动推导当前 modid。
     * 这时你只需要写 path，不需要手工传 namespace。
     *
     * 如果你担心调用时机不对，直接用 `registerExplicitNamespacePlaceholders()` 也可以。
     */
    public static void registerAutoNamespacePlaceholder() {
        PlaceholderRegistrationResult status = PlaceholderBridge.register("status", context -> {
            // 这里可以根据你的在线状态、世界状态、开服状态返回不同文本。
            return "在线";
        });
        logRegistration("auto/status", status);
    }

    /**
     * 把任意字符串里的占位符解析掉。
     *
     * 这个方法就是“读取 PAPI 数据”的最常见方式：
     * 你不需要单独调用某个 getXXX 接口，直接把包含占位符的字符串交给 LoaderBridge 即可。
     *
     * @param text    原始文本，例如 `TPS=%server_tps%`
     * @param subject  当前上下文主体，通常可以传玩家、命令源或你自己的业务对象
     * @return 解析后的文本
     */
    public static String renderText(final String text, final Object subject) {
        PlaceholderContext context = buildContext(subject);
        return PlaceholderBridge.render(text, context);
    }

    /**
     * 读取一个具体的 PAPI 变量。
     *
     * 例如：
     * - `readPlaceholderValue("server", "tps", null)` -> 读取 `%server_tps%`
     * - `readPlaceholderValue("player", "name", player)` -> 读取 `%player_name%`
     *
     * 如果变量不存在，返回空字符串对应的原始 token，是否存在可以交给调用方判断。
     *
     * @param namespace PAPI 命名空间，也就是 `%namespace_path%` 里的 namespace 部分
     * @param path      PAPI 路径，也就是 `%namespace_path%` 里的 path 部分
     * @param subject   当前上下文主体，通常可以传玩家、命令源或你自己的业务对象
     * @return 解析后的字符串；如果没有匹配到变量，则返回原始 token
     */
    public static String readPlaceholderValue(final String namespace, final String path, final Object subject) {
        String token = "%" + namespace + "_" + path + "%";
        return renderText(token, subject);
    }

    /**
     * 读取一个具体的 PAPI 变量，如果没有解析成功，则返回空。
     *
     * 这个写法更适合业务代码，因为你能明确区分“没有这个变量”和“变量值就是原 token”。
     *
     * @param namespace PAPI 命名空间，也就是 `%namespace_path%` 里的 namespace 部分
     * @param path      PAPI 路径，也就是 `%namespace_path%` 里的 path 部分
     * @param subject   当前上下文主体，通常可以传玩家、命令源或你自己的业务对象
     * @return 解析成功时返回值，否则返回空
     */
    public static Optional<String> readPlaceholderValueIfPresent(final String namespace, final String path, final Object subject) {
        String token = "%" + namespace + "_" + path + "%";
        String rendered = renderText(token, subject);
        if (token.equals(rendered)) {
            return Optional.empty();
        }
        return Optional.of(rendered);
    }

    /**
     * 列出当前桥接层已经镜像到 mod 侧的 PAPI 命名空间。
     *
     * @return 当前可见的 PAPI 命名空间列表
     */
    public static List<String> listMirroredPapiNamespaces() {
        return PlaceholderBridge.definitions().stream()
                .filter(definition -> definition.source() == PlaceholderSource.MIRRORED_PAPI)
                .map(definition -> definition.key().namespace())
                .distinct()
                .toList();
    }

    /**
     * 列出当前桥接层已经镜像到 mod 侧的 PAPI 占位符定义。
     *
     * 你可以用它做调试页面、命令输出，或者在控制台里打印当前可见的 PAPI 变量。
     *
     * @return 当前可见的 PAPI 占位符定义列表
     */
    public static List<PlaceholderDefinition> listMirroredPapiDefinitions() {
        return PlaceholderBridge.definitions().stream()
                .filter(definition -> definition.source() == PlaceholderSource.MIRRORED_PAPI)
                .toList();
    }

    /**
     * 构造占位符上下文。
     *
     * 这里我们只放一个 subject 和一个自定义属性，方便示范。
     * 真正项目里，你可以根据自己的业务继续塞 world、dimension、团队、权限等级等上下文。
     */
    private static PlaceholderContext buildContext(final Object subject) {
        return PlaceholderContext.builder()
                .subject(subject)
                .attribute("source", MODID)
                .build();
    }

    private static void logRegistration(final String label, final PlaceholderRegistrationResult result) {
        if (result.registered()) {
            LOGGER.info(() -> "占位符注册成功：" + label + " -> " + result.key().displayName());
            return;
        }

        LOGGER.info(() -> "占位符注册未生效：" + label + "，状态=" + result.status() + "，原因=" + result.message());
    }

    private static List<String> listMirroredPapiNames() {
        return PlaceholderBridge.definitions().stream()
                .filter(definition -> definition.source() == PlaceholderSource.MIRRORED_PAPI)
                .map(definition -> definition.key().displayName())
                .toList();
    }
}
