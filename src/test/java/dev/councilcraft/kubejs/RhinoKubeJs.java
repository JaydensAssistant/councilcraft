package dev.councilcraft.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.ast.AstNode;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PT44: runs CouncilCraft's KubeJS files through the pack's own Rhino jar (rhino-fabric 2001.2.3 build.10, the jar
 * KubeJS 2001.6.5 evaluates scripts with), so Rhino-only behavior such as a const declared in a loop body shows up here
 * exactly as in game. KubeJS globals are mocked by pack/tools/rhino/prelude.js.
 */
final class RhinoKubeJs {
    static final Path PACK = Path.of("pack");
    static final Path KUBEJS = PACK.resolve("kubejs");
    static final Path TOOLS = PACK.resolve("tools/rhino");

    private RhinoKubeJs() {}

    static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException(p.toString(), e);
        }
    }

    /** Loads prelude + script (optionally rewritten by {@code transform}) + test into one scope. Returns the failure messages. */
    static List<String> run(Path script, java.util.function.UnaryOperator<String> transform, Path test, String... preEval) {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();
            cx.evaluateString(scope, read(TOOLS.resolve("prelude.js")), "prelude.js", 1, null);
            for (String s : preEval) {
                cx.evaluateString(scope, s, "setup", 1, null);
            }
            cx.evaluateString(scope, transform.apply(read(script)), script.getFileName().toString(), 1, null);
            cx.evaluateString(scope, read(test), test.getFileName().toString(), 1, null);
            Object f = ScriptableObject.getProperty(scope, "failures", cx);
            Scriptable arr = (Scriptable) f;
            int n = ((Number) ScriptableObject.getProperty(arr, "length", cx)).intValue();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                out.add(String.valueOf(ScriptableObject.getProperty(arr, i, cx)));
            }
            return out;
        } catch (RuntimeException e) {
            return List.of("script error: " + e);
        }
    }

    static List<Path> kubejsFiles() throws IOException {
        try (Stream<Path> s = Files.walk(KUBEJS)) {
            return s.filter(p -> p.toString().endsWith(".js")).sorted().toList();
        }
    }

    /** Every AST node under root. This Rhino fork has no AstNode.visit, so walk the Node child links and the node fields. */
    static List<Object> walk(AstNode root) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Object> order = new ArrayList<>();
        Deque<Object> todo = new ArrayDeque<>();
        todo.add(root);
        while (!todo.isEmpty()) {
            Object n = todo.pop();
            if (!seen.add(n)) {
                continue;
            }
            order.add(n);
            if (n instanceof Node node) {
                for (Node c = node.getFirstChild(); c != null; c = c.getNext()) {
                    todo.push(c);
                }
            }
            for (Class<?> k = n.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) || f.getName().equals("parent")) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        Object v = f.get(n);
                        if (v instanceof AstNode) {
                            todo.push(v);
                        } else if (v instanceof Iterable<?> it) {
                            for (Object o : it) {
                                if (o instanceof AstNode) {
                                    todo.push(o);
                                }
                            }
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        // a field we cannot read holds no child we need
                    }
                }
            }
        }
        return order;
    }
}
