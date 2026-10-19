package dev.councilcraft.build;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The check must catch a yarn {@code getWorld} left on a Create owner, and it
 * must leave Create's own {@code tick} and {@code getInputInventory} alone.
 */
class UnmappedYarnCheckTest {
    private static final String MAPPINGS = """
        tiny\t2\t0\tofficial\tintermediary\tnamed
        c\ta\tnet/minecraft/class_2586\tnet/minecraft/block/entity/BlockEntity
        \tm\t()Lb;\tk\tmethod_10997\tgetWorld
        c\tb\tnet/minecraft/class_1937\tnet/minecraft/world/World
        c\tc\tnet/minecraft/class_1263\tnet/minecraft/inventory/Inventory
        \tm\t()I\tu\tmethod_5439\tsize
        c\te\tnet/minecraft/class_1297\tnet/minecraft/entity/Entity
        \tm\t()V\tt\tmethod_5773\ttick
        """;

    @TempDir
    Path dir;

    @Test
    void yarnGetWorldOnAModOwnerFailsAndModMethodsDoNot() throws IOException {
        Path mappings = dir.resolve("mappings.tiny");
        Files.writeString(mappings, MAPPINGS);
        Path classpath = classesJar(dir.resolve("classpath.jar"), blockEntity(), basin());
        Path jar = classesJar(dir.resolve("mod.jar"), caller(
            "dev/councilcraft/Bad",
            new String[] {
                "com/simibubi/create/content/processing/basin/BasinBlockEntity", "getWorld", "()Lnet/minecraft/class_1937;", "virtual"
            },
            new String[] {
                "com/simibubi/create/content/processing/basin/BasinBlockEntity", "getInputInventory", "()Lcom/simibubi/create/foundation/item/SmartInventory;", "virtual"
            },
            new String[] {
                "com/simibubi/create/content/processing/basin/BasinBlockEntity", "tick", "()V", "virtual"
            }
        ));

        String report = String.join("\n", UnmappedYarnCheck.findUnmapped(mappings, jar, List.of(classpath)));
        assertTrue(report.contains("getWorld"), report);
        assertTrue(report.contains("method_10997"), report);
        assertFalse(report.contains("getInputInventory"), report);
        assertFalse(report.contains("tick"), report);
    }

    @Test
    void remappedMinecraftCallsAndAYarnDescriptorAreDistinct() throws IOException {
        Path mappings = dir.resolve("mappings.tiny");
        Files.writeString(mappings, MAPPINGS);
        Path classpath = classesJar(dir.resolve("classpath.jar"), blockEntity(), basin(), inventory());
        Path good = classesJar(dir.resolve("good.jar"), caller(
            "dev/councilcraft/Good",
            new String[] {
                "net/minecraft/class_2586", "method_10997", "()Lnet/minecraft/class_1937;", "virtual"
            },
            new String[] {
                "net/minecraft/class_1263", "method_5439", "()I", "interface"
            }
        ));
        assertTrue(UnmappedYarnCheck.findUnmapped(mappings, good, List.of(classpath)).isEmpty());

        Path yarnDesc = classesJar(dir.resolve("yarndesc.jar"), caller(
            "dev/councilcraft/YarnDesc",
            new String[] {
                "net/minecraft/class_2586", "method_10997", "()Lnet/minecraft/world/World;", "virtual"
            }
        ));
        String report = String.join("\n", UnmappedYarnCheck.findUnmapped(mappings, yarnDesc, List.of(classpath)));
        assertTrue(report.contains("net/minecraft/world/World"), report);
    }

    private static byte[] blockEntity() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "net/minecraft/block/entity/BlockEntity", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "getWorld", "()Lnet/minecraft/world/World;", null, null);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] basin() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC,
            "com/simibubi/create/content/processing/basin/BasinBlockEntity",
            null,
            "net/minecraft/block/entity/BlockEntity",
            null
        );
        writer.visitMethod(
            Opcodes.ACC_PUBLIC,
            "getInputInventory",
            "()Lcom/simibubi/create/foundation/item/SmartInventory;",
            null,
            null
        ).visitEnd();
        writer.visitMethod(Opcodes.ACC_PUBLIC, "tick", "()V", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] inventory() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
            "net/minecraft/inventory/Inventory",
            null,
            "java/lang/Object",
            null
        );
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "size", "()I", null, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] caller(String internalName, String[]... calls) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.visitCode();
        for (String[] call : calls) {
            int opcode = "interface".equals(call[3]) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL;
            method.visitMethodInsn(opcode, call[0], call[1], call[2], opcode == Opcodes.INVOKEINTERFACE);
        }
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Path classesJar(Path path, byte[]... classes) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            for (byte[] bytes : classes) {
                String name = new org.objectweb.asm.ClassReader(bytes).getClassName() + ".class";
                out.putNextEntry(new JarEntry(name));
                out.write(bytes);
                out.closeEntry();
            }
        }
        return path;
    }
}
