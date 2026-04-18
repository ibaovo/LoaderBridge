# 第三方 Mod 接入占位符示例

这份示例说明两件事：

1. 其他 mod 如何在服务端注册自己的 PAPI 占位符。
2. 其他 mod 如何读取 PlaceholderAPI 已注册的数据。

前提条件：

- 你的 mod 只在服务端使用这套逻辑。
- `LoaderBridge` 已经安装在混合端服务端。
- 如果 `PlaceholderAPI` 不存在，这套占位符桥会自动禁用，注册会直接变成无效操作，不会把服务端弄崩。

## 推荐接入方式

建议把调用放在服务端生命周期里，例如：

- dedicated server 启动完成后
- common setup 结束后
- 服务器开始启动时

不要把注册逻辑写进静态初始化块，也不要放进客户端初始化逻辑。

## 示例代码

下面的类已经放在仓库里，你可以直接参考：

- [ExternalModPlaceholderExample.java](../bridge-test-harness/src/main/java/cn/ibax/loaderbridge/example/ExternalModPlaceholderExample.java)

### 1. 注册自己的占位符

最稳妥的写法是显式传入 `modid`，这样占位符就会落在你自己的 namespace 下面：

```java
PlaceholderRegistrationResult result = PlaceholderBridge.register("examplemod", "balance", context -> {
    return "100";
});
```

注册成功后，对外就可以使用：

```text
%examplemod_balance%
```

如果你希望少写一个 namespace 参数，也可以只传 path：

```java
PlaceholderRegistrationResult result = PlaceholderBridge.register("status", context -> {
    return "在线";
});
```

在你的 mod 生命周期里调用时，LoaderBridge 会尝试自动把 namespace 推导成当前 `modid`。

### 2. 读取 PlaceholderAPI 数据

读取 PAPI 数据最常见的方法是直接解析字符串：

```java
String text = "TPS=%server_tps%";
String parsed = PlaceholderBridge.render(text, PlaceholderContext.builder()
        .subject(subject)
        .build());
```

如果你只想取一个具体变量，也可以直接构造 token：

```java
String tps = PlaceholderBridge.render("%server_tps%", PlaceholderContext.empty());
```

返回值是已经替换后的字符串。

### 3. 枚举当前镜像到 mod 层的 PAPI 占位符

如果你想看当前能读到哪些 PAPI 变量，可以直接遍历定义列表：

```java
List<PlaceholderDefinition> definitions = PlaceholderBridge.definitions();
for (PlaceholderDefinition definition : definitions) {
    if (definition.source() == PlaceholderSource.MIRRORED_PAPI) {
        System.out.println(definition.key().displayName());
    }
}
```

这里的 `source == MIRRORED_PAPI` 表示这个定义来自 PlaceholderAPI 的镜像数据。

## 实际使用建议

- 你的 mod 只要依赖 `LoaderBridge` 的核心 API 即可。
- 客户端不要引用这套类。
- `PlaceholderBridge.available()` 为 `false` 时，说明占位符桥没有启用，直接跳过注册即可。
- 如果你注册的是玩家相关变量，记得把当前玩家或者命令源放进 `PlaceholderContext.subject(...)` 里。

## 当前可直接复用的 API

- `PlaceholderBridge.available()`
- `PlaceholderBridge.register(...)`
- `PlaceholderBridge.registerNamespace(...)`
- `PlaceholderBridge.render(...)`
- `PlaceholderBridge.definitions()`
- `PlaceholderBridge.namespaces()`

如果你后面要扩展成“命令输出里自动替换占位符”，直接复用 `render(...)` 就够了。
