package cn.ibax.loaderbridge.testharness;

/**
 * 测试插件模拟对象。
 */
public final class TestPlugin {
    public String getName() {
        return "示例插件";
    }

    public String greet(final String targetName) {
        return "你好，" + targetName;
    }

    public static String staticGreeting() {
        return "静态问候";
    }
}
