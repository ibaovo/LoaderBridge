package cn.ibax.loaderbridge.placeholder.internal;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * 动态生成 PlaceholderExpansion 子类。
 */
public final class PlaceholderExpansionGenerator {
    private static final String GENERATED_CLASS_INTERNAL_NAME = "me/clip/placeholderapi/expansion/LoaderBridgeExpansion";

    private PlaceholderExpansionGenerator() {
    }

    public static Class<?> defineExpansionClass(final Class<?> baseExpansionClass) {
        try {
            byte[] bytes = generateBytes(baseExpansionClass);
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(baseExpansionClass, MethodHandles.lookup());
            return lookup.defineClass(bytes);
        } catch (Throwable throwable) {
            throw new IllegalStateException("无法生成 PlaceholderAPI 镜像类", throwable);
        }
    }

    private static byte[] generateBytes(final Class<?> baseExpansionClass) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String baseInternalName = Type.getInternalName(baseExpansionClass);

        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                GENERATED_CLASS_INTERNAL_NAME,
                null,
                baseInternalName,
                null
        );

        addField(writer, "identifier", Type.getDescriptor(String.class));
        addField(writer, "author", Type.getDescriptor(String.class));
        addField(writer, "version", Type.getDescriptor(String.class));
        addField(writer, "resolver", Type.getDescriptor(java.util.function.BiFunction.class));
        addField(writer, "placeholdersSupplier", Type.getDescriptor(java.util.function.Supplier.class));

        addConstructor(writer, baseInternalName);
        addGetIdentifier(writer);
        addGetAuthor(writer);
        addGetVersion(writer);
        addPersist(writer);
        addCanRegister(writer);
        addGetPlaceholders(writer);
        addOnRequest(writer, baseInternalName);
        addOnPlaceholderRequest(writer, baseInternalName);

        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void addField(final ClassWriter writer, final String name, final String descriptor) {
        writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, name, descriptor, null, null).visitEnd();
    }

    private static void addConstructor(final ClassWriter writer, final String baseInternalName) {
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(" + Type.getDescriptor(String.class)
                        + Type.getDescriptor(String.class)
                        + Type.getDescriptor(String.class)
                        + Type.getDescriptor(java.util.function.BiFunction.class)
                        + Type.getDescriptor(java.util.function.Supplier.class)
                        + ")V",
                null,
                null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, baseInternalName, "<init>", "()V", false);
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitFieldInsn(Opcodes.PUTFIELD, GENERATED_CLASS_INTERNAL_NAME, "identifier", Type.getDescriptor(String.class));
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitFieldInsn(Opcodes.PUTFIELD, GENERATED_CLASS_INTERNAL_NAME, "author", Type.getDescriptor(String.class));
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 3);
        method.visitFieldInsn(Opcodes.PUTFIELD, GENERATED_CLASS_INTERNAL_NAME, "version", Type.getDescriptor(String.class));
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 4);
        method.visitFieldInsn(Opcodes.PUTFIELD, GENERATED_CLASS_INTERNAL_NAME, "resolver", Type.getDescriptor(java.util.function.BiFunction.class));
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 5);
        method.visitFieldInsn(Opcodes.PUTFIELD, GENERATED_CLASS_INTERNAL_NAME, "placeholdersSupplier", Type.getDescriptor(java.util.function.Supplier.class));
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addGetIdentifier(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getIdentifier", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "identifier", Type.getDescriptor(String.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addGetAuthor(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getAuthor", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "author", Type.getDescriptor(String.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addGetVersion(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getVersion", "()Ljava/lang/String;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "version", Type.getDescriptor(String.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addPersist(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "persist", "()Z", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addCanRegister(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "canRegister", "()Z", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ICONST_1);
        method.visitInsn(Opcodes.IRETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addGetPlaceholders(final ClassWriter writer) {
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getPlaceholders", "()Ljava/util/List;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "placeholdersSupplier", Type.getDescriptor(java.util.function.Supplier.class));
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(java.util.function.Supplier.class), "get", "()Ljava/lang/Object;", true);
        method.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(List.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addOnRequest(final ClassWriter writer, final String baseInternalName) {
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onRequest",
                "(Lorg/bukkit/OfflinePlayer;Ljava/lang/String;)Ljava/lang/String;",
                null,
                null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "resolver", Type.getDescriptor(java.util.function.BiFunction.class));
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(java.util.function.BiFunction.class), "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        method.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(String.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }

    private static void addOnPlaceholderRequest(final ClassWriter writer, final String baseInternalName) {
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onPlaceholderRequest",
                "(Lorg/bukkit/entity/Player;Ljava/lang/String;)Ljava/lang/String;",
                null,
                null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, GENERATED_CLASS_INTERNAL_NAME, "resolver", Type.getDescriptor(java.util.function.BiFunction.class));
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(java.util.function.BiFunction.class), "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        method.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(String.class));
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
    }
}
