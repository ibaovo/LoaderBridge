# 第三方 Mod 反射插件示例

这份示例说明如何在其他 mod 里通过 `LoaderBridge` 反射 Bukkit 插件。

和占位符示例不同，这里不是注册变量，而是直接：

- 找到插件
- 拿到插件句柄
- 读取插件主类
- 调用插件实例方法
- 调用插件静态方法
- 在插件自己的类加载器里创建对象

## 前提条件

- 你的代码只在服务端执行
- `LoaderBridge` 已经安装在混合端服务端
- 目标插件已经加载完成
- 你知道要调用的插件名、类名和方法名

如果 `LoaderBridge` 没启用，或者插件不存在，所有调用都会安全失败，不会把服端直接弄崩。

## 示例代码

下面的类已经放在仓库里，你可以直接复制：

- [ExternalModPluginReflectionExample.java](../bridge-test-harness/src/main/java/cn/ibax/loaderbridge/example/ExternalModPluginReflectionExample.java)

## 1. 先确认桥接可用

```java
if (!BridgeManager.available()) {
    return;
}
```

这一步是最基本的保护，避免纯 Forge 环境或者插件桥未启用时继续往下走。

## 2. 找到目标插件

```java
Optional<PluginHandle> handle = BridgeManager.findPlugin("PlaceholderAPI");
```

如果你自己的服务器插件名不是 `PlaceholderAPI`，就改成你自己的插件名。

## 3. 读取插件信息

```java
BridgeManager.findPlugin("PlaceholderAPI").ifPresent(plugin -> {
    System.out.println(plugin.name());
    System.out.println(plugin.mainClassName());
    System.out.println(plugin.classLoader());
});
```

这一步只是确认插件在不在、主类是谁、类加载器是不是可用。

## 4. 调用插件主类方法

```java
BridgeResult<Object> result = ExternalModPluginReflectionExample.invokePluginMainDeclaredMethod(
        "你的插件名",
        "你的方法名",
        new Class<?>[]{String.class},
        "参数"
);
```

注意：

- 这里的方法必须是目标类自己声明的
- 目标方法签名要写对
- 如果方法不存在，`BridgeResult` 会返回失败信息，不会抛到外面

## 5. 调用插件静态方法

```java
BridgeResult<Object> result = ExternalModPluginReflectionExample.invokePluginStaticDeclaredMethod(
        "你的插件名",
        "你的插件内类全限定名",
        "你的静态方法名",
        new Class<?>[]{String.class},
        "参数"
);
```

这种写法适合：

- 工具类
- 解析器
- 配置辅助类
- 你明确知道的插件内部静态方法

## 6. 创建插件内部对象

```java
BridgeResult<Object> result = ExternalModPluginReflectionExample.createPluginObject(
        "你的插件名",
        "你的插件内类全限定名",
        new Class<?>[]{String.class},
        "构造器参数"
);
```

如果目标类有构造器，LoaderBridge 会在插件自己的类加载器里帮你创建对象。

## 7. 失败怎么处理

每次调用的结果都在 `BridgeResult` 里：

- `success()`：是否成功
- `value()`：返回值
- `message()`：中文提示
- `error()`：异常对象

建议你的代码总是先判 `success()`，不要默认调用一定成功。

## 重要说明

当前桥接层是按 `declared method` 和 `declared constructor` 查找的，所以：

- 你要调用的方法最好直接写在目标类里
- 如果方法只是继承来的，当前实现不一定会自动找到
- 这个设计是为了让行为更明确，也更容易控制类加载边界

## 实际建议

- 只在服务端初始化后调用
- 目标插件没装就直接跳过
- 方法名和类名写成常量，别散落在业务代码里
- 如果插件已经把能力注册成 Bukkit Service，优先考虑 `findService(...)`
- 如果你只是要解析文本里的占位符，那应该走占位符桥，而不是这里的反射路径
