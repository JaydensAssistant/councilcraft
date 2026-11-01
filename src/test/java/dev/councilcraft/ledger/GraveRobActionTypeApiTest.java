package dev.councilcraft.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.quiltservertools.ledger.actions.ActionType;
import com.github.quiltservertools.ledger.actions.AbstractActionType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * CC-155/156 / CC-B21: GraveRobActionType must implement every ActionType method that
 * AbstractActionType leaves abstract. Compile stubs mirror getIdentifier + getTranslationType;
 * when ledger-1.3.18.jar is on disk, javap confirms the same gap list.
 */
class GraveRobActionTypeApiTest {
    @Test
    void implementsStubActionTypeContract() {
        GraveRobActionType action = new GraveRobActionType();
        assertTrue(action instanceof ActionType);
        assertTrue(action instanceof AbstractActionType);
        assertEquals("grave-rob", action.getIdentifier());
        assertEquals("item", action.getTranslationType());
    }

    @Test
    void declaresEveryAbstractMethodFromStubActionType() {
        Set<String> required = abstractInstanceMethods(ActionType.class);
        for (String name : required) {
            assertTrue(
                    hasPublicMethod(GraveRobActionType.class, name),
                    "GraveRobActionType missing ActionType method: " + name);
        }
    }

    @Test
    void ledgerJarAbstractGapCoveredWhenPresent() throws Exception {
        Path jar = findLedgerJar();
        if (jar == null) {
            return;
        }
        Set<String> actionAbstract = new HashSet<>(javapAbstractMethods(jar,
                "com.github.quiltservertools.ledger.actions.ActionType"));
        Set<String> abstractProvided = javapConcretePublicMethods(jar,
                "com.github.quiltservertools.ledger.actions.AbstractActionType");
        // Gap = still abstract after AbstractActionType (Kotlin props + defaults live there).
        List<String> gap = actionAbstract.stream()
                .filter(m -> !abstractProvided.contains(m))
                .sorted()
                .collect(Collectors.toList());
        assertTrue(gap.contains("getIdentifier"), "expected getIdentifier in gap, got " + gap);
        assertTrue(gap.contains("getTranslationType"), "expected getTranslationType in gap, got " + gap);

        Set<String> ours = Arrays.stream(GraveRobActionType.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        List<String> missing = gap.stream().filter(m -> !ours.contains(m)).collect(Collectors.toList());
        assertTrue(missing.isEmpty(),
                "GraveRobActionType missing Ledger ActionType gap methods: " + missing + " (gap=" + gap + ")");
    }

    private static Set<String> abstractInstanceMethods(Class<?> type) {
        Set<String> out = new HashSet<>();
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            out.add(m.getName());
        }
        return out;
    }

    private static boolean hasPublicMethod(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Path findLedgerJar() {
        List<Path> candidates = List.of(
                Path.of("/workspace/minecraft/build/test0427/mods"),
                Path.of("/workspace/minecraft/build/mod-cache"),
                Path.of("/workspace/minecraft/build/councilcraft-src/.gradle/compile-only"));
        for (Path dir : candidates) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (var stream = Files.list(dir)) {
                var hit = stream
                        .filter(p -> {
                            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return n.startsWith("ledger") && n.endsWith(".jar");
                        })
                        .findFirst();
                if (hit.isPresent()) {
                    return hit.get();
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private static List<String> javapAbstractMethods(Path jar, String className) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "javap", "-classpath", jar.toString(), "-public", className);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        List<String> methods = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("public abstract ") && trimmed.contains("(")) {
                    int paren = trimmed.indexOf('(');
                    int sp = trimmed.lastIndexOf(' ', paren);
                    if (sp >= 0 && paren > sp) {
                        methods.add(trimmed.substring(sp + 1, paren));
                    }
                }
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            fail("javap failed (" + code + ") for " + className + " in " + jar);
        }
        return methods;
    }

    private static Set<String> javapConcretePublicMethods(Path jar, String className) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "javap", "-classpath", jar.toString(), "-public", className);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        Set<String> methods = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("public ") || trimmed.contains(" abstract ") || !trimmed.contains("(")) {
                    continue;
                }
                if (trimmed.startsWith("public " + className) || trimmed.contains(" static ")) {
                    continue;
                }
                int paren = trimmed.indexOf('(');
                int sp = trimmed.lastIndexOf(' ', paren);
                if (sp >= 0 && paren > sp) {
                    methods.add(trimmed.substring(sp + 1, paren));
                }
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            fail("javap failed (" + code + ") for " + className + " in " + jar);
        }
        return methods;
    }
}
