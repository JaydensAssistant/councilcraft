package dev.councilcraft.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Fails the build when the remapped jar still refers to a Minecraft member by
 * its yarn name. Loom leaves that name in place when the owner is a mod class
 * that was not on the remap classpath, which is how {@code BasinBlockEntity.getWorld}
 * survived and crashed every basin recipe.
 *
 * <p>A mod's own method is left alone even when a Minecraft method shares the
 * name and descriptor ({@code tick()V}, {@code getInputInventory}). The member
 * is unmapped only when the method or field is actually declared on a Minecraft
 * class and the reference uses the yarn name.
 */
public final class UnmappedYarnCheck {
    private static final String OURS = "dev/councilcraft/";
    private static final String[] LIBRARY_PREFIXES = {
        "java/", "javax/", "jdk/", "sun/",
        "org/objectweb/", "org/slf4j/", "com/google/", "it/unimi/",
        "org/apache/", "com/mojang/", "net/fabricmc/", "org/spongepowered/"
    };

    private UnmappedYarnCheck() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("usage: UnmappedYarnCheck <mappings.tiny|yarn.jar> <remapped.jar> <classpath>");
            System.exit(2);
        }
        List<Path> classpath = new ArrayList<>();
        for (String entry : args[2].split(java.io.File.pathSeparator)) {
            if (!entry.isEmpty()) {
                classpath.add(Path.of(entry));
            }
        }
        List<String> failures = findUnmapped(Path.of(args[0]), Path.of(args[1]), classpath);
        if (!failures.isEmpty()) {
            for (String failure : failures) {
                System.err.println(failure);
            }
            System.exit(1);
        }
    }

    public static List<String> findUnmapped(Path mappings, Path jar, List<Path> classpath) throws IOException {
        Mappings mapped = Mappings.read(mappings);
        Hierarchy hierarchy = new Hierarchy(classpath);
        List<String> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (JarFile remapped = new JarFile(jar.toFile())) {
            var entries = remapped.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || !name.startsWith(OURS)) {
                    continue;
                }
                ClassNode node = new ClassNode();
                try (InputStream in = remapped.getInputStream(entry)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
                }
                scan(node, mapped, hierarchy, failures, seen);
            }
        }
        return failures;
    }

    private static void scan(ClassNode node, Mappings mapped, Hierarchy hierarchy, List<String> failures, Set<String> seen) {
        for (MethodNode method : node.methods) {
            yarnDescriptor(node.name, method.name + method.desc, method.desc, mapped, failures, seen);
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call) {
                    member(node.name, call.owner, call.name, call.desc, true, mapped, hierarchy, failures, seen);
                } else if (insn instanceof FieldInsnNode field) {
                    member(node.name, field.owner, field.name, field.desc, false, mapped, hierarchy, failures, seen);
                } else if (insn instanceof InvokeDynamicInsnNode dynamic) {
                    yarnDescriptor(node.name, method.name, dynamic.desc, mapped, failures, seen);
                    for (Object arg : dynamic.bsmArgs) {
                        if (arg instanceof Handle handle) {
                            handle(node.name, handle, mapped, hierarchy, failures, seen);
                        }
                    }
                } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Handle handle) {
                    handle(node.name, handle, mapped, hierarchy, failures, seen);
                }
            }
        }
        for (FieldNode field : node.fields) {
            yarnDescriptor(node.name, field.name, field.desc, mapped, failures, seen);
        }
    }

    private static void handle(
        String from,
        Handle handle,
        Mappings mapped,
        Hierarchy hierarchy,
        List<String> failures,
        Set<String> seen
    ) {
        boolean method = handle.getTag() != Opcodes.H_GETFIELD
            && handle.getTag() != Opcodes.H_GETSTATIC
            && handle.getTag() != Opcodes.H_PUTFIELD
            && handle.getTag() != Opcodes.H_PUTSTATIC;
        member(from, handle.getOwner(), handle.getName(), handle.getDesc(), method, mapped, hierarchy, failures, seen);
    }

    private static void member(
        String from,
        String owner,
        String name,
        String desc,
        boolean method,
        Mappings mapped,
        Hierarchy hierarchy,
        List<String> failures,
        Set<String> seen
    ) {
        yarnDescriptor(from, owner + "." + name + desc, desc, mapped, failures, seen);
        if (name.startsWith("<") || skipOwner(owner)) {
            return;
        }
        Declaration declaration = hierarchy.declare(owner, name, desc, method, mapped);
        if (declaration == null) {
            Member yarn = mapped.yarnMember(name, desc, method);
            if (yarn != null) {
                report(failures, seen, from + " calls " + owner + "." + name + desc
                + " and that yarn name belongs to " + yarn.ownerNamed + " but the declaring class could not be resolved");
            }
            return;
        }
        if (declaration.minecraft && declaration.member != null
            && !declaration.member.interName.equals(declaration.member.namedName)
            && name.equals(declaration.member.namedName)) {
            report(failures, seen, from + " calls " + owner + "." + name + desc
                + " but " + declaration.member.ownerNamed + "." + declaration.member.namedName
                + " must be remapped to " + declaration.member.interName);
        }
    }

    private static void yarnDescriptor(
        String from,
        String where,
        String desc,
        Mappings mapped,
        List<String> failures,
        Set<String> seen
    ) {
        int start = 0;
        while (start < desc.length()) {
            int object = desc.indexOf('L', start);
            if (object < 0) {
                return;
            }
            int semi = desc.indexOf(';', object);
            if (semi < 0) {
                return;
            }
            String internal = desc.substring(object + 1, semi);
            String intermediary = mapped.namedToIntermediary.get(internal);
            if (intermediary != null && !intermediary.equals(internal)) {
                report(failures, seen, from + " descriptor " + where + " still names yarn class " + internal);
            }
            start = semi + 1;
        }
    }

    private static void report(List<String> failures, Set<String> seen, String failure) {
        if (seen.add(failure)) {
            failures.add(failure);
        }
    }

    private static boolean skipOwner(String owner) {
        if (owner.startsWith(OURS)) {
            return true;
        }
        for (String prefix : LIBRARY_PREFIXES) {
            if (owner.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static final class Declaration {
        private final boolean minecraft;
        private final Member member;

        private Declaration(boolean minecraft, Member member) {
            this.minecraft = minecraft;
            this.member = member;
        }
    }

    private static final class Hierarchy {
        private final List<JarFile> jars = new ArrayList<>();
        private final Map<String, ClassNode> loaded = new HashMap<>();

        private Hierarchy(List<Path> classpath) throws IOException {
            for (Path path : classpath) {
                if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                    jars.add(new JarFile(path.toFile()));
                }
            }
        }

        private Declaration declare(String owner, String name, String desc, boolean method, Mappings mapped) {
            ArrayDeque<String> pending = new ArrayDeque<>();
            pending.add(owner);
            Set<String> seen = new HashSet<>();
            while (!pending.isEmpty()) {
                String current = pending.removeFirst();
                if (!seen.add(current)) {
                    continue;
                }
                ClassNode node = load(current, mapped);
                if (node == null) {
                    continue;
                }
                if (declares(node, name, desc, method, mapped)) {
                    return new Declaration(mapped.isMinecraft(node.name), mapped.lookup(node.name, name, desc, method));
                }
                if (node.superName != null) {
                    pending.add(node.superName);
                }
                if (node.interfaces != null) {
                    pending.addAll(node.interfaces);
                }
            }
            return null;
        }

        private boolean declares(ClassNode node, String name, String desc, boolean method, Mappings mapped) {
            if (method) {
                for (MethodNode candidate : node.methods) {
                    if (matches(node.name, candidate.name, candidate.desc, name, desc, true, mapped)) {
                        return true;
                    }
                }
            } else {
                for (FieldNode candidate : node.fields) {
                    if (matches(node.name, candidate.name, candidate.desc, name, desc, false, mapped)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean matches(
            String className,
            String haveName,
            String haveDesc,
            String wantName,
            String wantDesc,
            boolean method,
            Mappings mapped
        ) {
            Member known = mapped.lookup(className, haveName, haveDesc, method);
            if (!descEquals(haveDesc, wantDesc, mapped)) {
                return false;
            }
            if (known != null) {
                return wantName.equals(haveName) || wantName.equals(known.interName) || wantName.equals(known.namedName);
            }
            return wantName.equals(haveName);
        }

        private boolean descEquals(String left, String right, Mappings mapped) {
            return mapped.toIntermediaryDesc(left).equals(mapped.toIntermediaryDesc(right));
        }

        private ClassNode load(String internal, Mappings mapped) {
            ClassNode cached = loaded.get(internal);
            if (cached != null || loaded.containsKey(internal)) {
                return cached;
            }
            ClassNode node = read(internal);
            if (node == null) {
                String named = mapped.intermediaryToNamed.get(internal);
                if (named != null) {
                    node = read(named);
                }
            }
            if (node == null) {
                String intermediary = mapped.namedToIntermediary.get(internal);
                if (intermediary != null) {
                    node = read(intermediary);
                }
            }
            loaded.put(internal, node);
            if (node != null) {
                loaded.put(node.name, node);
            }
            return node;
        }

        private ClassNode read(String internal) {
            String entry = internal + ".class";
            for (JarFile jar : jars) {
                JarEntry classEntry = jar.getJarEntry(entry);
                if (classEntry == null) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(classEntry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
                    return node;
                } catch (IOException failure) {
                    return null;
                }
            }
            return null;
        }
    }

    private static final class Member {
        private final String ownerNamed;
        private final String interName;
        private final String namedName;
        private final String interDesc;
        private final String namedDesc;

        private Member(String ownerNamed, String interName, String namedName, String interDesc, String namedDesc) {
            this.ownerNamed = ownerNamed;
            this.interName = interName;
            this.namedName = namedName;
            this.interDesc = interDesc;
            this.namedDesc = namedDesc;
        }
    }

    static final class Mappings {
        private final Map<String, String> officialToIntermediary = new HashMap<>();
        private final Map<String, String> officialToNamed = new HashMap<>();
        private final Map<String, String> namedToIntermediary = new HashMap<>();
        private final Map<String, String> intermediaryToNamed = new HashMap<>();
        private final Map<String, Member> byClassFileKey = new HashMap<>();
        private final List<Member> members = new ArrayList<>();

        private static Mappings read(Path path) throws IOException {
            Mappings mappings = new Mappings();
            String text;
            if (path.getFileName().toString().endsWith(".jar")) {
                try (JarFile jar = new JarFile(path.toFile())) {
                    JarEntry entry = jar.getJarEntry("mappings/mappings.tiny");
                    if (entry == null) {
                        throw new IOException("no mappings/mappings.tiny in " + path);
                    }
                    try (InputStream in = jar.getInputStream(entry)) {
                        text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            } else {
                text = Files.readString(path);
            }
            mappings.parse(text);
            return mappings;
        }

        private void parse(String text) {
            int intermediaryIndex = 1;
            int namedIndex = 2;
            List<String> lines = new ArrayList<>();
            for (String raw : text.split("\n", -1)) {
                lines.add(raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw);
            }
            for (String line : lines) {
                if (line.startsWith("tiny\t")) {
                    String[] header = line.split("\t", -1);
                    intermediaryIndex = indexOf(header, "intermediary", 3);
                    namedIndex = indexOf(header, "named", 3);
                } else if (line.startsWith("c\t")) {
                    rememberClass(line.split("\t", -1), intermediaryIndex, namedIndex);
                }
            }
            // A member descriptor can name a class that appears later in the file.
            String ownerNamed = null;
            String ownerIntermediary = null;
            for (String line : lines) {
                if (line.startsWith("c\t")) {
                    String[] parts = line.split("\t", -1);
                    ownerIntermediary = parts[1 + intermediaryIndex];
                    ownerNamed = parts[1 + namedIndex];
                    continue;
                }
                if (ownerNamed == null || (!line.startsWith("\tm\t") && !line.startsWith("\tf\t"))) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                String interName = parts[3 + intermediaryIndex];
                String namedName = parts[3 + namedIndex];
                String interDesc = translate(parts[2], officialToIntermediary);
                String namedDesc = translate(parts[2], officialToNamed);
                Member member = new Member(ownerNamed, interName, namedName, interDesc, namedDesc);
                members.add(member);
                byClassFileKey.put(key(ownerNamed, namedName, namedDesc), member);
                byClassFileKey.put(key(ownerIntermediary, interName, interDesc), member);
            }
        }

        private void rememberClass(String[] parts, int intermediaryIndex, int namedIndex) {
            String ownerOfficial = parts[1];
            String ownerIntermediary = parts[1 + intermediaryIndex];
            String ownerNamed = parts[1 + namedIndex];
            officialToIntermediary.put(ownerOfficial, ownerIntermediary);
            officialToNamed.put(ownerOfficial, ownerNamed);
            namedToIntermediary.put(ownerNamed, ownerIntermediary);
            intermediaryToNamed.put(ownerIntermediary, ownerNamed);
        }

        private Member lookup(String className, String name, String desc, boolean method) {
            Member direct = byClassFileKey.get(key(className, name, desc));
            if (direct != null) {
                return direct;
            }
            String interDesc = toIntermediaryDesc(desc);
            String named = intermediaryToNamed.getOrDefault(className, className);
            for (Member member : members) {
                if (!member.ownerNamed.equals(named)) {
                    continue;
                }
                boolean nameMatch = name.equals(member.namedName) || name.equals(member.interName);
                if (!nameMatch || !member.interDesc.equals(interDesc)) {
                    continue;
                }
                return member;
            }
            return null;
        }

        private Member yarnMember(String name, String desc, boolean method) {
            String interDesc = toIntermediaryDesc(desc);
            for (Member member : members) {
                if (name.equals(member.namedName)
                    && !member.namedName.equals(member.interName)
                    && (member.namedDesc.equals(desc) || member.interDesc.equals(desc) || member.interDesc.equals(interDesc))) {
                    return member;
                }
            }
            return null;
        }

        private boolean isMinecraft(String className) {
            return namedToIntermediary.containsKey(className) || intermediaryToNamed.containsKey(className);
        }

        private String toIntermediaryDesc(String desc) {
            String fromNamed = translate(desc, namedToIntermediary);
            return translate(fromNamed, officialToIntermediary);
        }

        private static String translate(String desc, Map<String, String> classes) {
            StringBuilder out = new StringBuilder(desc.length());
            for (int i = 0; i < desc.length(); i++) {
                char c = desc.charAt(i);
                if (c != 'L') {
                    out.append(c);
                    continue;
                }
                int semi = desc.indexOf(';', i);
                String internal = desc.substring(i + 1, semi);
                out.append('L').append(classes.getOrDefault(internal, internal)).append(';');
                i = semi;
            }
            return out.toString();
        }

        private static String key(String owner, String name, String desc) {
            return owner + " " + name + " " + desc;
        }

        private static int indexOf(String[] header, String namespace, int from) {
            for (int i = from; i < header.length; i++) {
                if (namespace.equals(header[i])) {
                    return i - from;
                }
            }
            throw new IllegalArgumentException("mappings have no " + namespace + " namespace");
        }
    }
}
