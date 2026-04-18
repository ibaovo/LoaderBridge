package cn.ibax.loaderbridge.bridge;

/**
 * 桥接层当前支持的能力。
 */
public enum BridgeCapability {
    MOD_HANDLE,
    PLUGIN_LOOKUP,
    PLUGIN_CLASS_LOADING,
    PLUGIN_INSTANCE_ACCESS,
    METHOD_INVOCATION,
    SERVICE_LOOKUP,
    MIXIN_EXTENSION_SLOT
}
