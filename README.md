# LoaderBridge

LoaderBridge 是一个面向 Minecraft 1.20.1 混合端的纯服务端技术模组。

它不是内容模组，而是用来把 `mod` 侧和 `Bukkit/Spigot` 插件侧连接起来，提供双向桥接能力：

- `mod` 端反射插件类
- `mod` 端获取插件实例并调用方法
- `mod` 端读取插件注册的服务
- 插件侧继续访问 `mod` 的既有能力
- 当 `PlaceholderAPI` 存在时，`mod` 侧还能读取、枚举和解析 `PAPI` 占位符
- `mod` 侧注册的占位符会自动回挂到 `PlaceholderAPI`

## 运行范围

这个项目只面向带 `Bukkit/Spigot` 运行时的混合端，例如 `Mohist`、`Tenet` 这类服务端。

如果运行时没有检测到 `Bukkit` 环境，桥接层会自动降级，不会让服务端直接崩溃。

如果运行时没有检测到 `PlaceholderAPI`，占位符功能默认不启用，注册和解析都会变成安全的 `no-op`。

## 构建要求

- Minecraft `1.20.1`
- Forge `47.4.20`
- Java `17`
- Gradle `8.x`

## 项目结构

- 根项目：最终发布的 Forge 模组
- `bridge-core`：桥接契约、结果对象、启动流程和反射实现
- `bridge-test-harness`：测试夹具和 JUnit 回归测试

## 核心特性

### 1. 插件桥接

LoaderBridge 已经实现了以下基础能力：

- 插件发现
- 类加载
- 实例访问
- 方法调用
- 服务读取

### 2. 占位符桥接

当 `PlaceholderAPI` 存在时，LoaderBridge 会自动把它接入 `mod` 侧，提供以下能力：

- 枚举当前已注册的 `PAPI` 命名空间
- 枚举当前已注册的 `PAPI` 占位符定义
- 解析包含 `PAPI` 变量的字符串
- 注册 `mod` 自己的占位符，并反向暴露给 `PlaceholderAPI`

### 3. 纯服务端接入

这个项目不提供客户端方法，也不依赖客户端内容。

所有占位符注册和读取逻辑都应该放在服务端生命周期里执行。

## 内置占位符

LoaderBridge 自带 3 个占位符，命名空间固定为 `loaderbridge`：

- `%loaderbridge_version%`：当前模组版本
- `%loaderbridge_runtime%`：当前运行时名称
- `%loaderbridge_modid%`：当前模组的 `modid`

这些占位符只会在占位符桥启用时注册。

## 第三方 Mod 如何使用

如果你的 `mod` 想在服务端侧接入 `LoaderBridge`，建议遵循下面的方式：

- 编译期使用 `compileOnly` 方式依赖 `LoaderBridge`
- 注册逻辑只放在服务端生命周期里
- 客户端不要引用 `PlaceholderBridge`、`PlaceholderContext` 这类类
- 如果 `PlaceholderAPI` 不存在，直接跳过占位符注册即可

### 1. 注册自己的占位符

最稳妥的写法是显式指定自己的 `modid` 作为命名空间：

```java
import cn.ibax.loaderbridge.placeholder.PlaceholderBridge;
import cn.ibax.loaderbridge.placeholder.PlaceholderRegistrationResult;

public final class MyModPlaceholders {
    private static final String MODID = "examplemod";

    private MyModPlaceholders() {
    }

    public static void bootstrapServerSide() {
        // 占位符桥没启用时，直接跳过即可
        if (!PlaceholderBridge.available()) {
            return;
        }

        // 注册一个固定路径的占位符，对外会表现为 %examplemod_balance%
        PlaceholderRegistrationResult balance = PlaceholderBridge.register(
                MODID,
                "balance",
                context -> "100"
        );

        // 注册一个更偏“状态类”的占位符
        PlaceholderRegistrationResult status = PlaceholderBridge.register(
                MODID,
                "status",
                context -> "在线"
        );
    }
}
```

如果你的注册代码就是在本模组生命周期里执行，也可以只传 `path`，让桥接层自动推导当前 `modid`：

```java
PlaceholderBridge.register("status", context -> "在线");
```

建议优先使用显式 `modid` 版本，这样最清楚，也最不容易在调用时机上出问题。

### 2. 读取 PlaceholderAPI 数据

最常见的读取方式是直接把包含占位符的字符串交给 LoaderBridge 解析：

```java
import cn.ibax.loaderbridge.placeholder.PlaceholderBridge;
import cn.ibax.loaderbridge.placeholder.PlaceholderContext;

public final class MyPlaceholderReader {
    private MyPlaceholderReader() {
    }

    public static String renderText(final String text, final Object subject) {
        // subject 可以是玩家、命令源，或者你自己的业务对象
        PlaceholderContext context = PlaceholderContext.builder()
                .subject(subject)
                .attribute("source", "examplemod")
                .build();

        return PlaceholderBridge.render(text, context);
    }
}
```

使用时可以直接解析类似下面的文本：

```java
String text = "TPS=%server_tps%, 玩家数=%server_online%";
String parsed = MyPlaceholderReader.renderText(text, player);
```

如果你只想读取一个具体变量，也可以直接构造 token：

```java
String tps = PlaceholderBridge.render("%server_tps%", PlaceholderContext.empty());
```

### 3. 枚举当前镜像到 mod 层的 PAPI 占位符

如果你想做调试命令、管理面板，或者想确认当前能看到哪些 `PAPI` 变量，可以直接读取定义列表：

```java
import cn.ibax.loaderbridge.placeholder.PlaceholderBridge;
import cn.ibax.loaderbridge.placeholder.PlaceholderSource;

PlaceholderBridge.definitions().stream()
        .filter(definition -> definition.source() == PlaceholderSource.MIRRORED_PAPI)
        .forEach(definition -> System.out.println(definition.key().displayName()));
```

当前已经镜像到 `mod` 层的 `PAPI` 命名空间，也可以直接枚举：

```java
PlaceholderBridge.namespaces().forEach(System.out::println);
```

## 推荐的接入位置

建议把占位符注册和解析初始化放在下面这些位置之一：

- dedicated server 启动完成之后
- common setup 结束之后
- 服务器开始启动时

不要放在静态初始化块里，也不要放在客户端初始化逻辑里。

## 占位符规则

- 默认命名空间使用 `modid`
- 同一个 `modid` 下重复路径会被拒绝
- `PlaceholderAPI` 已占用的命名空间不会被本 mod 覆盖
- `PlaceholderAPI` 不存在时，占位符功能默认关闭
- 这个项目不做客户端 `GUI`、`render`、`screen` 相关实现
- 这个项目暂不实现 `Mixin`

## 示例与说明

更完整的第三方 `mod` 接入示例，可以看这里：

- [第三方 Mod 接入占位符示例](docs/placeholder-integration-example.md)
- [第三方 Mod 反射插件示例](docs/plugin-reflection-example.md)

## 测试

仓库里已经包含回归测试，覆盖以下场景：

- `PlaceholderAPI` 不存在时自动禁用
- `PlaceholderAPI` 存在时镜像其占位符目录
- `mod` 侧注册的占位符会反向回挂到 `PlaceholderAPI`
- 重复注册会被明确拒绝

本地构建：

```bash
./gradlew.bat build
```

如果你只想跑占位符相关测试：

```bash
./gradlew.bat :bridge-test-harness:test
```

## 许可证

`GNU LGPL 3.0`
