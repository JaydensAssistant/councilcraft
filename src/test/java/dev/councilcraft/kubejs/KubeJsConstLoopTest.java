package dev.councilcraft.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.ast.AstNode;
import dev.latvian.mods.rhino.ast.AstRoot;
import dev.latvian.mods.rhino.ast.Loop;
import dev.latvian.mods.rhino.ast.VariableDeclaration;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * PT44: Rhino binds a const declared inside a loop body only the first time that line runs, so every later pass keeps the
 * first value. These tests run the real scripts through the pack's Rhino jar and scan for the pattern.
 */
class KubeJsConstLoopTest {
    private static final Path WORLD = RhinoKubeJs.KUBEJS.resolve("server_scripts/councilcraft_world.js");
    private static final Path RECIPES = RhinoKubeJs.KUBEJS.resolve("server_scripts/councilcraft_recipes.js");
    private static final Path TOOLTIPS = RhinoKubeJs.KUBEJS.resolve("client_scripts/councilcraft_tooltips.js");
    private static final String TOOLTIP_CLASSES =
            "javaClasses['net.minecraft.core.registries.BuiltInRegistries'] = { ENCHANTMENT: { get: function (rl) {"
                    + " var m = { 'minecraft:sharpness': 5, 'minecraft:mending': 1, 'minecraft:unbreaking': 3 };"
                    + " return m[rl.id] === undefined ? null : { getMaxLevel: function () { return m[rl.id] } } } } };"
                    + "javaClasses['net.minecraft.resources.ResourceLocation'] = function (id) { this.id = String(id) };";

    @Test
    void ccSafeYFindsASafeSpotUnderTheRoof() {
        assertEquals(List.of(), RhinoKubeJs.run(WORLD, s -> s, RhinoKubeJs.TOOLS.resolve("world_ccsafey_test.js")));
    }

    @Test
    void ccSafeYTestFailsWithTheOldConst() {
        List<String> r = RhinoKubeJs.run(WORLD, s -> s.replace("let below =", "const below ="), RhinoKubeJs.TOOLS.resolve("world_ccsafey_test.js"));
        assertFalse(r.isEmpty(), "the regression test must catch `const below`");
    }

    @Test
    void woodLookKeyCoversAllElevenWoods() {
        assertEquals(List.of(), RhinoKubeJs.run(RECIPES, s -> s, RhinoKubeJs.TOOLS.resolve("recipes_woodlook_test.js")));
    }

    @Test
    void woodLookKeyTestFailsWithTheOldConst() {
        List<String> r = RhinoKubeJs.run(RECIPES, s -> s.replace("let v = NC_WOODS", "const v = NC_WOODS"), RhinoKubeJs.TOOLS.resolve("recipes_woodlook_test.js"));
        assertFalse(r.isEmpty(), "the regression test must catch `const v`");
    }

    @Test
    void tooltipChecksEveryStoredEnchantment() {
        assertEquals(List.of(), RhinoKubeJs.run(TOOLTIPS, s -> s, RhinoKubeJs.TOOLS.resolve("tooltips_test.js"), TOOLTIP_CLASSES));
    }

    @Test
    void tooltipTestFailsWithTheOldConst() {
        List<String> r = RhinoKubeJs.run(TOOLTIPS, s -> s.replace("let tag =", "const tag =").replace("let ench =", "const ench ="),
                RhinoKubeJs.TOOLS.resolve("tooltips_test.js"), TOOLTIP_CLASSES);
        assertFalse(r.isEmpty(), "the regression test must catch `const tag` / `const ench`");
    }

    /** The build scan: no const declaration inside a loop body anywhere under pack/kubejs (loop heads such as `for (const x of y)` are fine). */
    @Test
    void noConstInsideALoopBodyUnderPackKubejs() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : RhinoKubeJs.kubejsFiles()) {
            found.addAll(constInLoops(RhinoKubeJs.read(file), file.toString()));
        }
        assertEquals(List.of(), found, "const inside a loop body keeps its first value in Rhino: use let");
    }

    @Test
    void scanCatchesConstInLoopBodies() {
        String bad = "function f(a) {\n  for (let i = 0; i < a.length; i++) {\n    const v = a[i]\n  }\n"
                + "  while (a.length) {\n    if (a) { const w = 1 }\n  }\n  for (const k of a) { let ok = k }\n"
                + "  a.forEach(x => { const inFn = x })\n  for (const j in a) { const bad2 = j }\n}\n";
        List<String> found = constInLoops(bad, "x.js");
        assertEquals(3, found.size(), found.toString());
        for (String line : List.of("x.js:3 ", "x.js:6 ", "x.js:10 ")) {
            assertTrue(found.stream().anyMatch(f -> f.startsWith(line)), found.toString());
        }
    }

    static List<String> constInLoops(String src, String name) {
        Context cx = Context.enter();
        CompilerEnvirons env = new CompilerEnvirons();
        env.initFromContext(cx);
        AstRoot root = new Parser(cx, env).parse(src, name, 1);
        List<String> out = new ArrayList<>();
        for (Object node : walk(root)) {
            if (node instanceof VariableDeclaration d && d.isConst() && inLoopBody(d)) {
                out.add(name + ":" + d.getLineno() + " const inside a loop body");
            }
        }
        out.sort(null);
        return out;
    }

    /** Every AST node under root. This Rhino fork has no AstNode.visit, so walk the Node child links and the node fields. */
    private static List<Object> walk(AstNode root) {
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

    /** True when the declaration sits inside a loop's body (not its head) with no function boundary in between. */
    private static boolean inLoopBody(VariableDeclaration d) {
        AstNode child = d;
        for (AstNode p = d.getParent(); p != null; child = p, p = p.getParent()) {
            if (p instanceof dev.latvian.mods.rhino.ast.FunctionNode) {
                return false;
            }
            if (p instanceof Loop loop && child == loop.getBody()) {
                return true;
            }
        }
        return false;
    }
}
